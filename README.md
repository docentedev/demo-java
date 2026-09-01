# Demo Spring Boot - AWS EC2 & GitHub Actions Deployment

Proyecto de demostración de una API REST desarrollada en **Spring Boot (Java)** con un pipeline de integración y despliegue continuo (CI/CD) automatizado hacia una instancia **Amazon EC2** utilizando **GitHub Actions**.

---

## 🛠️ Tecnologías y Stack

- **Java 21** (Amazon Corretto)
- **Spring Boot** (Gradle como gestor de dependencias y construcción)
- **AWS EC2** (Instancia `t3.micro` con Amazon Linux)
- **GitHub Actions** (CI/CD nativo mediante OpenSSH y SCP)

---

## 🚀 Endpoints de la API

- **Base URL:** `http://<IP_PUBLICA>:8080`
- **Endpoint de prueba:** `GET /api/hello`
  - **Respuesta esperada:**
    ```json
    { "message": "Hello, World!" }
    ```

---

## ⚙️ Arquitectura del Despliegue Automatizado (CI/CD)

Cada vez que se realiza un `push` a la rama principal (`main` / `master`), el workflow de GitHub Actions (`.github/workflows/deploy.yml`) ejecuta los siguientes pasos de forma completamente autónoma:

1. **Compilación:** Utiliza el runner de GitHub para compilar el proyecto y empaquetarlo en un archivo `.jar` ejecutable mediante Gradle (`./gradlew bootJar -x test`).
2. **Transferencia:** Copia de forma segura el archivo `.jar` generado directamente a la instancia EC2 utilizando `scp` nativo.
3. **Provisionamiento y Aprovisionamiento Remoto (SSH):**
   - **Idempotencia de Java:** Comprueba e instala automáticamente el runtime `java-21-amazon-corretto` en la instancia si no se encuentra presente.
   - **Limpieza de procesos:** Detiene cualquier instancia previa de la aplicación escuchando en el puerto `8080` (`fuser -k 8080/tcp`) o procesos `java` residuales (`pkill -x java`).
   - **Ejecución en segundo plano:** Inicia la aplicación con `setsid` para asegurar que el proceso corra de manera independiente y persista tras finalizar la sesión SSH del pipeline.
4. **Concurrencia Controlada:** Configurado con `cancel-in-progress: true` para evitar conflictos si se realizan múltiples commits seguidos.

---

## 🔧 Configuración Requerida en AWS EC2

Para que la conexión y el despliegue funcionen correctamente, se configuraron los siguientes parámetros en la infraestructura de AWS:

- **Par de Claves (Key Pair):** Tipo RSA con formato `.pem`.
- **Grupo de Seguridad (Security Group):**
  - **Puerto 22 (SSH):** Acceso para administración y despliegue.
  - **Puerto 80 (HTTP) y 443 (HTTPS):** Tráfico web general.
  - **Puerto 8080 (TCP Personalizado):** Abierto a `0.0.0.0/0` para la recepción de peticiones de la API de Spring Boot.

---

## 🔐 Configuração de Secrets en GitHub

Para permitir el acceso seguro desde GitHub Actions hacia tu servidor AWS, configura los siguientes Secrets en tu repositorio (`Settings` > `Secrets and variables` > `Actions`):

- `EC2_HOST`: Dirección IP pública (IPv4) de tu instancia EC2.
- `EC2_SSH_KEY`: Contenido completo de tu archivo de clave privada `.pem`.

---

## 🔍 Notas de Troubleshooting y Lecciones Aprendidas

- **Gestión de procesos Java (`pkill`):** Se evita el uso de flags genéricas como `pkill -f "java -jar"` ya que afectan al propio comando del intérprete del shell en ejecución. Se utiliza estrictamente `pkill -x java` para apuntar únicamente al nombre exacto del proceso.
- **Permisos de la Clave Privada:** Si realizas conexiones manuales por SSH y obtienes un error de `Permission denied (publickey)`, asegúrate de restringir los permisos locales del archivo de llave con:
  ```bash
  chmod 600 tu-llave.pem
  ```
