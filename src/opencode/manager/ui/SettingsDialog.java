package opencode.manager.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;

public class SettingsDialog extends JDialog {
    private final Settings settings;
    private final Runnable onThemeChanged;

    public SettingsDialog(JFrame parent, Settings settings, Runnable onThemeChanged) {
        super(parent, "设置", true);
        this.settings = settings;
        this.onThemeChanged = onThemeChanged;
        setSize(480, 460);
        setLocationRelativeTo(parent);

        JPanel root = new JPanel(new BorderLayout(0, 16));
        root.setBorder(new EmptyBorder(24, 24, 20, 24));
        root.setBackground(Theme.BG);

        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setOpaque(false);

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
        lightBtn.setFont(new Font("SansSerif", Font.PLAIN, 14));
        darkBtn.setOpaque(false);
        darkBtn.setFont(new Font("SansSerif", Font.PLAIN, 14));
        themePanel.add(lightBtn);
        themePanel.add(Box.createHorizontalStrut(24));
        themePanel.add(darkBtn);
        center.add(themePanel);
        center.add(Box.createVerticalStrut(12));

        // --- Archive directory ---
        JPanel archiveSection = sectionPanel("归档目录");
        archiveSection.setLayout(new BorderLayout(8, 0));
        JTextField archiveField = new JTextField(settings.getArchiveDir());
        archiveField.setFont(new Font("SansSerif", Font.PLAIN, 13));
        JButton browseBtn = new JButton("浏览...");
        browseBtn.setFont(new Font("SansSerif", Font.PLAIN, 13));
        browseBtn.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (!archiveField.getText().isEmpty())
                chooser.setSelectedFile(new File(archiveField.getText()));
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                archiveField.setText(chooser.getSelectedFile().getAbsolutePath());
            }
        });
        archiveSection.add(archiveField, BorderLayout.CENTER);
        archiveSection.add(browseBtn, BorderLayout.EAST);
        center.add(archiveSection);
        center.add(Box.createVerticalStrut(12));

        // --- Shortcuts ---
        JPanel shortcutsPanel = sectionPanel("快捷键");
        shortcutsPanel.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(3, 0, 3, 0);

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
            keyLabel.setFont(new Font("Monospaced", Font.BOLD, 13));
            keyLabel.setForeground(Theme.ACCENT);
            shortcutsPanel.add(keyLabel, c);

            c.gridx = 1;
            c.weightx = 1;
            c.insets = new Insets(3, 16, 3, 0);
            JLabel descLabel = new JLabel(shortcuts[i][1]);
            descLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
            descLabel.setForeground(Theme.TEXT_PRIMARY);
            shortcutsPanel.add(descLabel, c);
            c.insets = new Insets(3, 0, 3, 0);
        }

        center.add(shortcutsPanel);
        root.add(center, BorderLayout.CENTER);

        // --- Bottom ---
        JButton closeBtn = new JButton("关闭");
        closeBtn.setFont(new Font("SansSerif", Font.PLAIN, 13));
        closeBtn.addActionListener(e -> {
            settings.setArchiveDir(archiveField.getText().trim());
            dispose();
        });
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        bottom.setOpaque(false);
        bottom.add(closeBtn);
        root.add(bottom, BorderLayout.SOUTH);

        // Theme toggle
        var listener = (java.awt.event.ActionListener) e -> {
            boolean dark = darkBtn.isSelected();
            settings.setDarkMode(dark);
            Theme.applyFlatLaf(dark);
            dispose();
            settings.setArchiveDir(archiveField.getText().trim());
            onThemeChanged.run();
        };
        lightBtn.addActionListener(listener);
        darkBtn.addActionListener(listener);

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
