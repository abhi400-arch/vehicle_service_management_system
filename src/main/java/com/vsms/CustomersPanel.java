package com.vsms;

import javax.swing.*;

final class CustomersPanel extends BasePanel {
    private final JCheckBox missing = new JCheckBox("Only customers with missing phone / e-mail (IS NULL)");

    CustomersPanel() {
        super("Customers", "Register customers and see how many vehicles each one owns");
        action("Add Customer", Ui.ACCENT, this::add);
        action("Edit", Ui.SLATE, this::edit);
        action("Delete", Ui.RED, this::delete);
        missing.setOpaque(false);
        missing.addActionListener(e -> refresh());
        filters.add(missing);
    }

    @Override protected Result fetch() throws Exception {
        String where = missing.isSelected() ? " WHERE c.phone IS NULL OR c.email IS NULL" : "";
        return Db.query("SELECT c.customer_id AS `ID`, c.customer_name AS `Customer`, c.phone AS `Phone`, "
                + "c.email AS `Email`, COUNT(v.vehicle_id) AS `Vehicles` "
                + "FROM CUSTOMER c LEFT JOIN VEHICLE v ON v.customer_id = c.customer_id" + where
                + " GROUP BY c.customer_id, c.customer_name, c.phone, c.email ORDER BY c.customer_id");
    }

    private void add() {
        Form f = new Form(this, "Add Customer", "Save");
        f.text("Name", ""); f.text("Phone", ""); f.text("Email", "");
        if (f.run(() -> save(f, null))) refresh();
    }

    private void edit() {
        if (!need()) return;
        long id = id();
        Form f = new Form(this, "Edit Customer #" + id, "Update");
        f.text("Name", Ui.s(cell("Customer"))); f.text("Phone", Ui.s(cell("Phone"))); f.text("Email", Ui.s(cell("Email")));
        if (f.run(() -> save(f, id))) refresh();
    }

    private void save(Form f, Long id) throws Exception {
        String name = f.str("Name");
        if (name.isEmpty()) throw new IllegalArgumentException("Customer name is required.");
        String phone = Ui.nn(f.str("Phone")), email = Ui.nn(f.str("Email"));
        Ui.checkPhone(phone);
        Ui.checkEmail(email);
        if (id == null) {
            Db.syncAutoIncrement("CUSTOMER", "customer_id");
            Db.update("INSERT INTO CUSTOMER (customer_name, phone, email) VALUES (?,?,?)", name, phone, email);
        } else {
            Db.update("UPDATE CUSTOMER SET customer_name=?, phone=?, email=? WHERE customer_id=?", name, phone, email, id);
        }
    }

    private void delete() {
        if (!need()) return;
        long id = id();
        if (!Ui.confirm(this, "Delete customer #" + id + "?\n(Only possible when the customer owns no vehicles.)")) return;
        if (Ui.run(this, () -> {
            Db.update("DELETE FROM CUSTOMER WHERE customer_id=?", id);
            Db.syncAutoIncrement("CUSTOMER", "customer_id");
        })) refresh();
    }
}
