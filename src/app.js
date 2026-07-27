import { invoke } from '@tauri-apps/api/core';
import { listen } from '@tauri-apps/api/event';
import { Terminal } from '@xterm/xterm';
import { FitAddon } from '@xterm/addon-fit';
import '@xterm/xterm/css/xterm.css';

const LANG_KEY = 'opencode_manager_lang';
let lang = localStorage.getItem(LANG_KEY) || 'en';

const translations = {
  zh: {
    title: 'OpenCode Manager', search: '搜索会话...', showArchived: '显示已归档',
    rename: '重命名', copy: '复制', paste: '粘贴', move: '移动', archive: '归档', del: '删除',
    refresh: '刷新', backup: '备份', selectAll: '全选', deselect: '取消选择', settings: '设置',
    directory: '目录', allSessions: '所有会话', allDirs: '所有目录', noDir: '无目录', loading: '加载中...',
    hint: 'Ctrl+A 全选 · 双击重命名 · 右键菜单',
    newTitle: '新标题:', newDir: '新目录路径:',
    confirmDelete: '确定要永久删除以下', sessionsConfirm: '个会话吗？', cannotUndo: '此操作不可撤销！',
    backupBefore: '删除前备份数据库', confirm: '确认', cancel: '取消', confirmDel: '确认删除',
    info: '信息', close: '关闭',     copyId: '复制会话 ID', detail: '查看详情', openTerminal: '在终端中打开',
    themeTitle: '外观主题', light: '浅色', dark: '深色', shortcutsTitle: '快捷键',
    langLabel: '界面语言', langEn: 'English', langZh: '中文',
    terminalLabel: '终端程序', terminalPlaceholder: '例如: kitty / konsole',
    copied: '已复制会话 ID: ', backupDone: '数据库已备份到:',
    loadFailed: '加载数据失败', renameFailed: '重命名失败', pasteFailed: '粘贴失败',
    moveFailed: '移动失败', archiveFailed: '归档失败', deleteFailed: '删除失败', backupFailed: '备份失败',
    selectSingle: '请选择单个会话',
    hTitle: '标题', hDir: '目录', hMsg: '消息', hTokens: 'Tokens',
    hAgent: 'Agent', hProvider: '供应商', hModel: '模型', hTime: '更新时间',
  },
  en: {
    title: 'OpenCode Manager', search: 'Search sessions...', showArchived: 'Show archived',
    rename: 'Rename', copy: 'Copy', paste: 'Paste', move: 'Move', archive: 'Archive', del: 'Delete',
    refresh: 'Refresh', backup: 'Backup', selectAll: 'Select All', deselect: 'Deselect', settings: 'Settings',
    directory: 'Directory', allSessions: 'All Sessions', allDirs: 'All Directories', noDir: 'No Directory', loading: 'Loading...',
    hint: 'Ctrl+A Select All · Double-click Rename · Right-click Menu',
    newTitle: 'New title:', newDir: 'New directory path:',
    confirmDelete: 'Permanently delete', sessionsConfirm: 'sessions?', cannotUndo: 'This cannot be undone!',
    backupBefore: 'Backup database before deleting', confirm: 'Confirm', cancel: 'Cancel', confirmDel: 'Confirm Delete',
    info: 'Info', close: 'Close',     copyId: 'Copy Session ID', detail: 'View Details', openTerminal: 'Open in Terminal',
    themeTitle: 'Theme', light: 'Light', dark: 'Dark', shortcutsTitle: 'Keyboard Shortcuts',
    langLabel: 'Language', langEn: 'English', langZh: '中文',
    terminalLabel: 'Terminal', terminalPlaceholder: 'e.g. kitty / powershell.exe',
    copied: 'Copied session ID: ', backupDone: 'Database backed up to:',
    loadFailed: 'Failed to load data', renameFailed: 'Rename failed', pasteFailed: 'Paste failed',
    moveFailed: 'Move failed', archiveFailed: 'Archive failed', deleteFailed: 'Delete failed', backupFailed: 'Backup failed',
    selectSingle: 'Please select a single session',
    hTitle: 'Title', hDir: 'Directory', hMsg: 'Messages', hTokens: 'Tokens',
    hAgent: 'Agent', hProvider: 'Provider', hModel: 'Model', hTime: 'Updated',
  },
};

function t(key) { return translations[lang][key] || key; }

function setLanguage(newLang) {
  lang = newLang; localStorage.setItem(LANG_KEY, newLang);
  document.title = t('title'); updateUIStrings();
  // Re-render tree with new language
  renderTree();
}

function updateUIStrings() {
  document.getElementById('search').placeholder = t('search');
  document.getElementById('showArchivedLabel').textContent = t('showArchived');
  document.querySelectorAll('.tool-btn[data-action="rename"]').forEach(b => b.textContent = '✏  ' + t('rename'));
  document.querySelectorAll('.tool-btn[data-action="copy"]').forEach(b => b.textContent = '📋  ' + t('copy'));
  document.querySelectorAll('.tool-btn[data-action="paste"]').forEach(b => b.textContent = '📌  ' + t('paste'));
  document.querySelectorAll('.tool-btn[data-action="move"]').forEach(b => b.textContent = '📦  ' + t('move'));
  document.querySelectorAll('.tool-btn[data-action="archive"]').forEach(b => b.textContent = '🗃  ' + t('archive'));
  document.querySelectorAll('.tool-btn[data-action="delete"]').forEach(b => b.textContent = '🗑  ' + t('del'));
  document.querySelectorAll('.tool-btn[data-action="refresh"]').forEach(b => b.textContent = '🔄  ' + t('refresh'));
  document.querySelectorAll('.tool-btn[data-action="backup"]').forEach(b => b.textContent = '💾  ' + t('backup'));
  document.querySelectorAll('.link-btn[data-action="selectAll"]').forEach(b => b.textContent = t('selectAll'));
  document.querySelectorAll('.link-btn[data-action="deselect"]').forEach(b => b.textContent = t('deselect'));
  document.getElementById('sidebar-title').textContent = '📁  ' + t('directory');
  document.getElementById('status-hint').textContent = t('hint');
  document.querySelectorAll('.menu-item[data-action="rename"]').forEach(b => b.textContent = t('rename'));
  document.querySelectorAll('.menu-item[data-action="copy"]').forEach(b => b.textContent = t('copy'));
  document.querySelectorAll('.menu-item[data-action="paste"]').forEach(b => b.textContent = t('paste'));
  document.querySelectorAll('.menu-item[data-action="move"]').forEach(b => b.textContent = t('move'));
  document.querySelectorAll('.menu-item[data-action="archive"]').forEach(b => b.textContent = t('archive'));
  document.querySelectorAll('.menu-item[data-action="delete"]').forEach(b => b.textContent = t('del'));
  document.querySelectorAll('.menu-item[data-action="copyId"]').forEach(b => b.textContent = t('copyId'));
  document.querySelectorAll('.menu-item[data-action="detail"]').forEach(b => b.textContent = t('detail'));
  document.querySelectorAll('.menu-item[data-action="openTerminal"]').forEach(b => b.textContent = t('openTerminal'));
  const h = { 'th-title': 'hTitle', 'th-dir': 'hDir', 'th-msg': 'hMsg', 'th-tokens': 'hTokens', 'th-agent': 'hAgent', 'th-provider': 'hProvider', 'th-model': 'hModel', 'th-time': 'hTime' };
  Object.entries(h).forEach(([cls, key]) => document.querySelectorAll('.' + cls).forEach(el => el.textContent = t(key)));
  updateStatus();
}

let allSessions = [], filteredSessions = [], selectedIds = new Set(), clipboardIds = [], dirPrefix = null;
let lastClicked = null, dragStartIdx = null;

document.addEventListener('DOMContentLoaded', async () => {
  document.title = t('title'); updateUIStrings();
  const savedTheme = localStorage.getItem('opencode_manager_theme');
  if (savedTheme === 'dark') document.body.classList.add('dark');
  await refreshAll(); setupEventListeners();
});

async function refreshAll() {
  const showArchived = document.getElementById('showArchived').checked;
  try {
    allSessions = await invoke('list_sessions', { showArchived, dirPrefix: null });
    if (dirPrefix !== null && !allSessions.some(s => s.directory === dirPrefix)) {
      dirPrefix = null;
    }
    applyFilter();
    renderTree();
  } catch (e) { alert(t('loadFailed') + ': ' + e); }
}

async function renderTree() {
  const showArchived = document.getElementById('showArchived').checked;
  const tree = document.getElementById('tree');
  try {
    const nodes = await invoke('build_tree', { showArchived });
    tree.innerHTML = '<div class="tree-children" style="padding-left:0"><div class="dir-container"><span class="tree-node" data-prefix="">📁  ' + t('allSessions') + '</span></div></div>';
    const rootNode = tree.querySelector('.tree-node');
    rootNode.addEventListener('click', () => selectTreeNode(rootNode, ''));
    const container = tree.querySelector('.tree-children');
    nodes.forEach(n => container.appendChild(renderDirNode(n)));
    const count = allSessions.length;
    rootNode.innerHTML = `📁  ${t('allSessions')}${count ? '  <span class="tree-count">' + count + '</span>' : ''}`;
    if (dirPrefix === null) {
      rootNode.classList.add('selected');
    } else {
      const node = tree.querySelector(`.tree-node[data-prefix="${dirPrefix}"]`);
      if (node) node.classList.add('selected');
    }
  } catch (e) { console.error('Tree error:', e); }
}

function renderDirNode(node) {
  const c = document.createElement('div'); c.className = 'dir-container';
  const tgl = document.createElement('span'); tgl.className = 'tree-toggle'; tgl.textContent = node.children?.length ? '▶' : '';
  c.appendChild(tgl);
  const lbl = document.createElement('span'); lbl.className = 'tree-node'; lbl.dataset.prefix = node.full_path;
  const hasKids = node.children?.length > 0;
  lbl.innerHTML = (hasKids ? '📂' : '📁') + '  ' + node.name + (node.session_count > 0 ? '  <span class="tree-count">' + node.session_count + '</span>' : '');
  lbl.addEventListener('click', (e) => { e.stopPropagation(); selectTreeNode(lbl, node.full_path); });
  c.appendChild(lbl);
  if (hasKids) {
    const kids = document.createElement('div'); kids.className = 'tree-children'; kids.style.display = 'none';
    node.children.forEach(ch => kids.appendChild(renderDirNode(ch)));
    tgl.addEventListener('click', () => { const exp = kids.style.display !== 'none'; kids.style.display = exp ? 'none' : ''; tgl.textContent = exp ? '▶' : '▼'; });
    c.appendChild(kids);
  }
  return c;
}

function selectTreeNode(el, prefix) {
  document.querySelectorAll('.tree-node.selected').forEach(n => n.classList.remove('selected'));
  el.classList.add('selected');
  dirPrefix = prefix === '' ? null : prefix;
  applyFilter();
}

function renderTable() {
  const body = document.getElementById('table-body'); body.innerHTML = '';
  filteredSessions.forEach((s, idx) => {
    const row = document.createElement('div');
    row.className = 'table-row' + (s.is_archived ? ' archived' : '') + (selectedIds.has(s.id) ? ' selected' : '');
    row.dataset.id = s.id; row.dataset.index = idx;

    const dot = document.createElement('span'); dot.className = 'status-dot ' + (s.is_archived ? 'archived' : 'active');
    const titleCell = document.createElement('div'); titleCell.className = 'td td-title'; titleCell.appendChild(dot); titleCell.appendChild(document.createTextNode(s.title || s.slug));
    const dirCell = document.createElement('div'); dirCell.className = 'td td-dir'; dirCell.textContent = shortDir(s.directory);
    const msgCell = document.createElement('div'); msgCell.className = 'td td-msg'; msgCell.textContent = s.message_count;
    const tokenCell = document.createElement('div'); tokenCell.className = 'td td-tokens'; tokenCell.textContent = formatTokens((s.tokens_input || 0) + (s.tokens_output || 0));
    const agentCell = document.createElement('div'); agentCell.className = 'td td-agent'; agentCell.textContent = s.agent || '';
    let modelId = s.model || '', provider = '';
    try { const p = JSON.parse(modelId); modelId = p.id || modelId; provider = p.providerID || ''; } catch (e) {}
    const providerCell = document.createElement('div'); providerCell.className = 'td td-provider'; providerCell.textContent = provider;
    const modelCell = document.createElement('div'); modelCell.className = 'td td-model'; modelCell.textContent = modelId;
    const timeCell = document.createElement('div'); timeCell.className = 'td td-time'; timeCell.textContent = s.time_updated || s.time_created;

    [titleCell, dirCell, msgCell, tokenCell, agentCell, providerCell, modelCell, timeCell].forEach(c => row.appendChild(c));

    row.addEventListener('mousedown', (e) => {
      if (e.button !== 0) return; dragStartIdx = idx;
      if (e.ctrlKey || e.metaKey) { toggleSelection(s.id); dragStartIdx = null; }
      else if (e.shiftKey && lastClicked !== null) { selectRange(lastClicked, idx); dragStartIdx = null; }
      else { clearSelection(); selectOne(s.id); lastClicked = idx; }
    });
    row.addEventListener('mouseenter', (e) => {
      if (e.buttons !== 1 || dragStartIdx === null) return;
      clearSelection(); selectRange(dragStartIdx, idx);
    });
    row.addEventListener('dblclick', () => openInTerminalSelected());
    row.addEventListener('contextmenu', (e) => {
      e.preventDefault();
      if (!selectedIds.has(s.id)) { clearSelection(); selectOne(s.id); }
      showContextMenu(e, s);
    });
    body.appendChild(row);
  });
}

function toggleSelection(id) { selectedIds.has(id) ? selectedIds.delete(id) : selectedIds.add(id); updateRowStyles(); updateStatus(); }
function selectOne(id) { selectedIds.add(id); updateRowStyles(); updateStatus(); }
function clearSelection() { selectedIds.clear(); updateRowStyles(); updateStatus(); }
function selectAll() { filteredSessions.forEach(s => selectedIds.add(s.id)); updateRowStyles(); updateStatus(); }
function selectRange(from, to) { for (let i = Math.min(from, to); i <= Math.max(from, to); i++) selectedIds.add(filteredSessions[i].id); updateRowStyles(); updateStatus(); }
function updateRowStyles() { document.querySelectorAll('.table-row').forEach(r => r.classList.toggle('selected', selectedIds.has(r.dataset.id))); }
function getSelectedSessions() { return filteredSessions.filter(s => selectedIds.has(s.id)); }

function applyFilter() {
  const showArchived = document.getElementById('showArchived').checked;
  const q = document.getElementById('search').value.toLowerCase();
  filteredSessions = allSessions.filter(s => {
    if (!showArchived && s.is_archived) return false;
    if (dirPrefix !== null) { if (dirPrefix === '') return !s.directory; return s.directory === dirPrefix; }
    return true;
  });
  if (q) filteredSessions = filteredSessions.filter(s => s.title.toLowerCase().includes(q) || s.directory.toLowerCase().includes(q));
  renderTable(); updateStatus();
}

function updateStatus() {
  const total = allSessions.length, shown = filteredSessions.length, sel = selectedIds.size;
  let dir = t('allDirs');
  if (dirPrefix !== null && dirPrefix !== '') dir = shortDir(dirPrefix);
  else if (dirPrefix === '') dir = t('noDir');
  const selInfo = sel > 0 ? `  |  ${t('selected')} ${sel} ${t('items')}` : '';
  document.getElementById('status-text').textContent = `${dir}  |  ${shown} ${t('sessions')}${total !== shown ? ` (${t('total')} ${total})` : ''}${selInfo}`;
}

async function renameSelected() {
  const s = getSelectedSessions();
  if (s.length !== 1) { showWarning(t('selectSingle')); return; }
  const title = await showPrompt(t('newTitle'), s[0].title);
  if (!title || title === s[0].title) return;
  try { await invoke('rename_session', { id: s[0].id, title }); await refreshAll(); } catch (e) { showError(t('renameFailed'), e); }
}

async function copySelected() { clipboardIds = getSelectedSessions().map(s => s.id); }

async function pasteSelected() {
  if (!clipboardIds.length) return;
  try {
    for (const id of clipboardIds) {
      const newId = await invoke('copy_session', { id });
      if (dirPrefix) await invoke('move_session', { ids: [newId], directory: dirPrefix });
    }
    await refreshAll();
  } catch (e) { showError(t('pasteFailed'), e); }
}

async function moveSelected() {
  const s = getSelectedSessions();
  if (!s.length) return;
  const dir = await showPrompt(t('newDir'), s[0].directory || '');
  if (!dir) return;
  try { await invoke('move_session', { ids: s.map(x => x.id), directory: dir }); await refreshAll(); } catch (e) { showError(t('moveFailed'), e); }
}

async function archiveSelected() {
  const s = getSelectedSessions();
  if (!s.length) return;
  try { await invoke('archive_session', { ids: s.map(x => x.id), archived: !s.every(x => x.is_archived) }); await refreshAll(); } catch (e) { showError(t('archiveFailed'), e); }
}

async function deleteSelected() {
  const s = getSelectedSessions();
  if (!s.length) return;
  const confirmed = await showConfirm(`${t('confirmDelete')} ${s.length} ${t('sessionsConfirm')}\n\n${t('cannotUndo')}`, true);
  if (!confirmed) return;
  try { await invoke('delete_session', { ids: s.map(x => x.id), backup: true }); await refreshAll(); } catch (e) { showError(t('deleteFailed'), e); }
}

async function backupDb() {
  try { const p = await invoke('backup_database'); showInfo(t('backupDone') + '\n' + p); } catch (e) { showError(t('backupFailed'), e); }
}

function copyIdSelected() {
  const s = getSelectedSessions();
  if (s.length !== 1) return;
  navigator.clipboard.writeText(s[0].id);
  showInfo(t('copied') + s[0].id);
}

function showDetailSelected() {
  const s = getSelectedSessions();
  if (s.length !== 1) return;
  const x = s[0];
  showInfo(`ID: ${x.id}\nTitle: ${x.title}\nSlug: ${x.slug}\nDirectory: ${x.directory}\nMessages: ${x.message_count}\nCreated: ${x.time_created}\nUpdated: ${x.time_updated}\nTokens: ${formatTokens((x.tokens_input || 0) + (x.tokens_output || 0))}\nAgent: ${x.agent || '-'}\nModel: ${x.model || '-'}`);
}

// ── Embedded terminal (tabbed overlay) ──
const terminalTabs = new Map();
let activeTabId = null;

async function openInTerminalSelected() {
  const s = getSelectedSessions();
  if (s.length !== 1) return;
  const session = s[0];
  const dir = session.directory || '';
  const sessionId = session.id;
  const title = session.title || sessionId;

  const tabId = `tab_${Date.now()}_${Math.random().toString(36).slice(2, 6)}`;

  // Show terminal view and create element FIRST, so xterm can measure dimensions
  showTerminalView();
  const container = document.getElementById('terminal-container');
  const termEl = document.createElement('div');
  termEl.id = `term-${tabId}`;
  termEl.style.height = '100%';
  termEl.style.width = '100%';
  container.appendChild(termEl);

  // Hide other tabs
  terminalTabs.forEach((info) => { info.el.style.display = 'none'; });

  const term = new Terminal({
    fontSize: 14,
    fontFamily: 'monospace',
    cursorBlink: true,
    scrollback: 10000,
  });
  const fitAddon = new FitAddon();
  term.loadAddon(fitAddon);

  term.onData((data) => {
    invoke('pty_write', { tabId, data });
  });

  term.open(termEl);
  fitAddon.fit();

  terminalTabs.set(tabId, {
    tabId, sessionId, title, term, fitAddon, el: termEl,
  });

  addTabButton(tabId, title);
  activeTabId = tabId;
  document.querySelectorAll('.terminal-tab').forEach(t => {
    t.classList.toggle('active', t.dataset.tabId === tabId);
  });
  term.focus();

  const cols = term.cols;
  const rows = term.rows;

  term.writeln('\x1b[90mStarting opencode...\x1b[0m\r');

  try {
    await setupPtyListeners();
    await invoke('pty_spawn', { tabId, program: 'opencode', args: ['-s', sessionId], cwd: dir || null, cols, rows });
    term.writeln('\x1b[32mpty_spawn OK\x1b[0m\r');
  } catch (e) {
    term.writeln(`\r\n\x1b[31mError: ${e}\x1b[0m`);
  }
}

function addTabButton(tabId, title) {
  const list = document.getElementById('terminal-tabs-list');
  const btn = document.createElement('div');
  btn.className = 'terminal-tab';
  btn.dataset.tabId = tabId;
  btn.innerHTML = `<span class="tab-title">${escapeHtml(title)}</span><span class="tab-close">×</span>`;
  btn.addEventListener('click', (e) => {
    if (e.target.classList.contains('tab-close')) {
      closeTab(tabId);
    } else {
      switchTab(tabId);
    }
  });
  list.appendChild(btn);
}

function switchTab(tabId) {
  activeTabId = tabId;
  document.querySelectorAll('.terminal-tab').forEach(t => {
    t.classList.toggle('active', t.dataset.tabId === tabId);
  });
  terminalTabs.forEach((info) => {
    info.el.style.display = info.tabId === tabId ? 'block' : 'none';
  });
  const info = terminalTabs.get(tabId);
  if (info) {
    setTimeout(() => { info.fitAddon.fit(); info.term.focus(); }, 50);
  }
}

function closeTab(tabId) {
  invoke('pty_kill', { tabId }).catch(() => {});
  const info = terminalTabs.get(tabId);
  if (info) {
    info.term.dispose();
    info.el.remove();
  }
  terminalTabs.delete(tabId);
  document.querySelector(`.terminal-tab[data-tab-id="${tabId}"]`)?.remove();
  if (activeTabId === tabId) {
    const next = terminalTabs.keys().next();
    if (next.done) {
      hideTerminalView();
    } else {
      switchTab(next.value);
    }
  }
}

function showTerminalView() {
  document.getElementById('terminal-view').classList.remove('hidden');
}

function hideTerminalView() {
  document.getElementById('terminal-view').classList.add('hidden');
}

function escapeHtml(s) {
  return s.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;');
}

// PTY event listeners - set up with error handling
let ptyListenersReady = false;
async function setupPtyListeners() {
  if (ptyListenersReady) return;
  ptyListenersReady = true;
  try {
    const unlisten1 = await listen('pty-data', (event) => {
      const [tabId, data] = event.payload;
      const info = terminalTabs.get(tabId);
      if (info) {
        info.term.write(data);
      }
    });
    const unlisten2 = await listen('pty-exit', (event) => {
      const tabId = event.payload;
      const info = terminalTabs.get(tabId);
      if (info) {
        info.term.writeln('\r\n\x1b[90m[process exited]\x1b[0m');
      }
    });
  } catch (e) {
    console.error('Failed to set up PTY listeners:', e);
    // Show error on any active terminal
    terminalTabs.forEach((info) => {
      info.term.writeln(`\r\n\x1b[31m[listener setup failed: ${e}]\x1b[0m`);
    });
  }
}
setupPtyListeners();

// Back button
document.getElementById('terminal-back-btn').addEventListener('click', () => {
  terminalTabs.forEach((_, id) => closeTab(id));
  hideTerminalView();
});

// Resize handling
let resizeTimer = null;
window.addEventListener('resize', () => {
  if (resizeTimer) clearTimeout(resizeTimer);
  resizeTimer = setTimeout(() => {
    const info = terminalTabs.get(activeTabId);
    if (info) {
      info.fitAddon.fit();
      invoke('pty_resize', { tabId: activeTabId, cols: info.term.cols, rows: info.term.rows });
    }
  }, 200);
});

function showContextMenu(e, session) {
  const menu = document.getElementById('context-menu');
  menu.style.left = e.clientX + 'px'; menu.style.top = e.clientY + 'px';
  menu.classList.remove('hidden');
  menu.querySelector('.paste-item').style.display = clipboardIds.length ? '' : 'none';
}

function showWarning(msg) { alert(msg); }

function showInfo(msg) {
  document.getElementById('dialog-body').innerHTML = `<pre style="font-family:var(--font-mono);font-size:12px;line-height:1.6;white-space:pre-wrap">${msg}</pre>`;
  document.getElementById('dialog-title').textContent = t('info');
  document.getElementById('dialog-buttons').innerHTML = `<button onclick="closeDialog()">${t('close')}</button>`;
  document.getElementById('dialog-overlay').classList.remove('hidden');
}

function showError(title, err) { alert(`${title}: ${err}`); }

function showPrompt(label, value) {
  return new Promise(resolve => {
    const body = document.getElementById('dialog-body');
    body.innerHTML = `<label style="font-size:14px;display:block;margin-bottom:6px">${label}</label><input type="text" id="prompt-input" value="${escapeHtml(value)}" />`;
    document.getElementById('dialog-title').textContent = '';
    document.getElementById('dialog-buttons').innerHTML = `<button onclick="closeDialog()">${t('cancel')}</button><button class="primary" id="prompt-ok">${t('confirm')}</button>`;
    document.getElementById('dialog-overlay').classList.remove('hidden');
    const input = document.getElementById('prompt-input'); input.focus(); input.select();
    const ok = document.getElementById('prompt-ok');
    ok.onclick = () => { closeDialog(); resolve(input.value.trim()); };
    input.onkeydown = (e) => { if (e.key === 'Enter') ok.click(); if (e.key === 'Escape') { closeDialog(); resolve(null); } };
  });
}

function showConfirm(msg, withBackup) {
  return new Promise(resolve => {
    const body = document.getElementById('dialog-body');
    body.innerHTML = `<div style="font-size:14px;line-height:1.6;white-space:pre-wrap">${escapeHtml(msg)}</div>`;
    if (withBackup) body.innerHTML += `<label style="display:flex;align-items:center;gap:6px;margin-top:12px;font-size:13px"><input type="checkbox" id="backup-check" checked /> ${t('backupBefore')}</label>`;
    document.getElementById('dialog-title').textContent = t('confirm');
    document.getElementById('dialog-buttons').innerHTML = `<button id="confirm-cancel">${t('cancel')}</button><button class="primary" id="confirm-ok">${t('confirmDel')}</button>`;
    document.getElementById('dialog-overlay').classList.remove('hidden');
    document.getElementById('confirm-ok').onclick = () => { closeDialog(); resolve(true); };
    document.getElementById('confirm-cancel').onclick = () => { closeDialog(); resolve(false); };
  });
}

function openSettings() {
  const body = document.getElementById('dialog-body');
  const isDark = document.body.classList.contains('dark');
  const themeChecked = (v) => `${{light: !isDark, dark: isDark}[v] ? 'checked' : ''}`;
  body.innerHTML = `
    <div class="settings-section"><div class="settings-title">${t('themeTitle')}</div>
      <label style="margin-right:20px"><input type="radio" name="theme" value="light" ${themeChecked('light')} /> ☀  ${t('light')}</label>
      <label><input type="radio" name="theme" value="dark" ${themeChecked('dark')} /> ☾  ${t('dark')}</label>
    </div>
    <div class="settings-section"><div class="settings-title">${t('langLabel')}</div>
      <label style="margin-right:20px"><input type="radio" name="lang" value="en" ${lang === 'en' ? 'checked' : ''} /> ${t('langEn')}</label>
      <label><input type="radio" name="lang" value="zh" ${lang === 'zh' ? 'checked' : ''} /> ${t('langZh')}</label>
    </div>
    <div class="settings-section"><div class="settings-title">${t('terminalLabel')}</div>
      <input type="text" id="terminal-input" style="width:100%;padding:6px 8px;border:1px solid var(--border);border-radius:6px;font-size:13px;font-family:var(--font-sans);outline:none;margin-top:6px" placeholder="${t('terminalPlaceholder')}" value="${localStorage.getItem('opencode_manager_terminal') || ''}" />
    </div>
    <div class="settings-section"><div class="settings-title">${t('shortcutsTitle')}</div>
      <div class="shortcut-row"><span class="shortcut-key">Ctrl+A</span><span class="shortcut-desc">${t('selectAll')}</span></div>
      <div class="shortcut-row"><span class="shortcut-key">Ctrl+Shift+C</span><span class="shortcut-desc">${t('copy')}</span></div>
      <div class="shortcut-row"><span class="shortcut-key">Ctrl+Shift+V</span><span class="shortcut-desc">${t('paste')}</span></div>
      <div class="shortcut-row"><span class="shortcut-key">Ctrl+R</span><span class="shortcut-desc">${t('rename')}</span></div>
      <div class="shortcut-row"><span class="shortcut-key">Ctrl+D</span><span class="shortcut-desc">${t('del')}</span></div>
      <div class="shortcut-row"><span class="shortcut-key">Ctrl+E</span><span class="shortcut-desc">${t('archive')}</span></div>
      <div class="shortcut-row"><span class="shortcut-key">Ctrl+M</span><span class="shortcut-desc">${t('move')}</span></div>
      <div class="shortcut-row"><span class="shortcut-key">F5</span><span class="shortcut-desc">${t('refresh')}</span></div>
      <div class="shortcut-row"><span class="shortcut-key">Ctrl+F</span><span class="shortcut-desc">${t('search')}</span></div>
      <div class="shortcut-row"><span class="shortcut-key">Delete</span><span class="shortcut-desc">${t('del')}</span></div>
    </div>`;
  document.getElementById('dialog-title').textContent = t('settings');
  document.getElementById('dialog-buttons').innerHTML = `<button id="settings-close-btn" class="primary">${t('close')}</button>`;
  document.getElementById('dialog-overlay').classList.remove('hidden');
  document.getElementById('settings-close-btn').onclick = () => {
    const input = document.getElementById('terminal-input');
    if (input) localStorage.setItem('opencode_manager_terminal', input.value.trim());
    closeDialog();
  };
  document.querySelectorAll('input[name="theme"]').forEach(r => {
    r.addEventListener('change', () => { document.body.classList.toggle('dark', r.value === 'dark'); localStorage.setItem('opencode_manager_theme', r.value); });
  });
  document.querySelectorAll('input[name="lang"]').forEach(r => {
    r.addEventListener('change', () => { if (r.checked) setLanguage(r.value); });
  });
}

window.closeDialog = function() { document.getElementById('dialog-overlay').classList.add('hidden'); };

document.addEventListener('keydown', (e) => {
  const k = e.key.toLowerCase(), c = e.ctrlKey || e.metaKey;
  if (c && k === 'a') { e.preventDefault(); selectAll(); }
  else if (c && k === 'f') { e.preventDefault(); document.getElementById('search').focus(); }
  else if (k === 'f5') { e.preventDefault(); refreshAll(); }
  else if (c && e.shiftKey && k === 'c') { e.preventDefault(); copySelected(); }
  else if (c && e.shiftKey && k === 'v') { e.preventDefault(); pasteSelected(); }
  else if (c && k === 'r') { e.preventDefault(); renameSelected(); }
  else if (k === 'delete' || k === 'del') { e.preventDefault(); deleteSelected(); }
  else if (c && k === 'e') { e.preventDefault(); archiveSelected(); }
  else if (c && k === 'm') { e.preventDefault(); moveSelected(); }
});

function setupEventListeners() {
  document.getElementById('search').addEventListener('input', applyFilter);
  document.getElementById('showArchived').addEventListener('change', refreshAll);
  const allCheck = document.getElementById('selectAllCheck');
  if (allCheck) allCheck.addEventListener('change', (e) => { e.target.checked ? selectAll() : clearSelection(); });
  document.querySelectorAll('.tool-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      const m = { rename: renameSelected, copy: copySelected, paste: pasteSelected, move: moveSelected, archive: archiveSelected, delete: deleteSelected, refresh: refreshAll, backup: backupDb };
      if (m[btn.dataset.action]) m[btn.dataset.action]();
    });
  });
  document.querySelectorAll('.link-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      if (btn.dataset.action === 'selectAll') selectAll();
      if (btn.dataset.action === 'deselect') clearSelection();
    });
  });
  document.getElementById('settings-btn').addEventListener('click', openSettings);
  setupColumnResize();
  document.addEventListener('click', () => document.getElementById('context-menu').classList.add('hidden'));
  document.getElementById('context-menu').addEventListener('click', (e) => {
    const item = e.target.closest('.menu-item');
    if (!item) return;
    document.getElementById('context-menu').classList.add('hidden');
    const m = { rename: renameSelected, copy: copySelected, paste: pasteSelected, move: moveSelected, archive: archiveSelected, delete: deleteSelected, copyId: copyIdSelected, detail: showDetailSelected, openTerminal: openInTerminalSelected };
    if (m[item.dataset.action]) m[item.dataset.action]();
  });
}

function shortDir(dir) {
  if (!dir) return '';
  const home = '/home/' + (window.__homeUser || 'zapei2');
  return dir.startsWith(home) ? '~' + dir.slice(home.length) : dir;
}

function formatTokens(n) {
  if (!n) return '';
  if (n >= 1000000) return (n / 1000000).toFixed(1) + 'M';
  if (n >= 1000) return (n / 1000).toFixed(1) + 'k';
  return n.toString();
}


function setupColumnResize() {
  const header = document.getElementById('table-header');
  let curCol = null, curX = 0, curW = 0, curColClass = '';

  // Set initial CSS variables from styles
  function syncCols() {
    const root = document.documentElement;
    const widths = { title: 300, dir: 180, msg: 65, tokens: 90, agent: 100, provider: 80, model: 160, time: 140 };
    Object.entries(widths).forEach(([k, v]) => {
      if (!root.style.getPropertyValue('--col-' + k)) root.style.setProperty('--col-' + k, v + 'px');
    });
  }
  syncCols();

  header.addEventListener('mousedown', (e) => {
    const th = e.target.closest('.th');
    if (!th) return;
    const rect = th.getBoundingClientRect();
    const nearRight = rect.right - e.clientX <= 10;
    const nearLeft = e.clientX - rect.left <= 10;
    if (!nearRight && !nearLeft) return;
    const target = nearLeft ? th.previousElementSibling : th;
    if (!target) return;
    const cls = Array.from(target.classList).find(c => c.startsWith('th-'));
    if (!cls) return;
    curCol = target; curX = e.clientX; curW = target.getBoundingClientRect().width; curColClass = cls.slice(3);
    document.body.style.cursor = 'col-resize'; document.body.style.userSelect = 'none';
    e.preventDefault();
  });

  header.addEventListener('mousemove', (e) => {
    const th = e.target.closest('.th');
    if (!th) return;
    const rect = th.getBoundingClientRect();
    const near = (e.clientX - rect.left <= 10) || (rect.right - e.clientX <= 10);
    th.style.cursor = near ? 'col-resize' : '';
  });

  document.addEventListener('mousemove', (e) => {
    if (!curCol) return;
    const newW = Math.max(60, curW + e.clientX - curX);
    document.documentElement.style.setProperty('--col-' + curColClass, newW + 'px');
  });

  document.addEventListener('mouseup', () => {
    if (curCol) { document.body.style.cursor = ''; document.body.style.userSelect = ''; curCol = null; curColClass = ''; }
  });
}
