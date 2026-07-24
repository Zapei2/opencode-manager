use rusqlite::{Connection, params};
use std::path::Path;
use std::sync::Mutex;
use chrono::Local;

use crate::models::*;

pub struct Database {
    conn: Mutex<Connection>,
}

impl Database {
    pub fn open() -> Result<Self, String> {
        let home = dirs_next::home_dir().ok_or("Cannot find home dir")?;
        let db_path = home.join(".local/share/opencode/opencode.db");
        if !db_path.exists() {
            let alt = home.join(".local/share/opencode/opencode-master.db");
            if alt.exists() {
                return Self::open_path(&alt);
            }
            return Err(format!("Database not found: {}", db_path.display()));
        }
        Self::open_path(&db_path)
    }

    fn open_path(path: &Path) -> Result<Self, String> {
        let conn = Connection::open(path).map_err(|e| e.to_string())?;
        conn.execute_batch("PRAGMA journal_mode=WAL; PRAGMA busy_timeout=8000; PRAGMA foreign_keys=ON;")
            .map_err(|e| e.to_string())?;
        Ok(Database { conn: Mutex::new(conn) })
    }

    pub fn list_sessions(&self) -> Result<Vec<SessionSummary>, String> {
        let conn = self.conn.lock().map_err(|e| e.to_string())?;
        let mut stmt = conn.prepare(
            "SELECT s.id, s.title, s.slug, s.directory, s.agent, s.model, s.cost,
                    s.tokens_input, s.tokens_output, s.time_created, s.time_updated, s.time_archived,
                    (SELECT COUNT(*) FROM message m WHERE m.session_id = s.id) AS msg_count
             FROM session s ORDER BY s.time_updated DESC"
        ).map_err(|e| e.to_string())?;

        let sessions = stmt.query_map([], |row| {
            let tc: i64 = row.get(9)?;
            let tu: i64 = row.get(10)?;
            let ta: Option<i64> = row.get(11)?;
            Ok(SessionSummary {
                id: row.get(0)?,
                title: row.get(1)?,
                slug: row.get(2)?,
                directory: row.get(3)?,
                agent: row.get(4)?,
                model: row.get(5)?,
                cost: row.get(6)?,
                tokens_input: row.get(7)?,
                tokens_output: row.get(8)?,
                time_created: format_epoch(tc),
                time_updated: format_epoch(tu),
                is_archived: ta.unwrap_or(0) > 0,
                message_count: row.get(12)?,
            })
        }).map_err(|e| e.to_string())?;

        let mut result = Vec::new();
        for s in sessions {
            result.push(s.map_err(|e| e.to_string())?);
        }
        Ok(result)
    }

    pub fn rename_session(&self, id: &str, new_title: &str) -> Result<(), String> {
        let conn = self.conn.lock().map_err(|e| e.to_string())?;
        let now = chrono::Utc::now().timestamp_millis();
        conn.execute(
            "UPDATE session SET title = ?, time_updated = ? WHERE id = ?",
            params![new_title, now, id]
        ).map_err(|e| e.to_string())?;
        Ok(())
    }

    pub fn copy_session(&self, id: &str) -> Result<String, String> {
        let conn = self.conn.lock().map_err(|e| e.to_string())?;
        let new_id = generate_id("ses_");

        // Read original session
        let mut stmt = conn.prepare(
            "SELECT project_id, slug, directory, title, version, cost,
                    tokens_input, tokens_output, tokens_reasoning, tokens_cache_read, tokens_cache_write,
                    agent, model
             FROM session WHERE id = ?"
        ).map_err(|e| e.to_string())?;

        let row = stmt.query_row(params![id], |row| {
            Ok((
                row.get::<_, String>(0)?,
                row.get::<_, String>(1)?,
                row.get::<_, String>(2)?,
                row.get::<_, String>(3)?,
                row.get::<_, String>(4)?,
                row.get::<_, f64>(5)?,
                row.get::<_, i64>(6)?,
                row.get::<_, i64>(7)?,
                row.get::<_, i64>(8)?,
                row.get::<_, i64>(9)?,
                row.get::<_, i64>(10)?,
                row.get::<_, Option<String>>(11)?,
                row.get::<_, Option<String>>(12)?,
            ))
        }).map_err(|e| e.to_string())?;

        let now = chrono::Utc::now().timestamp_millis();
        let title = format!("{} (fork)", &row.3);

        conn.execute(
            "INSERT INTO session (id, project_id, parent_id, slug, directory, title, version,
             cost, tokens_input, tokens_output, tokens_reasoning, tokens_cache_read, tokens_cache_write,
             agent, model, time_created, time_updated)
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            params![new_id, row.0, id, row.1, row.2, title, row.4,
                    row.5, row.6, row.7, row.8, row.9, row.10,
                    row.11, row.12, now, now]
        ).map_err(|e| e.to_string())?;

        // Copy messages
        Self::copy_messages(&conn, id, &new_id, now)?;

        Ok(new_id)
    }

    fn copy_messages(conn: &Connection, old_id: &str, new_id: &str, now: i64) -> Result<(), String> {
        let mut sel = conn.prepare(
            "SELECT id, time_created, time_updated, data FROM message WHERE session_id = ? ORDER BY time_created"
        ).map_err(|e| e.to_string())?;

        let mut ins = conn.prepare(
            "INSERT INTO message (id, session_id, time_created, time_updated, data) VALUES (?, ?, ?, ?, ?)"
        ).map_err(|e| e.to_string())?;

        let msgs: Vec<(String, i64, i64, String)> = sel.query_map(params![old_id], |row| {
            Ok((row.get(0)?, row.get(1)?, row.get(2)?, row.get(3)?))
        }).map_err(|e| e.to_string())?
            .filter_map(|r| r.ok())
            .collect();

        for (msg_id, _tc, _tu, data) in &msgs {
            let new_msg_id = generate_id("msg_");
            ins.execute(params![new_msg_id, new_id, now, now, data]).map_err(|e| e.to_string())?;

            // Copy parts for this message
            Self::copy_parts(conn, msg_id, &new_msg_id, new_id, now)?;
        }
        Ok(())
    }

    fn copy_parts(conn: &Connection, old_msg_id: &str, new_msg_id: &str, new_ses_id: &str, now: i64) -> Result<(), String> {
        let mut sel = conn.prepare(
            "SELECT data FROM part WHERE message_id = ?"
        ).map_err(|e| e.to_string())?;

        let mut ins = conn.prepare(
            "INSERT INTO part (id, message_id, session_id, time_created, time_updated, data) VALUES (?, ?, ?, ?, ?, ?)"
        ).map_err(|e| e.to_string())?;

        let parts: Vec<String> = sel.query_map(params![old_msg_id], |row| {
            row.get(0)
        }).map_err(|e| e.to_string())?
            .filter_map(|r| r.ok())
            .collect();

        for data in &parts {
            let new_part_id = generate_id("prt_");
            ins.execute(params![new_part_id, new_msg_id, new_ses_id, now, now, data])
                .map_err(|e| e.to_string())?;
        }
        Ok(())
    }

    pub fn move_session(&self, id: &str, new_dir: &str) -> Result<(), String> {
        let conn = self.conn.lock().map_err(|e| e.to_string())?;
        let now = chrono::Utc::now().timestamp_millis();
        conn.execute(
            "UPDATE session SET directory = ?, time_updated = ? WHERE id = ?",
            params![new_dir, now, id]
        ).map_err(|e| e.to_string())?;
        Ok(())
    }

    pub fn archive_session(&self, id: &str, archived: bool) -> Result<(), String> {
        let conn = self.conn.lock().map_err(|e| e.to_string())?;
        let now = chrono::Utc::now().timestamp_millis();
        let ta: i64 = if archived { now } else { 0 };
        conn.execute(
            "UPDATE session SET time_archived = ?, time_updated = ? WHERE id = ?",
            params![ta, now, id]
        ).map_err(|e| e.to_string())?;
        Ok(())
    }

    pub fn delete_session(&self, id: &str) -> Result<(), String> {
        let conn = self.conn.lock().map_err(|e| e.to_string())?;
        conn.execute("DELETE FROM session WHERE id = ?", params![id])
            .map_err(|e| e.to_string())?;
        Ok(())
    }

    pub fn backup_database(&self) -> Result<String, String> {
        let home = dirs_next::home_dir().ok_or("Cannot find home dir")?;
        let db_path = home.join(".local/share/opencode/opencode.db");
        let ts = chrono::Utc::now().timestamp_millis();
        let backup = db_path.with_file_name(format!("opencode.db.backup.{}", ts));
        std::fs::copy(&db_path, &backup).map_err(|e| e.to_string())?;
        Ok(backup.to_string_lossy().to_string())
    }
}

fn generate_id(prefix: &str) -> String {
    use rand::Rng;
    let hex: String = (0..24).map(|_| format!("{:x}", rand::thread_rng().gen_range(0..16))).collect();
    format!("{}{}", prefix, hex)
}

fn format_epoch(ms: i64) -> String {
    if ms == 0 { return String::new(); }
    let secs = ms / 1000;
    let nsecs = ((ms % 1000) * 1_000_000) as u32;
    let dt = chrono::DateTime::from_timestamp(secs, nsecs)
        .map(|d| d.with_timezone(&Local).format("%Y-%m-%d %H:%M").to_string())
        .unwrap_or_default();
    dt
}
