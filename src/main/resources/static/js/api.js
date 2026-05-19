async function apiFetch(path, options = {}) {
    const token = localStorage.getItem('token');
    const headers = { 'Content-Type': 'application/json', ...options.headers };
    if (token) headers['Authorization'] = 'Bearer ' + token;

    const res = await fetch(path, { ...options, headers });

    if (res.status === 401) {
        localStorage.clear();
        window.location.href = '/app/login.html';
        throw new Error('Sesión expirada');
    }

    const text = await res.text();
    const data = text ? JSON.parse(text) : null;

    if (!res.ok) {
        throw new Error(data?.error || `Error ${res.status}`);
    }

    return data;
}
