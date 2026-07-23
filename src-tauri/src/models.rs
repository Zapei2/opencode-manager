use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Session {
    pub id: String,
    pub project_id: String,
    pub slug: String,
    pub directory: String,
    pub title: String,
    pub version: String,
    pub cost: f64,
    pub tokens_input: i64,
    pub tokens_output: i64,
    pub tokens_reasoning: i64,
    pub tokens_cache_read: i64,
    pub tokens_cache_write: i64,
    pub agent: Option<String>,
    pub model: Option<String>,
    pub time_created: i64,
    pub time_updated: i64,
    pub time_archived: i64,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct SessionSummary {
    pub id: String,
    pub title: String,
    pub slug: String,
    pub directory: String,
    pub message_count: i64,
    pub cost: f64,
    pub tokens_input: i64,
    pub tokens_output: i64,
    pub agent: Option<String>,
    pub model: Option<String>,
    pub time_created: String,
    pub time_updated: String,
    pub is_archived: bool,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct DirNode {
    pub name: String,
    pub full_path: String,
    pub session_count: usize,
    pub children: Vec<DirNode>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct DbStats {
    pub total: usize,
    pub shown: usize,
    pub selected: usize,
    pub dir_label: String,
}
