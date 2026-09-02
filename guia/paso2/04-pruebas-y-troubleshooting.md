# 4. Pruebas y troubleshooting

## 4.1. Confirmar que sin token, la API rechaza la petición

```bash
curl -i http://<IP_PUBLICA_EC2>:8080/api/hello
```

Debe responder `401 Unauthorized`. Si en cambio responde `200` con el mensaje, la seguridad no quedó activa — revisa que el deploy haya llegado con los cambios del [paso 2](02-actualizar-proyecto-spring-boot.md) y que el proceso se haya reiniciado (compara el PID/hora de inicio en `ps aux | grep java` contra la hora del último deploy).

## 4.2. Obtener un token M2M y probar el endpoint protegido

Script de referencia `test.sh` (usa tu `.env` local del [paso 2.5](02-actualizar-proyecto-spring-boot.md#25-manejo-local-de-secretos-env-no-lo-subas-al-repo) — nunca el `AZURE_CLIENT_SECRET` en texto plano en un script commiteado):

```bash
#!/bin/bash

if [ -f .env ]; then
  set -o allexport
  source .env
  set +o allexport
else
  echo "❌ Error: No se encontró el archivo .env"
  exit 1
fi

echo "🔑 Solicitando Access Token a Microsoft Entra ID..."

TOKEN_RESPONSE=$(curl -s -X POST "https://login.microsoftonline.com/${AZURE_TENANT_ID}/oauth2/v2.0/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=${AZURE_CLIENT_ID}" \
  -d "scope=${AZURE_CLIENT_ID}/.default" \
  -d "client_secret=${AZURE_CLIENT_SECRET}" \
  -d "grant_type=client_credentials")

ACCESS_TOKEN=$(echo "$TOKEN_RESPONSE" | python3 -c "import sys, json; print(json.load(sys.stdin).get('access_token', ''))" 2>/dev/null)

if [ -z "$ACCESS_TOKEN" ] || [ "$ACCESS_TOKEN" == "None" ]; then
  echo "❌ Error al obtener el token:"
  echo "$TOKEN_RESPONSE"
  exit 1
fi

echo "✅ Token obtenido exitosamente."
echo "🚀 Enviando petición a $API_URL..."
echo ""

curl -i -X GET "$API_URL" \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

```bash
chmod +x test.sh
./test.sh
```

Resultado esperado:

```http
HTTP/1.1 200 OK
Content-Type: application/json

{"message":"Hello, World!"}
```

## 4.3. Matriz de troubleshooting

| Error encontrado | Causa raíz | Solución aplicada |
|---|---|---|
| `ClassNotFoundException` / `IllegalArgumentException` al levantar el contexto de Spring | Conflicto de versiones al usar `spring-cloud-azure-starter-active-directory`. | Se eliminó esa dependencia y se usó el starter estándar `spring-boot-starter-oauth2-resource-server` (ver [paso 2.1](02-actualizar-proyecto-spring-boot.md#21-dependencia-buildgradle)). |
| `401 Unauthorized` incluso con un token válido | `issuer-uri` o `audiences` mal configurados — no coinciden con lo que emite Entra ID. | Verificar que `issuer-uri` termine en `/v2.0` y que `audiences` sea exactamente el `AZURE_APP_ID_URI` configurado en "Expose an API" ([paso 2.2](02-actualizar-proyecto-spring-boot.md#22-propiedades-srcmainresourcesapplicationproperties)). |
| `403 Forbidden` (`insufficient_scope`) con un token que sí llega | El flujo `client_credentials` emite **App Roles** (`roles`), pero la API buscaba **scopes** delegados (`scp` / `SCOPE_OT.Read`). | 1) Crear el App Role `OT.Read` para *Applications* y otorgar *Admin Consent* ([paso 1.3-1.4](01-configurar-azure-entra-id.md#13-crear-el-app-role)). 2) Agregar el `JwtAuthenticationConverter` que lee `roles` y usar `hasRole('OT.Read')` en vez de `hasAuthority('SCOPE_...')` ([paso 2.3-2.4](02-actualizar-proyecto-spring-boot.md#23-configuración-de-seguridad-srcmainjavaconfigsecurityconfigjava)). |
| `GH013: Repository rule violations found` (Push Protection) al hacer `git push` | Un archivo `.env` con un secret real de Azure quedó incluido en un commit. | Agregar `.env` a `.gitignore`, sacarlo del staging con `git rm --cached .env`, y si ya estaba en el historial, reescribirlo (`git rebase`/`filter-branch` o recrear el commit) **antes** de reintentar el push. Nunca forzar el push dejando el secret en el historial. |

## 4.4. Checklist final

- [ ] `curl` sin `Authorization` → `401`
- [ ] `curl` con token válido y rol `OT.Read` → `200` con el JSON esperado
- [ ] `AZURE_CLIENT_SECRET` no está en ningún archivo commiteado (`git log -p -- .env` no debe mostrar nada)
- [ ] Los 3 secrets de Azure (`AZURE_CLIENT_ID`, `AZURE_TENANT_ID`, `AZURE_APP_ID_URI`) están configurados en GitHub Actions, no solo en tu `.env` local
