use rusqlite::{Connection, params};
use std::collections::HashSet;
use std::path::Path;
use std::sync::Mutex;
use chrono::Local;

use crate::models::*;

pub struct Database {
    conn: Mutex<Connection>,
    columns: HashSet<String>,
}

impl Database {
    pub fn open() -> Result<Self, String> {
        let home = dirs_next::home_dir().ok_or("Cannot find home dir")?;
        let data_dir = home.join(".local/share/opencode");

        // Try to get the actual database path from opencode itself.
        // opencode picks the DB file based on installation channel
        // (e.g. "master" -> opencode-master.db), so we must ask it
        // rather than hardcoding "opencode.db".
        if let Ok(output) = std::process::Command::new("opencode")
            .arg("db")
            .arg("path")
            .env_remove("OPENCODE_DISABLE_CHANNEL_DB")
            .output()
        {
            if output.status.success() {
                let path_str = String::from_utf8_lossy(&output.stdout).trim().to_string();
                if !path_str.is_empty() {
                    let p = std::path::Path::new(&path_str);
                    if p.exists() {
                        return Self::open_path(p);
                    }
                }
            }
        }

        // Fallback: try opencode.db, then opencode-master.db
        let db_path = data_dir.join("opencode.db");
        if db_path.exists() {
            return Self::open_path(&db_path);
        }
        let alt = data_dir.join("opencode-master.db");
        if alt.exists() {
            return Self::open_path(&alt);
        }
        Err(format!("Database not found in {}", data_dir.display()))
    }

    fn open_path(path: &Path) -> Result<Self, String> {
        let conn = Connection::open(path).map_err(|e| e.to_string())?;
        conn.execute_batch("PRAGMA journal_mode=WAL; PRAGMA busy_timeout=8000; PRAGMA foreign_keys=ON;")
            .map_err(|e| e.to_string())?;

        // Detect available columns in session table (schemas differ across opencode versions)
        let mut columns = HashSet::new();
        let col_names: Vec<String> = conn
            .prepare("PRAGMA table_info(session)")
            .map_err(|e| e.to_string())?
            .query_map([], |row| row.get::<_, String>(1))
            .map_err(|e| e.to_string())?
            .filter_map(|r| r.ok())
            .collect();
        for c in col_names {
            columns.insert(c);
        }

        Ok(Database { conn: Mutex::new(conn), columns })
    }

    fn has(&self, col: &str) -> bool {
        self.columns.contains(col)
    }

    pub fn list_sessions(&self) -> Result<Vec<SessionSummary>, String> {
        let conn = self.conn.lock().map_err(|e| e.to_string())?;

        // Build query dynamically — older opencode databases lack agent/model/cost/tokens columns
        let agent = if self.has("agent") { "s.agent" } else { "NULL" };
        let model = if self.has("model") { "s.model" } else { "NULL" };
        let cost = if self.has("cost") { "s.cost" } else { "0" };
        let ti = if self.has("tokens_input") { "s.tokens_input" } else { "0" };
        let to = if self.has("tokens_output") { "s.tokens_output" } else { "0" };

        let sql = format!(
            "SELECT s.id, s.title, s.slug, s.directory, {agent}, {model}, {cost},
                    {ti}, {to}, s.time_created, s.time_updated, s.time_archived,
                    (SELECT COUNT(*) FROM message m WHERE m.session_id = s.id) AS msg_count
             FROM session s ORDER BY s.time_updated DESC"
        );

        let mut stmt = conn.prepare(&sql).map_err(|e| e.to_string())?;

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

        // Build column lists dynamically - older opencode DBs lack cost/tokens/agent/model columns
        let base_cols = ["project_id", "slug", "directory", "title", "version"];
        let extra_cols: Vec<&str> = ["cost", "tokens_input", "tokens_output", "tokens_reasoning",
                                     "tokens_cache_read", "tokens_cache_write", "agent", "model"]
            .iter().copied().filter(|c| self.has(c)).collect();
        let all_cols: Vec<&str> = base_cols.iter().copied().chain(extra_cols.iter().copied()).collect();

        // SELECT existing values
        let sel_list = all_cols.join(", ");
        let sel_sql = format!("SELECT {} FROM session WHERE id = ?", sel_list);
        let mut stmt = conn.prepare(&sel_sql).map_err(|e| e.to_string())?;
        let row_vals: Vec<rusqlite::types::Value> = stmt.query_row(params![id], |row| {
            (0..all_cols.len()).map(|i| row.get::<_, rusqlite::types::Value>(i)).collect()
        }).map_err(|e| e.to_string())?;

        let now = chrono::Utc::now().timestamp_millis();
        let title = match &row_vals[3] {
            rusqlite::types::Value::Text(t) => format!("{} (fork)", t),
            _ => "fork".to_string(),
        };

        // Build INSERT: id, <all_cols>, parent_id, time_created, time_updated
        let ins_cols_str = all_cols.join(", ");
        let ph_count = 1 + all_cols.len() + 3; // new_id + cols + parent_id + created + updated
        let placeholders: Vec<String> = (0..ph_count).map(|_| "?".to_string()).collect();
        let ins_sql = format!(
            "INSERT INTO session (id, {}, parent_id, time_created, time_updated) VALUES ({})",
            ins_cols_str, placeholders.join(", ")
        );

        // Build params vector
        let mut params_vec: Vec<rusqlite::types::Value> = Vec::new();
        params_vec.push(rusqlite::types::Value::Text(new_id.clone()));
        for v in &row_vals {
            params_vec.push(v.clone());
        }
        params_vec[4] = rusqlite::types::Value::Text(title); // override title
        params_vec.push(rusqlite::types::Value::Text(id.to_string())); // parent_id
        params_vec.push(rusqlite::types::Value::Integer(now)); // time_created
        params_vec.push(rusqlite::types::Value::Integer(now)); // time_updated

        let params_refs: Vec<&dyn rusqlite::ToSql> = params_vec.iter().map(|v| v as &dyn rusqlite::ToSql).collect();
        conn.execute(&ins_sql, params_refs.as_slice()).map_err(|e| e.to_string())?;

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
        let data_dir = home.join(".local/share/opencode");

        // Determine the actual DB path (same logic as open())
        let db_path = Self::current_db_path(&data_dir)?;
        let ts = chrono::Utc::now().timestamp_millis();
        let backup = db_path.with_file_name(format!("opencode.db.backup.{}", ts));
        std::fs::copy(&db_path, &backup).map_err(|e| e.to_string())?;
        Ok(backup.to_string_lossy().to_string())
    }

    fn current_db_path(data_dir: &Path) -> Result<std::path::PathBuf, String> {
        if let Ok(output) = std::process::Command::new("opencode")
            .arg("db")
            .arg("path")
            .env_remove("OPENCODE_DISABLE_CHANNEL_DB")
            .output()
        {
            if output.status.success() {
                let s = String::from_utf8_lossy(&output.stdout).trim().to_string();
                if !s.is_empty() {
                    let p = std::path::Path::new(&s);
                    if p.exists() {
                        return Ok(p.to_path_buf());
                    }
                }
            }
        }
        let db_path = data_dir.join("opencode.db");
        if db_path.exists() {
            return Ok(db_path);
        }
        let alt = data_dir.join("opencode-master.db");
        if alt.exists() {
            return Ok(alt);
        }
        Err(format!("Database not found in {}", data_dir.display()))
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
