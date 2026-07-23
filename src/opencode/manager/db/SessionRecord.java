package opencode.manager.db;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class SessionRecord {
    public String id;
    public String projectId;
    public String workspaceId;
    public String parentId;
    public String slug;
    public String directory;
    public String title;
    public String version;
    public double cost;
    public int tokensInput;
    public int tokensOutput;
    public int tokensReasoning;
    public int tokensCacheRead;
    public int tokensCacheWrite;
    public long timeCreated;
    public long timeUpdated;
    public long timeCompacting;
    public long timeArchived;
    public String agent;
    public String model;
    public int messageCount;
    public String projectName;
    public String projectWorktree;

    public String getCreatedFormatted() {
        return formatEpochMs(timeCreated);
    }

    public String getUpdatedFormatted() {
        return formatEpochMs(timeUpdated);
    }

    public String getCostFormatted() {
        return String.format("%.6f", cost);
    }

    public String getTokensFormatted() {
        long total = (long)tokensInput + tokensOutput + tokensReasoning + tokensCacheRead + tokensCacheWrite;
        if (total < 1000) return total + " tok";
        if (total < 1_000_000) return String.format("%.1fK", total / 1000.0);
        return String.format("%.1fM", total / 1_000_000.0);
    }

    public String getProjectDisplay() {
        if (projectName != null && !projectName.isEmpty()) return projectName;
        if (projectWorktree != null && !projectWorktree.isEmpty()) {
            String s = projectWorktree;
            int idx = s.lastIndexOf('/');
            return idx >= 0 ? s.substring(idx + 1) : s;
        }
        return "(global)";
    }

    public String getDirectoryShort() {
        if (directory == null || directory.isEmpty()) return "";
        String home = System.getProperty("user.home");
        if (directory.startsWith(home)) return "~" + directory.substring(home.length());
        return directory;
    }

    private static final DateTimeFormatter FMT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

    private static String formatEpochMs(long ms) {
        if (ms == 0) return "";
        return FMT.format(Instant.ofEpochMilli(ms));
    }

    public boolean isArchived() {
        return timeArchived > 0;
    }
}
