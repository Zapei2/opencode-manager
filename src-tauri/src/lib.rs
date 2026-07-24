mod db;
mod models;

use db::Database;
use models::*;
use std::sync::Mutex;
use tauri::State;

struct AppState {
    db: Database,
}

fn home_short(path: &str) -> String {
    if let Some(home) = dirs_next::home_dir() {
        let hs = home.to_string_lossy().to_string();
        path.replace(&hs, "~")
    } else {
        path.to_string()
    }
}

fn resolve_abs(short: &str) -> String {
    if short == "~"  {
        dirs_next::home_dir().map(|h| h.to_string_lossy().to_string()).unwrap_or_default()
    } else if short.starts_with("~/") {
        let home = dirs_next::home_dir().map(|h| h.to_string_lossy().to_string()).unwrap_or_default();
        format!("{}/{}", home, &short[2..])
    } else {
        short.to_string()
    }
}

#[tauri::command]
fn build_tree(state: State<Mutex<AppState>>, show_archived: bool) -> Result<Vec<DirNode>, String> {
    let app = state.lock().map_err(|e| e.to_string())?;
    let all = app.db.list_sessions()?;

    let mut dir_map: std::collections::BTreeMap<String, Vec<&SessionSummary>> = std::collections::BTreeMap::new();
    for s in &all {
        if !show_archived && s.is_archived { continue; }
        let dir = if s.directory.is_empty() { "/" } else { &s.directory };
        dir_map.entry(dir.to_string()).or_default().push(s);
    }

    let mut dir_nodes: Vec<(String, String, String, usize)> = Vec::new(); // (short_path, abs_path, name, count)

    for (dir, sessions) in &dir_map {
        let rel = home_short(dir);
        let segments: Vec<&str> = rel.split('/').filter(|s| !s.is_empty()).collect();
        for (i, seg) in segments.iter().enumerate() {
            let short_path = segments[..=i].join("/");
            let abs_path = resolve_abs(&short_path);
            let count = if i == segments.len() - 1 { sessions.len() } else { 0 };
            if dir_nodes.iter().any(|(sp, _, _, _)| sp == &short_path) { continue; }
            dir_nodes.push((short_path, abs_path, seg.to_string(), count));
        }
    }

    // Build hierarchy
    fn children_of<'a>(nodes: &'a [(String, String, String, usize)], parent_short: &str)
        -> Vec<DirNode>
    {
        nodes.iter()
            .filter(|(sp, _, _, _)| {
                if parent_short.is_empty() { !sp.contains('/') }
                else { sp.starts_with(&format!("{}/", parent_short)) && !sp[parent_short.len()+1..].contains('/') }
            })
            .map(|(sp, ap, name, cnt)| {
                DirNode {
                    name: name.clone(),
                    full_path: ap.clone(),
                    session_count: *cnt,
                    children: children_of(nodes, sp),
                }
            })
            .collect()
    }

    Ok(children_of(&dir_nodes, ""))
}

#[tauri::command]
fn list_sessions(state: State<Mutex<AppState>>, show_archived: bool, dir_prefix: Option<String>) -> Result<Vec<SessionSummary>, String> {
    let app = state.lock().map_err(|e| e.to_string())?;
    let all = app.db.list_sessions()?;
    Ok(all.into_iter().filter(|s| {
        if !show_archived && s.is_archived { return false; }
        if let Some(ref prefix) = dir_prefix {
            if prefix.is_empty() {
                return s.directory.is_empty();
            }
            return s.directory == *prefix;
        }
        true
    }).collect())
}

#[tauri::command]
fn rename_session(state: State<Mutex<AppState>>, id: String, title: String) -> Result<(), String> {
    let app = state.lock().map_err(|e| e.to_string())?;
    app.db.rename_session(&id, &title)
}

#[tauri::command]
fn copy_session(state: State<Mutex<AppState>>, id: String) -> Result<String, String> {
    let app = state.lock().map_err(|e| e.to_string())?;
    app.db.copy_session(&id)
}

#[tauri::command]
fn move_session(state: State<Mutex<AppState>>, ids: Vec<String>, directory: String) -> Result<(), String> {
    let app = state.lock().map_err(|e| e.to_string())?;
    for id in &ids {
        app.db.move_session(id, &directory)?;
    }
    Ok(())
}

#[tauri::command]
fn archive_session(state: State<Mutex<AppState>>, ids: Vec<String>, archived: bool) -> Result<(), String> {
    let app = state.lock().map_err(|e| e.to_string())?;
    for id in &ids {
        app.db.archive_session(id, archived)?;
    }
    Ok(())
}

#[tauri::command]
fn delete_session(state: State<Mutex<AppState>>, ids: Vec<String>, backup: bool) -> Result<(), String> {
    let app = state.lock().map_err(|e| e.to_string())?;
    if backup {
        let _ = app.db.backup_database();
    }
    for id in &ids {
        app.db.delete_session(id)?;
    }
    Ok(())
}

#[tauri::command]
fn backup_database(state: State<Mutex<AppState>>) -> Result<String, String> {
    let app = state.lock().map_err(|e| e.to_string())?;
    app.db.backup_database()
}

#[tauri::command]
fn open_in_terminal(directory: String, terminal: String, slug: String) -> Result<(), String> {
    let dir = if directory.is_empty() {
        dirs_next::home_dir().ok_or("Cannot find home dir")?.to_string_lossy().to_string()
    } else {
        directory
    };
    let cmd_str = format!("opencode-s {}", slug);

    let mut cmd = if cfg!(target_os = "windows") {
        let term = if terminal.is_empty() { "powershell.exe".to_string() } else { terminal };
        let mut c = std::process::Command::new(&term);
        c.current_dir(&dir).arg("-NoExit").arg("-Command").arg(&cmd_str);
        c
    } else {
        // Linux: detect available terminal
        let term = if !terminal.is_empty() {
            terminal.clone()
        } else if std::process::Command::new("which").arg("kitty").output().map(|o| o.status.success()).unwrap_or(false) {
            "kitty".to_string()
        } else if std::process::Command::new("which").arg("konsole").output().map(|o| o.status.success()).unwrap_or(false) {
            "konsole".to_string()
        } else if std::process::Command::new("which").arg("gnome-terminal").output().map(|o| o.status.success()).unwrap_or(false) {
            "gnome-terminal".to_string()
        } else {
            "xterm".to_string()
        };
        let mut c = std::process::Command::new(&term);
        c.current_dir(&dir);
        if term.contains("kitty") { c.arg("--directory").arg(&dir).arg("-e").arg("sh").arg("-c").arg(&cmd_str); }
        else if term.contains("konsole") { c.arg("--workdir").arg(&dir).arg("-e").arg("sh").arg("-c").arg(&cmd_str); }
        else if term.contains("gnome-terminal") { c.arg("--working-directory").arg(&dir).arg("--").arg("sh").arg("-c").arg(&cmd_str); }
        else { c.arg("-e").arg("sh").arg("-c").arg(&cmd_str); }
        c
    };

    cmd.spawn().map_err(|e| format!("Failed to launch terminal: {}", e))?;
    Ok(())
}

#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    let database = Database::open().expect("Failed to open database");

    tauri::Builder::default()
        .plugin(tauri_plugin_shell::init())
        .manage(Mutex::new(AppState { db: database }))
        .invoke_handler(tauri::generate_handler![
            list_sessions,
            build_tree,
            rename_session,
            copy_session,
            move_session,
            archive_session,
            delete_session,
            backup_database,
            open_in_terminal,
        ])
        .run(tauri::generate_context!())
        .expect("error while running tauri application");
}
