package opencode.manager.ui;

import opencode.manager.db.SessionRecord;
import javax.swing.table.AbstractTableModel;
import java.util.List;

public class SessionTableModel extends AbstractTableModel {
    private List<SessionRecord> sessions;

    private static final String[] COLUMNS = {"", "标题", "目录", "消息", "费用", "Tokens", "更新时间", "Agent", "归档"};
    private static final Class<?>[] TYPES = {String.class, String.class, String.class, Integer.class, String.class, String.class, String.class, String.class, String.class};

    public void setSessions(List<SessionRecord> sessions) {
        this.sessions = sessions;
        fireTableDataChanged();
    }

    public SessionRecord getSessionAt(int modelRow) {
        if (sessions == null || modelRow < 0 || modelRow >= sessions.size()) return null;
        return sessions.get(modelRow);
    }

    @Override
    public int getRowCount() {
        return sessions == null ? 0 : sessions.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMNS.length;
    }

    @Override
    public String getColumnName(int col) {
        return COLUMNS[col];
    }

    @Override
    public Class<?> getColumnClass(int col) {
        return TYPES[col];
    }

    @Override
    public Object getValueAt(int row, int col) {
        SessionRecord s = sessions.get(row);
        switch (col) {
            case 0: return "";
            case 1: return s.title;
            case 2: return s.getDirectoryShort();
            case 3: return s.messageCount;
            case 4: return s.getCostFormatted();
            case 5: return s.getTokensFormatted();
            case 6: return s.getUpdatedFormatted();
            case 7: return s.agent != null ? s.agent : "-";
            case 8: return s.isArchived() ? "📦" : "";
            default: return "";
        }
    }

    @Override
    public boolean isCellEditable(int row, int col) {
        return false;
    }

    public int getProjectGroupStart(String projectName) {
        if (sessions == null) return -1;
        for (int i = 0; i < sessions.size(); i++) {
            if (sessions.get(i).getProjectDisplay().equals(projectName)) return i;
        }
        return -1;
    }
}
