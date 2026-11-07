package com.vsms;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

final class MainFrame extends JFrame {
    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final Map<String, Refreshable> pages = new LinkedHashMap<>();

    MainFrame() {
        super("Vehicle Service Management System");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1360, 820);
        setMinimumSize(new Dimension(1100, 650));
        setLocationRelativeTo(null);

        pages.put("Dashboard", new DashboardPanel());
        pages.put("Customers", new CustomersPanel());
        pages.put("Vehicles", new VehiclesPanel());
        pages.put("Mechanics", new MechanicsPanel());
        pages.put("Service Records", new ServicesPanel());
        pages.put("Spare Parts", new PartsPanel());
        pages.put("Payments", new PaymentsPanel());
        pages.put("Other options", new SmartToolsPanel());
        pages.put("Reports", new ReportsPanel());
        pages.forEach((name, p) -> content.add((JComponent) p, name));

        JPanel side = new JPanel();
        side.setBackground(Ui.NAVY);
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setPreferredSize(new Dimension(230, 0));
        side.setBorder(new EmptyBorder(22, 14, 16, 14));
        JLabel brand = Ui.label("VSMS", 26, true, Color.WHITE);
        JLabel sub = Ui.label("Vehicle Service Management", 12, false, new Color(0x9DB2CE));
        brand.setAlignmentX(LEFT_ALIGNMENT); sub.setAlignmentX(LEFT_ALIGNMENT);
        brand.setBorder(new EmptyBorder(0, 10, 0, 0)); sub.setBorder(new EmptyBorder(0, 10, 0, 0));
        side.add(brand); side.add(sub); side.add(Box.createVerticalStrut(26));

        ButtonGroup group = new ButtonGroup();
        boolean first = true;
        for (String name : pages.keySet()) {
            JToggleButton b = nav(name);
            group.add(b);
            side.add(b);
            side.add(Box.createVerticalStrut(4));
            if (first) { b.setSelected(true); first = false; }
        }
        side.add(Box.createVerticalGlue());
        Auth.User me = Auth.current();
        JLabel who = Ui.label("<html>Signed in as<br><b>" + me.fullName() + "</b> (" + me.role() + ")<br>Database: "
                + Db.databaseName() + "</html>", 11, false, new Color(0x9DB2CE));
        who.setBorder(new EmptyBorder(0, 10, 10, 0));
        who.setAlignmentX(LEFT_ALIGNMENT);
        side.add(who);
        JButton pw = Ui.button("Change Password", Ui.NAVY2);
        pw.addActionListener(e -> changePassword());
        JButton out = Ui.button("Log Out", Ui.RED);
        out.addActionListener(e -> logout());
        for (JButton b : new JButton[]{pw, out}) {
            b.setAlignmentX(LEFT_ALIGNMENT);
            b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
            side.add(b);
            side.add(Box.createVerticalStrut(6));
        }

        add(side, BorderLayout.WEST);
        add(content, BorderLayout.CENTER);
        show("Dashboard");
    }

    private void logout() {
        if (!Ui.confirm(this, "Log out of VSMS?")) return;
        Auth.logout();
        dispose();
        new LoginFrame().setVisible(true);
    }

    private void changePassword() {
        Form f = new Form(this, "Change Password", "Update");
        JPasswordField cur = new JPasswordField(), n1 = new JPasswordField(), n2 = new JPasswordField();
        f.custom("Current password", cur);
        f.custom("New password", n1);
        f.custom("Confirm new password", n2);
        if (f.run(() -> {
            if (!new String(n1.getPassword()).equals(new String(n2.getPassword())))
                throw new IllegalArgumentException("The new passwords do not match.");
            Auth.changePassword(new String(cur.getPassword()), new String(n1.getPassword()));
            Ui.info(this, "Password changed.");
        })) { /* done */ }
    }

    private void show(String name) {
        cards.show(content, name);
        pages.get(name).refresh();
    }

    private JToggleButton nav(String name) {
        JToggleButton b = new JToggleButton(name) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (isSelected()) {
                    g2.setColor(Ui.NAVY2);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                    g2.setColor(Ui.ACCENT);
                    g2.fillRoundRect(0, 8, 4, getHeight() - 16, 4, 4);
                } else if (getModel().isRollover()) {
                    g2.setColor(new Color(0x16385A));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setOpaque(false);
        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setForeground(Color.WHITE);
        b.setFont(Ui.font(Font.PLAIN, 14));
        b.setBorder(new EmptyBorder(11, 16, 11, 12));
        b.setAlignmentX(LEFT_ALIGNMENT);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addActionListener(e -> show(name));
        return b;
    }
}
