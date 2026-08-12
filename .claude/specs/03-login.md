# Spec: Login and Logout

## Overview
This feature adds authentication to Spendly: a visitor with a registered account can log in with their email and password, and a logged-in user can log out. It is the second web-facing feature, building directly on Step 2 (`02-registration`), which created the `users` table access, `BCryptPasswordEncoder` usage pattern, and the `login.html` page shell (currently a stub that only shows a flash success message from registration). This step wires up real authentication using the servlet `HttpSession` (already available via `spring-boot-starter-web`, no Spring Security framework is in the dependency tree) — a session attribute holding the logged-in user's id is the mechanism for tracking "logged in" state, since `CLAUDE.md` prohibits layering new frameworks on top of Spring Boot. It unblocks Step 4 (`profile`) and every subsequent authenticated feature (expenses), which need to know who the current user is.

## Depends on
- Step 1 (`01-database-setup`): `users` table in `schema.sql`, `User` record, `UserRepository` (`findByEmail`).
- Step 2 (`02-registration`): `layout.html`, `style.css`, `main.js`, `RegistrationController`/`RegistrationService` patterns, `login.html` page shell (to be extended, not replaced), `spring-security-crypto` already in `pom.xml` for `BCryptPasswordEncoder`.

## Routes
- `GET /login` — render the login form (email, password fields) — public — `LoginController` (already exists as a stub; extended to show a real form instead of "Login isn't available yet")
- `POST /login` — validate credentials, start a session, redirect to `/` on success — public — `LoginController`
- `GET /logout` — invalidate the session, redirect to `/login` — logged-in — `LoginController`

## Database changes
No database changes. The `users` table (`id`, `name`, `email`, `password_hash`, `created_at`) already exists from Step 1 and `UserRepository.findByEmail` already supports the credential lookup this feature needs.

## Service layer
New `LoginService` in `service/`, called by `LoginController`:
- `authenticate(String email, String rawPassword)` — normalizes the email (trim/lowercase, matching `RegistrationService`'s convention), looks up the user via `UserRepository.findByEmail`, and verifies `rawPassword` against `password_hash` using `BCryptPasswordEncoder.matches`. Returns the authenticated `User` on success. Throws a new `InvalidCredentialsException` (mirroring the `RegistrationValidationException`/`DuplicateEmailException` pattern from Step 2) on missing user or password mismatch — a single generic error ("Invalid email or password") to avoid revealing which field was wrong.

## Repository layer
No repository changes. `UserRepository.findByEmail` already implements everything this feature needs.

## Templates
- **Modify:** `templates/login.html` — replace the "Login isn't available yet" placeholder with a real form (email, password fields, submit button) posting to `/login`, matching `register.html`'s structure/error-display pattern; keep the existing flash `success` message display from registration; add a visible error message (`th:if="${error}"`) for failed login attempts.

## Files to change
- `src/main/java/com/spendly/controller/LoginController.java` — add `POST /login` and `GET /logout` handlers alongside the existing `GET /login`
- `src/main/resources/templates/login.html` — add the login form

## Files to create
- `src/main/java/com/spendly/service/LoginService.java`
- `src/main/java/com/spendly/service/InvalidCredentialsException.java`
- `src/test/java/com/spendly/service/LoginServiceTest.java`
- `src/test/java/com/spendly/controller/LoginControllerTest.java`

## New dependencies
No new dependencies. Session handling uses `jakarta.servlet.http.HttpSession`, already available transitively via `spring-boot-starter-web`. `BCryptPasswordEncoder` is already present via `spring-security-crypto` from Step 1.

## Rules for implementation
- No JPA/Hibernate or other ORM — `JdbcTemplate` only
- Parameterised queries only (`?` placeholders), never string-concatenated SQL
- Passwords hashed with `BCryptPasswordEncoder` (`spring-security-crypto`) — verify with `.matches`, never compare raw strings or log raw passwords
- DB logic stays in `repository/`; business logic (credential lookup, password verification) stays in `service/`; `LoginController` only calls `LoginService`, manages the `HttpSession`, and populates the model/redirect
- Use CSS variables — never hardcode hex values
- All templates extend `layout.html` via the Thymeleaf layout dialect
- Never hardcode URLs in templates — always `th:href="@{...}"`
- Store only the user's id in the session (e.g. `session.setAttribute("userId", user.id())`) — never store the `User` object or password hash in the session
- `GET /logout` must invalidate the session (`session.invalidate()`), not just remove the attribute
- Do not implement `GET /profile` or any `/expenses/*` route — those remain stubs for later steps per `CLAUDE.md`'s roadmap
- Do not add a Spring Security filter chain or any new authentication framework — session-attribute checks only, consistent with `CLAUDE.md`'s "Spring Boot only" constraint
- Do not touch `RegistrationController`/`RegistrationService` — out of scope for this step

## Definition of done
- [ ] `./mvnw spring-boot:run` starts the app on port 5001 without errors
- [ ] `GET /login` renders a form with email and password fields, styled via `style.css`, extending `layout.html`
- [ ] Submitting valid credentials via `POST /login` starts a session and redirects to `/`
- [ ] Submitting an unregistered email or wrong password re-renders `login.html` with a visible "Invalid email or password" error and does not start a session
- [ ] Registering a new account, then logging in with those exact credentials, succeeds end-to-end
- [ ] `GET /logout` invalidates the session and redirects to `/login`
- [ ] No hardcoded URLs in `login.html` — all links/forms use `th:href="@{...}"` / `th:action="@{...}"`
- [ ] `./mvnw test -Dtest=LoginServiceTest` passes
- [ ] `./mvnw test -Dtest=LoginControllerTest` passes
- [ ] `./mvnw test` (full suite) passes with no regressions to existing `RegistrationServiceTest` / `RegistrationControllerTest` / `UserRepositoryTest`
