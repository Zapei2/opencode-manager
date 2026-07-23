package opencode.manager.ui;

import opencode.manager.db.Database;
import opencode.manager.db.ProjectRecord;
import opencode.manager.db.SessionRecord;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class MainWindow extends JFrame {
    private final Database db;
    private List<SessionRecord> allSessions;
    private final JTable table;
    private final SessionTableModel tableModel;
    private final JTextField searchField;
    private final JComboBox<String> projectFilter;
    private final JLabel statusLabel;
    private final JCheckBox showArchived;

    public MainWindow(Database db) {
        super("OpenCode 会话管理器");
        this.db = db;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 700);
        setLocationRelativeTo(null);

        tableModel = new SessionTableModel();
        table = new JTable(tableModel);
        table.setRowHeight(32);
        table.setFont(new Font("SansSerif", Font.PLAIN, 13));
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.setAutoCreateRowSorter(true);
        table.setFillsViewportHeight(true);

        JTableHeader header = table.getTableHeader();
        header.setPreferredSize(new Dimension(header.getWidth(), 30));

        searchField = new JTextField();
        searchField.putClientProperty("JTextField.placeholderText", "搜索会话标题、目录、项目...");
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { applyFilter(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { applyFilter(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { applyFilter(); }
        });

        projectFilter = new JComboBox<>();
        projectFilter.addItem("所有项目");
        projectFilter.addActionListener(e -> applyFilter());

        showArchived = new JCheckBox("显示已归档");
        showArchived.addActionListener(e -> applyFilter());

        statusLabel = new JLabel("就绪");
        statusLabel.setBorder(new EmptyBorder(4, 8, 4, 8));

        setupColumns();
        setupRowRenderer();
        setupPopupMenu();
        setupLayout();
        refreshData();
    }

    private void setupColumns() {
        int[] widths = {32, 280, 200, 60, 80, 80, 140, 80, 60};
        for (int i = 0; i < widths.length; i++) {
            TableColumn col = table.getColumnModel().getColumn(i);
            col.setPreferredWidth(widths[i]);
            if (i == 0) { col.setMaxWidth(40); col.setMinWidth(30); }
        }

        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        table.getColumnModel().getColumn(4).setCellRenderer(rightRenderer);
        table.getColumnModel().getColumn(5).setCellRenderer(rightRenderer);

        table.getColumnModel().getColumn(7).setCellRenderer(new DefaultTableCellRenderer() {{
            setHorizontalAlignment(SwingConstants.CENTER);
        }});
        table.getColumnModel().getColumn(8).setCellRenderer(new DefaultTableCellRenderer() {{
            setHorizontalAlignment(SwingConstants.CENTER);
        }});

        table.getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer() {{
            setForeground(new Color(100, 100, 100));
            setFont(getFont().deriveFont(Font.PLAIN, 11f));
        }});
    }

    private void setupRowRenderer() {
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            private final Color EVEN_PROJECT = new Color(248, 250, 252);
            private final Color ODD_PROJECT = Color.WHITE;
            private final Color SELECTED_BG = new Color(59, 130, 246);
            private final Color SELECTED_FG = Color.WHITE;

            public Component getTableCellRendererComponent(JTable t, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, col);
                if (isSelected) {
                    c.setBackground(SELECTED_BG);
                    c.setForeground(SELECTED_FG);
                } else {
                    int modelRow = t.convertRowIndexToModel(row);
                    SessionRecord s = tableModel.getSessionAt(modelRow);
                    if (s != null) {
                        String project = s.getProjectDisplay();
                        int projectHash = Math.abs(project.hashCode());
                        c.setBackground(projectHash % 2 == 0 ? EVEN_PROJECT : ODD_PROJECT);
                    } else {
                        c.setBackground(Color.WHITE);
                    }
                    c.setForeground(Color.BLACK);
                }
                setBorder(noFocusBorder);
                return c;
            }
        });
    }

    private void setupPopupMenu() {
        JPopupMenu popup = new JPopupMenu();
        JMenuItem renameItem = new JMenuItem("重命名");
        JMenuItem copyItem = new JMenuItem("复制");
        JMenuItem moveItem = new JMenuItem("移动");
        JMenuItem deleteItem = new JMenuItem("删除");
        JMenuItem openDirItem = new JMenuItem("打开目录");
        JMenuItem copyIdItem = new JMenuItem("复制会话 ID");
        JMenuItem detailItem = new JMenuItem("查看详情");

        renameItem.addActionListener(e -> renameSelected());
        copyItem.addActionListener(e -> copySelected());
        moveItem.addActionListener(e -> moveSelected());
        deleteItem.addActionListener(e -> deleteSelected());
        openDirItem.addActionListener(e -> openDirSelected());
        copyIdItem.addActionListener(e -> copyIdSelected());
        detailItem.addActionListener(e -> showDetailSelected());

        popup.add(detailItem);
        popup.addSeparator();
        popup.add(renameItem);
        popup.add(copyItem);
        popup.add(moveItem);
        popup.addSeparator();
        popup.add(deleteItem);
        popup.addSeparator();
        popup.add(openDirItem);
        popup.add(copyIdItem);

        table.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) showPopup(e);
            }
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) showPopup(e);
                if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1) {
                    int row = table.rowAtPoint(e.getPoint());
                    if (row >= 0) {
                        table.setRowSelectionInterval(row, row);
                        renameSelected();
                    }
                }
            }
            private void showPopup(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                if (row >= 0) {
                    if (!table.isRowSelected(row)) table.setRowSelectionInterval(row, row);
                    popup.show(table, e.getX(), e.getY());
                }
            }
        });
    }

    private void setupLayout() {
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        toolbar.add(new JLabel(" \uD83D\uDD0D "));
        toolbar.add(searchField);
        toolbar.addSeparator(new Dimension(12, 0));
        toolbar.add(new JLabel("\uD83D\uDCC1 "));
        toolbar.add(projectFilter);
        toolbar.add(Box.createHorizontalStrut(8));
        toolbar.add(showArchived);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
        JButton renameBtn = new JButton("重命名");
        JButton copyBtn = new JButton("复制");
        JButton moveBtn = new JButton("移动");
        JButton deleteBtn = new JButton("删除");
        JButton refreshBtn = new JButton("刷新");
        JButton backupBtn = new JButton("备份数据库");

        Dimension btnSize = new Dimension(100, 30);
        renameBtn.setPreferredSize(btnSize);
        copyBtn.setPreferredSize(btnSize);
        moveBtn.setPreferredSize(btnSize);
        deleteBtn.setPreferredSize(btnSize);
        refreshBtn.setPreferredSize(btnSize);

        renameBtn.addActionListener(e -> renameSelected());
        copyBtn.addActionListener(e -> copySelected());
        moveBtn.addActionListener(e -> moveSelected());
        deleteBtn.addActionListener(e -> deleteSelected());
        refreshBtn.addActionListener(e -> refreshData());
        backupBtn.addActionListener(e -> backupDatabase());

        actionPanel.add(renameBtn);
        actionPanel.add(copyBtn);
        actionPanel.add(moveBtn);
        actionPanel.add(deleteBtn);
        actionPanel.add(refreshBtn);
        actionPanel.add(backupBtn);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(Color.WHITE);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(actionPanel, BorderLayout.WEST);
        bottomPanel.add(statusLabel, BorderLayout.EAST);

        getContentPane().add(toolbar, BorderLayout.NORTH);
        getContentPane().add(scrollPane, BorderLayout.CENTER);
        getContentPane().add(bottomPanel, BorderLayout.SOUTH);
    }

    public void refreshData() {
        try {
            allSessions = db.listSessions();
            List<ProjectRecord> projects = db.listProjects();
            projectFilter.removeAllItems();
            projectFilter.addItem("所有项目");
            for (ProjectRecord p : projects) {
                String label = p.getDisplayName();
                if (projectFilter.getItemCount() < 60) projectFilter.addItem(label);
            }
            applyFilter();
        } catch (Exception ex) {
            showError("加载数据失败", ex);
        }
    }

    private void applyFilter() {
        if (allSessions == null) return;
        String search = searchField.getText().toLowerCase().trim();
        String projectFilterStr = projectFilter.getSelectedItem() != null ? projectFilter.getSelectedItem().toString() : "所有项目";
        boolean showArchivedFlag = showArchived.isSelected();

        List<SessionRecord> filtered = allSessions.stream()
            .filter(s -> showArchivedFlag || !s.isArchived())
            .filter(s -> {
                if (projectFilterStr.equals("所有项目")) return true;
                return s.getProjectDisplay().equals(projectFilterStr);
            })
            .filter(s -> {
                if (search.isEmpty()) return true;
                return s.title.toLowerCase().contains(search)
                    || (s.directory != null && s.directory.toLowerCase().contains(search))
                    || s.getProjectDisplay().toLowerCase().contains(search)
                    || (s.slug != null && s.slug.toLowerCase().contains(search));
            })
            .sorted(Comparator.comparing((SessionRecord s) -> s.getProjectDisplay().toLowerCase())
                .thenComparing((SessionRecord s) -> s.timeUpdated).reversed())
            .collect(Collectors.toList());

        tableModel.setSessions(filtered);
        statusLabel.setText("共 " + filtered.size() + " 个会话"
            + (allSessions.size() != filtered.size() ? " (已过滤 " + allSessions.size() + " 个)" : ""));
    }

    private SessionRecord getSelectedSession() {
        int row = table.getSelectedRow();
        if (row < 0) { showWarning("请先选择一个会话"); return null; }
        int modelRow = table.convertRowIndexToModel(row);
        return tableModel.getSessionAt(modelRow);
    }

    private List<SessionRecord> getSelectedSessions() {
        int[] rows = table.getSelectedRows();
        List<SessionRecord> list = new ArrayList<>();
        for (int r : rows) {
            int modelRow = table.convertRowIndexToModel(r);
            SessionRecord s = tableModel.getSessionAt(modelRow);
            if (s != null) list.add(s);
        }
        return list;
    }

    private void renameSelected() {
        SessionRecord s = getSelectedSession();
        if (s == null) return;
        String newTitle = JOptionPane.showInputDialog(this, "新标题:", s.title);
        if (newTitle != null && !newTitle.trim().isEmpty() && !newTitle.equals(s.title)) {
            try {
                db.renameSession(s.id, newTitle.trim());
                refreshData();
            } catch (Exception ex) {
                showError("重命名失败", ex);
            }
        }
    }

    private void copySelected() {
        List<SessionRecord> selected = getSelectedSessions();
        if (selected.isEmpty()) { showWarning("请先选择要复制的会话"); return; }
        int n = JOptionPane.showConfirmDialog(this,
            "确定要复制 " + selected.size() + " 个会话吗？\n（包含所有消息和部件）",
            "确认复制", JOptionPane.YES_NO_OPTION);
        if (n != JOptionPane.YES_OPTION) return;
        try {
            for (SessionRecord s : selected) db.copySession(s.id);
            refreshData();
            showInfo("成功复制 " + selected.size() + " 个会话");
        } catch (Exception ex) {
            showError("复制失败", ex);
        }
    }

    private void moveSelected() {
        SessionRecord s = getSelectedSession();
        if (s == null) return;
        String dir = JOptionPane.showInputDialog(this, "新目录路径:", s.directory);
        if (dir == null || dir.trim().isEmpty()) return;
        dir = dir.trim();

        List<ProjectRecord> projects;
        try { projects = db.listProjects(); } catch (Exception ex) {
            showError("获取项目列表失败", ex); return;
        }
        String targetProjectId = "global";
        for (ProjectRecord p : projects) {
            if (dir.startsWith(p.worktree)) {
                targetProjectId = p.id;
                break;
            }
        }
        try {
            db.moveSession(s.id, dir, targetProjectId);
            refreshData();
            showInfo("已移动到:\n  " + dir + "\n  项目: " + targetProjectId);
        } catch (Exception ex) {
            showError("移动失败", ex);
        }
    }

    private void deleteSelected() {
        List<SessionRecord> selected = getSelectedSessions();
        if (selected.isEmpty()) { showWarning("请先选择要删除的会话"); return; }
        StringBuilder msg = new StringBuilder("确定要永久删除以下 " + selected.size() + " 个会话吗？\n\n");
        for (SessionRecord s : selected) {
            msg.append("  \u2022 ").append(s.title).append(" (").append(s.messageCount).append(" 条消息)\n");
        }
        msg.append("\n此操作不可撤销！");
        JCheckBox backupCheck = new JCheckBox("删除前备份数据库", true);
        int n = JOptionPane.showOptionDialog(this,
            new Object[]{msg.toString(), backupCheck},
            "确认删除", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE,
            null, new Object[]{"确认删除", "取消"}, "取消");
        if (n != 0) return;

        try {
            if (backupCheck.isSelected()) db.backupDatabase();
            for (SessionRecord s : selected) db.deleteSession(s.id);
            refreshData();
            showInfo("成功删除 " + selected.size() + " 个会话");
        } catch (Exception ex) {
            showError("删除失败", ex);
        }
    }

    private void openDirSelected() {
        SessionRecord s = getSelectedSession();
        if (s == null) return;
        if (s.directory == null || s.directory.isEmpty()) {
            showWarning("该会话没有关联目录");
            return;
        }
        try {
            File dir = new File(s.directory);
            if (!dir.exists()) { showWarning("目录不存在: " + s.directory); return; }
            Desktop.getDesktop().open(dir);
        } catch (Exception ex) {
            showError("打开目录失败", ex);
        }
    }

    private void copyIdSelected() {
        SessionRecord s = getSelectedSession();
        if (s == null) return;
        Clipboard clip = Toolkit.getDefaultToolkit().getSystemClipboard();
        clip.setContents(new StringSelection(s.id), null);
        showInfo("已复制会话 ID: " + s.id);
    }

    private void showDetailSelected() {
        SessionRecord s = getSelectedSession();
        if (s == null) return;
        String msg = String.format("""
            ID: %s
            标题: %s
            Slug: %s
            目录: %s
            项目: %s
            消息数: %d
            创建时间: %s
            更新时间: %s
            费用: %.6f
            总 Tokens: %s
            Agent: %s
            版本: %s
            """,
            s.id, s.title, s.slug,
            s.getDirectoryShort(), s.getProjectDisplay(),
            s.messageCount, s.getCreatedFormatted(), s.getUpdatedFormatted(),
            s.cost, s.getTokensFormatted(),
            s.agent != null ? s.agent : "-",
            s.version != null ? s.version : "-");
        JTextArea ta = new JTextArea(msg);
        ta.setEditable(false);
        ta.setFont(new Font("Monospaced", Font.PLAIN, 12));
        ta.setBackground(new Color(245, 245, 245));
        JOptionPane.showMessageDialog(this, new JScrollPane(ta), "会话详情", JOptionPane.INFORMATION_MESSAGE);
    }

    private void backupDatabase() {
        try {
            db.backupDatabase();
            showInfo("数据库已备份到:\n" + db.getDbPath().getParent().resolve("opencode.db.backup.*"));
        } catch (Exception ex) {
            showError("备份失败", ex);
        }
    }

    private void showWarning(String msg) {
        JOptionPane.showMessageDialog(this, msg, "提示", JOptionPane.WARNING_MESSAGE);
    }

    private void showInfo(String msg) {
        JOptionPane.showMessageDialog(this, msg, "信息", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showError(String title, Exception ex) {
        JOptionPane.showMessageDialog(this, title + ":\n" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
    }
}
