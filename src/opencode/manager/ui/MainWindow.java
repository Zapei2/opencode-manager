package opencode.manager.ui;

import opencode.manager.db.Database;
import opencode.manager.db.ProjectRecord;
import opencode.manager.db.SessionRecord;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

public class MainWindow extends JFrame {
    private final Database db;
    private List<SessionRecord> allSessions;
    private List<SessionRecord> filteredSessions;
    private final JTable table;
    private final SessionTableModel tableModel;
    private final JTree tree;
    private final DefaultTreeModel treeModel;
    private final DefaultMutableTreeNode treeRoot;
    private final JTextField searchField;
    private final JLabel statusLabel;
    private final JCheckBox showArchived;
    private String selectedDirPrefix;
    private SessionRecord selectedSession;
    private boolean updatingTree;

    static class DirNode {
        String name;
        String fullPath;
        boolean hasSessions;
        int sessionCount;

        DirNode(String name, String fullPath, boolean hasSessions, int sessionCount) {
            this.name = name;
            this.fullPath = fullPath;
            this.hasSessions = hasSessions;
            this.sessionCount = sessionCount;
        }
    }

    public MainWindow(Database db) {
        super("OpenCode 会话管理器");
        this.db = db;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1300, 780);
        setLocationRelativeTo(null);

        tableModel = new SessionTableModel();
        table = new JTable(tableModel);
        setupTable();

        treeRoot = new DefaultMutableTreeNode("所有会话");
        treeModel = new DefaultTreeModel(treeRoot);
        tree = new JTree(treeModel);
        setupTree();

        searchField = new JTextField();
        searchField.putClientProperty("JTextField.placeholderText", "搜索会话...");
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { applyFilter(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { applyFilter(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { applyFilter(); }
        });

        showArchived = new JCheckBox("显示已归档");
        showArchived.addActionListener(e -> refreshAll());

        statusLabel = new JLabel("就绪");
        statusLabel.setBorder(new EmptyBorder(4, 12, 4, 12));
        statusLabel.setFont(statusLabel.getFont().deriveFont(12f));

        setupLayout();
        refreshAll();
    }

    private void setupTable() {
        table.setRowHeight(32);
        table.setFont(new Font("SansSerif", Font.PLAIN, 13));
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        table.getTableHeader().setPreferredSize(new Dimension(0, 30));
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.setAutoCreateRowSorter(true);
        table.setFillsViewportHeight(true);

        int[] widths = {36, 250, 200, 55, 80, 80, 140, 65, 55};
        for (int i = 0; i < widths.length; i++)
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);

        table.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {{
            setHorizontalAlignment(SwingConstants.RIGHT);
        }});
        table.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {{
            setHorizontalAlignment(SwingConstants.RIGHT);
        }});
        table.getColumnModel().getColumn(6).setCellRenderer(new DefaultTableCellRenderer() {{
            setHorizontalAlignment(SwingConstants.CENTER);
        }});
        table.getColumnModel().getColumn(7).setCellRenderer(new DefaultTableCellRenderer() {{
            setHorizontalAlignment(SwingConstants.CENTER);
        }});
        table.getColumnModel().getColumn(1).setCellRenderer(new DefaultTableCellRenderer() {{
            setForeground(new Color(100, 100, 100));
            setFont(getFont().deriveFont(Font.PLAIN, 11f));
        }});

        JPopupMenu popup = new JPopupMenu();
        popup.add(menuItem("重命名", e -> renameSelected()));
        popup.add(menuItem("复制", e -> copySelected()));
        popup.add(menuItem("移动", e -> moveSelected()));
        popup.addSeparator();
        popup.add(menuItem("删除", e -> deleteSelected()));
        popup.addSeparator();
        popup.add(menuItem("打开目录", e -> openDirSelected()));
        popup.add(menuItem("复制会话 ID", e -> copyIdSelected()));
        popup.add(menuItem("查看详情", e -> showDetailSelected()));
        table.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) { if (e.isPopupTrigger()) popup.show(table, e.getX(), e.getY()); }
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) popup.show(table, e.getX(), e.getY());
                if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1) {
                    int row = table.rowAtPoint(e.getPoint());
                    if (row >= 0) { table.setRowSelectionInterval(row, row); renameSelected(); }
                }
            }
        });
    }

    private JMenuItem menuItem(String text, java.awt.event.ActionListener l) {
        JMenuItem item = new JMenuItem(text);
        item.addActionListener(l);
        return item;
    }

    private void setupTree() {
        tree.setRootVisible(true);
        tree.setShowsRootHandles(true);
        tree.setRowHeight(26);
        tree.setFont(new Font("SansSerif", Font.PLAIN, 13));
        tree.setBorder(new EmptyBorder(4, 0, 4, 0));
        tree.setToggleClickCount(1);
        tree.setScrollsOnExpand(true);

        tree.setCellRenderer(new DefaultTreeCellRenderer() {
            private final JLabel label = new JLabel();
            {
                label.setOpaque(false);
                label.setBorder(new EmptyBorder(0, 2, 0, 0));
            }
            public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded,
                                                           boolean leaf, int row, boolean hasFocus) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
                Object obj = node.getUserObject();
                Color fg = sel ? UIManager.getColor("Tree.selectionForeground") : UIManager.getColor("Tree.foreground");

                if (node == treeRoot) {
                    int n = allSessions != null ? allSessions.size() : 0;
                    label.setText("\uD83D\uDCC1  所有会话  (" + n + ")");
                } else if (obj instanceof DirNode d) {
                    String icon = expanded ? "\uD83D\uDCC2" : "\uD83D\uDCC1";
                    String count = d.sessionCount > 0 ? "  (" + d.sessionCount + ")" : "";
                    label.setText(icon + "  " + d.name + count);
                } else if (obj instanceof SessionRecord s) {
                    String ic = s.isArchived() ? "\uD83D\uDCE6" : "\uD83D\uDCDD";
                    label.setText(ic + "  " + s.title);
                } else {
                    label.setText(String.valueOf(obj));
                }
                label.setFont(tree.getFont());
                label.setForeground(fg);
                if (sel) label.setBackground(UIManager.getColor("Tree.selectionBackground"));
                return label;
            }
        });

        tree.addTreeSelectionListener(e -> {
            if (updatingTree) return;
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
            if (node == null) return;
            Object obj = node.getUserObject();
            if (node == treeRoot) {
                selectedDirPrefix = null;
                selectedSession = null;
            } else if (obj instanceof DirNode d) {
                selectedDirPrefix = d.fullPath;
                selectedSession = null;
            } else if (obj instanceof SessionRecord s) {
                selectedSession = s;
                selectedDirPrefix = s.directory;
            }
            applyFilter();
        });
    }

    private void setupLayout() {
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        toolbar.setBorder(new EmptyBorder(4, 8, 4, 8));
        toolbar.add(new JLabel("\uD83D\uDD0D "));
        toolbar.add(searchField);
        toolbar.add(Box.createHorizontalStrut(12));
        toolbar.add(showArchived);

        JPanel actionBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 5));
        actionBar.setBorder(new EmptyBorder(0, 0, 4, 0));
        actionBar.add(createBtn("✏ 重命名", e -> renameSelected()));
        actionBar.add(createBtn("📋 复制", e -> copySelected()));
        actionBar.add(createBtn("📦 移动", e -> moveSelected()));
        actionBar.add(createBtn("🗑 删除", e -> deleteSelected()));
        actionBar.add(createBtn("🔄 刷新", e -> refreshAll()));
        actionBar.add(createBtn("💾 备份", e -> backupDatabase()));

        JScrollPane treeScroll = new JScrollPane(tree);
        treeScroll.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1,
            UIManager.getColor("Label.disabledForeground")));
        treeScroll.setMinimumSize(new Dimension(200, 0));
        treeScroll.setPreferredSize(new Dimension(250, 0));

        JPanel rightPanel = new JPanel(new BorderLayout(0, 0));
        rightPanel.add(actionBar, BorderLayout.NORTH);
        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBorder(null);
        rightPanel.add(tableScroll, BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treeScroll, rightPanel);
        split.setDividerSize(6);
        split.setResizeWeight(0.2);
        split.setBorder(null);

        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBorder(new EmptyBorder(2, 0, 2, 0));
        statusBar.add(statusLabel, BorderLayout.WEST);

        getContentPane().add(toolbar, BorderLayout.NORTH);
        getContentPane().add(split, BorderLayout.CENTER);
        getContentPane().add(statusBar, BorderLayout.SOUTH);
    }

    private JButton createBtn(String text, java.awt.event.ActionListener l) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("SansSerif", Font.PLAIN, 12));
        btn.addActionListener(l);
        return btn;
    }

    public void refreshAll() {
        try {
            allSessions = db.listSessions();
            buildTree();
            applyFilter();
        } catch (Exception ex) {
            showError("加载数据失败", ex);
        }
    }

    private void buildTree() {
        updatingTree = true;
        treeRoot.removeAllChildren();

        String home = System.getProperty("user.home");

        // Collect directories with sessions
        Map<String, List<SessionRecord>> byDir = new LinkedHashMap<>();
        List<SessionRecord> noDir = new ArrayList<>();
        for (SessionRecord s : allSessions) {
            if (!showArchived.isSelected() && s.isArchived()) continue;
            String dir = (s.directory != null && !s.directory.isEmpty()) ? s.directory : null;
            if (dir != null) {
                byDir.computeIfAbsent(dir, k -> new ArrayList<>()).add(s);
            } else {
                noDir.add(s);
            }
        }

        // Track path -> node mapping for reuse
        Map<String, DefaultMutableTreeNode> pathNodes = new LinkedHashMap<>();
        pathNodes.put("", treeRoot);

        List<String> sortedDirs = new ArrayList<>(byDir.keySet());
        Collections.sort(sortedDirs);

        for (String fullDir : sortedDirs) {
            List<SessionRecord> dirSessions = byDir.get(fullDir);
            dirSessions.sort((a, b) -> Long.compare(b.timeUpdated, a.timeUpdated));

            // Build relative path
            String relPath = fullDir.startsWith(home) ? "~" + fullDir.substring(home.length()) : fullDir;
            if (relPath.endsWith("/")) relPath = relPath.substring(0, relPath.length() - 1);
            String[] segments = relPath.split("/");

            StringBuilder curRel = new StringBuilder();
            DefaultMutableTreeNode parent = treeRoot;

            for (int i = 0; i < segments.length; i++) {
                String seg = segments[i];
                if (seg.isEmpty()) continue;

                if (curRel.length() > 0) curRel.append("/");
                curRel.append(seg);
                String curRelStr = curRel.toString();

                DefaultMutableTreeNode node = pathNodes.get(curRelStr);
                if (node == null) {
                    // Build full filesystem path for this segment
                    String segFullPath;
                    if (curRelStr.equals("~")) {
                        segFullPath = home;
                    } else if (curRelStr.startsWith("~/")) {
                        segFullPath = home + "/" + curRelStr.substring(2);
                    } else {
                        segFullPath = curRelStr;
                    }

                    boolean isLast = (i == segments.length - 1);
                    List<SessionRecord> leafSessions = isLast ? dirSessions : null;
                    int cnt = isLast ? dirSessions.size() : 0;
                    DirNode dn = new DirNode(seg, segFullPath, isLast, cnt);
                    node = new DefaultMutableTreeNode(dn);
                    parent.add(node);
                    pathNodes.put(curRelStr, node);
                }

                parent = node;
            }

            // Add session leaf nodes
            for (SessionRecord s : dirSessions) {
                parent.add(new DefaultMutableTreeNode(s));
            }
        }

        // Add "(无目录)" node
        if (!noDir.isEmpty()) {
            noDir.sort((a, b) -> Long.compare(b.timeUpdated, a.timeUpdated));
            DirNode dn = new DirNode("(无目录)", "", true, noDir.size());
            DefaultMutableTreeNode node = new DefaultMutableTreeNode(dn);
            treeRoot.add(node);
            for (SessionRecord s : noDir) {
                node.add(new DefaultMutableTreeNode(s));
            }
        }

        // Traverse to update intermediate directory counts
        updateDirCounts(treeRoot);

        treeModel.reload();
        expandAllPaths(tree, new TreePath(treeRoot));
        updatingTree = false;
    }

    private int updateDirCounts(DefaultMutableTreeNode node) {
        int total = 0;
        for (int i = 0; i < node.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
            Object obj = child.getUserObject();
            if (obj instanceof SessionRecord) {
                total++;
            } else if (obj instanceof DirNode dn) {
                if (dn.hasSessions) {
                    total += dn.sessionCount;
                } else {
                    total += updateDirCounts(child);
                }
            }
        }
        // Update count for intermediate parent directories
        if (node != treeRoot && node.getUserObject() instanceof DirNode dn && !dn.hasSessions) {
            dn.sessionCount = total;
        }
        return total;
    }

    private void expandAllPaths(JTree tree, TreePath parent) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) parent.getLastPathComponent();
        if (node.getChildCount() > 0) {
            tree.expandPath(parent);
            for (int i = 0; i < node.getChildCount(); i++) {
                DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
                expandAllPaths(tree, parent.pathByAddingChild(child));
            }
        }
    }

    private void applyFilter() {
        if (allSessions == null) return;
        String search = searchField.getText().toLowerCase().trim();
        boolean showArchivedFlag = showArchived.isSelected();

        filteredSessions = allSessions.stream()
            .filter(s -> showArchivedFlag || !s.isArchived())
            .filter(s -> matchesDirPrefix(s))
            .filter(s -> {
                if (search.isEmpty()) return true;
                return s.title.toLowerCase().contains(search)
                    || (s.directory != null && s.directory.toLowerCase().contains(search))
                    || (s.slug != null && s.slug.toLowerCase().contains(search));
            })
            .sorted(Comparator.comparing((SessionRecord s) -> s.timeUpdated).reversed())
            .collect(Collectors.toList());

        tableModel.setSessions(filteredSessions);

        if (selectedSession != null) {
            for (int i = 0; i < filteredSessions.size(); i++) {
                if (filteredSessions.get(i).id.equals(selectedSession.id)) {
                    int viewRow = table.convertRowIndexToView(i);
                    if (viewRow >= 0) {
                        table.setRowSelectionInterval(viewRow, viewRow);
                        table.scrollRectToVisible(table.getCellRect(viewRow, 0, true));
                    }
                    break;
                }
            }
        } else if (!filteredSessions.isEmpty()) {
            table.clearSelection();
        }

        updateStatus();
    }

    private boolean matchesDirPrefix(SessionRecord s) {
        if (selectedDirPrefix == null) return true;
        if (selectedDirPrefix.isEmpty()) return s.directory == null || s.directory.isEmpty();
        return s.directory != null && (s.directory.equals(selectedDirPrefix) || s.directory.startsWith(selectedDirPrefix + "/"));
    }

    private void updateStatus() {
        int total = allSessions != null ? allSessions.size() : 0;
        int shown = filteredSessions != null ? filteredSessions.size() : 0;
        String home = System.getProperty("user.home");
        String dir = selectedDirPrefix;
        String dirShort = dir != null ?
            (dir.startsWith(home) ? "~" + dir.substring(home.length()) : dir)
            : "所有目录";
        statusLabel.setText(dirShort + "  |  " + shown + " 个会话" + (total != shown ? "  (共 " + total + ")" : ""));
    }

    private SessionRecord getSelectedSession() {
        int row = table.getSelectedRow();
        if (row < 0) { showWarning("请先选择一个会话"); return null; }
        return tableModel.getSessionAt(table.convertRowIndexToModel(row));
    }

    private List<SessionRecord> getSelectedSessions() {
        int[] rows = table.getSelectedRows();
        List<SessionRecord> list = new ArrayList<>();
        for (int r : rows) {
            SessionRecord s = tableModel.getSessionAt(table.convertRowIndexToModel(r));
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
                refreshAll();
            } catch (Exception ex) { showError("重命名失败", ex); }
        }
    }

    private void copySelected() {
        List<SessionRecord> selected = getSelectedSessions();
        if (selected.isEmpty()) { showWarning("请先选择要复制的会话"); return; }
        if (JOptionPane.showConfirmDialog(this, "确定要复制 " + selected.size() + " 个会话吗？\n（包含所有消息和部件）",
                "确认复制", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        try {
            for (SessionRecord s : selected) db.copySession(s.id);
            refreshAll();
            showInfo("成功复制 " + selected.size() + " 个会话");
        } catch (Exception ex) { showError("复制失败", ex); }
    }

    private void moveSelected() {
        SessionRecord s = getSelectedSession();
        if (s == null) return;
        String dir = JOptionPane.showInputDialog(this, "新目录路径:", s.directory);
        if (dir == null || dir.trim().isEmpty()) return;
        dir = dir.trim();
        try {
            List<ProjectRecord> projects = db.listProjects();
            String targetProjectId = "global";
            for (ProjectRecord p : projects)
                if (dir.startsWith(p.worktree)) { targetProjectId = p.id; break; }
            db.moveSession(s.id, dir, targetProjectId);
            refreshAll();
            showInfo("已移动到:\n  " + dir);
        } catch (Exception ex) { showError("移动失败", ex); }
    }

    private void deleteSelected() {
        List<SessionRecord> selected = getSelectedSessions();
        if (selected.isEmpty()) { showWarning("请先选择要删除的会话"); return; }
        StringBuilder msg = new StringBuilder("确定要永久删除以下 " + selected.size() + " 个会话吗？\n\n");
        for (SessionRecord s : selected)
            msg.append("  \u2022 ").append(s.title).append(" (").append(s.messageCount).append(" 条消息)\n");
        msg.append("\n此操作不可撤销！");
        JCheckBox backupCheck = new JCheckBox("删除前备份数据库", true);
        int n = JOptionPane.showOptionDialog(this, new Object[]{msg.toString(), backupCheck},
            "确认删除", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE,
            null, new Object[]{"确认删除", "取消"}, "取消");
        if (n != 0) return;
        try {
            if (backupCheck.isSelected()) db.backupDatabase();
            for (SessionRecord s : selected) db.deleteSession(s.id);
            refreshAll();
            showInfo("成功删除 " + selected.size() + " 个会话");
        } catch (Exception ex) { showError("删除失败", ex); }
    }

    private void openDirSelected() {
        SessionRecord s = getSelectedSession();
        if (s == null) return;
        if (s.directory == null || s.directory.isEmpty()) { showWarning("该会话没有关联目录"); return; }
        try {
            File dir = new File(s.directory);
            if (!dir.exists()) { showWarning("目录不存在: " + s.directory); return; }
            Desktop.getDesktop().open(dir);
        } catch (Exception ex) { showError("打开目录失败", ex); }
    }

    private void copyIdSelected() {
        SessionRecord s = getSelectedSession();
        if (s == null) return;
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(s.id), null);
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
        } catch (Exception ex) { showError("备份失败", ex); }
    }

    private void showWarning(String msg) { JOptionPane.showMessageDialog(this, msg, "提示", JOptionPane.WARNING_MESSAGE); }
    private void showInfo(String msg) { JOptionPane.showMessageDialog(this, msg, "信息", JOptionPane.INFORMATION_MESSAGE); }
    private void showError(String title, Exception ex) {
        JOptionPane.showMessageDialog(this, title + ":\n" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
    }
}
