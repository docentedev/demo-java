# Demo Spring Boot — AWS EC2 & GitHub Actions + Microsoft Entra ID

Proyecto de demostración de una API REST en **Spring Boot (Java)**, con despliegue continuo (CI/CD) automatizado a una instancia **AWS EC2** vía **GitHub Actions**, y autenticación **OAuth2 machine-to-machine** con **Microsoft Entra ID**.

## 🚀 Endpoint

- **Base URL:** `http://<IP_PUBLICA_EC2>:8080`
- **Endpoint:** `GET /api/hello` → `{"message":"Hello, World!"}` (requiere `Authorization: Bearer <token>` desde el Paso 2 en adelante)

## 🛠️ Stack

- Java 21 (Amazon Corretto en el servidor, Temurin en CI)
- Spring Boot 3.3.x + Gradle
- AWS EC2 (`t3.micro`, Amazon Linux 2023)
- GitHub Actions (SSH/SCP nativo de OpenSSH)
- Microsoft Entra ID (OAuth2 Resource Server, flujo `client_credentials`)

## 📖 Guía paso a paso

La guía completa, ordenada y con las lecciones aprendidas de cada problema real encontrado en el camino, está en [`guia/`](guia/):

1. **[Paso 1 — API en EC2 con despliegue automático](guia/paso1/README.md)**: crear el proyecto con Spring Initializr, configurar la instancia EC2, y desplegar con GitHub Actions.
2. **[Paso 2 — Seguridad con Microsoft Entra ID](guia/paso2/README.md)**: registrar la app en Azure, agregar validación de JWT/roles en Spring Security, y actualizar el pipeline para desplegar los cambios.

Cada paso incluye una sección de troubleshooting con los errores reales que aparecieron durante la implementación (no solo la teoría) — vale la pena leerla si algo no funciona a la primera.
