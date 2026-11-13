// Frontend der Aufgabenverwaltung (Aufgabe 8.1).
// Reines HTML/CSS/JavaScript, wird direkt von Quarkus unter "/" ausgeliefert.
// Kommuniziert ueber die REST-API und schickt das JWT im Authorization-Header mit.

const STORAGE_KEY = 'taskmanagement.session';

const state = {
    session: null,          // { token, username, role }
    tasks: [],
    projects: [],
    labels: [],
    filter: { status: '', projectId: null, labelId: null },
    editingId: null,
    authMode: 'login',
};

const $ = (selector) => document.querySelector(selector);

// ---------- Hilfsfunktionen ----------

function escapeHtml(value) {
    return String(value ?? '').replace(/[&<>"']/g, (c) => ({
        '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;',
    })[c]);
}

function loadSession() {
    try {
        const raw = localStorage.getItem(STORAGE_KEY);
        return raw ? JSON.parse(raw) : null;
    } catch {
        return null;
    }
}

function saveSession(session) {
    state.session = session;
    try {
        if (session) {
            localStorage.setItem(STORAGE_KEY, JSON.stringify(session));
        } else {
            localStorage.removeItem(STORAGE_KEY);
        }
    } catch {
        // Ohne localStorage funktioniert die Seite trotzdem, nur ohne "eingeloggt bleiben"
    }
}

let toastTimer;
function toast(message, isError = false) {
    const el = $('#toast');
    el.textContent = message;
    el.classList.toggle('error', isError);
    el.hidden = false;
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => { el.hidden = true; }, 3500);
}

function formatDate(isoDate) {
    if (!isoDate) return '';
    const [y, m, d] = isoDate.split('-');
    return `${d}.${m}.${y}`;
}

function todayIso() {
    const now = new Date();
    const offset = now.getTimezoneOffset() * 60000;
    return new Date(now - offset).toISOString().slice(0, 10);
}

// ---------- API ----------

async function api(path, options = {}) {
    const headers = { ...(options.headers || {}) };
    if (options.body !== undefined) headers['Content-Type'] = 'application/json';
    if (state.session?.token) headers['Authorization'] = `Bearer ${state.session.token}`;

    const response = await fetch(path, {
        ...options,
        headers,
        body: options.body !== undefined ? JSON.stringify(options.body) : undefined,
    });

    // Token abgelaufen oder ungueltig -> zurueck zum Login
    if (response.status === 401 && !path.startsWith('/auth/')) {
        saveSession(null);
        showAuth();
        throw new Error('Sitzung abgelaufen, bitte erneut anmelden');
    }
    if (response.status === 204) return null;

    const text = await response.text();
    const data = text ? JSON.parse(text) : null;
    if (!response.ok) {
        throw new Error(errorMessage(response.status, data));
    }
    return data;
}

/** Liest die Fehlermeldung aus der Antwort (eigene Meldung oder Bean-Validation-Fehler). */
function errorMessage(status, data) {
    if (data?.message) return data.message;
    if (Array.isArray(data?.violations) && data.violations.length) {
        return data.violations.map((v) => v.message).join(', ');
    }
    if (status === 403) return 'Dafür fehlt dir die Berechtigung (nur Admin)';
    return `Fehler ${status}`;
}

// ---------- Login / Registrierung ----------

function showAuth() {
    $('#app-view').hidden = true;
    $('#auth-view').hidden = false;
    $('#auth-form').reset();
    $('#auth-error').hidden = true;
}

function setAuthMode(mode) {
    state.authMode = mode;
    document.querySelectorAll('.tab').forEach((tab) => {
        tab.classList.toggle('active', tab.dataset.mode === mode);
    });
    $('#auth-submit').textContent = mode === 'login' ? 'Anmelden' : 'Konto erstellen';
    $('#auth-form').password.autocomplete = mode === 'login' ? 'current-password' : 'new-password';
    $('#auth-error').hidden = true;
}

async function submitAuth(event) {
    event.preventDefault();
    const form = event.target;
    const errorEl = $('#auth-error');
    errorEl.hidden = true;
    try {
        const session = await api(`/auth/${state.authMode}`, {
            method: 'POST',
            body: { username: form.username.value.trim(), password: form.password.value },
        });
        saveSession(session);
        await showApp();
        if (state.authMode === 'register') toast(`Willkommen, ${session.username}!`);
    } catch (error) {
        errorEl.textContent = error.message;
        errorEl.hidden = false;
    }
}

function logout() {
    saveSession(null);
    showAuth();
}

// ---------- Hauptansicht ----------

async function showApp() {
    $('#auth-view').hidden = true;
    $('#app-view').hidden = false;
    $('#user-name').textContent = state.session.username;
    const roleEl = $('#user-role');
    roleEl.textContent = state.session.role;
    roleEl.classList.toggle('admin', isAdmin());
    await reloadAll();
}

function isAdmin() {
    return state.session?.role === 'Admin';
}

async function reloadAll() {
    try {
        const [projects, labels] = await Promise.all([api('/projects'), api('/labels')]);
        state.projects = projects;
        state.labels = labels;
        renderSidebar();
        await loadTasks();
    } catch (error) {
        toast(error.message, true);
    }
}

async function loadTasks() {
    const params = new URLSearchParams();
    if (state.filter.status) params.set('status', state.filter.status);
    if (state.filter.projectId) params.set('projectId', state.filter.projectId);
    if (state.filter.labelId) params.set('labelId', state.filter.labelId);
    const query = params.toString();
    state.tasks = await api(`/tasks${query ? `?${query}` : ''}`);
    renderTasks();
}

function renderSidebar() {
    const projectItems = [
        `<li data-project="" class="${state.filter.projectId ? '' : 'active'}">
            <span class="name">Alle Projekte</span></li>`,
        ...state.projects.map((p) => `
            <li data-project="${p.id}" class="${state.filter.projectId === p.id ? 'active' : ''}"
                title="${escapeHtml(p.description || p.name)}">
                <span class="name">${escapeHtml(p.name)}</span>
                <span class="count">${p.openTaskCount}/${p.taskCount}</span>
                ${isAdmin() ? `<button class="icon-btn danger" data-delete-project="${p.id}" title="Projekt löschen">×</button>` : ''}
            </li>`),
    ];
    $('#project-list').innerHTML = projectItems.join('');

    $('#label-list').innerHTML = state.labels.map((l) => `
        <li data-label="${l.id}" class="${state.filter.labelId === l.id ? 'active' : ''}">
            <span class="dot" style="background:${escapeHtml(l.color)}"></span>
            <span class="name">${escapeHtml(l.name)}</span>
            ${isAdmin() ? `<button class="icon-btn danger" data-delete-label="${l.id}" title="Label löschen">×</button>` : ''}
        </li>`).join('');
}

function renderTasks() {
    const project = state.projects.find((p) => p.id === state.filter.projectId);
    const label = state.labels.find((l) => l.id === state.filter.labelId);
    let title = project ? project.name : 'Alle Aufgaben';
    if (label) title += ` · ${label.name}`;
    $('#list-title').textContent = title;

    const open = state.tasks.filter((t) => t.status === 'offen').length;
    $('#list-summary').textContent = `${state.tasks.length} Aufgaben, davon ${open} offen`;

    const today = todayIso();
    $('#task-list').innerHTML = state.tasks.map((t) => {
        const done = t.status === 'erledigt';
        const overdue = !done && t.dueDate && t.dueDate < today;
        return `
        <li class="task ${done ? 'done' : ''}">
            <input type="checkbox" data-toggle="${t.id}" ${done ? 'checked' : ''}
                   title="${done ? 'Als offen markieren' : 'Als erledigt markieren'}">
            <div class="task-body">
                <div class="task-title">${escapeHtml(t.title)}</div>
                ${t.description ? `<p class="task-desc">${escapeHtml(t.description)}</p>` : ''}
                <div class="task-meta">
                    ${t.dueDate ? `<span class="chip ${overdue ? 'overdue' : ''}">📅 ${formatDate(t.dueDate)}${overdue ? ' · überfällig' : ''}</span>` : ''}
                    ${t.projectName ? `<span class="chip">📁 ${escapeHtml(t.projectName)}</span>` : ''}
                    ${t.labels.map((l) => `<span class="chip label" style="background:${escapeHtml(l.color)}">${escapeHtml(l.name)}</span>`).join('')}
                    ${t.createdBy ? `<span>von ${escapeHtml(t.createdBy)}</span>` : ''}
                </div>
            </div>
            <div class="task-actions">
                <button class="icon-btn" data-edit="${t.id}" title="Bearbeiten">✎</button>
                <button class="icon-btn danger" data-delete="${t.id}" title="Löschen">🗑</button>
            </div>
        </li>`;
    }).join('');
    $('#empty').hidden = state.tasks.length > 0;
}

// ---------- Aufgaben bearbeiten ----------

function openTaskDialog(task = null) {
    state.editingId = task?.id ?? null;
    const form = $('#task-form');
    form.reset();
    $('#task-error').hidden = true;
    $('#task-dialog-title').textContent = task ? 'Aufgabe bearbeiten' : 'Neue Aufgabe';

    form.projectId.innerHTML = '<option value="">– kein Projekt –</option>'
        + state.projects.map((p) => `<option value="${p.id}">${escapeHtml(p.name)}</option>`).join('');

    const selectedLabels = new Set(task?.labelIds ?? (state.filter.labelId ? [state.filter.labelId] : []));
    $('#task-labels').innerHTML = state.labels.length
        ? state.labels.map((l) => `
            <label><input type="checkbox" name="labels" value="${l.id}" ${selectedLabels.has(l.id) ? 'checked' : ''}>
            <span class="dot" style="background:${escapeHtml(l.color)}"></span>${escapeHtml(l.name)}</label>`).join('')
        : '<span class="muted">Noch keine Labels vorhanden</span>';

    form.title.value = task?.title ?? '';
    form.description.value = task?.description ?? '';
    form.status.value = task?.status ?? 'offen';
    form.dueDate.value = task?.dueDate ?? '';
    form.projectId.value = task?.projectId ?? state.filter.projectId ?? '';

    $('#task-dialog').showModal();
    form.title.focus();
}

function taskFromForm(form) {
    return {
        title: form.title.value.trim(),
        description: form.description.value.trim() || null,
        status: form.status.value,
        dueDate: form.dueDate.value || null,
        projectId: form.projectId.value ? Number(form.projectId.value) : null,
        labelIds: [...form.querySelectorAll('input[name="labels"]:checked')].map((c) => Number(c.value)),
    };
}

async function submitTask(event) {
    event.preventDefault();
    const form = event.target;
    const errorEl = $('#task-error');
    const task = taskFromForm(form);
    if (!task.title) {
        errorEl.textContent = 'Titel ist ein Pflichtfeld';
        errorEl.hidden = false;
        return;
    }
    try {
        if (state.editingId) {
            await api(`/tasks/${state.editingId}`, { method: 'PUT', body: task });
            toast('Aufgabe gespeichert');
        } else {
            await api('/tasks', { method: 'POST', body: task });
            toast('Aufgabe erstellt');
        }
        $('#task-dialog').close();
        await reloadAll();
    } catch (error) {
        errorEl.textContent = error.message;
        errorEl.hidden = false;
    }
}

async function toggleTask(id, done) {
    const task = state.tasks.find((t) => t.id === id);
    try {
        await api(`/tasks/${id}`, {
            method: 'PUT',
            body: {
                title: task.title,
                description: task.description,
                status: done ? 'erledigt' : 'offen',
                dueDate: task.dueDate,
                projectId: task.projectId,
                labelIds: task.labelIds,
            },
        });
        await reloadAll();
    } catch (error) {
        toast(error.message, true);
        await loadTasks();
    }
}

async function deleteTask(id) {
    const task = state.tasks.find((t) => t.id === id);
    if (!confirm(`Aufgabe "${task.title}" löschen?`)) return;
    try {
        await api(`/tasks/${id}`, { method: 'DELETE' });
        toast('Aufgabe gelöscht');
        await reloadAll();
    } catch (error) {
        toast(error.message, true);
    }
}

// ---------- Projekte & Labels ----------

async function createProject(event) {
    event.preventDefault();
    const form = event.target;
    try {
        const project = await api('/projects', { method: 'POST', body: { name: form.name.value.trim() } });
        form.reset();
        state.filter.projectId = project.id;
        toast('Projekt erstellt');
        await reloadAll();
    } catch (error) {
        toast(error.message, true);
    }
}

async function createLabel(event) {
    event.preventDefault();
    const form = event.target;
    try {
        await api('/labels', { method: 'POST', body: { name: form.name.value.trim(), color: form.color.value } });
        form.name.value = '';
        toast('Label erstellt');
        await reloadAll();
    } catch (error) {
        toast(error.message, true);
    }
}

async function deleteProject(id) {
    const project = state.projects.find((p) => p.id === id);
    if (!confirm(`Projekt "${project.name}" löschen? Die Aufgaben bleiben ohne Projekt erhalten.`)) return;
    try {
        await api(`/projects/${id}`, { method: 'DELETE' });
        if (state.filter.projectId === id) state.filter.projectId = null;
        toast('Projekt gelöscht');
        await reloadAll();
    } catch (error) {
        toast(error.message, true);
    }
}

async function deleteLabel(id) {
    const label = state.labels.find((l) => l.id === id);
    if (!confirm(`Label "${label.name}" löschen?`)) return;
    try {
        await api(`/labels/${id}`, { method: 'DELETE' });
        if (state.filter.labelId === id) state.filter.labelId = null;
        toast('Label gelöscht');
        await reloadAll();
    } catch (error) {
        toast(error.message, true);
    }
}

// ---------- Events ----------

function bindEvents() {
    document.querySelectorAll('.tab').forEach((tab) => {
        tab.addEventListener('click', () => setAuthMode(tab.dataset.mode));
    });
    $('#auth-form').addEventListener('submit', submitAuth);
    $('#logout').addEventListener('click', logout);

    $('#status-filter').addEventListener('click', (event) => {
        const button = event.target.closest('button');
        if (!button) return;
        state.filter.status = button.dataset.status;
        document.querySelectorAll('#status-filter button').forEach((b) => b.classList.toggle('active', b === button));
        loadTasks().catch((error) => toast(error.message, true));
    });

    $('#project-list').addEventListener('click', (event) => {
        const del = event.target.closest('[data-delete-project]');
        if (del) {
            event.stopPropagation();
            deleteProject(Number(del.dataset.deleteProject));
            return;
        }
        const item = event.target.closest('li');
        if (!item) return;
        state.filter.projectId = item.dataset.project ? Number(item.dataset.project) : null;
        renderSidebar();
        loadTasks().catch((error) => toast(error.message, true));
    });

    $('#label-list').addEventListener('click', (event) => {
        const del = event.target.closest('[data-delete-label]');
        if (del) {
            event.stopPropagation();
            deleteLabel(Number(del.dataset.deleteLabel));
            return;
        }
        const item = event.target.closest('li');
        if (!item) return;
        const id = Number(item.dataset.label);
        // Nochmals klicken entfernt den Label-Filter
        state.filter.labelId = state.filter.labelId === id ? null : id;
        renderSidebar();
        loadTasks().catch((error) => toast(error.message, true));
    });

    $('#project-form').addEventListener('submit', createProject);
    $('#label-form').addEventListener('submit', createLabel);

    $('#new-task').addEventListener('click', () => openTaskDialog());
    $('#task-form').addEventListener('submit', submitTask);
    $('#task-cancel').addEventListener('click', () => $('#task-dialog').close());

    $('#task-list').addEventListener('click', (event) => {
        const edit = event.target.closest('[data-edit]');
        if (edit) {
            openTaskDialog(state.tasks.find((t) => t.id === Number(edit.dataset.edit)));
            return;
        }
        const del = event.target.closest('[data-delete]');
        if (del) deleteTask(Number(del.dataset.delete));
    });
    $('#task-list').addEventListener('change', (event) => {
        if (event.target.dataset.toggle) {
            toggleTask(Number(event.target.dataset.toggle), event.target.checked);
        }
    });
}

// ---------- Start ----------

bindEvents();
const saved = loadSession();
if (saved?.token) {
    state.session = saved;
    showApp();
} else {
    showAuth();
}
