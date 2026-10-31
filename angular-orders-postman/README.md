# Postman — Angular Orders API

Collection is grounded in `OrdersController.java`, the DTOs, `SecurityConfiguration.java`, the Keycloak realm JSON, and the seed data of the previously generated `angular-orders-backend.zip`.

## Import and configure
1. Import both JSON files into Postman; select **Angular Orders — Local Docker** environment.
2. Start infrastructure (`docker compose up -d`) and API (`mvn spring-boot:run`) in the backend project.
3. **Required one-time Keycloak setup:** Open `http://localhost:8180/admin/`, choose realm `bootcamp-orders`, Clients → `angular-orders` → Settings. Add `https://oauth.pstmn.io/v1/callback` to **Valid redirect URIs** and Save. This callback is **not** included in the original realm export, which permits only `http://localhost:4200/*`. Do not use an unrestricted `*` redirect. Some Postman versions display a different callback URL; use the exact callback shown by your Postman Authorization panel instead.
4. Select collection → Authorization → OAuth 2.0. Verify Authorization Code + PKCE settings (S256), auth URL `http://localhost:8180/realms/bootcamp-orders/protocol/openid-connect/auth`, token URL `http://localhost:8180/realms/bootcamp-orders/protocol/openid-connect/token`, client ID `angular-orders`, scope `openid profile email`, no client secret. Click **Get New Access Token**, sign in as `alice` / `AliceDev123!`, then **Use Token**.
5. Execute `02 — Profile & catalog`, then `03 — Customer order workflow`. Change token to `admin` / `AdminDev123!` for admin tests.

### Important
- Use the **access token**, never an ID token or refresh token, in the API Authorization header. The API checks `iss`, `aud=orders-api`, expiry, and roles.
- The public `angular-orders` client **disables** direct access grants. Do not use Postman's Password grant.
- Postman may require checking **Authorize using browser** and **PKCE**. If the collection doesn't automatically populate an OAuth2 setting in your Postman version, enter it manually as above.
- The existing Keycloak realm JSON isn't automatically updated if the realm was imported already; the redirect URI must be added via Admin Console.
- Product 101 has seeded inventory 12, so the valid create test consumes inventory and can later fail with 409. Repeating tests mutates demo data.
- `GET /api/orders/{{seedOrderId}}` accesses Alice's seeded order when using Alice (or ADMIN) token. Customer users who don't own it get 404, not 403, to avoid order enumeration.
- Per user/role tests intentionally require switching accounts; running the entire collection under one token will make role-specific assertions fail.
- The API routes are protected, including `/api/me` which needs a bearer token.
- The collection has no client secret and doesn't store user passwords. Never export live access tokens.
