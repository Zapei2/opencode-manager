package opencode.manager.db;

public class ProjectRecord {
    public String id;
    public String worktree;
    public String name;
    public String vcs;
    public long timeCreated;
    public long timeUpdated;

    public String getDisplayName() {
        if (name != null && !name.isEmpty()) return name;
        if (worktree != null && !worktree.isEmpty()) {
            String s = worktree;
            int idx = s.lastIndexOf('/');
            return idx >= 0 ? s.substring(idx + 1) : s;
        }
        return id != null ? id : "(unknown)";
    }
}
