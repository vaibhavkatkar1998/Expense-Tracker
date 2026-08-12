---
description: Create a spec file and feature branch for the next Spendly step
argument-hint: "Step number and feature name e.g. 2 registration"
allowed-tools: Read, Write, Glob, Bash(git:*)
---

You are a senior developer spinning up a new feature for the
Spendly expense tracker. Always follow the rules in CLAUDE.md.

User input: $ARGUMENTS

## Step 1 — Check working directory is clean
Run `git status` and check for uncommitted, unstaged, or
untracked files. If any exist, stop immediately and tell
the user to commit or stash changes before proceeding.
DO NOT CONTINUE until the working directory is clean.

## Step 2 — Parse the arguments
From $ARGUMENTS extract:

1. `step_number` — zero-padded to 2 digits: 2 → 02, 11 → 11

2. `feature_title` — human readable title in Title Case
   - Example: "Registration" or "Login and Logout"

3. `feature_slug` — git and file safe slug
   - Lowercase, kebab-case
   - Only a-z, 0-9 and -
   - Maximum 40 characters
   - Example: registration, login-logout

4. `branch_name` — format: `feature/<feature_slug>`
   - Example: `feature/registration`

If you cannot infer these from $ARGUMENTS, ask the user
to clarify before proceeding.

## Step 3 — Check branch name is not taken
Run `git branch` to list existing branches.
If `branch_name` is already taken, append a number:
`feature/registration-01`, `feature/registration-02` etc.

## Step 4 — Switch to main and pull latest
Run:
```
git checkout main
git pull origin main
```

## Step 5 — Create and switch to the feature branch
Run:
```
git checkout -b <branch_name>
```

## Step 6 — Research the codebase
Read these before writing the spec:
- `CLAUDE.md` — roadmap, conventions, schema, implemented-vs-stub route table
- `src/main/java/com/spendly/controller/` — existing routes and structure
- `src/main/java/com/spendly/service/` — existing business logic
- `src/main/java/com/spendly/repository/` — existing DB access (`JdbcTemplate`)
- `src/main/java/com/spendly/model/` — existing records/POJOs
- `src/main/resources/schema.sql` — existing schema
- `src/main/resources/templates/layout.html` — shared Thymeleaf layout
- All files in `.claude/specs/` — avoid duplicating existing specs

Check `CLAUDE.md`'s "Implemented vs stub routes" table to confirm the
requested step is not already marked complete. If it is, warn the user
and stop.

## Step 7 — Write the spec
Generate a spec document with this exact structure:

---
# Spec: <feature_title>

## Overview
One paragraph describing what this feature does and why
it exists at this stage of the Spendly roadmap.

## Depends on
Which previous steps this feature requires to be complete.

## Routes
Every new route needed, and which controller it belongs to:
- `METHOD /path` — description — access level (public/logged-in) — `XController`

If no new routes: state "No new routes".

## Database changes
Any new tables, columns, or constraints needed for `schema.sql`.
Always verify against the current `src/main/resources/schema.sql` and
the relevant `repository/` classes before writing this.
If none: state "No database changes".

## Service layer
New or modified methods in `service/`, and which controller(s) call them.
If none: state "No service changes".

## Repository layer
New or modified methods in `repository/` (`JdbcTemplate`, `?` placeholders).
If none: state "No repository changes".

## Templates
- **Create:** list new templates with their path, noting they extend
  `layout.html` via the Thymeleaf layout dialect
- **Modify:** list existing templates and what changes

## Files to change
Every file that will be modified.

## Files to create
Every new file that will be created.

## New dependencies
Any new Maven dependencies (`pom.xml`). Per CLAUDE.md, these must be
flagged explicitly — never add mid-feature without calling it out.
If none: state "No new dependencies".

## Rules for implementation
Specific constraints Claude must follow. Always include:
- No JPA/Hibernate or other ORM — `JdbcTemplate` only
- Parameterised queries only (`?` placeholders), never string-concatenated SQL
- Passwords hashed with `BCryptPasswordEncoder` (`spring-security-crypto`)
- DB logic stays in `repository/`; business logic stays in `service/`;
  controllers only call the service and populate the model
- Use CSS variables — never hardcode hex values
- All templates extend `layout.html` via the Thymeleaf layout dialect
- Never hardcode URLs in templates — always `th:href="@{...}"`

## Definition of done
A specific testable checklist. Each item must be
something that can be verified by running the app.
---

## Step 8 — Save the spec
Save to: `.claude/specs/<step_number>-<feature_slug>.md`

## Step 9 — Report to the user
Print a short summary in this exact format:
```
Branch:    <branch_name>
Spec file: .claude/specs/<step_number>-<feature_slug>.md
Title:     <feature_title>
```

Then tell the user:
"Review the spec at `.claude/specs/<step_number>-<feature_slug>.md`
then enter Plan Mode with Shift+Tab twice to begin implementation."

Do not print the full spec in chat unless explicitly asked.