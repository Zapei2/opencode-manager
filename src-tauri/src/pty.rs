use portable_pty::{native_pty_system, CommandBuilder, PtySize, MasterPty, Child};
use std::collections::HashMap;
use std::io::{Read, Write};
use std::sync::Mutex;
use tauri::{AppHandle, Emitter};

pub struct PtyInstance {
    pub writer: Box<dyn Write + Send>,
    pub master: Box<dyn MasterPty + Send>,
    pub child: Box<dyn Child + Send>,
    pub id: String,
}

pub struct PtyManager {
    instances: Mutex<HashMap<String, PtyInstance>>,
}

impl PtyManager {
    pub fn new() -> Self {
        PtyManager {
            instances: Mutex::new(HashMap::new()),
        }
    }

    pub fn spawn(
        &self,
        app: AppHandle,
        id: String,
        program: String,
        args: Vec<String>,
        cwd: Option<String>,
        cols: u16,
        rows: u16,
        dark_mode: bool,
    ) -> Result<(), String> {
        let pty_system = native_pty_system();
        let pair = pty_system
            .openpty(PtySize {
                rows,
                cols,
                pixel_width: 0,
                pixel_height: 0,
            })
            .map_err(|e| format!("Failed to open PTY: {}", e))?;

        // Build full command string for sh -c
        let cmd_str = if program == "sh" {
            // Already a shell command, pass through
            let mut s = String::new();
            for (i, a) in args.iter().enumerate() {
                if i > 0 { s.push(' '); }
                s.push_str(&shell_quote(a));
            }
            s
        } else {
            let mut s = shell_quote(&program);
            for a in &args {
                s.push(' ');
                s.push_str(&shell_quote(a));
            }
            s
        };

        let mut cmd = CommandBuilder::new("sh");
        cmd.arg("-c");
        cmd.arg(&cmd_str);
        if let Some(dir) = &cwd {
            cmd.cwd(dir);
        }
        // Ensure essential env vars are set
        cmd.env("TERM", "xterm-256color");
        cmd.env("OPENCODE_DISABLE_CHANNEL_DB", "true");
        // Tell TUI apps about terminal color scheme so they pick the right theme
        // COLORFGBG format: "foreground;background" (15;0 = light-on-dark, 0;15 = dark-on-light)
        cmd.env("COLORFGBG", if dark_mode { "15;0" } else { "0;15" });
        if let Some(home) = dirs_next::home_dir() {
            cmd.env("HOME", home.to_string_lossy().to_string());
            let path = format!("{}/.local/bin:/usr/local/bin:/usr/bin:/bin", home.to_string_lossy());
            cmd.env("PATH", path);
        }

        let child = pair
            .slave
            .spawn_command(cmd)
            .map_err(|e| format!("Failed to spawn: {}", e))?;
        drop(pair.slave);

        let master = pair.master;
        let writer = master
            .take_writer()
            .map_err(|e| format!("Failed to take writer: {}", e))?;
        let mut reader = master
            .try_clone_reader()
            .map_err(|e| format!("Failed to clone reader: {}", e))?;

        let event_id = id.clone();
        let app_clone = app.clone();
        std::thread::spawn(move || {
            let mut buf = [0u8; 4096];
            loop {
                match reader.read(&mut buf) {
                    Ok(0) => break,
                    Ok(n) => {
                        let data = String::from_utf8_lossy(&buf[..n]).to_string();
                        let _ = app_clone.emit("pty-data", (event_id.clone(), data));
                    }
                    Err(_) => break,
                }
            }
            let _ = app_clone.emit("pty-exit", event_id.clone());
        });

        let instance = PtyInstance {
            writer,
            master,
            child,
            id: id.clone(),
        };
        let emit_id = id.clone();
        self.instances.lock().map_err(|e| e.to_string())?.insert(id, instance);

        Ok(())
    }

    pub fn write(&self, id: &str, data: &[u8]) -> Result<(), String> {
        let mut instances = self.instances.lock().map_err(|e| e.to_string())?;
        if let Some(instance) = instances.get_mut(id) {
            instance
                .writer
                .write_all(data)
                .map_err(|e| format!("Write failed: {}", e))?;
            instance.writer.flush().ok();
        }
        Ok(())
    }

    pub fn resize(&self, id: &str, cols: u16, rows: u16) -> Result<(), String> {
        let instances = self.instances.lock().map_err(|e| e.to_string())?;
        if let Some(instance) = instances.get(id) {
            instance
                .master
                .resize(PtySize {
                    rows,
                    cols,
                    pixel_width: 0,
                    pixel_height: 0,
                })
                .map_err(|e| format!("Resize failed: {}", e))?;
        }
        Ok(())
    }

    pub fn kill(&self, id: &str) -> Result<(), String> {
        let mut instances = self.instances.lock().map_err(|e| e.to_string())?;
        if let Some(mut instance) = instances.remove(id) {
            let _ = instance.child.kill();
            let _ = instance.child.wait();
        }
        Ok(())
    }

    pub fn list(&self) -> Vec<String> {
        self.instances
            .lock()
            .map(|m| m.keys().cloned().collect())
            .unwrap_or_default()
    }
}

fn shell_quote(s: &str) -> String {
    if s.chars().all(|c| c.is_alphanumeric() || c == '_' || c == '-' || c == '/' || c == '.' || c == '=') {
        s.to_string()
    } else {
        format!("'{}'", s.replace('\'', "'\\''"))
    }
}
