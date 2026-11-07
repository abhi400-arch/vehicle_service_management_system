package com.vsms;

import javax.swing.*;
import java.awt.Component;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.List;

final class PaymentsPanel extends BasePanel {
    private static final List<String> METHODS = List.of("", "Cash", "UPI", "Card", "Bank Transfer");
    private static final List<String> STATUSES = List.of("Paid", "Partial", "Pending", "Failed", "Refunded");
    private final JComboBox<String> status = new JComboBox<>(new String[]{"All", "Paid", "Partial", "Pending", "Failed", "Refunded"});
    private final JCheckBox noMethod = new JCheckBox("Method not recorded (IS NULL)");

    PaymentsPanel() {
        super("Payments", "Record payments through the RecordPayment procedure and track collections with SUM()");
        action("Record Payment", Ui.ACCENT, () -> { if (recordPayment(this, null)) refresh(); });
        action("Edit Payment", Ui.SLATE, this::edit);
        action("Delete", Ui.RED, this::delete);
        filters.add(Ui.label("Status", 12, false, Ui.MUTED));
        filters.add(status);
        noMethod.setOpaque(false);
        filters.add(noMethod);
        status.addActionListener(e -> refresh());
        noMethod.addActionListener(e -> refresh());
    }

    @Override protected Result fetch() throws Exception {
        StringBuilder w = new StringBuilder(" WHERE 1=1");
        List<Object> p = new ArrayList<>();
        if (!"All".equals(status.getSelectedItem())) { w.append(" AND p.payment_status = ?"); p.add(status.getSelectedItem()); }
        if (noMethod.isSelected()) w.append(" AND p.payment_method IS NULL");
        Object total = Db.query("SELECT COALESCE(SUM(amount),0) FROM PAYMENT WHERE payment_status IN ('Paid','Partial')").first();
        summary.setText("Total payments received (Paid + Partial): " + Ui.money(total));
        return Db.query("SELECT p.payment_id AS `Payment ID`, p.service_id AS `Service`, v.registration_no AS `Vehicle`, "
                + "c.customer_name AS `Customer`, p.payment_date AS `Date`, p.amount AS `Amount`, "
                + "p.payment_method AS `Method`, p.payment_status AS `Status` "
                + "FROM PAYMENT p JOIN SERVICE s ON s.service_id = p.service_id "
                + "JOIN VEHICLE v ON v.vehicle_id = s.vehicle_id JOIN CUSTOMER c ON c.customer_id = v.customer_id"
                + w + " ORDER BY p.payment_date DESC, p.payment_id DESC", p.toArray());
    }

    /** Shared by the Payments and Services screens. Calls the RecordPayment stored procedure. */
    static boolean recordPayment(Component owner, Long preset) {
        try {
            Result r = Db.query("SELECT t.service_id, t.registration_no, t.total_cost, t.paid FROM ("
                    + "SELECT s.service_id, v.registration_no, s.service_status, " + Sql.TOTAL + " AS total_cost, "
                    + Sql.PAID + " AS paid FROM SERVICE s JOIN VEHICLE v ON v.vehicle_id = s.vehicle_id) t "
                    + "WHERE t.service_status <> 'Cancelled' ORDER BY t.service_id DESC");
            if (r.isEmpty()) { Ui.info(owner, "There are no services to take a payment for."); return false; }
            List<Ui.Item> items = new ArrayList<>();
            Map<Long, BigDecimal> balance = new HashMap<>();
            for (Object[] row : r.rows()) {
                long id = ((Number) row[0]).longValue();
                BigDecimal bal = new BigDecimal(row[2].toString()).subtract(new BigDecimal(row[3].toString())).max(BigDecimal.ZERO);
                balance.put(id, bal);
                items.add(new Ui.Item(id, "#" + id + "   " + row[1] + "   |   balance " + Ui.money(bal)));
            }
            Form f = new Form(owner, "Record Payment", "Record");
            JComboBox<Object> svc = f.combo("Service", items, Ui.find(items, preset), false);
            JTextField amount = f.text("Amount", "");
            f.text("Payment date", LocalDate.now().toString());
            f.combo("Method", METHODS, "", false);
            f.combo("Status", STATUSES, "Paid", false);
            Runnable fill = () -> {
                if (svc.getSelectedItem() instanceof Ui.Item it) amount.setText(balance.get(it.id()).toPlainString());
            };
            svc.addActionListener(e -> fill.run());
            fill.run();
            return f.run(() -> {
                Ui.Item it = f.item("Service");
                BigDecimal amt = Ui.parseMoney("Amount", f.str("Amount"));
                java.sql.Date d = Ui.parseDate("Payment date", f.str("Payment date"));
                String method = Ui.nn(f.str("Method"));
                Result res = Db.call("{CALL RecordPayment(?,?,?,?,?)}", it.id(), d, amt, method, f.str("Status"));
                Ui.info(f.owner(), res.isEmpty() ? "Payment recorded." : "Payment #" + res.rows().get(0)[0] + " recorded successfully.");
            });
        } catch (Exception e) {
            Ui.error(owner, Ui.friendly(e));
            return false;
        }
    }

    private void edit() {
        if (!need()) return;
        long id = id();
        Form f = new Form(this, "Edit Payment #" + id, "Update");
        f.text("Amount", ((BigDecimal) cell("Amount")).toPlainString());
        f.text("Payment date", Ui.s(cell("Date")));
        f.combo("Method", METHODS, Ui.s(cell("Method")), false);
        f.combo("Status", STATUSES, Ui.s(cell("Status")), false);
        if (f.run(() -> Db.update("UPDATE PAYMENT SET amount=?, payment_date=?, payment_method=?, payment_status=? WHERE payment_id=?",
                Ui.parseMoney("Amount", f.str("Amount")), Ui.parseDate("Payment date", f.str("Payment date")),
                Ui.nn(f.str("Method")), f.str("Status"), id))) refresh();
    }

    private void delete() {
        if (!need()) return;
        long id = id();
        if (!Ui.confirm(this, "Delete payment #" + id + "?")) return;
        if (Ui.run(this, () -> Db.update("DELETE FROM PAYMENT WHERE payment_id=?", id))) refresh();
    }
}
