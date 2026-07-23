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
        setSize(420, 340);
        setLocationRelativeTo(parent);

        JPanel root = new JPanel(new BorderLayout(0, 16));
        root.setBorder(new EmptyBorder(20, 20, 16, 20));
        root.setBackground(Theme.BG);

        // --- Theme ---
        JPanel themePanel = sectionPanel("外观主题");
        JRadioButton lightBtn = new JRadioButton("☀  浅色");
        JRadioButton darkBtn = new JRadioButton("☾  深色");
        ButtonGroup group = new ButtonGroup();
        group.add(lightBtn);
        group.add(darkBtn);
        lightBtn.setSelected(!settings.isDarkMode());
        darkBtn.setSelected(settings.isDarkMode());
        lightBtn.setOpaque(false);
        lightBtn.setFont(new Font("SansSerif", Font.PLAIN, 13));
        darkBtn.setOpaque(false);
        darkBtn.setFont(new Font("SansSerif", Font.PLAIN, 13));
        themePanel.add(lightBtn);
        themePanel.add(Box.createHorizontalStrut(16));
        themePanel.add(darkBtn);
        root.add(themePanel, BorderLayout.NORTH);

        // --- Shortcuts ---
        JPanel shortcutsPanel = sectionPanel("快捷键");
        shortcutsPanel.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(2, 0, 2, 0);

        String[][] shortcuts = {
            {"Ctrl+A", "全选"},
            {"Ctrl+Shift+C", "复制会话"},
            {"Ctrl+Shift+V", "粘贴会话"},
            {"Ctrl+R", "重命名"},
            {"Ctrl+D", "删除"},
            {"Ctrl+M", "移动"},
            {"F5", "刷新"},
            {"Ctrl+F", "搜索"},
            {"Delete", "删除选中"},
        };

        for (int i = 0; i < shortcuts.length; i++) {
            c.gridy = i;
            c.gridx = 0;
            c.weightx = 0;
            JLabel keyLabel = new JLabel("  " + shortcuts[i][0]);
            keyLabel.setFont(new Font("Monospaced", Font.BOLD, 12));
            keyLabel.setForeground(Theme.ACCENT);
            shortcutsPanel.add(keyLabel, c);

            c.gridx = 1;
            c.weightx = 1;
            c.insets = new Insets(2, 12, 2, 0);
            JLabel descLabel = new JLabel(shortcuts[i][1]);
            descLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
            descLabel.setForeground(Theme.TEXT_PRIMARY);
            shortcutsPanel.add(descLabel, c);
            c.insets = new Insets(2, 0, 2, 0);
        }

        root.add(shortcutsPanel, BorderLayout.CENTER);

        // --- Close ---
        JButton closeBtn = new JButton("关闭");
        closeBtn.addActionListener(e -> dispose());
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.setOpaque(false);
        bottom.add(closeBtn);
        root.add(bottom, BorderLayout.SOUTH);

        // Apply on radio select
        javax.swing.event.ChangeListener listener = e -> {
            boolean dark = darkBtn.isSelected();
            settings.setDarkMode(dark);
            Theme.applyFlatLaf(dark);
            dispose();
            onThemeChanged.run();
        };
        lightBtn.addActionListener(e -> listener.stateChanged(null));
        darkBtn.addActionListener(e -> listener.stateChanged(null));

        getContentPane().add(root);
    }

    private JPanel sectionPanel(String title) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 6));
        panel.setOpaque(false);
        JLabel label = new JLabel(title);
        label.setFont(new Font("SansSerif", Font.BOLD, 13));
        label.setForeground(Theme.TEXT_SECONDARY);
        panel.add(label);
        panel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER));
        return panel;
    }
}
