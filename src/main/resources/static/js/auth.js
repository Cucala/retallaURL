if (localStorage.getItem('token')) {
    window.location.href = '/app/dashboard.html';
}

function showTab(tab) {
    document.getElementById('form-login').classList.toggle('hidden', tab !== 'login');
    document.getElementById('form-register').classList.toggle('hidden', tab !== 'register');
    document.getElementById('tab-login').classList.toggle('active', tab === 'login');
    document.getElementById('tab-register').classList.toggle('active', tab === 'register');
    document.getElementById('login-error').classList.add('hidden');
    document.getElementById('reg-error').classList.add('hidden');
}

async function handleLogin(e) {
    e.preventDefault();
    const errorEl = document.getElementById('login-error');
    const btn = document.getElementById('login-submit');

    errorEl.classList.add('hidden');
    btn.disabled = true;
    btn.textContent = 'Entrando…';

    try {
        const data = await apiFetch('/auth/login', {
            method: 'POST',
            body: JSON.stringify({
                email: document.getElementById('login-email').value,
                password: document.getElementById('login-password').value
            })
        });
        saveSession(data);
        window.location.href = '/app/dashboard.html';
    } catch (err) {
        showError(errorEl, err.message);
        btn.disabled = false;
        btn.textContent = 'Entrar';
    }
}

async function handleRegister(e) {
    e.preventDefault();
    const errorEl = document.getElementById('reg-error');
    const btn = document.getElementById('reg-submit');

    errorEl.classList.add('hidden');
    btn.disabled = true;
    btn.textContent = 'Creando cuenta…';

    try {
        const data = await apiFetch('/auth/register', {
            method: 'POST',
            body: JSON.stringify({
                name: document.getElementById('reg-name').value,
                email: document.getElementById('reg-email').value,
                password: document.getElementById('reg-password').value
            })
        });
        saveSession(data);
        window.location.href = '/app/dashboard.html';
    } catch (err) {
        showError(errorEl, err.message);
        btn.disabled = false;
        btn.textContent = 'Crear cuenta';
    }
}

function saveSession(data) {
    localStorage.setItem('token', data.token);
    localStorage.setItem('userEmail', data.user.email);
    localStorage.setItem('userName', data.user.name);
}

function showError(el, message) {
    el.textContent = message;
    el.classList.remove('hidden');
}
