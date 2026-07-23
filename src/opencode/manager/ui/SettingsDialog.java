package opencode.manager.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class SettingsDialog extends JDialog {
    private final Settings settings;
    private final Runnable onThemeChanged;

    public SettingsDialog(JFrame parent, Settings settings, Runnable onThemeChanged) {
        super(parent, "设置", true);
        this.settings = settings;
        this.onThemeChanged = onThemeChanged;
        setSize(500, 460);
        setLocationRelativeTo(parent);

        JPanel root = new JPanel(new BorderLayout());
        root.setBorder(new EmptyBorder(20, 20, 16, 20));

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);

        // ── Theme ──
        JPanel themeHeader = sectionTitle("外观主题");
        content.add(themeHeader);
        content.add(Box.createVerticalStrut(10));
        JPanel themeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 24, 0));
        themeRow.setOpaque(false);
        JRadioButton lightBtn = new JRadioButton("☀  浅色");
        JRadioButton darkBtn = new JRadioButton("☾  深色");
        lightBtn.setFont(new Font("SansSerif", Font.PLAIN, 14));
        darkBtn.setFont(new Font("SansSerif", Font.PLAIN, 14));
        lightBtn.setOpaque(false);
        darkBtn.setOpaque(false);
        ButtonGroup group = new ButtonGroup();
        group.add(lightBtn);
        group.add(darkBtn);
        lightBtn.setSelected(!settings.isDarkMode());
        darkBtn.setSelected(settings.isDarkMode());
        themeRow.add(lightBtn);
        themeRow.add(darkBtn);
        content.add(themeRow);
        content.add(Box.createVerticalStrut(22));

        // ── Shortcuts ──
        JPanel shortcutsHeader = sectionTitle("快捷键");
        content.add(shortcutsHeader);
        content.add(Box.createVerticalStrut(10));
        JPanel shortcutsPanel = new JPanel(new GridBagLayout());
        shortcutsPanel.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(3, 8, 3, 0);

        String[][] shortcuts = {
            {"Ctrl+A", "全选"},
            {"Ctrl+Shift+C", "复制会话"},
            {"Ctrl+Shift+V", "粘贴会话"},
            {"Ctrl+R", "重命名"},
            {"Ctrl+D", "删除"},
            {"Ctrl+E", "归档"},
            {"Ctrl+M", "移动"},
            {"F5", "刷新"},
            {"Ctrl+F", "搜索"},
            {"Delete", "删除选中"},
        };

        for (int i = 0; i < shortcuts.length; i++) {
            c.gridy = i;
            c.gridx = 0;
            c.weightx = 0;
            JLabel keyLabel = new JLabel(shortcuts[i][0]);
            keyLabel.setFont(new Font("Monospaced", Font.BOLD, 13));
            keyLabel.setForeground(Theme.ACCENT);
            shortcutsPanel.add(keyLabel, c);

            c.gridx = 1;
            c.weightx = 1;
            c.insets = new Insets(3, 24, 3, 0);
            JLabel descLabel = new JLabel(shortcuts[i][1]);
            descLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
            descLabel.setForeground(Theme.TEXT_PRIMARY);
            shortcutsPanel.add(descLabel, c);
            c.insets = new Insets(3, 8, 3, 0);
        }

        content.add(shortcutsPanel);
        root.add(content, BorderLayout.CENTER);

        // ── Bottom ──
        JButton closeBtn = new JButton("关闭");
        closeBtn.setFont(new Font("SansSerif", Font.PLAIN, 13));
        closeBtn.addActionListener(e -> dispose());
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.setOpaque(false);
        bottom.add(closeBtn);
        root.add(bottom, BorderLayout.SOUTH);

        // Theme toggle
        var listener = (java.awt.event.ActionListener) e -> {
            boolean dark = darkBtn.isSelected();
            settings.setDarkMode(dark);
            Theme.applyFlatLaf(dark);
            dispose();
            onThemeChanged.run();
        };
        lightBtn.addActionListener(listener);
        darkBtn.addActionListener(listener);

        getContentPane().add(root);
    }

    private JPanel sectionTitle(String title) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        JLabel label = new JLabel(title);
        label.setFont(new Font("SansSerif", Font.BOLD, 14));
        label.setForeground(Theme.TEXT_SECONDARY);
        p.add(label, BorderLayout.WEST);
        p.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER));
        return p;
    }
}
