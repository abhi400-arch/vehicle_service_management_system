package com.vsms;

import javax.swing.*;
import java.time.Year;
import java.util.List;

final class VehiclesPanel extends BasePanel {
    private static final String[] STATUSES = {"Available", "In Service", "Completed", "Inactive"};
    private final JComboBox<String> status = new JComboBox<>(new String[]{"All", "Available", "In Service", "Completed", "Inactive"});

    VehiclesPanel() {
        super("Vehicles", "Register vehicles, track their status and service count (uses GetVehicleServiceCount)");
        action("Register Customer + Vehicle", Ui.GREEN, this::registerBoth);
        action("Add Vehicle", Ui.ACCENT, this::add);
        action("Edit", Ui.SLATE, this::edit);
        action("Delete", Ui.RED, this::delete);
        filters.add(Ui.label("Status", 12, false, Ui.MUTED));
        filters.add(status);
        status.addActionListener(e -> refresh());
    }

    @Override protected Result fetch() throws Exception {
        boolean all = "All".equals(status.getSelectedItem());
        return Db.query("SELECT v.vehicle_id AS `ID`, v.registration_no AS `Registration`, v.model AS `Model`, "
                + "v.vehicle_year AS `Year`, c.customer_name AS `Owner`, v.vehicle_status AS `Status`, "
                + "GetVehicleServiceCount(v.vehicle_id) AS `Services` "
                + "FROM VEHICLE v JOIN CUSTOMER c ON c.customer_id = v.customer_id"
                + (all ? "" : " WHERE v.vehicle_status = ?") + " ORDER BY v.vehicle_id",
                all ? new Object[0] : new Object[]{status.getSelectedItem()});
    }

    private void add() {
        Ui.run(this, () -> {
            List<Ui.Item> owners = Ui.items("SELECT customer_id, customer_name FROM CUSTOMER ORDER BY customer_name");
            Form f = new Form(this, "Add Vehicle", "Save");
            f.combo("Owner", owners, null, false);
            f.text("Registration no", ""); f.text("Model", ""); f.text("Year", "");
            f.combo("Status", List.of(STATUSES), "Available", false);
            if (f.run(() -> save(f, null))) refresh();
        });
    }

    private void edit() {
        if (!need()) return;
        long id = id();
        Ui.run(this, () -> {
            List<Ui.Item> owners = Ui.items("SELECT customer_id, customer_name FROM CUSTOMER ORDER BY customer_name");
            Object ownerId = Db.query("SELECT customer_id FROM VEHICLE WHERE vehicle_id=?", id).first();
            Form f = new Form(this, "Edit Vehicle #" + id, "Update");
            f.combo("Owner", owners, Ui.find(owners, ownerId), false);
            f.text("Registration no", Ui.s(cell("Registration"))); f.text("Model", Ui.s(cell("Model")));
            f.text("Year", Ui.s(cell("Year")));
            f.combo("Status", List.of(STATUSES), Ui.s(cell("Status")), false);
            if (f.run(() -> save(f, id))) refresh();
        });
    }

    private void save(Form f, Long id) throws Exception {
        Ui.Item owner = f.item("Owner");
        String reg = f.str("Registration no").toUpperCase().replace(" ", "");
        if (reg.length() < 4) throw new IllegalArgumentException("Enter a valid registration number.");
        String model = f.str("Model");
        if (model.isEmpty()) throw new IllegalArgumentException("Model is required.");
        String y = f.str("Year");
        Integer year = y.isEmpty() ? null : Ui.parseInt("Year", y, 1980, Year.now().getValue() + 1);
        String st = f.str("Status");
        if (id == null) {
            Db.syncAutoIncrement("VEHICLE", "vehicle_id");
            Db.update("INSERT INTO VEHICLE (customer_id, registration_no, model, vehicle_year, vehicle_status) VALUES (?,?,?,?,?)",
                    owner.id(), reg, model, year, st);
        } else {
            Db.update("UPDATE VEHICLE SET customer_id=?, registration_no=?, model=?, vehicle_year=?, vehicle_status=? WHERE vehicle_id=?",
                    owner.id(), reg, model, year, st, id);
        }
    }

    /** Module "Register customer and vehicle": customer first, then vehicle with customer_id as FK, in one transaction. */
    private void registerBoth() {
        Form f = new Form(this, "Register Customer & Vehicle", "Register");
        f.text("Customer name", ""); f.text("Phone", ""); f.text("Email", "");
        f.text("Registration no", ""); f.text("Model", ""); f.text("Year", "");
        boolean done = f.run(() -> {
            String name = f.str("Customer name");
            if (name.isEmpty()) throw new IllegalArgumentException("Customer name is required.");
            String phone = Ui.nn(f.str("Phone")), email = Ui.nn(f.str("Email"));
            Ui.checkPhone(phone);
            Ui.checkEmail(email);
            String reg = f.str("Registration no").toUpperCase().replace(" ", "");
            if (reg.length() < 4) throw new IllegalArgumentException("Enter a valid registration number.");
            String model = f.str("Model");
            if (model.isEmpty()) throw new IllegalArgumentException("Model is required.");
            String y = f.str("Year");
            Integer year = y.isEmpty() ? null : Ui.parseInt("Year", y, 1980, Year.now().getValue() + 1);
            Db.syncAutoIncrement("CUSTOMER", "customer_id");
            Db.syncAutoIncrement("VEHICLE", "vehicle_id");
            Db.transaction(c -> {
                long cid = Db.insert(c, "INSERT INTO CUSTOMER (customer_name, phone, email) VALUES (?,?,?)", name, phone, email);
                Db.exec(c, "INSERT INTO VEHICLE (customer_id, registration_no, model, vehicle_year) VALUES (?,?,?,?)",
                        cid, reg, model, year);
            });
        });
        if (done) refresh();
    }

    private void delete() {
        if (!need()) return;
        long id = id();
        if (!Ui.confirm(this, "Delete vehicle #" + id + "?\n(Only possible when it has no service records.)")) return;
        if (Ui.run(this, () -> {
            Db.update("DELETE FROM VEHICLE WHERE vehicle_id=?", id);
            Db.syncAutoIncrement("VEHICLE", "vehicle_id");
        })) refresh();
    }
}
