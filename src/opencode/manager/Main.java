package opencode.manager;

import opencode.manager.ui.Settings;
import opencode.manager.ui.Theme;
import opencode.manager.db.Database;
import opencode.manager.ui.MainWindow;
import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        Settings settings = new Settings();
        Theme.applyFlatLaf(settings.isDarkMode());

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