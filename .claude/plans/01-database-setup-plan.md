# Implementation Plan — Step 01: Database Setup (Spendly)

## Context

`.claude/specs/01-database-setup.md` defines the data-layer foundation for Spendly: an embedded, file-based **H2** database accessed only via `JdbcTemplate` (no ORM), per `CLAUDE.md`. The repo is currently **fully greenfield** — verified read-only: no `pom.xml`, no `src/`, no `.gitignore`, no Maven wrapper. Only `CLAUDE.md`, `.claude/specs/01-database-setup.md`, and `.idea/` exist (git branch `feature/database-setup`).

This plan takes the project from empty to a working, tested data layer that satisfies the spec's Definition of Done (§14): H2 running in file-based persistent mode, `users`/`expenses` tables created via `schema.sql`, `UserRepository`/`ExpenseRepository` with exactly the methods the spec allows (no extra query methods — those are reserved for later steps), and a `DataSeeder` that seeds one demo user + 8 sample expenses idempotently.

**Spring Boot version**: 3.5.16 (latest 3.x line — chosen over 4.1.0 for ecosystem maturity while bootstrapping). Both support Java 21; `pom.xml` explicitly sets `java.version=21`.

**Flagged new Maven dependencies** (per `CLAUDE.md`'s "flag before adding" rule):
- `spring-security-crypto` — for `BCryptPasswordEncoder`, explicitly required by spec §7 (not full `spring-boot-starter-security`, which belongs to a later auth step)
- `spring-boot-starter-web` — needed so the app runs as a web app at all (later steps add controllers); implied by spec §8's file list
- `com.h2database:h2` — the database itself, spec's core requirement
- `spring-boot-starter-jdbc` — provides `JdbcTemplate`

---

## File-creation order (each stage independently testable)

1. `.gitignore` + empty package skeleton (`model/`, `repository/`, `config/` under `src/main/java/com/spendly/`, plus `src/main/resources/`, `src/test/java/com/spendly/`)
2. `pom.xml`
3. Generate Maven wrapper: `mvn -N wrapper:wrapper -Dmaven=3.9.9` from repo root, then `chmod +x mvnw` — required because `CLAUDE.md`'s Commands section assumes `./mvnw` exists; commit the generated `.mvn/wrapper/maven-wrapper.jar` (standard practice, not gitignored)
4. `SpendlyApplication.java` (bare `@SpringBootApplication`) — proves `./mvnw spring-boot:run` boots
5. `application.properties` + `schema.sql` — boot again, confirm `./data/spendly.mv.db` is created and tables exist, independent of any repository code
6. `User.java`, `Expense.java` records
7. `UserRepository.java`, `ExpenseRepository.java` — write repository tests here before moving on
8. `DataSeeder.java` — last, since it depends on both repositories
9. Full verification pass against spec §12–14

---

## 1. `pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.5.16</version>
        <relativePath/>
    </parent>

    <groupId>com.spendly</groupId>
    <artifactId>spendly</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>spendly</name>
    <description>Spendly - lightweight personal expense tracker</description>

    <properties>
        <java.version>21</java.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-jdbc</artifactId>
        </dependency>
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-crypto</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

Do **not** add `spring-boot-starter-thymeleaf` in this step — no templates are created (spec §3: "No new routes"), even though `CLAUDE.md`'s architecture references Thymeleaf for later steps.

---

## 2. `src/main/resources/schema.sql`

```sql
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS expenses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    amount DOUBLE NOT NULL,
    category VARCHAR(50) NOT NULL,
    date VARCHAR(10) NOT NULL,
    description VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_expenses_user FOREIGN KEY (user_id) REFERENCES users(id)
);
```

`IF NOT EXISTS` keeps this safe to re-run every startup (`spring.sql.init.mode=always`). H2 enforces the `FOREIGN KEY` by default — no pragma needed.

---

## 3. `src/main/resources/application.properties`

```properties
server.port=5001

spring.datasource.url=jdbc:h2:file:./data/spendly;DB_CLOSE_ON_EXIT=FALSE
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

spring.sql.init.mode=always
```

Keep scope tight to the spec — no `spring.h2.console.enabled` or other additions.

---

## 4. Model records — `src/main/java/com/spendly/model/`

```java
// User.java
package com.spendly.model;
import java.time.LocalDateTime;
public record User(long id, String name, String email, String passwordHash, LocalDateTime createdAt) {}

// Expense.java
package com.spendly.model;
import java.time.LocalDateTime;
public record Expense(long id, long userId, double amount, String category, String date, String description, LocalDateTime createdAt) {}
```

---

## 5. Repositories — `src/main/java/com/spendly/repository/`

Constructor-injected `JdbcTemplate`, `?` placeholders only, generated keys via `KeyHolder`. **Do not add methods beyond what's listed** — spec §6C explicitly reserves list/update/delete for later steps.

**`UserRepository.java`** — `findByEmail`, `findById`, `insert`, `count`:
```java
package com.spendly.repository;

import com.spendly.model.User;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Optional;

@Repository
public class UserRepository {

    private static final RowMapper<User> USER_ROW_MAPPER = (rs, rowNum) -> new User(
            rs.getLong("id"),
            rs.getString("name"),
            rs.getString("email"),
            rs.getString("password_hash"),
            rs.getTimestamp("created_at").toLocalDateTime()
    );

    private final JdbcTemplate jdbcTemplate;

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<User> findByEmail(String email) {
        String sql = "SELECT id, name, email, password_hash, created_at FROM users WHERE email = ?";
        return jdbcTemplate.query(sql, USER_ROW_MAPPER, email).stream().findFirst();
    }

    public Optional<User> findById(long id) {
        String sql = "SELECT id, name, email, password_hash, created_at FROM users WHERE id = ?";
        return jdbcTemplate.query(sql, USER_ROW_MAPPER, id).stream().findFirst();
    }

    public long insert(String name, String email, String passwordHash) {
        String sql = "INSERT INTO users (name, email, password_hash) VALUES (?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, name);
            ps.setString(2, email);
            ps.setString(3, passwordHash);
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    public int count() {
        Integer result = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
        return result == null ? 0 : result;
    }
}
```

**`ExpenseRepository.java`** — `insert` only:
```java
package com.spendly.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Types;

@Repository
public class ExpenseRepository {

    private final JdbcTemplate jdbcTemplate;

    public ExpenseRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long insert(long userId, double amount, String category, String date, String description) {
        String sql = "INSERT INTO expenses (user_id, amount, category, date, description) VALUES (?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, userId);
            ps.setDouble(2, amount);
            ps.setString(3, category);
            ps.setString(4, date);
            if (description != null) {
                ps.setString(5, description);
            } else {
                ps.setNull(5, Types.VARCHAR);
            }
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }
}
```

---

## 6. `src/main/java/com/spendly/config/DataSeeder.java`

`CommandLineRunner`, gated by `userRepository.count() > 0`, BCrypt-hashes `demo123`, inserts 8 expenses spanning all 7 fixed categories with dates in the current month:

```java
package com.spendly.config;

import com.spendly.repository.ExpenseRepository;
import com.spendly.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ExpenseRepository expenseRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public DataSeeder(UserRepository userRepository, ExpenseRepository expenseRepository) {
        this.userRepository = userRepository;
        this.expenseRepository = expenseRepository;
    }

    private record SeedExpense(int dayOfMonth, double amount, String category, String description) {}

    private static final List<SeedExpense> SEED_EXPENSES = List.of(
            new SeedExpense(1, 45.50, "Food", "Groceries"),
            new SeedExpense(3, 12.00, "Transport", "Bus pass top-up"),
            new SeedExpense(5, 89.99, "Bills", "Electricity bill"),
            new SeedExpense(8, 25.00, "Health", "Pharmacy"),
            new SeedExpense(11, 15.75, "Entertainment", "Movie ticket"),
            new SeedExpense(14, 60.00, "Shopping", "New shoes"),
            new SeedExpense(18, 9.50, "Other", "Miscellaneous"),
            new SeedExpense(21, 32.20, "Food", "Restaurant dinner")
    );

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }

        long userId = userRepository.insert(
                "Demo User", "demo@spendly.com", passwordEncoder.encode("demo123"));

        LocalDate today = LocalDate.now();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        int lastDay = today.lengthOfMonth();

        for (SeedExpense se : SEED_EXPENSES) {
            int day = Math.min(se.dayOfMonth(), lastDay);
            String date = today.withDayOfMonth(day).format(fmt);
            expenseRepository.insert(userId, se.amount(), se.category(), date, se.description());
        }
    }
}
```

---

## 7. `SpendlyApplication.java`

```java
package com.spendly;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SpendlyApplication {
    public static void main(String[] args) {
        SpringApplication.run(SpendlyApplication.class, args);
    }
}
```

---

## 8. `.gitignore`

```gitignore
# Build output
target/

# H2 file-based database store (spec §5)
data/

# IDE
.idea/
*.iml

# OS
.DS_Store
```

Do not ignore `.claude/` — it holds the spec files, which are intended to be tracked. Do not ignore `.mvn/wrapper/maven-wrapper.jar` — committing it is standard Maven wrapper practice.

---

## 9. Verification plan

**a. Bootstrap the wrapper** (after `pom.xml` exists):
```bash
cd /Users/vaibhav/Expense-Tracker
mvn -N wrapper:wrapper -Dmaven=3.9.9
chmod +x mvnw
./mvnw -v
```

**b. Smoke-test schema + datasource** (after stage 5, before repositories exist):
```bash
./mvnw spring-boot:run
```
Confirm: app starts on port 5001 (not 8080), no `SQLException`/`DataAccessException` in logs. Then:
```bash
ls -la data/   # expect spendly.mv.db
```
Ctrl-C, restart, confirm the file persists and no schema errors reappear (idempotent DDL).

**c. Repository-level tests** (after stage 7) — use a separate in-memory H2 datasource so tests never touch or lock `./data/spendly.mv.db`:

`src/test/resources/application.properties`:
```properties
spring.datasource.url=jdbc:h2:mem:spendlytest;DB_CLOSE_DELAY=-1
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.sql.init.mode=always
```

`src/test/java/com/spendly/repository/UserRepositoryTest.java` — covers spec §13's error-handling requirements directly:
```java
package com.spendly.repository;

import com.spendly.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class UserRepositoryTest {

    @Autowired private UserRepository userRepository;
    @Autowired private ExpenseRepository expenseRepository;

    @Test
    void insertAndFindByEmail() {
        long id = userRepository.insert("Test User", "test@example.com", "hashed");
        Optional<User> found = userRepository.findByEmail("test@example.com");
        assertThat(found).isPresent();
        assertThat(found.get().id()).isEqualTo(id);
    }

    @Test
    void findByIdReturnsEmptyWhenMissing() {
        assertThat(userRepository.findById(999_999L)).isEmpty();
    }

    @Test
    void duplicateEmailFailsUniqueConstraint() {
        userRepository.insert("A", "dup@example.com", "hash1");
        assertThatThrownBy(() -> userRepository.insert("B", "dup@example.com", "hash2"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void expenseWithInvalidUserIdFailsForeignKey() {
        assertThatThrownBy(() ->
                expenseRepository.insert(999_999L, 10.0, "Food", "2026-08-10", "test")
        ).isInstanceOf(DataIntegrityViolationException.class);
    }
}
```

**d. Seeder idempotency test** (after stage 8):

`src/test/java/com/spendly/config/DataSeederTest.java`:
```java
package com.spendly.config;

import com.spendly.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class DataSeederTest {

    @Autowired private DataSeeder dataSeeder;
    @Autowired private UserRepository userRepository;

    @Test
    void seedsDemoUserAndDoesNotDuplicateOnRerun() {
        assertThat(userRepository.count()).isEqualTo(1);
        assertThat(userRepository.findByEmail("demo@spendly.com")).isPresent();

        dataSeeder.run(); // simulate a second startup

        assertThat(userRepository.count()).isEqualTo(1); // no duplication
    }
}
```

**e. Context-load sanity check**:

`src/test/java/com/spendly/SpendlyApplicationTests.java`:
```java
package com.spendly;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class SpendlyApplicationTests {
    @Test
    void contextLoads() {
    }
}
```

**f. Run everything**:
```bash
./mvnw test
./mvnw spring-boot:run   # Ctrl-C, run again — confirm no duplicate seed rows
```

**g. Definition-of-Done mapping (spec §14)** — every checkbox is covered by (b) for boot/persistence, (c) for schema/constraints, (d) for seed idempotency, plus code review for parameterized SQL and the 7-category coverage in `DataSeeder.SEED_EXPENSES`.

---

### Critical files
- `/Users/vaibhav/Expense-Tracker/pom.xml`
- `/Users/vaibhav/Expense-Tracker/src/main/resources/schema.sql`
- `/Users/vaibhav/Expense-Tracker/src/main/resources/application.properties`
- `/Users/vaibhav/Expense-Tracker/src/main/java/com/spendly/repository/UserRepository.java`
- `/Users/vaibhav/Expense-Tracker/src/main/java/com/spendly/repository/ExpenseRepository.java`
- `/Users/vaibhav/Expense-Tracker/src/main/java/com/spendly/config/DataSeeder.java`
