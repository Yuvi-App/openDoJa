package opendoja.launcher;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

final class KeybindSettingsController {
    void showDialog(Component parent) {
        JTable table = new JTable(new DefaultTableModel(keybindRows(), new String[]{"Action", "Shortcut"}) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        });
        table.setRowSelectionAllowed(false);
        table.setColumnSelectionAllowed(false);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setPreferredSize(new Dimension(420, 300));

        JOptionPane optionPane = new JOptionPane(
                scrollPane,
                JOptionPane.INFORMATION_MESSAGE,
                JOptionPane.DEFAULT_OPTION);
        JDialog dialog = optionPane.createDialog(parent, "Keybinds");
        dialog.setModal(true);
        dialog.setResizable(false);
        dialog.setVisible(true);
    }

    private Object[][] keybindRows() {
        return new Object[][]{
                {"Digits 0-9", "0-9 or Numpad 0-9"},
                {"Asterisk", "* or Numpad *"},
                {"Pound", "# or Numpad /"},
                {"Move left", "Left Arrow"},
                {"Move up", "Up Arrow"},
                {"Move right", "Right Arrow"},
                {"Move down", "Down Arrow"},
                {"Select", "Enter or Space"},
                {"Clear", "Escape or Backspace"},
                {"Soft key left", "F1 or A"},
                {"Soft key right", "F2 or S"},
                {"Menu", "M"},
                {"Camera", "C"}
        };
    }
}
