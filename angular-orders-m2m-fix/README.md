# FIX: Keycloak client_credentials + Postman for Angular Orders REST API

## The defect in the original deliverable
The `angular-orders` public SPA client uses Authorization Code + PKCE and **cannot** authenticate with `client_credentials`. The initial project had no service-account client able to call the API. This package fixes that omission.

## Existing Keycloak realm (your running server at localhost:8080)
**Do not re-import the whole realm into a running instance:** Keycloak will not automatically apply modified realm JSON to an already-existing realm. Instead, use `configure_existing_keycloak.py`:

1. In Terminal set `KEYCLOAK_URL=http://localhost:8080`; run `python3 configure_existing_keycloak.py`.
2. Enter your **master realm admin** password at the prompt (or set `KEYCLOAK_ADMIN_PASSWORD` in the environment). This one-time setup uses Keycloak Admin REST APIs, creates confidential client `postman-orders-m2m`, enables service accounts, assigns `CUSTOMER` and `ADMIN` realm roles to the service-account user, and adds access-token audience `orders-api`.
3. Copy the output **client secret** into `m2mClientSecret` in Postman environment **Angular Orders M2M — Local 8080**. Set its variable type to *secret* if your Postman version offers it. Do not store the secret in source control.
4. Import the two `.postman_*.json` files into Postman; select environment **Angular Orders M2M — Local 8080**.
5. Run folder `00 — START HERE — M2M token` > `01 POST Get M2M Access Token — client_credentials`. The test script saves `accessToken` in the environment and validates audience and roles. Then run `GET /api/me`, `GET /api/products` and the order tests.
6. After obtaining a token, check `GET /api/admin/orders` — expected `200`, because the service account was explicitly assigned ADMIN.

**Important:** This is a *machine identity*, not `alice`. The `sub` claim belongs to the Keycloak service-account user. New orders belong to the service-account principal. `GET /api/orders` lists its orders, not Alice's. `GET seeded Alice order` works under ADMIN because this API grants admins visibility of other orders.

## Fresh Keycloak realm
The included `bootcamp-orders-realm-with-m2m.json` defines the additional confidential client alongside the original sample clients/users. It is intended **only for a fresh realm import**, not to override your existing environment. For reliable service-account role assignment and secret retrieval, run `configure_existing_keycloak.py` after startup, including for a fresh import.

## Why `orders-api` cannot simply be used without changes
The original `orders-api` Keycloak client had `serviceAccountsEnabled=false`; and the Spring Boot backend rejects tokens missing `aud=orders-api` or the appropriate `realm_access.roles`. The setup configures a **separate, least-confusing dedicated confidential test client**. In production prefer least privilege, separate ADMIN and CUSTOMER machine clients, and secrets managed by a vault or short-lived workload credentials. Here the service account has ADMIN only to exercise every training endpoint.

## Token call equivalent
```
POST http://localhost:8080/realms/bootcamp-orders/protocol/openid-connect/token
Content-Type: application/x-www-form-urlencoded

grant_type=client_credentials
client_id=postman-orders-m2m
client_secret=<secret copied from Keycloak setup>
```
The response contains an **access_token**. Send only that token as `Authorization: Bearer <access_token>` to the REST API on localhost:9090. No interactive browser, redirect URI, PKCE, or ID token is used for this grant. Refresh by requesting another token when it expires.

## Scope of changes
- New Postman collection with exact API paths, M2M token request, bearer variable, and tests.
- New Postman environment (Keycloak **8080**, backend **9090**, blank secret field).
- Python standard-library setup utility for your **existing** Keycloak realm.
- An amended **fresh-import** realm JSON artifact.
- **No changes required** to Spring Boot Java code, Angular code, or MySQL tables.

## Validation and limits
Local offline generation verified JSON parsing, ZIP integrity and request list. Live Keycloak role assignment, Postman execution, and Spring Boot endpoint tests have **not** been performed here.
