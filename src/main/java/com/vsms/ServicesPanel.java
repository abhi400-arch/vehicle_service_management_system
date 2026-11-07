package com.vsms;

import javax.swing.*;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.*;
import java.util.List;

final class ServicesPanel extends BasePanel {
    private static final List<String> TYPES = List.of("General Service", "Engine Service", "Brake Service",
            "Electrical Check", "Transmission Check", "AC Service", "Routine Inspection", "Oil Change", "Tyre Rotation");
    private final JComboBox<String> status = new JComboBox<>(new String[]{"All", "Open", "In Progress", "Completed", "Cancelled"});
    private final JTextField from = Ui.field(9), to = Ui.field(9);

    ServicesPanel() {
        super("Service Records", "Check vehicles in, add parts, complete or cancel jobs and take payments");
        action("New Service", Ui.ACCENT, this::checkIn);
        action("Add Part", Ui.SLATE, this::addPart);
        action("Parts Used", Ui.SLATE, this::partsUsed);
        action("Complete", Ui.GREEN, this::complete);
        action("Cancel Service", Ui.RED, this::cancel);
        action("Record Payment", Ui.AMBER, this::pay);
        filters.add(Ui.label("Status", 12, false, Ui.MUTED));
        filters.add(status);
        filters.add(Ui.label("From", 12, false, Ui.MUTED));
        filters.add(from);
        filters.add(Ui.label("To", 12, false, Ui.MUTED));
        filters.add(to);
        JButton apply = Ui.button("Apply", Ui.SLATE);
        apply.addActionListener(e -> refresh());
        JButton clear = Ui.button("Clear", Ui.SLATE);
        clear.addActionListener(e -> { from.setText(""); to.setText(""); status.setSelectedItem("All"); refresh(); });
        filters.add(apply);
        filters.add(clear);
        from.setToolTipText("yyyy-MM-dd");
        to.setToolTipText("yyyy-MM-dd");
        status.addActionListener(e -> refresh());
    }

    @Override protected Result fetch() throws Exception {
        StringBuilder w = new StringBuilder(" WHERE 1=1");
        List<Object> p = new ArrayList<>();
        if (!"All".equals(status.getSelectedItem())) { w.append(" AND s.service_status = ?"); p.add(status.getSelectedItem()); }
        if (!from.getText().isBlank()) { w.append(" AND s.service_date >= ?"); p.add(Ui.parseDate("From", from.getText())); }
        if (!to.getText().isBlank()) { w.append(" AND s.service_date <= ?"); p.add(Ui.parseDate("To", to.getText())); }
        return Db.query("SELECT s.service_id AS `ID`, v.registration_no AS `Vehicle`, v.model AS `Model`, "
                + "c.customer_name AS `Customer`, m.mechanic_name AS `Mechanic`, s.service_date AS `Date`, "
                + "s.service_type AS `Type`, s.odometer_km AS `Odometer`, s.labor_cost AS `Labor Cost`, "
                + "COALESCE((SELECT SUM(sp.part_cost) FROM SERVICE_PART sp WHERE sp.service_id = s.service_id), 0) AS `Parts Cost`, "
                + Sql.TOTAL + " AS `Total`, " + Sql.PAID + " AS `Paid`, s.service_status AS `Status` "
                + "FROM SERVICE s JOIN VEHICLE v ON v.vehicle_id = s.vehicle_id "
                + "JOIN CUSTOMER c ON c.customer_id = v.customer_id JOIN MECHANIC m ON m.mechanic_id = s.mechanic_id"
                + w + " ORDER BY s.service_id DESC", p.toArray());
    }

    private boolean isActive() {
        String st = Ui.s(cell("Status"));
        return st.equals("Open") || st.equals("In Progress");
    }

    /** Phase 4: check-in = AddServiceRecord procedure (transaction + SAVEPOINT + vehicle lock). */
    private void checkIn() {
        Ui.run(this, () -> {
            List<Ui.Item> vehicles = Ui.items("SELECT vehicle_id, CONCAT(registration_no, '  -  ', model) FROM VEHICLE "
                    + "WHERE vehicle_status = 'Available' ORDER BY registration_no");
            if (vehicles.isEmpty()) {
                Ui.info(this, "No vehicle is currently 'Available'. Complete an active service or change a vehicle's status first.");
                return;
            }
            List<Ui.Item> mechanics = Ui.items("SELECT mechanic_id, CONCAT(mechanic_name, ' (', COALESCE(specialization,'General'), ')') "
                    + "FROM MECHANIC ORDER BY mechanic_name");
            Form f = new Form(this, "New Service (Vehicle Check-in)", "Create Service");
            f.combo("Vehicle", vehicles, null, false);
            f.combo("Mechanic", mechanics, null, false);
            f.combo("Service type", TYPES, "General Service", true);
            f.text("Labor cost", "0.00");
            if (f.run(() -> {
                String type = f.str("Service type");
                if (type.isEmpty()) throw new IllegalArgumentException("Service type is required.");
                Result r = Db.call("{CALL AddServiceRecord(?,?,?,?)}", f.item("Vehicle").id(), f.item("Mechanic").id(),
                        type, Ui.parseMoney("Labor cost", f.str("Labor cost")));
                Ui.info(f.owner(), r.isEmpty() ? "Service created." :
                        "Service #" + r.rows().get(0)[0] + " created and the vehicle is now In Service.");
            })) refresh();
        });
    }

    /** Calls AddServicePart: checks stock under a row lock, stores the part and reduces stock. */
    private void addPart() {
        if (!need()) return;
        if (!isActive()) { Ui.info(this, "Parts can only be added to an Open or In Progress service."); return; }
        long sid = id();
        Ui.run(this, () -> {
            List<Ui.Item> parts = Ui.items("SELECT part_id, CONCAT(part_name, '   |   ', FORMAT(unit_price,2), ' each   |   stock ', stock_qty) "
                    + "FROM PART ORDER BY part_name");
            Form f = new Form(this, "Add Part to Service #" + sid, "Add Part");
            f.combo("Part", parts, null, false);
            f.text("Quantity", "1");
            if (f.run(() -> {
                int q = Ui.parseInt("Quantity", f.str("Quantity"), 1, 100000);
                Result r = Db.call("{CALL AddServicePart(?,?,?)}", sid, f.item("Part").id(), q);
                Ui.info(f.owner(), r.isEmpty() ? "Part added." :
                        "Part recorded. Cost " + Ui.money(r.rows().get(0)[3]) + " - stock updated.");
            })) refresh();
        });
    }

    private void partsUsed() {
        if (!need()) return;
        long sid = id();
        Ui.run(this, () -> {
            Result r = Db.query("SELECT p.part_name AS `Part`, sp.quantity AS `Qty`, "
                    + "ROUND(sp.part_cost / sp.quantity, 2) AS `Unit Cost`, sp.part_cost AS `Part Cost` "
                    + "FROM SERVICE_PART sp JOIN PART p ON p.part_id = sp.part_id WHERE sp.service_id = ? ORDER BY p.part_name", sid);
            if (r.isEmpty()) Ui.info(this, "No parts were used in service #" + sid + ".");
            else Ui.showTable(this, "Parts used in service #" + sid, r);
        });
    }

    /** Completes a service: service -> Completed and vehicle -> Available, in one transaction. */
    private void complete() {
        if (!need()) return;
        if (!isActive()) { Ui.info(this, "Only Open or In Progress services can be completed."); return; }
        long sid = id();
        Form f = new Form(this, "Complete Service #" + sid, "Complete");
        f.text("Odometer (km)", Ui.s(cell("Odometer")).equals("0") ? "" : Ui.s(cell("Odometer")));
        f.text("Final labor cost", ((BigDecimal) cell("Labor Cost")).toPlainString());
        if (f.run(() -> {
            int odo = Ui.parseInt("Odometer", f.str("Odometer (km)"), 0, 5_000_000);
            BigDecimal labor = Ui.parseMoney("Labor cost", f.str("Final labor cost"));
            Db.transaction(c -> {
                long prev = ((Number) Db.query(c, "SELECT COALESCE(MAX(odometer_km),0) FROM SERVICE WHERE vehicle_id = "
                        + "(SELECT vehicle_id FROM SERVICE WHERE service_id = ?) AND service_id <> ?", sid, sid).first()).longValue();
                if (odo < prev) throw new IllegalArgumentException("Odometer cannot be lower than the previous reading (" + prev + " km).");
                int n = Db.exec(c, "UPDATE SERVICE SET service_status='Completed', odometer_km=?, labor_cost=? "
                        + "WHERE service_id=? AND service_status IN ('Open','In Progress')", odo, labor, sid);
                if (n != 1) throw new SQLException("This service is no longer active.");
                Db.exec(c, "UPDATE VEHICLE SET vehicle_status='Available' WHERE vehicle_id = "
                        + "(SELECT vehicle_id FROM SERVICE WHERE service_id = ?)", sid);
            });
        })) refresh();
    }

    /** Cancels a service: returns parts to stock, removes usage rows, frees the vehicle. One transaction. */
    private void cancel() {
        if (!need()) return;
        if (!isActive()) { Ui.info(this, "Only Open or In Progress services can be cancelled."); return; }
        long sid = id();
        if (!Ui.confirm(this, "Cancel service #" + sid + "?\nParts used will be returned to stock and the vehicle becomes Available.")) return;
        if (Ui.run(this, () -> Db.transaction(c -> {
            int n = Db.exec(c, "UPDATE SERVICE SET service_status='Cancelled' WHERE service_id=? AND service_status IN ('Open','In Progress')", sid);
            if (n != 1) throw new SQLException("This service is no longer active.");
            for (Object[] row : Db.query(c, "SELECT part_id, quantity FROM SERVICE_PART WHERE service_id=?", sid).rows())
                Db.exec(c, "UPDATE PART SET stock_qty = stock_qty + ? WHERE part_id = ?", row[1], row[0]);
            Db.exec(c, "DELETE FROM SERVICE_PART WHERE service_id=?", sid);
            Db.exec(c, "UPDATE VEHICLE SET vehicle_status='Available' WHERE vehicle_id = "
                    + "(SELECT vehicle_id FROM SERVICE WHERE service_id = ?)", sid);
        }))) refresh();
    }

    private void pay() {
        if (!need()) return;
        if (PaymentsPanel.recordPayment(this, id())) refresh();
    }
}
