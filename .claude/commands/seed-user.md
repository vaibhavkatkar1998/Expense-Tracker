---
description: Create a single dummy user in the database
allowed-tools: Read, Write, Edit, Bash(./mvnw:*), Bash(rm:*)
---

Read src/main/java/com/spendly/model/User.java and
src/main/java/com/spendly/repository/UserRepository.java to understand the
users schema and the repository's findByEmail/insert/count methods.

Then:

1. Generate a realistic random Indian user using your own knowledge of
   common Indian names across regions:
    - Name: a realistic Indian first + last name
    - Email: derived from the name with a random 2-3 digit number suffix
      (e.g. rahul.sharma91@gmail.com)
    - Password: "password123" hashed with Spring Security's
      BCryptPasswordEncoder (already a project dependency — see
      config/DataSeeder.java for the exact usage pattern)
    - created_at: leave unset — schema.sql defaults it to
      CURRENT_TIMESTAMP

2. Check if the generated email already exists via
   userRepository.findByEmail(email). If it does, regenerate until unique.

3. Write a temporary JUnit test class at
   src/test/java/com/spendly/SeedUserTest.java that:
    - Is annotated @SpringBootTest so UserRepository can be autowired
    - Performs the uniqueness check and insert described above via
      userRepository.insert(name, email, passwordHash)
    - Prints the returned id, name, and email to stdout

4. Run it with:
   ./mvnw test -Dtest=SeedUserTest

5. Delete src/test/java/com/spendly/SeedUserTest.java afterward — it's a
   one-off seeding tool, not a permanent test, and must not be left behind.

6. Print confirmation:
    - id
    - name
    - email

Constraints: no new Maven dependencies, no DB logic outside
UserRepository, no string-concatenated SQL — everything goes through the
existing JdbcTemplate-backed repository methods.
