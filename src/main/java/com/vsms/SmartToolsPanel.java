package com.vsms;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Innovation modules built entirely on top of the existing seven VSMS tables.
 * No table, column, datatype, procedure or database object is added/changed.
 */
final class SmartToolsPanel extends JPanel implements Refreshable {
    private final JComboBox<Ui.Item> matchVehicle = new JComboBox<>();
    private final JComboBox<String> matchType = new JComboBox<>(new String[]{
            "General Service", "Engine Service", "Brake Service", "Electrical Check",
            "Transmission Check", "AC Service", "Routine Inspection", "Oil Change", "Tyre Rotation"});
    private final JTextArea matchResult = resultArea();

    private final JComboBox<String> estimateType = new JComboBox<>(new String[]{
            "General Service", "Engine Service", "Brake Service", "Electrical Check",
            "Transmission Check", "AC Service", "Routine Inspection", "Oil Change", "Tyre Rotation"});
    private final JTextField labor = Ui.field(12);
    private final JTable quoteParts = Ui.table();
    private final JLabel estimateTotal = Ui.label("Estimated Total: ₹0.00", 18, true, Ui.ACCENT);

    private final JComboBox<Ui.Item> healthVehicle = new JComboBox<>();
    private final JLabel healthScore = Ui.label("—", 28, true, Ui.TEXT);
    private final JLabel healthStatus = Ui.label("Select a vehicle", 13, true, Ui.MUTED);
    private final JLabel healthDetails = Ui.label("", 12, false, Ui.MUTED);
    private final JTable history = Ui.table();

    private final JTable stockTable = Ui.table();
    private final JLabel stockSummary = Ui.label("", 13, true, Ui.TEXT);

    SmartToolsPanel() {
        super(new BorderLayout(0, 16));
        setBackground(Ui.BG);
        setBorder(new EmptyBorder(22, 28, 20, 28));

        JPanel head = new JPanel();
        head.setOpaque(false);
        head.setLayout(new BoxLayout(head, BoxLayout.Y_AXIS));
        JLabel title = Ui.label("Other options", 24, true, Ui.TEXT);
        JLabel sub = Ui.label("Advanced options", 13, false, Ui.MUTED);
        title.setAlignmentX(LEFT_ALIGNMENT);
        sub.setAlignmentX(LEFT_ALIGNMENT);
        head.add(title);
        head.add(sub);
        add(head, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(Ui.font(Font.BOLD, 12));
        tabs.addTab("Smart Mechanic Match", mechanicCard());
        tabs.addTab("Service Cost Estimator", estimatorCard());
        tabs.addTab("Vehicle Health Center", healthCard());
        tabs.addTab("Smart Stock Predictor", stockCard());
        add(tabs, BorderLayout.CENTER);
    }

    @Override public void refresh() {
        Ui.run(this, () -> {
            loadVehicles(matchVehicle, true);
            loadVehicles(healthVehicle, false);
            loadQuoteParts();
            refreshHealth();
            refreshStock();
        });
    }

    private JPanel mechanicCard() {
        JPanel root = page();
        JPanel controls = new JPanel(new GridBagLayout());
        controls.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 4, 4, 12);
        g.anchor = GridBagConstraints.WEST;
        addField(controls, g, 0, "Vehicle", matchVehicle);
        addField(controls, g, 1, "Service type", matchType);
        JButton recommend = Ui.button("Find Best Mechanic", Ui.ACCENT);
        recommend.addActionListener(e -> recommendMechanic());
        g.gridx = 2; g.gridy = 0; g.gridheight = 2; g.insets = new Insets(4, 10, 4, 4);
        controls.add(recommend, g);
        root.add(controls, BorderLayout.NORTH);

        JPanel info = Ui.card(new BorderLayout(12, 0));
        info.add(Ui.label("How it works", 13, true, Ui.TEXT), BorderLayout.NORTH);
        JTextArea help = resultArea();
        help.setText("Ranks mechanics using specialization match + active workload + total workload.\n"
                + "The recommendation is read-only, so it never changes your database.");
        info.add(help, BorderLayout.CENTER);
        root.add(info, BorderLayout.CENTER);

        JPanel result = Ui.card(new BorderLayout(8, 8));
        result.add(Ui.label("Recommendation", 14, true, Ui.TEXT), BorderLayout.NORTH);
        result.add(matchResult, BorderLayout.CENTER);
        root.add(result, BorderLayout.SOUTH);
        return root;
    }

    private void recommendMechanic() {
        Ui.run(this, () -> {
            String type = String.valueOf(matchType.getSelectedItem());
            List<Object[]> rows = Db.query("SELECT m.mechanic_id, m.mechanic_name, COALESCE(m.specialization,'General'), "
                    + "COALESCE(SUM(s.service_status IN ('Open','In Progress')),0), COUNT(s.service_id) "
                    + "FROM MECHANIC m LEFT JOIN SERVICE s ON s.mechanic_id=m.mechanic_id "
                    + "GROUP BY m.mechanic_id,m.mechanic_name,m.specialization").rows();
            if (rows.isEmpty()) { matchResult.setText("No mechanics found."); return; }
            rows.sort(Comparator.comparingInt((Object[] r) -> mechanicScore(String.valueOf(r[2]), type, ((Number) r[3]).intValue(), ((Number) r[4]).intValue())).reversed());
            StringBuilder out = new StringBuilder();
            for (int i = 0; i < Math.min(3, rows.size()); i++) {
                Object[] r = rows.get(i);
                int score = mechanicScore(String.valueOf(r[2]), type, ((Number) r[3]).intValue(), ((Number) r[4]).intValue());
                out.append(i + 1).append(". ").append(r[1]).append("  |  ")
                        .append(r[2]).append("  |  Active: ").append(r[3])
                        .append("  |  Total jobs: ").append(r[4])
                        .append("  |  Score: ").append(score).append("/100\n");
            }
            matchResult.setText(out.toString().trim());
        });
    }

    private int mechanicScore(String spec, String type, int active, int total) {
        String s = spec.toLowerCase(Locale.ROOT);
        String t = type.toLowerCase(Locale.ROOT);
        int score = 45;
        if ((t.contains("engine") && s.contains("engine")) || (t.contains("brake") && s.contains("brake"))
                || (t.contains("electrical") && s.contains("electrical")) || (t.contains("transmission") && s.contains("transmission"))
                || (t.contains("ac") && s.contains("ac")) || ((t.contains("general") || t.contains("routine") || t.contains("oil") || t.contains("tyre")) && s.contains("general"))) score += 40;
        score += Math.max(0, 12 - active * 6);
        score += Math.max(0, 3 - Math.min(total, 3));
        return Math.min(100, score);
    }

    private JPanel estimatorCard() {
        JPanel root = page();
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        top.setOpaque(false);
        top.add(Ui.label("Service type", 12, false, Ui.MUTED));
        top.add(estimateType);
        top.add(Ui.label("Labor estimate", 12, false, Ui.MUTED));
        labor.setText("0.00");
        top.add(labor);
        JButton avg = Ui.button("Use Historical Average", Ui.SLATE);
        avg.addActionListener(e -> loadAverageLabor());
        JButton calc = Ui.button("Calculate Estimate", Ui.ACCENT);
        calc.addActionListener(e -> calculateEstimate());
        top.add(avg); top.add(calc);
        root.add(top, BorderLayout.NORTH);

        JPanel tableCard = Ui.card(new BorderLayout(0, 8));
        tableCard.add(Ui.label("Select parts and adjust quantity", 13, true, Ui.TEXT), BorderLayout.NORTH);
        JScrollPane sp = new JScrollPane(quoteParts);
        sp.setBorder(new LineBorder(Ui.LINE));
        tableCard.add(sp, BorderLayout.CENTER);
        root.add(tableCard, BorderLayout.CENTER);

        JPanel south = new JPanel(new BorderLayout(10, 0));
        south.setOpaque(false);
        south.add(Ui.label("No records are inserted or updated by this estimator.", 12, false, Ui.MUTED), BorderLayout.WEST);
        south.add(estimateTotal, BorderLayout.EAST);
        root.add(south, BorderLayout.SOUTH);
        return root;
    }

    private void loadAverageLabor() {
        Ui.run(this, () -> {
            Object v = Db.query("SELECT COALESCE(AVG(labor_cost),0) FROM SERVICE WHERE service_type=?", estimateType.getSelectedItem()).first();
            labor.setText(new BigDecimal(String.valueOf(v)).setScale(2, RoundingMode.HALF_UP).toPlainString());
        });
    }

    private void loadQuoteParts() throws Exception {
        DefaultTableModel m = new DefaultTableModel(new Object[]{"Select", "Part", "Unit Price", "Qty", "Cost"}, 0) {
            @Override public boolean isCellEditable(int row, int col) { return col == 0 || col == 3; }
            @Override public Class<?> getColumnClass(int col) { return col == 0 ? Boolean.class : col == 3 ? Integer.class : Object.class; }
        };
        for (Object[] r : Db.query("SELECT part_name, unit_price, stock_qty FROM PART ORDER BY part_name").rows())
            m.addRow(new Object[]{false, r[0], r[1], 1, BigDecimal.ZERO});
        quoteParts.setModel(m);
        quoteParts.getColumnModel().getColumn(0).setPreferredWidth(60);
        quoteParts.getColumnModel().getColumn(1).setPreferredWidth(260);
        quoteParts.getColumnModel().getColumn(2).setPreferredWidth(120);
        quoteParts.getColumnModel().getColumn(3).setPreferredWidth(70);
        quoteParts.getColumnModel().getColumn(4).setPreferredWidth(120);
        Ui.fit(quoteParts);
        m.addTableModelListener(e -> {
            if (e.getType() != TableModelEvent.DELETE && (e.getColumn() == 0 || e.getColumn() == 3 || e.getColumn() == TableModelEvent.ALL_COLUMNS))
                recalcQuoteRows();
        });
        recalcQuoteRows();
    }

    private void recalcQuoteRows() {
        if (!(quoteParts.getModel() instanceof DefaultTableModel m)) return;
        for (int r = 0; r < m.getRowCount(); r++) {
            BigDecimal price = new BigDecimal(String.valueOf(m.getValueAt(r, 2)));
            int qty;
            try { qty = Math.max(1, Integer.parseInt(String.valueOf(m.getValueAt(r, 3)))); }
            catch (Exception e) { qty = 1; m.setValueAt(1, r, 3); }
            m.setValueAt(price.multiply(BigDecimal.valueOf(qty)), r, 4);
        }
    }

    private void calculateEstimate() {
        Ui.run(this, () -> {
            BigDecimal total = Ui.parseMoney("Labor cost", labor.getText());
            if (quoteParts.getModel() instanceof DefaultTableModel m) {
                for (int r = 0; r < m.getRowCount(); r++) if (Boolean.TRUE.equals(m.getValueAt(r, 0)))
                    total = total.add(new BigDecimal(String.valueOf(m.getValueAt(r, 4))));
            }
            estimateTotal.setText("Estimated Total: " + Ui.money(total));
        });
    }

    private JPanel healthCard() {
        JPanel root = page();
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        top.setOpaque(false);
        top.add(Ui.label("Vehicle", 12, false, Ui.MUTED));
        top.add(healthVehicle);
        JButton check = Ui.button("Analyze Vehicle", Ui.ACCENT);
        check.addActionListener(e -> refreshHealth());
        top.add(check);
        JPanel north = new JPanel(new BorderLayout(0, 10));
        north.setOpaque(false);
        north.add(top, BorderLayout.NORTH);

        JPanel overview = Ui.card(new BorderLayout(14, 0));
        JPanel score = new JPanel(new BorderLayout(0, 2)); score.setOpaque(false);
        score.add(Ui.label("HEALTH SCORE", 11, false, Ui.MUTED), BorderLayout.NORTH);
        score.add(healthScore, BorderLayout.CENTER);
        overview.add(score, BorderLayout.WEST);
        JPanel statusBox = new JPanel(new BorderLayout(0, 4)); statusBox.setOpaque(false);
        statusBox.add(Ui.label("SERVICE HEALTH", 11, false, Ui.MUTED), BorderLayout.NORTH);
        statusBox.add(healthStatus, BorderLayout.CENTER);
        statusBox.add(healthDetails, BorderLayout.SOUTH);
        overview.add(statusBox, BorderLayout.CENTER);
        north.add(overview, BorderLayout.CENTER);
        root.add(north, BorderLayout.NORTH);

        JPanel hist = Ui.card(new BorderLayout(0, 8));
        hist.add(Ui.label("Recent service history", 13, true, Ui.TEXT), BorderLayout.NORTH);
        JScrollPane sp = new JScrollPane(history); sp.setBorder(new LineBorder(Ui.LINE)); hist.add(sp, BorderLayout.CENTER);
        root.add(hist, BorderLayout.CENTER);
        return root;
    }

    private void refreshHealth() {
        Ui.run(this, () -> {
            Ui.Item item = (Ui.Item) healthVehicle.getSelectedItem();
            if (item == null) return;
            Result r = Db.query("SELECT v.registration_no,v.model,v.vehicle_year,v.vehicle_status,COUNT(s.service_id),MAX(s.service_date), "
                    + "COALESCE(SUM(s.labor_cost + COALESCE((SELECT SUM(sp.part_cost) FROM SERVICE_PART sp WHERE sp.service_id=s.service_id),0)),0), "
                    + "COALESCE(MAX(s.odometer_km),0) FROM VEHICLE v LEFT JOIN SERVICE s ON s.vehicle_id=v.vehicle_id "
                    + "WHERE v.vehicle_id=? GROUP BY v.vehicle_id,v.registration_no,v.model,v.vehicle_year,v.vehicle_status", item.id());
            if (r.isEmpty()) return;
            Object[] x = r.rows().get(0);
            int services = ((Number)x[4]).intValue();
            LocalDate last = x[5] instanceof Date d ? d.toLocalDate() : null;
            int score = 100;
            if ("Inactive".equals(x[3])) score -= 35;
            if (services == 0) score -= 25;
            if (last != null) {
                long days = ChronoUnit.DAYS.between(last, LocalDate.now());
                if (days > 180) score -= 25; else if (days > 90) score -= 12;
            }
            score = Math.max(0, score);
            healthScore.setText(score + "/100");
            String level = score >= 80 ? "Healthy" : score >= 60 ? "Needs attention" : "Service recommended";
            healthStatus.setText(level);
            String lastText = last == null ? "No service history" : "Last service: " + last;
            healthDetails.setText("Services: " + services + "  •  Latest odometer: " + x[7] + " km  •  Lifetime cost: " + Ui.money(x[6]) + "  •  " + lastText);
            history.setModel(Ui.toModel(Db.query("SELECT s.service_date AS `Date`,s.service_type AS `Service`,m.mechanic_name AS `Mechanic`, "
                    + "s.odometer_km AS `Odometer`," + Sql.TOTAL + " AS `Cost`,s.service_status AS `Status` "
                    + "FROM SERVICE s JOIN MECHANIC m ON m.mechanic_id=s.mechanic_id WHERE s.vehicle_id=? ORDER BY s.service_date DESC LIMIT 8", item.id())));
            Ui.fit(history);
        });
    }

    private JPanel stockCard() {
        JPanel root = page();
        JPanel top = new JPanel(new BorderLayout(10, 0)); top.setOpaque(false);
        top.add(Ui.label("Predictive reorder recommendation", 14, true, Ui.TEXT), BorderLayout.WEST);
        JButton refresh = Ui.button("Recalculate", Ui.ACCENT); refresh.addActionListener(e -> refreshStock()); top.add(refresh, BorderLayout.EAST);
        root.add(top, BorderLayout.NORTH);
        JScrollPane sp = new JScrollPane(stockTable); sp.setBorder(new LineBorder(Ui.LINE)); root.add(sp, BorderLayout.CENTER);
        root.add(stockSummary, BorderLayout.SOUTH);
        return root;
    }

    private void refreshStock() {
        Ui.run(this, () -> {
            Result r = Db.query("SELECT p.part_id,p.part_name,p.stock_qty,COALESCE(SUM(CASE WHEN s.service_date>=CURDATE()-INTERVAL 90 DAY THEN sp.quantity ELSE 0 END),0) AS used90 "
                    + "FROM PART p LEFT JOIN SERVICE_PART sp ON sp.part_id=p.part_id LEFT JOIN SERVICE s ON s.service_id=sp.service_id "
                    + "GROUP BY p.part_id,p.part_name,p.stock_qty ORDER BY used90 DESC,p.stock_qty ASC");
            DefaultTableModel m = new DefaultTableModel(new Object[]{"Part","Current Stock","Used (90d)","Avg / Month","Suggested Reorder","Priority"},0) {
                @Override public boolean isCellEditable(int row,int col){return false;}
            };
            int urgent=0;
            for (Object[] x : r.rows()) {
                int stock=((Number)x[2]).intValue(); int used=((Number)x[3]).intValue();
                int avg=(int)Math.ceil(used/3.0); int reorder=Math.max(0,avg*2-stock);
                String priority=reorder>0 && stock<=avg ? "URGENT" : reorder>0 ? "PLAN" : "OK";
                if ("URGENT".equals(priority)) urgent++;
                m.addRow(new Object[]{x[1],stock,used,avg,reorder,priority});
            }
            stockTable.setModel(m); Ui.fit(stockTable);
            stockSummary.setText("90-day usage model • " + urgent + " part(s) need urgent attention • no stock values are modified");
        });
    }

    private void loadVehicles(JComboBox<Ui.Item> box, boolean availableOnly) throws Exception {
        Ui.Item keep = (Ui.Item) box.getSelectedItem();
        box.removeAllItems();
        String where = availableOnly ? " WHERE vehicle_status='Available'" : "";
        for (Ui.Item i : Ui.items("SELECT vehicle_id, CONCAT(registration_no,'  -  ',model) FROM VEHICLE" + where + " ORDER BY registration_no")) box.addItem(i);
        if (keep != null) box.setSelectedItem(keep);
    }

    private static JPanel page() {
        JPanel p = new JPanel(new BorderLayout(0, 14));
        p.setOpaque(false);
        return p;
    }

    private static JTextArea resultArea() {
        JTextArea a = new JTextArea();
        a.setEditable(false); a.setOpaque(false); a.setLineWrap(true); a.setWrapStyleWord(true);
        a.setFont(Ui.font(Font.PLAIN, 13)); a.setForeground(Ui.TEXT); a.setBorder(new EmptyBorder(8, 10, 8, 10));
        return a;
    }

    private static void addField(JPanel p, GridBagConstraints g, int row, String label, JComponent c) {
        g.gridx=0; g.gridy=row; g.gridheight=1; g.insets=new Insets(4,4,4,8); p.add(Ui.label(label,12,false,Ui.MUTED),g);
        g.gridx=1; g.insets=new Insets(4,0,4,14); p.add(c,g);
    }
}
