package opencode.manager.db;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.SecureRandom;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class Database implements AutoCloseable {
    private final Connection conn;
    private final Path dbPath;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final char[] HEX = "0123456789abcdef".toCharArray();

    public Database() throws Exception {
        String home = System.getProperty("user.home");
        Path primary = Path.of(home, ".local", "share", "opencode", "opencode.db");
        Path alt = Path.of(home, ".local", "share", "opencode", "opencode-master.db");
        if (Files.exists(primary)) {
            dbPath = primary;
        } else if (Files.exists(alt)) {
            dbPath = alt;
        } else {
            throw new Exception("找不到 opencode 数据库:\n  " + primary + "\n  " + alt);
        }
        conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath.toAbsolutePath() + "?journal_mode=WAL&busy_timeout=8000");
        try (Statement st = conn.createStatement()) {
            st.execute("PRAGMA foreign_keys=ON");
        }
    }

    public Path getDbPath() { return dbPath; }

    public List<ProjectRecord> listProjects() throws Exception {
        List<ProjectRecord> list = new ArrayList<>();
        String sql = "SELECT id, worktree, name, vcs, time_created, time_updated FROM project ORDER BY time_updated DESC";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                ProjectRecord p = new ProjectRecord();
                p.id = rs.getString("id");
                p.worktree = rs.getString("worktree");
                p.name = rs.getString("name");
                p.vcs = rs.getString("vcs");
                p.timeCreated = rs.getLong("time_created");
                p.timeUpdated = rs.getLong("time_updated");
                list.add(p);
            }
        }
        return list;
    }

    public List<SessionRecord> listSessions() throws Exception {
        List<SessionRecord> list = new ArrayList<>();
        String sql = """
            SELECT s.id, s.project_id, s.workspace_id, s.parent_id, s.slug, s.directory,
                   s.title, s.version, s.cost, s.tokens_input, s.tokens_output,
                   s.tokens_reasoning, s.tokens_cache_read, s.tokens_cache_write,
                   s.time_created, s.time_updated, s.time_compacting, s.time_archived,
                   s.agent, s.model,
                   (SELECT COUNT(*) FROM message m WHERE m.session_id = s.id) AS msg_count,
                   p.name AS project_name, p.worktree AS project_worktree
            FROM session s
            LEFT JOIN project p ON p.id = s.project_id
            ORDER BY s.time_updated DESC
            """;
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                SessionRecord s = new SessionRecord();
                s.id = rs.getString("id");
                s.projectId = rs.getString("project_id");
                s.workspaceId = rs.getString("workspace_id");
                s.parentId = rs.getString("parent_id");
                s.slug = rs.getString("slug");
                s.directory = rs.getString("directory");
                s.title = rs.getString("title");
                s.version = rs.getString("version");
                s.cost = rs.getDouble("cost");
                s.tokensInput = rs.getInt("tokens_input");
                s.tokensOutput = rs.getInt("tokens_output");
                s.tokensReasoning = rs.getInt("tokens_reasoning");
                s.tokensCacheRead = rs.getInt("tokens_cache_read");
                s.tokensCacheWrite = rs.getInt("tokens_cache_write");
                s.timeCreated = rs.getLong("time_created");
                s.timeUpdated = rs.getLong("time_updated");
                s.timeCompacting = rs.getLong("time_compacting");
                s.timeArchived = rs.getLong("time_archived");
                s.agent = rs.getString("agent");
                s.model = rs.getString("model");
                s.messageCount = rs.getInt("msg_count");
                s.projectName = rs.getString("project_name");
                s.projectWorktree = rs.getString("project_worktree");
                list.add(s);
            }
        }
        return list;
    }

    public List<SessionRecord> listSessionsByProject(String projectId) throws Exception {
        List<SessionRecord> all = listSessions();
        List<SessionRecord> filtered = new ArrayList<>();
        for (SessionRecord s : all) {
            if (projectId.equals(s.projectId)) filtered.add(s);
        }
        return filtered;
    }

    public void renameSession(String sessionId, String newTitle) throws Exception {
        String sql = "UPDATE session SET title = ?, time_updated = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newTitle);
            ps.setLong(2, Instant.now().toEpochMilli());
            ps.setString(3, sessionId);
            ps.executeUpdate();
        }
    }

    public void moveSession(String sessionId, String newDirectory, String newProjectId) throws Exception {
        String sql = "UPDATE session SET directory = ?, project_id = ?, time_updated = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newDirectory);
            ps.setString(2, newProjectId);
            ps.setLong(3, Instant.now().toEpochMilli());
            ps.setString(4, sessionId);
            ps.executeUpdate();
        }
    }

    public String copySession(String sessionId) throws Exception {
        String newId = generateId("ses_");
        String now = String.valueOf(Instant.now().toEpochMilli());

        String select = "SELECT * FROM session WHERE id = ?";
        String insert = """
            INSERT INTO session (id, project_id, workspace_id, parent_id, slug, directory, path,
                title, version, share_url, summary_additions, summary_deletions, summary_files,
                summary_diffs, metadata, cost, tokens_input, tokens_output, tokens_reasoning,
                tokens_cache_read, tokens_cache_write, revert, permission, agent, model,
                time_created, time_updated, time_compacting, time_archived)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
            """;

        conn.setAutoCommit(false);
        try (PreparedStatement sel = conn.prepareStatement(select);
             PreparedStatement ins = conn.prepareStatement(insert)) {
            sel.setString(1, sessionId);
            ResultSet rs = sel.executeQuery();
            if (!rs.next()) throw new Exception("会话不存在: " + sessionId);

            ins.setString(1, newId);
            ins.setString(2, rs.getString("project_id"));
            ins.setString(3, rs.getString("workspace_id"));
            ins.setString(4, rs.getString("parent_id"));
            ins.setString(5, rs.getString("slug") + "-copy");
            ins.setString(6, rs.getString("directory"));
            ins.setString(7, rs.getString("path"));
            String oldTitle = rs.getString("title");
            ins.setString(8, oldTitle != null ? oldTitle + " (副本)" : "(副本)");
            ins.setString(9, rs.getString("version"));
            ins.setString(10, rs.getString("share_url"));
            ins.setInt(11, rs.getInt("summary_additions"));
            ins.setInt(12, rs.getInt("summary_deletions"));
            ins.setInt(13, rs.getInt("summary_files"));
            ins.setString(14, rs.getString("summary_diffs"));
            ins.setString(15, rs.getString("metadata"));
            ins.setDouble(16, rs.getDouble("cost"));
            ins.setInt(17, rs.getInt("tokens_input"));
            ins.setInt(18, rs.getInt("tokens_output"));
            ins.setInt(19, rs.getInt("tokens_reasoning"));
            ins.setInt(20, rs.getInt("tokens_cache_read"));
            ins.setInt(21, rs.getInt("tokens_cache_write"));
            ins.setString(22, rs.getString("revert"));
            ins.setString(23, rs.getString("permission"));
            ins.setString(24, rs.getString("agent"));
            ins.setString(25, rs.getString("model"));
            ins.setString(26, now);
            ins.setString(27, now);
            ins.setString(28, "0");
            ins.setString(29, "0");
            ins.executeUpdate();
            rs.close();

            copyMessages(sessionId, newId, now);
            conn.commit();

            copySessionDiffFile(sessionId, newId);
        } catch (Exception e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
        return newId;
    }

    private void copyMessages(String oldSessionId, String newSessionId, String now) throws Exception {
        String selMsg = "SELECT * FROM message WHERE session_id = ? ORDER BY time_created";
        String insMsg = "INSERT INTO message (id, session_id, time_created, time_updated, data) VALUES (?,?,?,?,?)";
        String selPart = "SELECT * FROM part WHERE message_id = ?";
        String insPart = "INSERT INTO part (id, message_id, session_id, time_created, time_updated, data) VALUES (?,?,?,?,?,?)";

        try (PreparedStatement selMsgPs = conn.prepareStatement(selMsg);
             PreparedStatement insMsgPs = conn.prepareStatement(insMsg);
             PreparedStatement selPartPs = conn.prepareStatement(selPart);
             PreparedStatement insPartPs = conn.prepareStatement(insPart)) {

            selMsgPs.setString(1, oldSessionId);
            ResultSet msgRs = selMsgPs.executeQuery();

            while (msgRs.next()) {
                String newMsgId = generateId("msg_");
                insMsgPs.setString(1, newMsgId);
                insMsgPs.setString(2, newSessionId);
                insMsgPs.setString(3, now);
                insMsgPs.setString(4, now);
                insMsgPs.setString(5, msgRs.getString("data"));
                insMsgPs.executeUpdate();

                selPartPs.setString(1, msgRs.getString("id"));
                ResultSet partRs = selPartPs.executeQuery();
                while (partRs.next()) {
                    String newPartId = generateId("prt_");
                    insPartPs.setString(1, newPartId);
                    insPartPs.setString(2, newMsgId);
                    insPartPs.setString(3, newSessionId);
                    insPartPs.setString(4, now);
                    insPartPs.setString(5, now);
                    insPartPs.setString(6, partRs.getString("data"));
                    insPartPs.executeUpdate();
                }
                partRs.close();
            }
            msgRs.close();
        }
    }

    private void copySessionDiffFile(String oldId, String newId) {
        try {
            Path diffDir = Path.of(System.getProperty("user.home"), ".local", "share", "opencode", "storage", "session_diff");
            Path oldFile = diffDir.resolve(oldId + ".json");
            if (Files.exists(oldFile)) {
                Path newFile = diffDir.resolve(newId + ".json");
                Files.copy(oldFile, newFile, StandardCopyOption.COPY_ATTRIBUTES);
            }
        } catch (Exception ignored) {}
    }

    public void deleteSession(String sessionId) throws Exception {
        conn.setAutoCommit(false);
        try {
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM part WHERE session_id = ?")) {
                ps.setString(1, sessionId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM message WHERE session_id = ?")) {
                ps.setString(1, sessionId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM todo WHERE session_id = ?")) {
                ps.setString(1, sessionId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM session_entry WHERE session_id = ?")) {
                ps.setString(1, sessionId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM session_input WHERE session_id = ?")) {
                ps.setString(1, sessionId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM session WHERE id = ?")) {
                ps.setString(1, sessionId);
                ps.executeUpdate();
            }
            conn.commit();

            try {
                Path diffDir = Path.of(System.getProperty("user.home"), ".local", "share", "opencode", "storage", "session_diff");
                Files.deleteIfExists(diffDir.resolve(sessionId + ".json"));
            } catch (Exception ignored) {}
        } catch (Exception e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    public void backupDatabase() throws Exception {
        Path backup = dbPath.resolveSibling("opencode.db.backup." + Instant.now().toEpochMilli());
        Files.copy(dbPath, backup, StandardCopyOption.COPY_ATTRIBUTES);
        Path wal = dbPath.resolveSibling("opencode.db-wal");
        if (Files.exists(wal)) {
            Files.copy(wal, backup.resolveSibling(backup.getFileName() + "-wal"));
        }
    }

    public static String generateId(String prefix) {
        byte[] bytes = new byte[12];
        RANDOM.nextBytes(bytes);
        char[] hex = new char[24];
        for (int i = 0; i < 12; i++) {
            hex[i * 2] = HEX[(bytes[i] >> 4) & 0xf];
            hex[i * 2 + 1] = HEX[bytes[i] & 0xf];
        }
        return prefix + new String(hex);
    }

    @Override
    public void close() throws Exception {
        if (conn != null && !conn.isClosed()) conn.close();
    }
}
