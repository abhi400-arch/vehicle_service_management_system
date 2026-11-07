package com.vsms;

import javax.swing.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** Every analytical query from the proposal, plus the GetVehicleServiceCount function tester. */
final class ReportsPanel extends BasePanel {
    private record Rep(String name, String description, String sql) {
        @Override public String toString() { return name; }
    }

    private static Rep r(String name, String desc, String sql) {
        return new Rep(name, desc, sql.replace("@T", Sql.TOTAL).replace("@P", Sql.PAID));
    }

    private static final Rep[] REPORTS = {
        r("Vehicle_Service_Summary (View)",
          "Reusable view when installed; automatically falls back to the same JOIN query if the view is missing.",
          "SELECT registration_no AS `Registration`, customer AS `Customer`, service_type AS `Service`, mechanic AS `Mechanic`, "
        + "service_date AS `Date`, status AS `Status`, total_service_cost AS `Total Cost` FROM Vehicle_Service_Summary ORDER BY service_date DESC"),
        r("Customer + Vehicle ownership (JOIN)", "CUSTOMER joined with VEHICLE.",
          "SELECT c.customer_id AS `Customer ID`, c.customer_name AS `Customer`, c.phone AS `Phone`, c.email AS `Email`, "
        + "v.registration_no AS `Registration`, v.model AS `Model`, v.vehicle_status AS `Status` "
        + "FROM CUSTOMER c JOIN VEHICLE v ON c.customer_id = v.customer_id ORDER BY c.customer_name, v.registration_no"),
        r("Service report: Vehicle + Service + Mechanic (JOIN)", "VEHICLE, SERVICE and MECHANIC joined.",
          "SELECT v.registration_no AS `Registration`, v.model AS `Model`, s.service_id AS `Service`, s.service_date AS `Date`, "
        + "s.service_type AS `Type`, m.mechanic_name AS `Mechanic`, m.specialization AS `Specialization`, "
        + "s.labor_cost AS `Labor Cost`, s.service_status AS `Status` "
        + "FROM VEHICLE v JOIN SERVICE s ON v.vehicle_id = s.vehicle_id JOIN MECHANIC m ON s.mechanic_id = m.mechanic_id ORDER BY s.service_date DESC"),
        r("Parts used per service (JOIN)", "SERVICE, SERVICE_PART and PART joined.",
          "SELECT s.service_id AS `Service`, v.registration_no AS `Vehicle`, s.service_date AS `Date`, p.part_name AS `Part`, "
        + "sp.quantity AS `Qty`, sp.part_cost AS `Part Cost`, ROUND(sp.part_cost / sp.quantity, 2) AS `Unit Cost` "
        + "FROM SERVICE s JOIN VEHICLE v ON s.vehicle_id = v.vehicle_id JOIN SERVICE_PART sp ON s.service_id = sp.service_id "
        + "JOIN PART p ON sp.part_id = p.part_id ORDER BY s.service_id, p.part_name"),
        r("Services per vehicle (COUNT)", "COUNT() per vehicle, cross-checked with the GetVehicleServiceCount function.",
          "SELECT v.vehicle_id AS `ID`, v.registration_no AS `Registration`, COUNT(s.service_id) AS `Services (COUNT)`, "
        + "GetVehicleServiceCount(v.vehicle_id) AS `Services (Function)` FROM VEHICLE v LEFT JOIN SERVICE s ON v.vehicle_id = s.vehicle_id "
        + "GROUP BY v.vehicle_id, v.registration_no ORDER BY `Services (COUNT)` DESC"),
        r("Average service cost (AVG)", "AVG() of labour cost and of total cost (labour + parts).",
          "SELECT COUNT(*) AS `Services`, ROUND(AVG(t.labor_cost), 2) AS `Average Labor`, ROUND(AVG(t.total), 2) AS `Average Total Cost` "
        + "FROM (SELECT s.labor_cost, @T AS total FROM SERVICE s) t"),
        r("Total expenditure and payments (SUM)", "SUM() of all service costs and of payments received.",
          "SELECT ROUND(SUM(@T), 2) AS `Total Service Expenditure`, "
        + "(SELECT ROUND(SUM(amount), 2) FROM PAYMENT WHERE payment_status IN ('Paid','Partial')) AS `Total Payments Received` FROM SERVICE s"),
        r("Vehicles above average service count (Subquery)", "Nested query comparing each vehicle's count with the average count.",
          "SELECT v.vehicle_id AS `ID`, v.registration_no AS `Registration`, v.model AS `Model`, COUNT(s.service_id) AS `Services` "
        + "FROM VEHICLE v JOIN SERVICE s ON v.vehicle_id = s.vehicle_id GROUP BY v.vehicle_id, v.registration_no, v.model "
        + "HAVING COUNT(s.service_id) > (SELECT AVG(c) FROM (SELECT COUNT(*) AS c FROM SERVICE GROUP BY vehicle_id) x) ORDER BY `Services` DESC"),
        r("Services above average total cost (Subquery)", "Services whose labour + parts cost is above the overall average.",
          "SELECT s.service_id AS `Service`, v.registration_no AS `Vehicle`, s.service_date AS `Date`, s.service_type AS `Type`, "
        + "ROUND(@T, 2) AS `Total Cost` FROM SERVICE s JOIN VEHICLE v ON s.vehicle_id = v.vehicle_id "
        + "WHERE @T > (SELECT AVG(t.total) FROM (SELECT @T AS total FROM SERVICE s) t) ORDER BY `Total Cost` DESC"),
        r("Outstanding balances", "Total cost minus payments received, per service.",
          "SELECT service_id AS `Service`, registration_no AS `Vehicle`, customer_name AS `Customer`, service_status AS `Status`, "
        + "total_cost AS `Total Cost`, paid AS `Paid`, ROUND(total_cost - paid, 2) AS `Balance` FROM ("
        + "SELECT s.service_id, v.registration_no, c.customer_name, s.service_status, @T AS total_cost, @P AS paid "
        + "FROM SERVICE s JOIN VEHICLE v ON v.vehicle_id = s.vehicle_id JOIN CUSTOMER c ON c.customer_id = v.customer_id) t "
        + "WHERE service_status <> 'Cancelled' ORDER BY `Balance` DESC"),
        r("Mechanics by specialization", "Mechanics grouped by specialization.",
          "SELECT COALESCE(specialization, '(none)') AS `Specialization`, COUNT(*) AS `Mechanics`, "
        + "GROUP_CONCAT(mechanic_name ORDER BY mechanic_name SEPARATOR ', ') AS `Names` FROM MECHANIC GROUP BY specialization ORDER BY specialization"),
        r("Low-stock parts (< 10)", "Parts that need restocking.",
          "SELECT part_id AS `ID`, part_name AS `Part`, unit_price AS `Unit Price`, stock_qty AS `Stock` FROM PART WHERE stock_qty < 10 ORDER BY stock_qty ASC"),
        r("NULL handling: customers with missing contact", "IS NULL / IS NOT NULL on optional customer fields.",
          "SELECT customer_id AS `ID`, customer_name AS `Customer`, phone AS `Phone`, email AS `Email`, "
        + "CASE WHEN phone IS NULL THEN 'Missing' ELSE 'Present' END AS `Phone Status`, "
        + "CASE WHEN email IS NULL THEN 'Missing' ELSE 'Present' END AS `Email Status` FROM CUSTOMER "
        + "ORDER BY (phone IS NULL) + (email IS NULL) DESC, customer_id"),
        r("NULL handling: payment method", "Counts payments with and without a recorded method.",
          "SELECT COUNT(*) AS `Payments`, SUM(payment_method IS NOT NULL) AS `Method Recorded (IS NOT NULL)`, "
        + "SUM(payment_method IS NULL) AS `Method Missing (IS NULL)` FROM PAYMENT"),
        r("EXPLAIN: Vehicle + Service + Mechanic join", "Query plan of the vehicle-service-mechanic join (uses the indexes).",
          "EXPLAIN SELECT v.registration_no, s.service_date, s.service_type, m.mechanic_name FROM VEHICLE v "
        + "JOIN SERVICE s ON v.vehicle_id = s.vehicle_id JOIN MECHANIC m ON s.mechanic_id = m.mechanic_id WHERE v.vehicle_id = 1"),
        r("Indexes on SERVICE and SERVICE_PART", "Indexes created for frequently searched columns.",
          "SELECT TABLE_NAME AS `Table`, INDEX_NAME AS `Index`, COLUMN_NAME AS `Column`, "
        + "IF(NON_UNIQUE = 0, 'Unique', 'Non-unique') AS `Type` FROM information_schema.STATISTICS "
        + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME IN ('SERVICE','SERVICE_PART') ORDER BY TABLE_NAME, INDEX_NAME, SEQ_IN_INDEX"),
        r("Final validation: integrity checks", "All Phase 4 validation checks in one table (PASS = no problems found).",
          "SELECT check_name AS `Check`, issues AS `Issues`, IF(issues = 0, 'PASS', 'FAIL') AS `Result` FROM ("
        + "SELECT 'Services referencing a missing vehicle' AS check_name, COUNT(*) AS issues FROM SERVICE s LEFT JOIN VEHICLE v ON v.vehicle_id = s.vehicle_id WHERE v.vehicle_id IS NULL "
        + "UNION ALL SELECT 'Services referencing a missing mechanic', COUNT(*) FROM SERVICE s LEFT JOIN MECHANIC m ON m.mechanic_id = s.mechanic_id WHERE m.mechanic_id IS NULL "
        + "UNION ALL SELECT 'Service-part rows with missing service or part', COUNT(*) FROM SERVICE_PART sp LEFT JOIN SERVICE s ON s.service_id = sp.service_id LEFT JOIN PART p ON p.part_id = sp.part_id WHERE s.service_id IS NULL OR p.part_id IS NULL "
        + "UNION ALL SELECT 'Payments referencing a missing service', COUNT(*) FROM PAYMENT p LEFT JOIN SERVICE s ON s.service_id = p.service_id WHERE s.service_id IS NULL "
        + "UNION ALL SELECT 'Parts with negative stock', COUNT(*) FROM PART WHERE stock_qty < 0 "
        + "UNION ALL SELECT 'Active services whose vehicle is not In Service', COUNT(*) FROM SERVICE s JOIN VEHICLE v ON v.vehicle_id = s.vehicle_id WHERE s.service_status IN ('Open','In Progress') AND v.vehicle_status <> 'In Service' "
        + "UNION ALL SELECT 'Vehicles In Service without an active service', COUNT(*) FROM VEHICLE v WHERE v.vehicle_status = 'In Service' "
        + "AND NOT EXISTS (SELECT 1 FROM SERVICE s WHERE s.vehicle_id = v.vehicle_id AND s.service_status IN ('Open','In Progress'))) checks")
    };

    private final JComboBox<Rep> reports = new JComboBox<>(REPORTS);
    private final JComboBox<Ui.Item> vehicles = new JComboBox<>();
    private final JLabel fnResult = Ui.label("", 13, true, Ui.ACCENT);

    ReportsPanel() {
        super("Reports & Analysis", "Joins, aggregates, subqueries, the view, query plan and validation checks");
        action("Export CSV", Ui.GREEN, this::export);
        actions.add(Ui.label("   GetVehicleServiceCount(", 12, false, Ui.MUTED));
        actions.add(vehicles);
        actions.add(Ui.label(")", 12, false, Ui.MUTED));
        JButton run = Ui.button("Run function", Ui.ACCENT);
        run.addActionListener(e -> callFunction());
        actions.add(run);
        actions.add(fnResult);
        filters.add(Ui.label("Report", 12, false, Ui.MUTED));
        filters.add(reports);
        reports.addActionListener(e -> refresh());
    }

    @Override public void refresh() {
        Ui.run(this, () -> {
            Ui.Item keep = (Ui.Item) vehicles.getSelectedItem();
            vehicles.removeAllItems();
            for (Ui.Item i : Ui.items("SELECT vehicle_id, registration_no FROM VEHICLE ORDER BY registration_no")) vehicles.addItem(i);
            if (keep != null) vehicles.setSelectedItem(keep);
        });
        super.refresh();
    }

    @Override protected Result fetch() throws Exception {
        Rep rep = (Rep) reports.getSelectedItem();
        if (rep == null) return new Result(List.of(), List.of());
        subtitle.setText(rep.description());

        // Some existing student databases contain all base tables but do not
        // contain the optional Phase-3 view/function.  Do not modify the
        // database just to open the Reports screen; use an equivalent query.
        if (rep.name().startsWith("Vehicle_Service_Summary")) {
            if (!objectExists("VIEW", "vehicle_service_summary")) {
                return Db.query("SELECT v.registration_no AS `Registration`, c.customer_name AS `Customer`, "
                        + "s.service_type AS `Service`, m.mechanic_name AS `Mechanic`, s.service_date AS `Date`, "
                        + "s.service_status AS `Status`, ROUND(s.labor_cost + COALESCE((SELECT SUM(sp.part_cost) "
                        + "FROM SERVICE_PART sp WHERE sp.service_id = s.service_id), 0), 2) AS `Total Cost` "
                        + "FROM CUSTOMER c JOIN VEHICLE v ON c.customer_id=v.customer_id "
                        + "JOIN SERVICE s ON v.vehicle_id=s.vehicle_id "
                        + "JOIN MECHANIC m ON s.mechanic_id=m.mechanic_id ORDER BY s.service_date DESC");
            }
        }
        return Db.query(rep.sql());
    }

    private static boolean objectExists(String type, String name) throws Exception {
        Result r = Db.query("SELECT COUNT(*) FROM information_schema.TABLES "
                + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_TYPE = ? AND LOWER(TABLE_NAME) = LOWER(?)", type, name);
        return !r.rows().isEmpty() && ((Number) r.first()).intValue() > 0;
    }

    private void callFunction() {
        Ui.Item v = (Ui.Item) vehicles.getSelectedItem();
        if (v == null) {
            Ui.warn(this, "Please select a vehicle first.");
            return;
        }
        Ui.run(this, () -> {
            Object value;
            if (objectExists("FUNCTION", "GetVehicleServiceCount")) {
                value = Db.query("SELECT GetVehicleServiceCount(?)", v.id()).first();
            } else {
                // Same result without creating the missing stored function.
                value = Db.query("SELECT COUNT(*) FROM SERVICE WHERE vehicle_id = ?", v.id()).first();
            }
            fnResult.setText("  = " + value + " service(s)");
        });
    }

    private void export() {
        if (table.getRowCount() == 0) { Ui.info(this, "Nothing to export."); return; }
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("report.csv"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        Ui.run(this, () -> {
            try (PrintWriter w = new PrintWriter(fc.getSelectedFile(), StandardCharsets.UTF_8)) {
                var m = table.getModel();
                StringBuilder sb = new StringBuilder();
                for (int c = 0; c < m.getColumnCount(); c++) sb.append(c > 0 ? "," : "").append(csv(m.getColumnName(c)));
                w.println(sb);
                for (int r = 0; r < table.getRowCount(); r++) {
                    sb.setLength(0);
                    int mr = table.convertRowIndexToModel(r);
                    for (int c = 0; c < m.getColumnCount(); c++) sb.append(c > 0 ? "," : "").append(csv(Ui.s(m.getValueAt(mr, c))));
                    w.println(sb);
                }
            }
            Ui.info(this, "Exported to " + fc.getSelectedFile().getAbsolutePath());
        });
    }

    private static String csv(String s) { return "\"" + s.replace("\"", "\"\"") + "\""; }
}
