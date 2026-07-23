package opencode.manager;

import opencode.manager.db.Database;
import opencode.manager.ui.MainWindow;
import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

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
