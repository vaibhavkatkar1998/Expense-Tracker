## 1. Overview

Establish the **data layer foundation** for Spendly using an embedded, file-based **H2** database, accessed via `JdbcTemplate` — no ORM, per `CLAUDE.md`.

This is the first implementation step. The project currently has no Maven/Spring Boot scaffolding at all, so this step also creates the minimum project skeleton needed for the data layer to run and be tested (`pom.xml`, `SpendlyApplication.java`, `application.properties`).

All future features (authentication, profile, expense tracking) depend on this being correct.

---

## 2. Depends on

Nothing — this is the first step.

---

## 3. Routes

- No new routes
- Controller package is not touched in this step

---

## 4. Database Schema (`src/main/resources/schema.sql`)

Auto-run on startup by Spring Boot (H2 is an embedded database, so `schema.sql` is picked up automatically).

---

### A. `users`

| Column | Type | Constraints |
| --- | --- | --- |
| id | BIGINT | Primary key, `AUTO_INCREMENT` |
| name | VARCHAR(255) | Not null |
| email | VARCHAR(255) | Unique, not null |
| password_hash | VARCHAR(255) | Not null |
| created_at | TIMESTAMP | Not null, default `CURRENT_TIMESTAMP` |

---

### B. `expenses`

| Column | Type | Constraints |
| --- | --- | --- |
| id | BIGINT | Primary key, `AUTO_INCREMENT` |
| user_id | BIGINT | Foreign key → `users.id`, not null |
| amount | DOUBLE | Not null |
| category | VARCHAR(50) | Not null |
| date | VARCHAR(10) | Not null (`YYYY-MM-DD` format) |
| description | VARCHAR(500) | Nullable |
| created_at | TIMESTAMP | Not null, default `CURRENT_TIMESTAMP` |

H2 enforces the `FOREIGN KEY` constraint by default — no pragma or connection-init step needed (unlike SQLite).

---

## 5. Datasource Configuration (`application.properties`)

```properties
server.port=5001

spring.datasource.url=jdbc:h2:file:./data/spendly;DB_CLOSE_ON_EXIT=FALSE
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

spring.sql.init.mode=always
```

- File-based mode (`jdbc:h2:file:...`), not in-memory — data must survive app restarts, same behavior the project previously relied on with SQLite
- `./data/spendly` keeps the `.mv.db` file out of the project root; add `data/` to `.gitignore`
- `spring.sql.init.mode=always` guarantees `schema.sql` runs every startup (safe — schema uses idempotent `CREATE TABLE IF NOT EXISTS`)

---

## 6. Classes to Implement

---

### A. `SpendlyApplication.java` (`com.spendly`)

- Standard `@SpringBootApplication` entry point
- No business logic

---

### B. `UserRepository` (`com.spendly.repository`)

- `Optional<User> findByEmail(String email)`
- `Optional<User> findById(long id)`
- `long insert(String name, String email, String passwordHash)` — returns generated id
- `int count()` — used by the seeder to check if data already exists

---

### C. `ExpenseRepository` (`com.spendly.repository`)

- `long insert(long userId, double amount, String category, String date, String description)`
- (Only what step 1 needs — `seed_db`. Query methods for listing/updating/deleting expenses belong to later steps and must not be added here.)

---

### D. `User` / `Expense` (`com.spendly.model`)

- Java records
- `User(long id, String name, String email, String passwordHash, LocalDateTime createdAt)`
- `Expense(long id, long userId, double amount, String category, String date, String description, LocalDateTime createdAt)`

---

### E. `DataSeeder` (`com.spendly.config`)

- Implements `CommandLineRunner` (or `ApplicationRunner`), registered as a `@Component`
- On startup:
    - If `UserRepository.count() > 0` → return early (no duplication)
    - Else insert one demo user:
        - name: Demo User
        - email: demo@spendly.com
        - password: `demo123`, hashed (see §7)
    - Insert **8 sample expenses** linked to the demo user, covering all 7 categories, dates spread across the current month, at least one per category

---

## 7. Password Hashing — Dependency Flag

**⚠ New Maven dependency required**, per `CLAUDE.md`'s "flag before adding" rule:

- Add `spring-security-crypto` (for `BCryptPasswordEncoder`) — this is the smallest dependency that provides safe password hashing without pulling in full Spring Security (no auth/filter chain needed yet)
- Do not add `spring-boot-starter-security` in this step — that belongs to the login/auth step, not the data layer

---

## 8. Files to Create

- `pom.xml` — Spring Boot parent, `spring-boot-starter-web`, `spring-boot-starter-jdbc`, `com.h2database:h2`, `spring-security-crypto`
- `src/main/java/com/spendly/SpendlyApplication.java`
- `src/main/java/com/spendly/model/User.java`
- `src/main/java/com/spendly/model/Expense.java`
- `src/main/java/com/spendly/repository/UserRepository.java`
- `src/main/java/com/spendly/repository/ExpenseRepository.java`
- `src/main/java/com/spendly/config/DataSeeder.java`
- `src/main/resources/application.properties`
- `src/main/resources/schema.sql`
- `.gitignore` entry for `data/` (H2 file store) and `target/`

## 9. Files to Change

- None (no existing controller/service/repository code yet)

---

## 10. Categories (Fixed List)

Use exactly these values:

- Food
- Transport
- Bills
- Health
- Entertainment
- Shopping
- Other

---

## 11. Rules for Implementation

- No ORMs (no JPA/Hibernate) — `JdbcTemplate` only
- Use **parameterized queries only** (`?` placeholders) — never string-concatenated SQL
- Store `amount` as `DOUBLE`, not integer cents
- Hash passwords with `BCryptPasswordEncoder` (see §7)
- `DataSeeder` must prevent duplicate inserts on repeated startups
- Dates must follow **`YYYY-MM-DD`** format consistently
- Repository classes contain DB logic only — no business logic (that's `service/`, not touched in this step)

---

## 12. Expected Behavior

- App starts on port 5001 and creates `./data/spendly.mv.db` if absent
- `schema.sql` creates both tables safely on every startup (`CREATE TABLE IF NOT EXISTS`)
- `DataSeeder` inserts demo data only once; re-running `./mvnw spring-boot:run` does not duplicate rows
- Database enforces:
    - unique email constraint
    - valid foreign key relationships (H2 default behavior)

---

## 13. Error Handling Expectations

- Inserting duplicate email → should fail (`UNIQUE` constraint violation)
- Inserting expense with invalid `user_id` → should fail (foreign key constraint violation)
- Invalid queries → propagate as `DataAccessException` (Spring's translated exception), not swallowed

---

## 14. Definition of Done

- [ ]  `pom.xml` exists with H2, JdbcTemplate starter, and `spring-security-crypto` — flagged to the user as new dependencies
- [ ]  App starts on port 5001 without errors
- [ ]  `./data/spendly.mv.db` file is created on startup and persists across restarts
- [ ]  Both tables exist with correct schema and constraints
- [ ]  Demo user exists with a BCrypt-hashed password
- [ ]  8 sample expenses exist, covering all 7 categories
- [ ]  No duplicate seed data on repeated runs
- [ ]  Foreign key enforcement works (reject invalid `user_id`)
- [ ]  All queries use parameterized SQL via `JdbcTemplate`
