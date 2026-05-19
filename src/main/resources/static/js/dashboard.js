if (!localStorage.getItem('token')) window.location.href = '/app/login.html';

document.getElementById('user-name').textContent = localStorage.getItem('userName') || '';

const host = window.location.origin;

function logout() {
    localStorage.clear();
    window.location.href = '/app/login.html';
}

async function loadDashboard() {
    const [stats, urls] = await Promise.all([
        apiFetch('/dashboard'),
        apiFetch('/urls/mine')
    ]);

    document.getElementById('stat-total').textContent  = stats.totalUrls;
    document.getElementById('stat-clicks').textContent = stats.totalClicks;
    document.getElementById('stat-active').textContent = stats.activeUrls;

    renderTable(urls);
}

function renderTable(urls) {
    const tbody = document.getElementById('urls-body');
    if (urls.length === 0) {
        tbody.innerHTML = '<tr><td colspan="4" class="empty">No tienes URLs todavía. ¡Crea la primera!</td></tr>';
        return;
    }
    tbody.innerHTML = urls.map(url => {
        const shortUrl = `${host}/${url.code}`;
        const safeCode = url.code.replace(/'/g, "\\'");
        const safeShort = shortUrl.replace(/'/g, "\\'");
        return `<tr>
            <td>
                <a href="${shortUrl}" target="_blank" class="short-link">${shortUrl}</a>
                <button class="btn-copy" onclick="copyUrl('${safeShort}', this)" title="Copiar">⎘</button>
            </td>
            <td class="long-url" title="${url.longUrl}">${url.longUrl}</td>
            <td class="date">${formatDate(url.createdAt)}</td>
            <td><button class="btn-delete" onclick="deleteUrl('${safeCode}', this)">Eliminar</button></td>
        </tr>`;
    }).join('');
}

function formatDate(iso) {
    return new Date(iso).toLocaleDateString('es-ES', { day: '2-digit', month: 'short', year: 'numeric' });
}

async function handleCreate(e) {
    e.preventDefault();
    const errorEl  = document.getElementById('create-error');
    const btn      = document.getElementById('create-submit');
    const urlInput = document.getElementById('new-url');
    const aliasInput = document.getElementById('new-alias');

    errorEl.classList.add('hidden');
    btn.disabled = true;
    btn.textContent = 'Creando…';

    try {
        const body = { url: urlInput.value };
        const alias = aliasInput.value.trim();
        if (alias) body.alias = alias;

        await apiFetch('/urls', { method: 'POST', body: JSON.stringify(body) });
        urlInput.value = '';
        aliasInput.value = '';
        await loadDashboard();
    } catch (err) {
        errorEl.textContent = err.message;
        errorEl.classList.remove('hidden');
    } finally {
        btn.disabled = false;
        btn.textContent = 'Acortar';
    }
}

async function deleteUrl(code, btn) {
    btn.disabled = true;
    btn.textContent = '…';
    try {
        await apiFetch('/urls/' + code, { method: 'DELETE' });
        await loadDashboard();
    } catch (err) {
        alert(err.message);
        btn.disabled = false;
        btn.textContent = 'Eliminar';
    }
}

function copyUrl(url, btn) {
    navigator.clipboard.writeText(url).then(() => {
        const prev = btn.textContent;
        btn.textContent = '✓';
        setTimeout(() => btn.textContent = prev, 1500);
    });
}

loadDashboard();
