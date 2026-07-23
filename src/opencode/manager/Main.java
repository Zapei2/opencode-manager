package opencode.manager;

import com.formdev.flatlaf.FlatLightLaf;
import opencode.manager.db.Database;
import opencode.manager.ui.MainWindow;
import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        FlatLightLaf.setup();
        UIManager.put("Button.arc", 8);
        UIManager.put("Component.arrowType", "chevron");
        UIManager.put("Tree.rowHeight", 28);
        UIManager.put("Table.rowHeight", 30);
        UIManager.put("TabbedPane.tabHeight", 32);
        UIManager.put("ScrollBar.width", 10);
        UIManager.put("SplitPaneDivider.style", "grip");

        SwingUtilities.invokeLater(() -> {
            try {
                Database db = new Database();
                MainWindow window = new MainWindow(db);
                window.setVisible(true);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null,
                    "无法打开 opencode 数据库:\n" + e.getMessage(),
                    "初始化失败", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
                System.exit(1);
            }
        });
    }
}
