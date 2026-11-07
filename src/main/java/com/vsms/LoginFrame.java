package com.vsms;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.SQLException;

final class LoginFrame extends JFrame {
    private final JTextField username = Ui.field(20);
    private final JPasswordField password = new JPasswordField(20);
    private final JLabel error = Ui.label(" ", 12, false, Ui.RED);

    LoginFrame() {
        super("VSMS - Sign in");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(900, 540);
        setResizable(false);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // ---- branding side
        JPanel brand = new JPanel();
        brand.setBackground(Ui.NAVY);
        brand.setPreferredSize(new Dimension(430, 0));
        brand.setLayout(new BoxLayout(brand, BoxLayout.Y_AXIS));
        brand.setBorder(new EmptyBorder(70, 48, 40, 40));
        brand.add(left(Ui.label("VSMS", 44, true, Color.WHITE)));
        brand.add(left(Ui.label("Vehicle Service Management System", 15, false, new Color(0x9DB2CE))));
        brand.add(Box.createVerticalStrut(46));
        for (String s : new String[]{"Customers, vehicles and mechanics", "Service jobs with parts and stock control",
                "Payments and outstanding balances", "Reports, analysis and validation checks"}) {
            brand.add(left(Ui.label("\u2022  " + s, 14, false, new Color(0xDCE6F5))));
            brand.add(Box.createVerticalStrut(12));
        }
        add(brand, BorderLayout.WEST);

        // ---- form side
        JPanel form = new JPanel();
        form.setBackground(Color.WHITE);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(new EmptyBorder(80, 52, 40, 52));
        form.add(left(Ui.label("Welcome back", 26, true, Ui.TEXT)));
        form.add(left(Ui.label("Sign in to continue", 13, false, Ui.MUTED)));
        form.add(Box.createVerticalStrut(30));
        form.add(left(Ui.label("Username", 12, false, Ui.MUTED)));
        form.add(Box.createVerticalStrut(5));
        form.add(full(username));
        form.add(Box.createVerticalStrut(16));
        form.add(left(Ui.label("Password", 12, false, Ui.MUTED)));
        form.add(Box.createVerticalStrut(5));
        password.setFont(Ui.font(Font.PLAIN, 13));
        password.setBorder(username.getBorder());
        form.add(full(password));
        form.add(Box.createVerticalStrut(10));
        form.add(left(error));
        form.add(Box.createVerticalStrut(12));
        JButton signIn = Ui.button("Sign In", Ui.ACCENT);
        signIn.setFont(Ui.font(Font.BOLD, 14));
        signIn.setBorder(new EmptyBorder(11, 16, 11, 16));
        signIn.addActionListener(e -> signIn());
        form.add(full(signIn));
        form.add(Box.createVerticalStrut(22));
        form.add(left(Ui.label("First time? Sign in with admin / admin123, then change the password.", 11, false, Ui.MUTED)));
        add(form, BorderLayout.CENTER);

        getRootPane().setDefaultButton(signIn);
    }

    private static JComponent left(JComponent c) { c.setAlignmentX(LEFT_ALIGNMENT); return c; }

    private static JComponent full(JComponent c) {
        c.setAlignmentX(LEFT_ALIGNMENT);
        c.setMaximumSize(new Dimension(Integer.MAX_VALUE, c.getPreferredSize().height));
        return c;
    }

    private void signIn() {
        String u = username.getText().trim();
        String p = new String(password.getPassword());
        if (u.isEmpty() || p.isEmpty()) { error.setText("Enter your username and password."); return; }
        try {
            if (Auth.login(u, p)) {
                dispose();
                new MainFrame().setVisible(true);
            } else {
                error.setText("Invalid username or password.");
                password.setText("");
                password.requestFocusInWindow();
            }
        } catch (SQLException e) {
            error.setText("<html>Database error: " + e.getMessage() + "</html>");
        }
    }
}
