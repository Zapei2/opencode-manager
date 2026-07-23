package opencode.manager.ui;

import opencode.manager.db.Database;
import opencode.manager.db.ProjectRecord;
import opencode.manager.db.SessionRecord;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
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

    private static final Color BG = new Color(248, 250, 252);
    private static final Color SIDEBAR_BG = Color.WHITE;
    private static final Color ACCENT = new Color(99, 102, 241);
    private static final Color TEXT_PRIMARY = new Color(30, 41, 59);
    private static final Color TEXT_SECONDARY = new Color(100, 116, 139);
    private static final Color BORDER = new Color(226, 232, 240);
    private static final Color ROW_ALT = new Color(248, 250, 252);
    private static final Color SELECT_BG = new Color(238, 242, 255);

    static class DirNode {
        String name, fullPath;
        boolean hasSessions;
        int sessionCount;
        DirNode(String name, String fullPath, boolean hasSessions, int sessionCount) {
            this.name = name; this.fullPath = fullPath;
            this.hasSessions = hasSessions; this.sessionCount = sessionCount;
        }
    }

    public MainWindow(Database db) {
        super("OpenCode 会话管理器");
        this.db = db;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1320, 800);
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG);

        tableModel = new SessionTableModel();
        table = new JTable(tableModel);
        setupTable();

        treeRoot = new DefaultMutableTreeNode("所有会话");
        treeModel = new DefaultTreeModel(treeRoot);
        tree = new JTree(treeModel);
        setupTree();

        searchField = new JTextField();
        searchField.putClientProperty("JTextField.placeholderText", "搜索会话...");
        searchField.putClientProperty("JTextField.trailingIcon", "\uD83D\uDD0D");
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { applyFilter(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { applyFilter(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { applyFilter(); }
        });

        showArchived = new JCheckBox("显示已归档");
        showArchived.setFont(new Font("SansSerif", Font.PLAIN, 12));
        showArchived.setOpaque(false);
        showArchived.addActionListener(e -> refreshAll());

        statusLabel = new JLabel(" ");
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        statusLabel.setForeground(TEXT_SECONDARY);
        statusLabel.setBorder(new EmptyBorder(6, 14, 6, 14));

        setupLayout();
        refreshAll();
    }

    // ======================== TABLE ========================

    private void setupTable() {
        table.setFont(new Font("SansSerif", Font.PLAIN, 13));
        table.setForeground(TEXT_PRIMARY);
        table.setRowHeight(36);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setFillsViewportHeight(true);
        table.setAutoCreateRowSorter(true);
        table.getTableHeader().setPreferredSize(new Dimension(0, 30));

        int[] widths = {30, 280, 180, 55, 80, 80, 140, 65, 50};
        for (int i = 0; i < widths.length; i++)
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);

        table.getColumnModel().getColumn(0).setMaxWidth(35);
        table.getColumnModel().getColumn(8).setMaxWidth(55);

        table.setDefaultRenderer(Object.class, new TableRenderer());
        table.getTableHeader().setDefaultRenderer(new HeaderRenderer());

        // Ctrl+A → select all
        table.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("ctrl A"), "selectAllSessions");
        table.getActionMap().put("selectAllSessions", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { table.selectAll(); }
        });

        setupTablePopup();
    }

    private void setupTablePopup() {
        JPopupMenu popup = new JPopupMenu();
        popup.add(popupItem("重命名", e -> renameSelected()));
        popup.add(popupItem("复制", e -> copySelected()));
        popup.add(popupItem("移动", e -> moveSelected()));
        popup.addSeparator();
        popup.add(popupItem("删除", e -> deleteSelected()));
        popup.addSeparator();
        popup.add(popupItem("打开目录", e -> openDirSelected()));
        popup.add(popupItem("复制会话 ID", e -> copyIdSelected()));
        popup.add(popupItem("查看详情", e -> showDetailSelected()));

        table.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) { maybePopup(e); }
            public void mouseReleased(MouseEvent e) {
                maybePopup(e);
                if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1) {
                    int row = table.rowAtPoint(e.getPoint());
                    if (row >= 0) { table.setRowSelectionInterval(row, row); renameSelected(); }
                }
            }
            private void maybePopup(MouseEvent e) {
                if (!e.isPopupTrigger()) return;
                int row = table.rowAtPoint(e.getPoint());
                if (row >= 0) {
                    if (!table.isRowSelected(row)) table.setRowSelectionInterval(row, row);
                    popup.show(table, e.getX(), e.getY());
                }
            }
        });
    }

    private JMenuItem popupItem(String text, ActionListener l) {
        JMenuItem item = new JMenuItem(text);
        item.setFont(new Font("SansSerif", Font.PLAIN, 13));
        item.addActionListener(l);
        return item;
    }

    // ======================== TABLE RENDERER ========================

    private class TableRenderer extends DefaultTableCellRenderer {
        private final JLabel label = new JLabel();
        {
            label.setOpaque(true);
            label.setBorder(new EmptyBorder(0, 8, 0, 8));
            label.setFont(new Font("SansSerif", Font.PLAIN, 13));
        }
        public Component getTableCellRendererComponent(JTable t, Object value, boolean sel, boolean focus, int row, int col) {
            int modelRow = t.convertRowIndexToModel(row);
            SessionRecord s = tableModel.getSessionAt(modelRow);
            boolean archived = s != null && s.isArchived();

            label.setFont(new Font("SansSerif", col == 1 ? Font.PLAIN : Font.PLAIN, col == 1 ? 13 : 12));
            label.setForeground(TEXT_PRIMARY);

            if (sel) {
                label.setBackground(SELECT_BG);
                label.setForeground(TEXT_PRIMARY);
            } else if (archived) {
                label.setBackground(new Color(250, 250, 250));
            } else {
                label.setBackground(row % 2 == 0 ? Color.WHITE : ROW_ALT);
            }

            if (col == 0) {
                label.setText("");
                label.setHorizontalAlignment(SwingConstants.CENTER);
            } else if (col == 1) {
                String icon = archived ? "\uD83D\uDCE6 " : "\uD83D\uDCDD ";
                label.setText(icon + (value != null ? value.toString() : ""));
                label.setHorizontalAlignment(SwingConstants.LEFT);
            } else if (col == 2) {
                String d = value != null ? value.toString() : "";
                label.setText(d);
                label.setForeground(TEXT_SECONDARY);
                label.setHorizontalAlignment(SwingConstants.LEFT);
            } else if (col == 3 || col == 4 || col == 5) {
                label.setText(value != null ? value.toString() : "");
                label.setHorizontalAlignment(SwingConstants.RIGHT);
            } else if (col == 7) {
                label.setText(value != null ? value.toString() : "");
                label.setHorizontalAlignment(SwingConstants.CENTER);
            } else if (col == 8) {
                label.setText(archived ? "\uD83D\uDCE6" : "");
                label.setHorizontalAlignment(SwingConstants.CENTER);
            } else {
                label.setText(value != null ? value.toString() : "");
                label.setHorizontalAlignment(SwingConstants.LEFT);
            }
            return label;
        }
    }

    private class HeaderRenderer extends DefaultTableCellRenderer {
        private final JLabel label = new JLabel();
        {
            label.setOpaque(true);
            label.setBackground(BG);
            label.setForeground(TEXT_SECONDARY);
            label.setFont(new Font("SansSerif", Font.BOLD, 11));
            label.setBorder(BorderFactory.createCompoundBorder(
                new EmptyBorder(0, 8, 0, 8),
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER)));
        }
        public Component getTableCellRendererComponent(JTable t, Object value, boolean sel, boolean focus, int row, int col) {
            label.setText(value != null ? value.toString() : "");
            return label;
        }
    }

    // ======================== TREE ========================

    private void setupTree() {
        tree.setRootVisible(true);
        tree.setShowsRootHandles(true);
        tree.setRowHeight(28);
        tree.setBorder(new EmptyBorder(6, 0, 6, 0));
        tree.setToggleClickCount(-1);
        tree.setScrollsOnExpand(true);
        tree.setBackground(SIDEBAR_BG);
        tree.setFont(new Font("SansSerif", Font.PLAIN, 13));

        tree.setCellRenderer(new DefaultTreeCellRenderer() {
            {
                setBorder(new EmptyBorder(2, 4, 2, 8));
                setFont(new Font("SansSerif", Font.PLAIN, 13));
                setLeafIcon(null);
                setOpenIcon(null);
                setClosedIcon(null);
            }
            public Component getTreeCellRendererComponent(JTree t, Object value, boolean sel, boolean expanded,
                                                           boolean leaf, int row, boolean hasFocus) {
                super.getTreeCellRendererComponent(t, value, sel, expanded, leaf, row, hasFocus);
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
                Object obj = node.getUserObject();

                if (node == treeRoot) {
                    int n = allSessions != null ? allSessions.size() : 0;
                    setText("\uD83D\uDCC1  所有会话  " + n);
                } else if (obj instanceof DirNode d) {
                    String icon = expanded ? "\uD83D\uDCC2" : "\uD83D\uDCC1";
                    setText(icon + "  " + d.name + (d.sessionCount > 0 ? "  " + d.sessionCount : ""));
                } else {
                    setText(String.valueOf(obj));
                }
                return this;
            }
        });

        tree.addTreeSelectionListener(e -> {
            if (updatingTree) return;
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
            if (node == null) return;
            Object obj = node.getUserObject();
            if (node == treeRoot) {
                selectedDirPrefix = null;
            } else if (obj instanceof DirNode d) {
                selectedDirPrefix = d.fullPath;
            }
            selectedSession = null;
            applyFilter();
        });
    }

    // ======================== LAYOUT ========================

    private void setupLayout() {
        // Top toolbar
        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setBackground(BG);
        toolbar.setBorder(new EmptyBorder(8, 12, 6, 12));

        JPanel leftTool = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        leftTool.setOpaque(false);
        searchField.setPreferredSize(new Dimension(240, 30));
        searchField.setFont(new Font("SansSerif", Font.PLAIN, 12));
        leftTool.add(searchField);
        leftTool.add(Box.createHorizontalStrut(10));
        leftTool.add(showArchived);
        toolbar.add(leftTool, BorderLayout.WEST);

        JPanel rightTool = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        rightTool.setOpaque(false);
        JButton selectAllBtn = miniBtn("全选", e -> table.selectAll());
        JButton deselectBtn = miniBtn("取消选择", e -> table.clearSelection());
        rightTool.add(selectAllBtn);
        rightTool.add(deselectBtn);
        toolbar.add(rightTool, BorderLayout.EAST);

        // Action bar
        JPanel actionBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 6));
        actionBar.setBackground(Color.WHITE);
        actionBar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER),
            new EmptyBorder(0, 8, 0, 8)));
        actionBar.add(actionBtn("✏  重命名", e -> renameSelected()));
        actionBar.add(actionBtn("📋  复制", e -> copySelected()));
        actionBar.add(actionBtn("📦  移动", e -> moveSelected()));
        actionBar.add(actionBtn("🗑  删除", e -> deleteSelected()));
        actionBar.add(Box.createHorizontalStrut(12));
        actionBar.add(new JSeparator(JSeparator.VERTICAL) {{ setPreferredSize(new Dimension(1, 22)); setForeground(BORDER); }});
        actionBar.add(Box.createHorizontalStrut(8));
        actionBar.add(actionBtn("🔄  刷新", e -> refreshAll()));
        actionBar.add(actionBtn("💾  备份", e -> backupDatabase()));

        // Sidebar
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(SIDEBAR_BG);
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, BORDER));

        JLabel sideTitle = new JLabel("\uD83D\uDCC1  目录");
        sideTitle.setFont(new Font("SansSerif", Font.BOLD, 12));
        sideTitle.setForeground(TEXT_SECONDARY);
        sideTitle.setBorder(new EmptyBorder(10, 12, 6, 12));
        sidebar.add(sideTitle, BorderLayout.NORTH);

        JScrollPane treeScroll = new JScrollPane(tree);
        treeScroll.setBorder(null);
        treeScroll.setBackground(SIDEBAR_BG);
        sidebar.add(treeScroll, BorderLayout.CENTER);

        sidebar.setPreferredSize(new Dimension(250, 0));
        sidebar.setMinimumSize(new Dimension(180, 0));

        // Table area
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBackground(Color.WHITE);
        tablePanel.add(actionBar, BorderLayout.NORTH);
        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBorder(null);
        tableScroll.setBackground(Color.WHITE);
        tablePanel.add(tableScroll, BorderLayout.CENTER);

        // Split
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, sidebar, tablePanel);
        split.setDividerSize(4);
        split.setResizeWeight(0.18);
        split.setBorder(null);

        // Status bar
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBackground(BG);
        statusBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER));
        statusBar.add(statusLabel, BorderLayout.WEST);
        JLabel hint = new JLabel("Ctrl+A 全选  |  双击重命名  |  右键菜单");
        hint.setFont(new Font("SansSerif", Font.PLAIN, 11));
        hint.setForeground(TEXT_SECONDARY);
        hint.setBorder(new EmptyBorder(6, 0, 6, 14));
        statusBar.add(hint, BorderLayout.EAST);

        getContentPane().add(toolbar, BorderLayout.NORTH);
        getContentPane().add(split, BorderLayout.CENTER);
        getContentPane().add(statusBar, BorderLayout.SOUTH);
    }

    private JButton actionBtn(String text, ActionListener l) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("SansSerif", Font.PLAIN, 12));
        btn.setForeground(TEXT_PRIMARY);
        btn.setFocusPainted(false);
        btn.addActionListener(l);
        return btn;
    }

    private JButton miniBtn(String text, ActionListener l) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("SansSerif", Font.PLAIN, 11));
        btn.setForeground(ACCENT);
        btn.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(l);
        return btn;
    }

    // ======================== DATA ========================

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
        Map<String, List<SessionRecord>> byDir = new LinkedHashMap<>();
        for (SessionRecord s : allSessions) {
            if (!showArchived.isSelected() && s.isArchived()) continue;
            String dir = (s.directory != null && !s.directory.isEmpty()) ? s.directory : null;
            if (dir != null) byDir.computeIfAbsent(dir, k -> new ArrayList<>()).add(s);
        }

        Map<String, DefaultMutableTreeNode> pathNodes = new LinkedHashMap<>();
        pathNodes.put("", treeRoot);
        List<String> sortedDirs = new ArrayList<>(byDir.keySet());
        Collections.sort(sortedDirs);

        // Pass 1: Build directory nodes only (no sessions)
        for (String fullDir : sortedDirs) {
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
                    String segFullPath;
                    if (curRelStr.equals("~")) segFullPath = home;
                    else if (curRelStr.startsWith("~/")) segFullPath = home + "/" + curRelStr.substring(2);
                    else segFullPath = curRelStr;

                    boolean isLeaf = (i == segments.length - 1);
                    DirNode dn = new DirNode(seg, segFullPath, isLeaf, 0);
                    node = new DefaultMutableTreeNode(dn);
                    parent.add(node);
                    pathNodes.put(curRelStr, node);
                }
                parent = node;
            }
        }

        // Pass 2: Set session counts from data (no session nodes in tree)
        for (String fullDir : sortedDirs) {
            String relPath = fullDir.startsWith(home) ? "~" + fullDir.substring(home.length()) : fullDir;
            if (relPath.endsWith("/")) relPath = relPath.substring(0, relPath.length() - 1);
            DefaultMutableTreeNode leafNode = pathNodes.get(relPath);
            if (leafNode != null && leafNode.getUserObject() instanceof DirNode dn) {
                dn.sessionCount = byDir.get(fullDir).size();
            }
        }

        // Propagate counts upward for intermediate directories
        updateDirCounts(treeRoot);

        treeModel.reload();
        expandVisible(tree, new TreePath(treeRoot));
        updatingTree = false;
    }

    private int updateDirCounts(DefaultMutableTreeNode node) {
        int total = 0;
        for (int i = 0; i < node.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
            if (child.getUserObject() instanceof DirNode dn) {
                total += dn.hasSessions ? dn.sessionCount : updateDirCounts(child);
            }
        }
        if (node != treeRoot && node.getUserObject() instanceof DirNode dn && !dn.hasSessions)
            dn.sessionCount = total;
        return total;
    }

    private void expandVisible(JTree tree, TreePath parent) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) parent.getLastPathComponent();
        if (node.getChildCount() > 0) {
            tree.expandPath(parent);
            for (int i = 0; i < node.getChildCount(); i++) {
                DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
                if (child.getUserObject() instanceof DirNode)
                    expandVisible(tree, parent.pathByAddingChild(child));
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
        int selected = table.getSelectedRowCount();
        String dir = selectedDirPrefix;
        String home = System.getProperty("user.home");
        String dirShort = dir != null ? (dir.startsWith(home) ? "~" + dir.substring(home.length()) : dir) : "所有目录";
        String selInfo = selected > 0 ? "  |  已选 " + selected + " 项" : "";
        statusLabel.setText(dirShort + "  |  " + shown + " 个会话" + (total != shown ? "  (共 " + total + ")" : "") + selInfo);
    }

    // ======================== ACTIONS ========================

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
            try { db.renameSession(s.id, newTitle.trim()); refreshAll(); }
            catch (Exception ex) { showError("重命名失败", ex); }
        }
    }

    private void copySelected() {
        List<SessionRecord> selected = getSelectedSessions();
        if (selected.isEmpty()) { showWarning("请先选择要复制的会话"); return; }
        if (JOptionPane.showConfirmDialog(this,
                "确定要复制 " + selected.size() + " 个会话吗？\n（包含所有消息和部件）",
                "确认复制", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        try {
            for (SessionRecord s : selected) db.copySession(s.id);
            refreshAll();
            showInfo("成功复制 " + selected.size() + " 个会话");
        } catch (Exception ex) { showError("复制失败", ex); }
    }

    private void moveSelected() {
        List<SessionRecord> selected = getSelectedSessions();
        if (selected.isEmpty()) { showWarning("请先选择要移动的会话"); return; }
        SessionRecord first = selected.get(0);
        String dir = JOptionPane.showInputDialog(this, "新目录路径（将应用到所有选中的会话）:", first.directory);
        if (dir == null || dir.trim().isEmpty()) return;
        dir = dir.trim();
        try {
            List<ProjectRecord> projects = db.listProjects();
            String targetProjectId = "global";
            for (ProjectRecord p : projects)
                if (dir.startsWith(p.worktree)) { targetProjectId = p.id; break; }
            for (SessionRecord s : selected) db.moveSession(s.id, dir, targetProjectId);
            refreshAll();
            showInfo("已移动 " + selected.size() + " 个会话到:\n  " + dir);
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

    // ======================== DIALOGS ========================

    private void showWarning(String msg) { JOptionPane.showMessageDialog(this, msg, "提示", JOptionPane.WARNING_MESSAGE); }
    private void showInfo(String msg) { JOptionPane.showMessageDialog(this, msg, "信息", JOptionPane.INFORMATION_MESSAGE); }
    private void showError(String title, Exception ex) {
        JOptionPane.showMessageDialog(this, title + ":\n" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
    }
}
