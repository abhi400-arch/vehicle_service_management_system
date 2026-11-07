package com.vsms;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/** Small modal form. The submit action runs inside the dialog; on failure the dialog stays open. */
public class Form {
    private final JDialog dialog;
    private final JPanel body = new JPanel(new GridBagLayout());
    private final Map<String, JComponent> fields = new LinkedHashMap<>();
    private int row = 0;
    private boolean ok;
    private Ui.Action action;

    public Form(Component owner, String title, String submitText) {
        dialog = new JDialog(SwingUtilities.getWindowAncestor(owner), title, Dialog.ModalityType.APPLICATION_MODAL);
        JPanel root = new JPanel(new BorderLayout(0, 16));
        root.setBorder(new EmptyBorder(20, 24, 18, 24));
        root.setBackground(Color.WHITE);
        root.add(Ui.label(title, 17, true, Ui.TEXT), BorderLayout.NORTH);
        body.setOpaque(false);
        root.add(body, BorderLayout.CENTER);

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        bar.setOpaque(false);
        JButton cancel = Ui.button("Cancel", Ui.SLATE);
        cancel.addActionListener(e -> dialog.dispose());
        JButton submit = Ui.button(submitText, Ui.ACCENT);
        submit.addActionListener(e -> {
            if (action != null && Ui.run(dialog, action)) { ok = true; dialog.dispose(); }
        });
        bar.add(cancel);
        bar.add(submit);
        root.add(bar, BorderLayout.SOUTH);
        dialog.setContentPane(root);
        dialog.getRootPane().setDefaultButton(submit);
    }

    private void addRow(String label, JComponent c) {
        GridBagConstraints g = new GridBagConstraints();
        g.gridy = row++;
        g.anchor = GridBagConstraints.WEST;
        g.gridx = 0;
        g.insets = new Insets(6, 0, 6, 14);
        body.add(Ui.label(label, 12, false, Ui.MUTED), g);
        g.gridx = 1;
        g.weightx = 1;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(6, 0, 6, 0);
        body.add(c, g);
        fields.put(label, c);
    }

    public JTextField text(String label, String init) {
        JTextField f = Ui.field(24);
        f.setText(init == null ? "" : init);
        addRow(label, f);
        return f;
    }

    public JComboBox<Object> combo(String label, Collection<?> items, Object selected, boolean editable) {
        JComboBox<Object> c = new JComboBox<>(items.toArray());
        c.setEditable(editable);
        if (selected != null) c.setSelectedItem(selected);
        else if (editable) c.setSelectedItem("");
        addRow(label, c);
        return c;
    }

    /** Adds any ready-made component (for example a password field). */
    public void custom(String label, JComponent c) {
        c.setBorder(Ui.field(1).getBorder());
        addRow(label, c);
    }

    public String str(String label) {
        JComponent c = fields.get(label);
        if (c instanceof JTextField t) return t.getText().trim();
        if (c instanceof JComboBox<?> b) {
            Object o = b.isEditable() ? b.getEditor().getItem() : b.getSelectedItem();
            return o == null ? "" : o.toString().trim();
        }
        return "";
    }

    public Ui.Item item(String label) {
        JComboBox<?> b = (JComboBox<?>) fields.get(label);
        if (b.getSelectedItem() instanceof Ui.Item i) return i;
        throw new IllegalArgumentException("Please select a value for '" + label + "'.");
    }

    public Component owner() { return dialog; }

    /** Shows the dialog; returns true when the action completed successfully. */
    public boolean run(Ui.Action a) {
        action = a;
        ok = false;
        dialog.pack();
        dialog.setMinimumSize(new Dimension(460, 0));
        dialog.pack();
        dialog.setLocationRelativeTo(dialog.getOwner());
        dialog.setVisible(true);
        return ok;
    }
}
