# CLAUDE.md

## Project overview

Spendly is a lightweight personal expense tracker built with Java 21 and Spring Boot.

---

## Architecture
```
spendly/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/spendly/
    │   │   ├── SpendlyApplication.java   # Spring Boot entry point
    │   │   ├── controller/               # @Controller classes — one per resource
    │   │   ├── service/                  # Business logic, called by controllers
    │   │   ├── repository/               # JdbcTemplate data access — no ORM
    │   │   └── model/                    # Plain record/POJO domain classes
    │   └── resources/
    │       ├── application.properties    # Config, incl. server.port, H2 datasource
    │       ├── schema.sql                # H2 schema (auto-run on startup)
    │       ├── templates/
    │       │   ├── layout.html           # Shared Thymeleaf layout — all pages extend this
    │       │   └── *.html                # One template per page
    │       └── static/
    │           ├── css/
    │           │   ├── style.css         # Global styles
    │           │   └── landing.css       # Landing-page-only styles
    │           └── js/
    │               └── main.js           # Vanilla JS only
    └── test/java/com/spendly/            # Mirrors main/java package structure
```

**Where things belong:**
- New endpoints → `controller/`, one controller per resource, no business logic inline
- Business logic → `service/`, called by controllers, never inline in controller methods
- DB logic → `repository/` only, using `JdbcTemplate`, never inline in services or controllers
- New pages → new `.html` file in `templates/`, extending `layout.html` via Thymeleaf layout dialect
- Page-specific styles → new `.css` file, not inline `<style>` tags

---

## Code style

- Java 21: use records for immutable data carriers, pattern matching / switch expressions where they simplify control flow
- Naming: camelCase for variables/methods, PascalCase for classes, UPPER_SNAKE_CASE for constants
- Templates: Thymeleaf with `th:href="@{...}"` for every internal link — never hardcode URLs
- Controller methods: one responsibility only — call service, populate model, return view name
- DB queries: always use `JdbcTemplate` with `?` placeholders — never string-concatenated SQL
- Error handling: throw `ResponseStatusException` for HTTP errors, not silently returning error strings

---

## Tech constraints

- **Spring Boot only** — no separate frameworks layered on top (no Micronaut, no Quarkus)
- **H2 only** — no PostgreSQL/MySQL, no JPA/Hibernate ORM; access via the H2 JDBC driver + `JdbcTemplate`, running in **file-based, persistent mode** (not in-memory) so data survives restarts
- **Vanilla JS only** — no React, no jQuery, no npm packages
- **No new Maven dependencies** — work within `pom.xml` as-is unless explicitly told otherwise
- Java 21 (LTS) assumed — virtual threads and records are fine to use

---

## Subagent Policy
- Always use a builtin explore subagent for codebase exploration 
  before implementing any new feature
- Always use a subagent to verify test results 
  after any implementation
- When asked to plan, delegate codebase research 
  to a subagent before presenting the plan
- always use a builtin plan subagent in plan mode

---

## Commands
```bash
# Setup / build
./mvnw clean install

# Run dev server (port 5001)
./mvnw spring-boot:run

# Run all tests
./mvnw test

# Run a specific test class
./mvnw test -Dtest=ExpenseControllerTest

# Run a specific test method
./mvnw test -Dtest=ExpenseControllerTest#shouldAddExpense

# Run tests with output visible
./mvnw test -Dspring-boot.run.arguments="-Ddebug"
```

---

## Implemented vs stub routes

| Route | Status |
|---|---|
| `GET /` | Implemented — renders `landing.html` |
| `GET /register` | Implemented — renders `register.html` |
| `GET /login` | Implemented — renders `login.html` |
| `GET /logout` | Stub — Step 3 |
| `GET /profile` | Stub — Step 4 |
| `GET /expenses/add` | Stub — Step 7 |
| `GET /expenses/{id}/edit` | Stub — Step 8 |
| `GET /expenses/{id}/delete` | Stub — Step 9 |

**Do not implement a stub route unless the active task explicitly targets that step.**

---

## Warnings and things to avoid

- **Never return raw strings for stub routes** once a step is implemented — always return a view name that resolves to a template
- **Never hardcode URLs** in templates — always use `th:href="@{...}"`
- **Never put DB logic in controllers or services** — it belongs in `repository/`
- **Never add new Maven dependencies** mid-feature without flagging it — keep `pom.xml` in sync
- **Never use JS frameworks** — the frontend is intentionally vanilla
- **Repository classes are currently empty** — do not assume methods exist until the step that implements them
- **FK enforcement is automatic** — unlike SQLite, H2 enforces `FOREIGN KEY` constraints by default; no connection-init pragma is needed, just declare the constraint in `schema.sql`
- The app runs on **port 5001**, not the Spring Boot default 8080 — configure via `server.port` in `application.properties`
