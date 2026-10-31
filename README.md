# Angular Orders Backend — Spring Boot 3.5.9 + Spring Security + Keycloak + MySQL 8

## What was provided vs. assumed
The uploaded `lesson-angular-crash-course.html` is only a Thymeleaf wrapper referencing `internships/angular-crash-course :: lesson`. It does **not** contain API specifications. This project is an illustrative e-commerce Orders API designed as a companion to an Angular crash-course exercise; it is not extracted from an unseen detailed lesson.

## Stack
- Java 21, Spring Boot 3.5.9, Spring Security 6, OAuth2 Resource Server, JDBC transactions, Jakarta Validation
- Keycloak 26.2.5 realm import; public Angular client (`angular-orders`) with Authorization Code + PKCE (S256)
- Keycloak JWT issuer `http://localhost:8180/realms/bootcamp-orders`; audience `orders-api`
- MySQL 8.4 (MySQL 8 LTS family), InnoDB schema and seed data

## Start infrastructure
```bash
cd angular-orders-backend
docker compose up -d
# MySQL on localhost:3307; Keycloak on localhost:8180
# After the infrastructure is healthy:
mvn clean package
mvn spring-boot:run
```
**IMPORTANT** The MySQL init scripts run only when the `mysql_data` Docker volume is first created. To inspect: `docker compose logs mysql keycloak`. This is a development-only demo: replace all credentials, remove demo users, require TLS, configure persistent production Keycloak storage, and use a non-root production database account.

The sample uses Keycloak `start-dev`, which is not production-grade. The Keycloak realm import will be skipped if the realm already exists. If changing realm JSON with existing Keycloak state, recreate the development Keycloak state or import the revised realm using supported admin tooling.

## Log in (Angular)
- Authorization endpoint: `http://localhost:8180/realms/bootcamp-orders/protocol/openid-connect/auth`
- Token endpoint: `http://localhost:8180/realms/bootcamp-orders/protocol/openid-connect/token`
- Discovery: `http://localhost:8180/realms/bootcamp-orders/.well-known/openid-configuration`
- Angular client ID: `angular-orders` (public client; no client secret; Authorization Code + PKCE S256)
- Valid development accounts: `alice / AliceDev123!` (CUSTOMER) and `admin / AdminDev123!` (ADMIN). Never deploy with these accounts/passwords.
- SPA should use a standards-compliant OIDC client library and keep tokens out of localStorage when possible. Use Authorization: Bearer <access_token> for API calls (not ID token).
- Refresh tokens, when issued, are managed by the OIDC client and must not be sent to resource APIs.

## API endpoints (Authorization: Bearer ACCESS_TOKEN)
| Method | URL | Access | Description |
|---|---|---|---|
| GET | `/api/me` | Authenticated | JWT subject and preferred username |
| GET | `/api/products` | CUSTOMER / ADMIN | Catalog |
| GET | `/api/orders` | CUSTOMER / ADMIN | Own orders (JWT `sub`) |
| GET | `/api/orders/{id}` | Owner or ADMIN | Read order with lines |
| POST | `/api/orders` | CUSTOMER / ADMIN | Transactionally create order and decrement stock |
| GET | `/api/admin/orders` | ADMIN | Latest 100 orders |

### Example POST payload
```json
{"items":[{"productId":101,"quantity":1},{"productId":102,"quantity":2}]}
```
The backend retrieves trusted unit prices from MySQL and uses conditional stock updates inside one database transaction. A failure rolls back all inventory and order changes. It does not integrate payments, delivery/shipping, an idempotency key, or production-grade concurrency/load tests.

### Angular HTTP example
```typescript
const headers = new HttpHeaders({Authorization: `Bearer ${accessToken}`});
this.http.get<Product[]>('http://localhost:9090/api/products', {headers}).subscribe(...);
```
Use an interceptor in a real Angular app to apply bearer tokens to API calls. The Angular UI project itself is not part of this ZIP.

## Useful commands
```bash
curl -i http://localhost:9090/api/products  # Expected 401 without token
curl -H "Authorization: Bearer $ACCESS_TOKEN" http://localhost:9090/api/products
curl -H "Authorization: Bearer $ACCESS_TOKEN" http://localhost:9090/api/orders
curl -X POST http://localhost:9090/api/orders -H "Authorization: Bearer $ACCESS_TOKEN" -H "Content-Type: application/json" -d '{"items":[{"productId":101,"quantity":1}]}'
```

## Architecture and security boundaries
Angular SPA → Keycloak login → access token containing `aud=orders-api` and roles → Spring Boot validates signature/issuer/expiry/audience → method authorization + order ownership → MySQL InnoDB transaction. CORS permits only `http://localhost:4200`; it is not a security boundary. CSRF is disabled because this is a stateless bearer-token API, not a cookie-authenticated session application.

## Notes
- `mysql:8.4` is part of the MySQL 8 family; replace with a supported pinned patch image for reproducible production builds.
- Keycloak realm user UUIDs are intentionally fixed so seeded orders match Alice's JWT `sub`.
- `allowPublicKeyRetrieval=true` and `useSSL=false` are for localhost development only.
- Spring Boot 3.5.9 is deliberately pinned as requested; review current security advisories and upgrades for any non-demo deployment.
