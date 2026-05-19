# Snap API

Base URL: `http://localhost:3000`

Todos los cuerpos de petición y respuesta son JSON (`Content-Type: application/json`).  
Los endpoints marcados con 🔒 requieren cabecera `Authorization: Bearer <token>`.

---

## Autenticación

### POST /auth/register

Registra un nuevo usuario.

**Body**
```json
{
  "email":    "usuario@example.com",
  "password": "mipassword",
  "name":     "Nombre Apellido"
}
```

**Respuestas**

| Código | Descripción |
|--------|-------------|
| `201`  | Usuario creado. Devuelve token + datos del usuario. |
| `400`  | Faltan campos o el email/password no cumple el formato. |
| `409`  | El email ya está registrado. |

**Respuesta 201**
```json
{
  "token": "eyJhbGci...",
  "user": {
    "id": 1,
    "email": "usuario@example.com",
    "name": "Nombre Apellido",
    "createdAt": "2026-05-20T10:00:00Z"
  }
}
```

---

### POST /auth/login

Inicia sesión con credenciales existentes.

**Body**
```json
{
  "email":    "usuario@example.com",
  "password": "mipassword"
}
```

**Respuestas**

| Código | Descripción |
|--------|-------------|
| `200`  | Login correcto. Devuelve token + datos del usuario. |
| `400`  | Faltan los campos email o password. |
| `401`  | Credenciales incorrectas. |

**Respuesta 200** — mismo esquema que `POST /auth/register 201`.

---

## URLs

### POST /urls 🔒

Crea una URL corta.

**Body**
```json
{ "url": "https://ejemplo.com/ruta/muy/larga" }
```

**Respuestas**

| Código | Descripción |
|--------|-------------|
| `201`  | URL creada. |
| `400`  | Falta el campo `url`. |
| `401`  | Token ausente o inválido. |

**Respuesta 201**
```json
{
  "code":      "aB3x9z",
  "longUrl":   "https://ejemplo.com/ruta/muy/larga",
  "createdAt": "2026-05-20T10:05:00Z"
}
```

---

### GET /urls

Lista todas las URLs del sistema, ordenadas por fecha descendente. Público.

**Respuesta 200**
```json
[
  {
    "code":      "aB3x9z",
    "longUrl":   "https://ejemplo.com/ruta/muy/larga",
    "createdAt": "2026-05-20T10:05:00Z"
  }
]
```

---

### DELETE /urls/{code} 🔒

Elimina una URL corta. Solo el propietario puede borrarla.

**Respuestas**

| Código | Descripción |
|--------|-------------|
| `204`  | Eliminada correctamente. |
| `401`  | Token ausente o inválido. |
| `403`  | La URL pertenece a otro usuario. |
| `404`  | Código no encontrado. |

---

### GET /{code}

Redirige a la URL larga asociada al código. Registra un click.

**Respuestas**

| Código | Descripción |
|--------|-------------|
| `302`  | Redirección a la URL larga (`Location` header). |
| `404`  | Código no encontrado. |

---

## Dashboard

### GET /dashboard 🔒

Devuelve un resumen analítico de las URLs del usuario autenticado:
totales globales y series temporales de actividad.

**Respuestas**

| Código | Descripción |
|--------|-------------|
| `200`  | Estadísticas del usuario. |
| `401`  | Token ausente o inválido. |

**Respuesta 200**
```json
{
  "totalUrls":   12,
  "totalClicks": 847,
  "activeUrls":  9,
  "clicksLast7Days": [
    { "date": "2026-05-14", "count": 120 },
    { "date": "2026-05-15", "count":  98 },
    { "date": "2026-05-20", "count":  44 }
  ],
  "urlsCreatedLast30Days": [
    { "date": "2026-04-20", "count": 3 },
    { "date": "2026-05-01", "count": 1 }
  ]
}
```

| Campo | Descripción |
|-------|-------------|
| `totalUrls` | Número total de URLs creadas por el usuario. |
| `totalClicks` | Suma de todos los clicks sobre sus URLs. |
| `activeUrls` | URLs con al menos un click registrado. |
| `clicksLast7Days` | Clicks agrupados por día, últimos 7 días. Solo días con actividad. |
| `urlsCreatedLast30Days` | URLs creadas por día, últimos 30 días. Solo días con actividad. |

---

## Infraestructura

### GET /health

Comprueba que el servidor está en marcha. Público.

**Respuesta 200**
```json
{ "status": "ok" }
```

---

## Errores

Todas las respuestas de error siguen el mismo esquema:

```json
{ "error": "Descripción del error" }
```
