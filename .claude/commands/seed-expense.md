---
description: Seed realistic dummy expenses for a specific user
argument-hint: "<user_id> <count> <months>"
allowed-tools: Read, Write, Edit, Bash(./mvnw:*), Bash(rm:*), Bash(lsof:*)
---

Read src/main/java/com/spendly/model/User.java,
src/main/java/com/spendly/repository/UserRepository.java, and
src/main/java/com/spendly/repository/ExpenseRepository.java to understand
the expenses table schema, the JdbcTemplate-based repository pattern, and
UserRepository.findById.

User input: $ARGUMENTS

## Step 0 — Check for a running dev server

H2 file-mode DBs (jdbc:h2:file:./data/spendly) only allow one exclusive
connection at a time. Run `lsof -i :5001` — if it shows a process, stop
and tell the user to shut down the dev server first (`./mvnw
spring-boot:run`), since seeding will otherwise fail to connect to the
same file.

## Step 1 — Parse arguments

Extract from $ARGUMENTS:
- user_id — integer
- count — integer, number of expenses to create
- months — integer, how many past months to spread them across

If any argument is missing or not a valid integer, stop and say:
"Usage: /seed-expense <user_id> <count> <months>
Example: /seed-expense 1 50 6"

## Step 2 — Verify user exists

Before generating anything, confirm the user_id exists via
userRepository.findById(userId). If not, stop and say:
"No user found with id <user_id>."

## Step 3 — Generate and insert expenses

Write a temporary JUnit test class at
src/test/java/com/spendly/SeedExpenseTest.java that:

1. Is annotated @SpringBootTest with
   properties = {"spring.datasource.url=jdbc:h2:file:./data/spendly;DB_CLOSE_ON_EXIT=FALSE"}
   so it writes to the real dev database, not the in-memory test DB
2. Autowires UserRepository, ExpenseRepository, and
   PlatformTransactionManager
3. Confirms the user exists (per Step 2)
4. Spreads <count> expenses randomly across the past <months> months
5. Uses these categories with realistic Indian descriptions and amounts
   (₹), matching the columns already used by ExpenseRepository.insert
   (amount, category, date as yyyy-MM-dd, description):
   - Food: 50–800
   - Transport: 20–500
   - Bills: 200–3000
   - Health: 100–2000
   - Entertainment: 100–1500
   - Shopping: 200–5000
   - Other: 50–1000
6. Distributes categories roughly proportionally (Food most common,
   Health and Entertainment least)
7. Wraps all inserts in a single transaction using a TransactionTemplate
   built from the autowired PlatformTransactionManager — roll back
   everything if any insert fails
8. Uses only ExpenseRepository.insert (parameterised JdbcTemplate under
   the hood) — no raw/string-concatenated SQL in the test
9. Prints:
   - How many expenses were inserted
   - The date range they span
   - A sample of 5 inserted records (id, amount, category, date,
     description)

Run it with:
./mvnw test -Dtest=SeedExpenseTest

Then delete src/test/java/com/spendly/SeedExpenseTest.java — it's a
one-off seeding tool, not a permanent test, and must not be left behind.

## Step 4 — Confirm

Print:
- How many expenses were inserted
- The date range they span
- A sample of 5 inserted records
