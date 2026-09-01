# 🛡️ Guía: Configurando Spring Security en el Backend (Microsoft Entra ID)

Esta guía detalla los pasos para configurar el backend como un **Resource Server** capaz de validar tokens JWT emitidos por **Microsoft Entra ID**, protegiendo los endpoints mediante scopes (como `OT.Read`).

---

## 📋 Contexto y Objetivo

Cuando una SPA (Angular, React, etc.) utiliza **MSAL** para obtener un `Access Token` desde Microsoft Entra ID, dicho token debe ser validado por el backend antes de permitir el acceso a los recursos.

Nuestro servidor actúa como un **Resource Server** que:

- Recibe tokens JWT emitidos por Microsoft Entra ID.
- Valida la **firma**, la **audiencia (`aud`)**, el **issuer** y los **scopes**.
- Permite el acceso únicamente si el token es válido y está autorizado.

---

## ⚙️ 1. Requisitos Previos y Registro en Entra ID

Antes de configurar el código, la aplicación debe estar registrada en Microsoft Entra ID para obtener los siguientes parámetros obligatorios:

- **Application (client ID):** Identifica el backend como una API protegida.
- **Directory (tenant ID):** Identificador del tenant.
- **Application ID URI:** Define la audiencia aceptada por la API (ej. `api://tallerpro360`) o deja el nombre por defecto.
- **Scopes expuestos:** Permisos específicos requeridos (ej. `OT.Read`).

---

## 📦 2. Dependencias en Gradle (Groovy)

Agrega las siguientes dependencias en tu archivo `build.gradle` dentro del bloque `dependencies`:

```groovy
dependencies {
    // Convierte el backend en un validador de tokens JWT (OAuth2 Resource Server)
    implementation 'org.springframework.boot:spring-boot-starter-oauth2-resource-server'

    // Soporte de Spring Cloud Azure para integrar con Microsoft Entra ID
    implementation 'com.azure.spring:spring-cloud-azure-starter-active-directory'
}

```

---

## 🔐 3. Configuración de Variables de Entorno y `application.properties`

Para mantener la seguridad del proyecto y evitar exponer credenciales en el código fuente, la configuración se maneja mediante variables de entorno que se leen en el archivo `src/main/resources/application.properties`:

```properties
# Microsoft Entra ID (Azure AD) Resource Server Configuration
spring.cloud.azure.active-directory.credential.client-id=${AZURE_CLIENT_ID}
spring.cloud.azure.active-directory.tenant-id=${AZURE_TENANT_ID}
spring.cloud.azure.active-directory.app-id-uri=${AZURE_APP_ID_URI}

```

---

## 🔒 4. Configuración de Seguridad (`SecurityConfig.java`)

Crea la clase de configuración para habilitar la protección basada en anotaciones y exigir autenticación obligatoria en todas las rutas:

```java
package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity // Permite proteger métodos usando @PreAuthorize
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // Ninguna ruta puede accederse sin un token autorizado
                .anyRequest().authenticated()
            )
            // Indica que el backend validará JWT emitidos por Microsoft Entra ID
            .oauth2ResourceServer(oauth2 -> oauth2.jwt());

        return http.build();
    }
}

```

---

## 🌐 5. Implementación del Controlador (`HelloController.java`)

Protege endpoints específicos utilizando la validación de scopes concretos:

```java
package com.example.demo.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class HelloController {

    @GetMapping("/hello")
    @PreAuthorize("hasAuthority('SCOPE_OT.Read')")
    public Map<String, String> hello() {
        return Map.of("message", "Hello, World!");
    }
}

```

---

## 🚀 6. Configuración de Secrets en GitHub y Pipeline (`deploy.yml`)

### Configuración de Secrets

Dirígete a tu repositorio en GitHub y ve a **Settings > Secrets and variables > Actions**. Crea los siguientes secrets con los datos de tu registro en Azure:

* `AZURE_CLIENT_ID` -> Application (client ID) de tu API.
* `AZURE_TENANT_ID` -> Directory (tenant ID) de tu directorio.
* `AZURE_APP_ID_URI` -> Application ID URI (ej. `api://tallerpro360`).

### Inyección en el Workflow de Despliegue

En el paso de tu pipeline encargado de ejecutar la aplicación en la instancia EC2 por SSH, exporta las variables de entorno antes de levantar el archivo `.jar`:

```yaml
- name: Start Application on EC2
  run: |
    ssh -i "tu-llave.pem" ubuntu@tu-ip-publica << 'EOF'
      # Exportar variables de entorno desde los Secrets de GitHub
      export AZURE_CLIENT_ID="${{ secrets.AZURE_CLIENT_ID }}"
      export AZURE_TENANT_ID="${{ secrets.AZURE_TENANT_ID }}"
      export AZURE_APP_ID_URI="${{ secrets.AZURE_APP_ID_URI }}"

      # Limpiar procesos anteriores y arrancar la app en segundo plano
      fuser -k 8080/tcp || true
      pkill -x java || true
      setsid java -jar tu-proyecto.jar > app.log 2>&1 &
    EOF

```

```
curl -X POST "https://login.microsoftonline.com/de20110d-c44f-4da1-9a22-4c390d9d7e26/oauth2/v2.0/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=426120e7-937d-4322-9595-549ab987e7d8" \
  -d "scope=api://426120e7-937d-4322-9595-549ab987e7d8/.default" \
  -d "client_secret=TU_CLIENT_SECRET_AQUI" \
  -d "grant_type=client_credentials"

curl -X GET http://3.15.232.215:8080/api/hello \
  -H "Authorization: Bearer <TU_ACCESS_TOKEN_AQUI>"
```