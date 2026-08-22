# Spec: Profile Backend Connection

## Overview
Step 4 built the `/profile` page against hardcoded data in `ProfileService` so the UI could be validated in isolation (see `.claude/specs/04-profile-page.md`). This step replaces that hardcoded data with real `JdbcTemplate` queries against the logged-in user's row and expenses, and adds basic account-management routes (edit name/email, change password, paginated/AJAX expense history, delete account) so the profile page becomes a fully functional account hub rather than a static display.

## Depends on
- Step 1: Database setup (`users`, `expenses` tables must exist)
- Step 2: Registration (accounts must be creatable, `BCryptPasswordEncoder` hashing pattern established)
- Step 3: Login + Logout (session must carry `userId`; `/profile` must be a protected route)
- Step 4: Profile page (template and hardcoded layout already in place)

## Routes
- `GET /profile` — render the profile page using the logged-in user's real data from `users`/`expenses` — logged-in only — `ProfileController` (modify existing)
- `POST /profile/edit` — update the logged-in user's name and email — logged-in only — `ProfileController`
- `POST /profile/password` — change the logged-in user's password (requires current password) — logged-in only — `ProfileController`
- `GET /profile/expenses` — return a paginated/AJAX slice of the user's expense history (for "load more" on the transaction table) — logged-in only — `ProfileController`
- `POST /profile/delete` — delete the logged-in user's account and their expenses, then invalidate the session — logged-in only — `ProfileController`

## Database changes
No schema changes. `users` and `expenses` already support everything needed:
- `users(id, name, email, password_hash, created_at)` — sufficient for edit/password routes
- `expenses(id, user_id, amount, category, date, description, created_at)` — sufficient for paginated history
- FK `expenses.user_id → users.id` already enforces cascade-relevant integrity; account deletion must delete `expenses` rows before the `users` row since H2 enforces the FK and there is no `ON DELETE CASCADE` in the current schema.

## Service layer
- `ProfileService` (modify):
  - Replace `getUser()` with a real lookup: accept `userId`, call `UserRepository.findById(userId)`, throw `ResponseStatusException(NOT_FOUND)` if absent
  - Replace `getSummary()` with a real lookup: accept `userId`, call `ExpenseRepository.findByUserId(userId)`, compute `totalSpent`/`categoryTotals` from the real list (same aggregation logic as today, just fed real data)
  - `getInitials(User user)` — derive from the real `User` passed in, not an internal hardcoded call
  - `getTopCategory(ExpenseSummary summary)` — derive from the real summary passed in
  - New `updateProfile(long userId, String name, String email)` — validates non-blank name, valid email format, and email not already used by another user (via `UserRepository.findByEmail`); throws `DuplicateEmailException` / `RegistrationValidationException` (reuse existing exception types from Step 2) on failure
  - New `changePassword(long userId, String currentPassword, String newPassword)` — loads user, verifies `currentPassword` against `password_hash` with `BCryptPasswordEncoder.matches`, validates new password meets the same rules used at registration, hashes and updates; throws `InvalidCredentialsException` (reuse from Step 3) if the current password doesn't match
  - New `getExpensesPage(long userId, int page, int pageSize)` — returns a page of the user's expenses, most recent first
  - New `deleteAccount(long userId)` — deletes the user's expenses then the user row
- Called by `ProfileController` for all five routes.

## Repository layer
- `UserRepository` (modify):
  - New `void update(long id, String name, String email)` — `UPDATE users SET name = ?, email = ? WHERE id = ?`
  - New `void updatePassword(long id, String passwordHash)` — `UPDATE users SET password_hash = ? WHERE id = ?`
  - New `void deleteById(long id)` — `DELETE FROM users WHERE id = ?`
- `ExpenseRepository` (modify):
  - New `List<Expense> findByUserId(long userId, int limit, int offset)` — `SELECT ... FROM expenses WHERE user_id = ? ORDER BY date DESC, id DESC LIMIT ? OFFSET ?`
  - New `void deleteByUserId(long userId)` — `DELETE FROM expenses WHERE user_id = ?`
- All queries use `JdbcTemplate` with `?` placeholders, consistent with existing methods.

## Templates
- **Modify:** `templates/profile.html`
  - Add an "Edit Profile" form (name, email) posting to `/profile/edit`
  - Add a "Change Password" form (current password, new password) posting to `/profile/password`
  - Add a "Delete Account" form/button posting to `/profile/delete`, with a confirmation step (no native `confirm()` dialog — use an inline confirm state driven by `main.js`, since JS `confirm()`/`alert()` blocks and is disallowed for this app's UX pattern)
  - Add a "Load more" control under the transaction table wired to `GET /profile/expenses` via vanilla JS `fetch`, appending returned rows
  - Surface flash `success`/`error` messages (same pattern as `login.html`/`register.html`) for edit/password/delete outcomes
- No new templates needed.

## Files to change
- `src/main/java/com/spendly/controller/ProfileController.java`
- `src/main/java/com/spendly/service/ProfileService.java`
- `src/main/java/com/spendly/repository/UserRepository.java`
- `src/main/java/com/spendly/repository/ExpenseRepository.java`
- `src/main/resources/templates/profile.html`
- `src/main/resources/static/js/main.js` (load-more fetch + inline delete-confirm behavior)
- `src/main/resources/static/css/profile.css` (styles for new forms/controls)
- `src/test/java/com/spendly/controller/ProfileControllerTest.java`

## Files to create
None — all changes are to existing files.

## New dependencies
No new dependencies. `spring-security-crypto` (`BCryptPasswordEncoder`) is already present in `pom.xml` from registration/login.

## Rules for implementation
- No JPA/Hibernate or other ORM — `JdbcTemplate` only
- Parameterised queries only (`?` placeholders), never string-concatenated SQL
- Passwords hashed with `BCryptPasswordEncoder` (`spring-security-crypto`) — verify current password with `.matches()` before allowing a change
- DB logic stays in `repository/`; business logic stays in `service/`; controllers only call the service and populate the model
- Use CSS variables — never hardcode hex values
- All templates extend `layout.html` via the Thymeleaf layout dialect
- Never hardcode URLs in templates — always `th:href="@{...}"` / `th:action="@{...}"`
- Every new route must check `session.getAttribute("userId") == null` and redirect to `/login` (or return 401 for the `GET /profile/expenses` AJAX endpoint) — no route in this step is reachable without an active session
- `POST /profile/delete` must delete `expenses` rows before the `users` row (FK enforcement is automatic in H2; deleting the user first will throw a constraint violation)
- Reuse existing exception types (`DuplicateEmailException`, `RegistrationValidationException`, `InvalidCredentialsException`) rather than inventing new ones, to stay consistent with Steps 2–3

## Definition of done
- [ ] Visiting `/profile` while logged in shows the real logged-in user's name, email, member-since date, and actual expense data from the database (no hardcoded values)
- [ ] Visiting `/profile` or posting to any new profile route while logged out redirects to `/login` (or returns 401 for `/profile/expenses`)
- [ ] Submitting the edit-profile form with a valid new name/email updates the `users` row and the page reflects the change after redirect
- [ ] Submitting the edit-profile form with an email already used by another account shows an error and does not update the row
- [ ] Submitting the change-password form with the correct current password and a valid new password updates `password_hash`, and the user can log in with the new password afterward
- [ ] Submitting the change-password form with an incorrect current password shows an error and does not update the row
- [ ] `GET /profile/expenses` returns additional expense rows beyond the first page when called with a later page parameter
- [ ] Submitting the delete-account form deletes the user's `expenses` rows and `users` row, invalidates the session, and redirects to a logged-out page
- [ ] No hex colour values appear in any modified template or CSS file — only CSS variables
- [ ] `./mvnw test` passes, including updated `ProfileControllerTest`
