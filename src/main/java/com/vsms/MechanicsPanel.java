package com.vsms;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

final class MechanicsPanel extends BasePanel {
    private static final List<String> SPECS = List.of("Engine", "Electrical", "Brakes", "Transmission",
            "General Service", "AC and Cooling", "Suspension", "Body Work");
    private final JComboBox<String> spec = new JComboBox<>(new String[]{"All"});
    private boolean loading;

    MechanicsPanel() {
        super("Mechanics", "Add and manage mechanics, filter by specialization and see their workload");
        action("Add Mechanic", Ui.ACCENT, this::add);
        action("Edit", Ui.SLATE, this::edit);
        action("Delete", Ui.RED, this::delete);
        filters.add(Ui.label("Specialization", 12, false, Ui.MUTED));
        filters.add(spec);
        spec.addActionListener(e -> { if (!loading) refresh(); });
    }

    @Override public void refresh() {
        Ui.run(this, () -> {
            Object keep = spec.getSelectedItem();
            loading = true;
            spec.removeAllItems();
            spec.addItem("All");
            for (Object[] r : Db.query("SELECT DISTINCT specialization FROM MECHANIC WHERE specialization IS NOT NULL ORDER BY 1").rows())
                spec.addItem(Ui.s(r[0]));
            spec.setSelectedItem(keep == null ? "All" : keep);
            if (spec.getSelectedItem() == null) spec.setSelectedItem("All");
            loading = false;
            show(fetch());
        });
    }

    @Override protected Result fetch() throws Exception {
        boolean all = spec.getSelectedItem() == null || "All".equals(spec.getSelectedItem());
        return Db.query("SELECT m.mechanic_id AS `ID`, m.mechanic_name AS `Mechanic`, m.phone AS `Phone`, "
                + "m.specialization AS `Specialization`, COUNT(s.service_id) AS `Total Jobs`, "
                + "COALESCE(SUM(s.service_status IN ('Open','In Progress')), 0) AS `Active Jobs` "
                + "FROM MECHANIC m LEFT JOIN SERVICE s ON s.mechanic_id = m.mechanic_id"
                + (all ? "" : " WHERE m.specialization = ?")
                + " GROUP BY m.mechanic_id, m.mechanic_name, m.phone, m.specialization ORDER BY m.mechanic_id",
                all ? new Object[0] : new Object[]{spec.getSelectedItem()});
    }

    private void add() {
        Form f = new Form(this, "Add Mechanic", "Save");
        f.text("Name", ""); f.text("Phone", ""); f.combo("Specialization", SPECS, "", true);
        if (f.run(() -> save(f, null))) refresh();
    }

    private void edit() {
        if (!need()) return;
        long id = id();
        Form f = new Form(this, "Edit Mechanic #" + id, "Update");
        f.text("Name", Ui.s(cell("Mechanic"))); f.text("Phone", Ui.s(cell("Phone")));
        List<String> specs = new ArrayList<>(SPECS);
        f.combo("Specialization", specs, Ui.s(cell("Specialization")), true);
        if (f.run(() -> save(f, id))) refresh();
    }

    private void save(Form f, Long id) throws Exception {
        String name = f.str("Name");
        if (name.isEmpty()) throw new IllegalArgumentException("Mechanic name is required.");
        String phone = Ui.nn(f.str("Phone"));
        Ui.checkPhone(phone);
        String sp = Ui.nn(f.str("Specialization"));
        if (id == null) {
            Db.syncAutoIncrement("MECHANIC", "mechanic_id");
            Db.update("INSERT INTO MECHANIC (mechanic_name, phone, specialization) VALUES (?,?,?)", name, phone, sp);
        } else {
            Db.update("UPDATE MECHANIC SET mechanic_name=?, phone=?, specialization=? WHERE mechanic_id=?", name, phone, sp, id);
        }
    }

    private void delete() {
        if (!need()) return;
        long id = id();
        if (!Ui.confirm(this, "Delete mechanic #" + id + "?\n(Only possible when no service references this mechanic.)")) return;
        if (Ui.run(this, () -> {
            Db.update("DELETE FROM MECHANIC WHERE mechanic_id=?", id);
            Db.syncAutoIncrement("MECHANIC", "mechanic_id");
        })) refresh();
    }
}
