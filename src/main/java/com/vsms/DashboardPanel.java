package com.vsms;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;

final class DashboardPanel extends JPanel implements Refreshable {
    private static final class Kpi extends JPanel {
        final JLabel value = Ui.label("-", 26, true, Ui.TEXT);
        Kpi(String title, Color c) {
            super(new BorderLayout(0, 4));
            setBackground(Color.WHITE);
            setBorder(new CompoundBorder(new MatteBorder(0, 4, 0, 0, c), new EmptyBorder(14, 16, 14, 16)));
            add(Ui.label(title, 12, false, Ui.MUTED), BorderLayout.NORTH);
            add(value, BorderLayout.CENTER);
        }
    }

    private final Kpi customers = new Kpi("CUSTOMERS", Ui.ACCENT), vehicles = new Kpi("VEHICLES", Ui.ACCENT),
            inService = new Kpi("VEHICLES IN SERVICE", Ui.AMBER), active = new Kpi("ACTIVE SERVICES", Ui.AMBER),
            revenue = new Kpi("PAYMENTS RECEIVED", Ui.GREEN), lowStock = new Kpi("LOW-STOCK PARTS", Ui.RED);
    private final JTable recent = Ui.table(), low = Ui.table();

    DashboardPanel() {
        super(new BorderLayout(0, 18));
        setBackground(Ui.BG);
        setBorder(new EmptyBorder(22, 28, 20, 28));

        JPanel north = new JPanel();
        north.setOpaque(false);
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        JLabel t = Ui.label("Dashboard", 24, true, Ui.TEXT);
        JLabel s = Ui.label("Live overview of the workshop", 13, false, Ui.MUTED);
        t.setAlignmentX(LEFT_ALIGNMENT); s.setAlignmentX(LEFT_ALIGNMENT);
        north.add(t); north.add(s); north.add(Box.createVerticalStrut(16));
        JPanel row = new JPanel(new GridLayout(1, 6, 14, 0));
        row.setOpaque(false);
        row.setAlignmentX(LEFT_ALIGNMENT);
        for (Kpi k : new Kpi[]{customers, vehicles, inService, active, revenue, lowStock}) row.add(k);
        north.add(row);
        add(north, BorderLayout.NORTH);

        JPanel center = new JPanel(new GridBagLayout());
        center.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.BOTH; g.weighty = 1; g.gridy = 0;
        g.gridx = 0; g.weightx = 0.65; g.insets = new Insets(0, 0, 0, 9);
        center.add(tableCard("Recent services (Vehicle_Service_Summary view)", recent), g);
        g.gridx = 1; g.weightx = 0.35; g.insets = new Insets(0, 9, 0, 0);
        center.add(tableCard("Parts to restock", low), g);
        add(center, BorderLayout.CENTER);
    }

    private JPanel tableCard(String title, JTable t) {
        JPanel p = new JPanel(new BorderLayout(0, 10));
        p.setBackground(Color.WHITE);
        p.setBorder(new CompoundBorder(new LineBorder(Ui.LINE), new EmptyBorder(14, 14, 14, 14)));
        p.add(Ui.label(title, 14, true, Ui.TEXT), BorderLayout.NORTH);
        JScrollPane sp = new JScrollPane(t);
        sp.setBorder(new LineBorder(Ui.LINE));
        sp.getViewport().setBackground(Color.WHITE);
        p.add(sp, BorderLayout.CENTER);
        return p;
    }

    private Result recentServices() throws Exception {
        Result exists = Db.query("SELECT COUNT(*) FROM information_schema.TABLES "
                + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_TYPE = 'VIEW' "
                + "AND LOWER(TABLE_NAME) = LOWER('Vehicle_Service_Summary')");
        boolean hasView = !exists.rows().isEmpty() && ((Number) exists.first()).intValue() > 0;
        if (hasView) {
            return Db.query("SELECT registration_no AS `Vehicle`, customer AS `Customer`, service_type AS `Service`, "
                    + "mechanic AS `Mechanic`, service_date AS `Date`, status AS `Status`, total_service_cost AS `Total` "
                    + "FROM Vehicle_Service_Summary ORDER BY service_date DESC LIMIT 10");
        }
        return Db.query("SELECT v.registration_no AS `Vehicle`, c.customer_name AS `Customer`, "
                + "s.service_type AS `Service`, m.mechanic_name AS `Mechanic`, s.service_date AS `Date`, "
                + "s.service_status AS `Status`, ROUND(s.labor_cost + COALESCE((SELECT SUM(sp.part_cost) "
                + "FROM SERVICE_PART sp WHERE sp.service_id=s.service_id),0),2) AS `Total` "
                + "FROM CUSTOMER c JOIN VEHICLE v ON c.customer_id=v.customer_id "
                + "JOIN SERVICE s ON v.vehicle_id=s.vehicle_id JOIN MECHANIC m ON s.mechanic_id=m.mechanic_id "
                + "ORDER BY s.service_date DESC LIMIT 10");
    }

    @Override public void refresh() {
        Ui.run(this, () -> {
            Object[] k = Db.query("SELECT (SELECT COUNT(*) FROM CUSTOMER), (SELECT COUNT(*) FROM VEHICLE), "
                    + "(SELECT COUNT(*) FROM VEHICLE WHERE vehicle_status = 'In Service'), "
                    + "(SELECT COUNT(*) FROM SERVICE WHERE service_status IN ('Open','In Progress')), "
                    + "(SELECT COALESCE(SUM(amount),0) FROM PAYMENT WHERE payment_status IN ('Paid','Partial')), "
                    + "(SELECT COUNT(*) FROM PART WHERE stock_qty < 10)").rows().get(0);
            customers.value.setText(Ui.s(k[0]));
            vehicles.value.setText(Ui.s(k[1]));
            inService.value.setText(Ui.s(k[2]));
            active.value.setText(Ui.s(k[3]));
            revenue.value.setText(Ui.money(k[4]));
            lowStock.value.setText(Ui.s(k[5]));
            recent.setModel(Ui.toModel(recentServices()));
            Ui.fit(recent);
            low.setModel(Ui.toModel(Db.query("SELECT part_name AS `Part`, stock_qty AS `Stock`, "
                    + "CASE WHEN stock_qty < 10 THEN 'LOW' ELSE 'OK' END AS `Level` FROM PART ORDER BY stock_qty ASC LIMIT 10")));
            Ui.fit(low);
        });
    }
}
