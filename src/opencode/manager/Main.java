package opencode.manager;

import opencode.manager.ui.Settings;
import opencode.manager.ui.Theme;
import opencode.manager.db.Database;
import opencode.manager.ui.MainWindow;
import javax.swing.*;
import java.awt.*;

public class Main {
    public static void main(String[] args) {
        Settings settings = new Settings();
        Theme.applyFlatLaf(settings.isDarkMode());

        UIManager.put("Button.arc", 10);
        UIManager.put("Button.margin", new Insets(4, 12, 4, 12));
        UIManager.put("Button.font", new Font("SansSerif", Font.PLAIN, 12));
        UIManager.put("Component.arrowType", "chevron");
        UIManager.put("Table.rowHeight", 34);
        UIManager.put("Table.showHorizontalLines", false);
        UIManager.put("Table.showVerticalLines", false);
        UIManager.put("Table.intercellSpacing", new Dimension(0, 0));
        UIManager.put("TableHeader.font", new Font("SansSerif", Font.BOLD, 11));
        UIManager.put("TableHeader.height", 28);
        UIManager.put("Tree.rowHeight", 28);
        UIManager.put("Tree.font", new Font("SansSerif", Font.PLAIN, 13));
        UIManager.put("ScrollBar.width", 8);
        UIManager.put("ScrollBar.thumbArc", 8);
        UIManager.put("ScrollBar.thumbInsets", new Insets(2, 2, 2, 2));
        UIManager.put("TabbedPane.tabHeight", 32);
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
