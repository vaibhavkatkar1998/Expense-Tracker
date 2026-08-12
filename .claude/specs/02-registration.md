# Spec: Registration

## Overview
This feature adds user account creation to Spendly. It is the first web-facing feature built on top of the data layer from Step 1 (`01-database-setup`) — it introduces the project's first `controller/`, `service/`, and `templates/` code, since none of that scaffolding exists yet despite `CLAUDE.md`'s route table marking `GET /register` as "Implemented" (that entry is stale documentation, not actual code — confirmed by inspecting the repo). Registration lets a visitor create a Spendly account with a name, email, and password, which is validated, hashed, and persisted via the existing `UserRepository`. It unblocks the login step (Step 3) and every authenticated feature after it.

## Depends on
- Step 1 (`01-database-setup`): `users` table in `schema.sql`, `User` record, `UserRepository` (`findByEmail`, `insert`, `count`), `spring-security-crypto` dependency already in `pom.xml`.

## Routes
- `GET /register` — render the registration form — public — `RegistrationController`
- `POST /register` — process form submission, create the account, redirect to `/login` on success — public — `RegistrationController`

## Database changes
No database changes. The `users` table (`id`, `name`, `email`, `password_hash`, `created_at`) already exists from Step 1 and covers everything registration needs. `UserRepository.findByEmail` (duplicate-email check) and `UserRepository.insert` (account creation) already exist and require no modification.

## Service layer
New `RegistrationService` in `service/`, called by `RegistrationController`:
- `register(String name, String email, String rawPassword)` — validates input (non-blank name, well-formed email, minimum password length), checks `UserRepository.findByEmail` for an existing account and rejects duplicates, hashes the password with `BCryptPasswordEncoder`, then calls `UserRepository.insert`. Throws a checked/unchecked exception (e.g. `DuplicateEmailException` or similar) on validation failure, which the controller translates into a re-rendered form with an error message.

## Repository layer
No repository changes. `UserRepository.findByEmail` and `UserRepository.insert` already implement everything this feature needs.

## Templates
- **Create:** `templates/layout.html` — the shared Thymeleaf layout (header/nav shell, content block, footer) that every future page will extend via the Thymeleaf layout dialect. This is the first template in the project, so it establishes the pattern.
- **Create:** `templates/register.html` — registration form (name, email, password fields), extends `layout.html`, displays validation/duplicate-email errors, links to `/login` for existing users via `th:href="@{...}"`.
- **Create:** `static/css/style.css` — global styles (first stylesheet in the project), using CSS variables for colors per `CLAUDE.md` rules.
- **Create:** `static/js/main.js` — vanilla JS placeholder for future client-side interactions (e.g. basic form feedback); no framework code.

## Files to change
- `pom.xml` — add `spring-boot-starter-thymeleaf` (flagged below; required to render templates, which no prior step needed)

## Files to create
- `src/main/java/com/spendly/controller/RegistrationController.java`
- `src/main/java/com/spendly/service/RegistrationService.java`
- `src/main/resources/templates/layout.html`
- `src/main/resources/templates/register.html`
- `src/main/resources/static/css/style.css`
- `src/main/resources/static/js/main.js`
- `src/test/java/com/spendly/service/RegistrationServiceTest.java`
- `src/test/java/com/spendly/controller/RegistrationControllerTest.java`

## New dependencies
**⚠ New Maven dependency required, flagged per `CLAUDE.md`:**
- `spring-boot-starter-thymeleaf` — needed to render any HTML template at all; no prior step added a template engine, and `CLAUDE.md`'s architecture doc assumes Thymeleaf templates exist. This is the minimum addition to render `layout.html`/`register.html`.

No other new dependencies — `spring-security-crypto` (for `BCryptPasswordEncoder`) is already present in `pom.xml` from Step 1.

## Rules for implementation
- No JPA/Hibernate or other ORM — `JdbcTemplate` only (already satisfied by existing `UserRepository`)
- Parameterised queries only (`?` placeholders), never string-concatenated SQL
- Passwords hashed with `BCryptPasswordEncoder` (`spring-security-crypto`) — never store or log raw passwords
- DB logic stays in `repository/`; business logic (validation, duplicate check, hashing) stays in `service/`; `RegistrationController` only calls `RegistrationService` and populates the model/redirect
- Use CSS variables — never hardcode hex values in `style.css`
- All templates extend `layout.html` via the Thymeleaf layout dialect
- Never hardcode URLs in templates — always `th:href="@{...}"`
- Do not implement `GET /login`, `GET /logout`, `GET /profile`, or any `/expenses/*` route — those remain stubs for later steps per `CLAUDE.md`'s roadmap; `POST /register` should redirect to `/login` even though that route isn't implemented yet (link/redirect target, not a new route to build)
- Do not touch the landing page (`GET /`) — out of scope for this step despite the route table listing it as implemented

## Definition of done
- [ ] `./mvnw spring-boot:run` starts the app on port 5001 without errors
- [ ] `GET /register` renders a form with name, email, and password fields, styled via `style.css`, extending `layout.html`
- [ ] Submitting valid data via `POST /register` creates a row in `users` with a BCrypt-hashed `password_hash` and redirects to `/login`
- [ ] Submitting a duplicate email re-renders `register.html` with a visible error and does not create a second row
- [ ] Submitting invalid input (blank name, malformed email, too-short password) re-renders the form with a visible validation error and does not insert a row
- [ ] No hardcoded URLs in `register.html` — all links use `th:href="@{...}"`
- [ ] `./mvnw test -Dtest=RegistrationServiceTest` passes
- [ ] `./mvnw test -Dtest=RegistrationControllerTest` passes
- [ ] `./mvnw test` (full suite) passes with no regressions to existing `UserRepositoryTest` / `DataSeederTest`
