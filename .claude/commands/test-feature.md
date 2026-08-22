---
description: Writes and runs tests for a specific Spendly feature. Pass the spec name as argument e.g. /test-feature 05-backend-connection
allowed-tools: Bash(./mvnw:*)
---

Run the full testing pipeline for the feature specified
in $ARGUMENTS.

If no argument is provided, stop immediately and say:
"Please provide a spec name. Usage: /test-feature
<spec-name> e.g. /test-feature 05-backend-connection"

If `.claude/specs/$ARGUMENTS.md` does not exist, stop
immediately and say:
"Spec file not found at .claude/specs/$ARGUMENTS.md.
Please check the spec name and try again."

---

## Step 1: Write Tests

Invoke the **spendly-test-writer** subagent with the
following context:

- Spec file to base tests on:
  `.claude/specs/$ARGUMENTS.md`
- Source files to read for structure:
    - `src/main/java/com/spendly/controller/`
    - `src/main/java/com/spendly/service/`
    - `src/main/java/com/spendly/repository/`
    - `src/main/java/com/spendly/model/`
- Output test file(s) to create:
  under `src/test/java/com/spendly/...`, mirroring the
  `main/java` package structure, using the exact class
  names listed in the spec's "Files to create" section
  (e.g. `LoginControllerTest.java`,
  `LoginServiceTest.java`). If the spec names no test
  files explicitly, place them alongside the
  corresponding `main` class's package.
- Instruction: Write tests based on what the spec says
  the feature SHOULD do. Do NOT derive test logic from
  reading the implementation. Cover happy paths, edge
  cases, auth guards (session-based, per CLAUDE.md),
  validation errors, and DB side effects via the
  `repository/` layer.

Wait for spendly-test-writer to fully complete and
confirm the test file(s) have been written before
proceeding to Step 2.

---

## Step 2: Run Tests

Once spendly-test-writer has finished, invoke the
**spendly-test-runner** subagent with the following
context:

- Test class(es) to execute: the exact class names
  written in Step 1, e.g.
  `src/test/java/com/spendly/controller/LoginControllerTest.java`
- Spec file for context:
  `.claude/specs/$ARGUMENTS.md`
- Source files to analyze against when diagnosing
  failures:
    - `src/main/java/com/spendly/controller/`
    - `src/main/java/com/spendly/service/`
    - `src/main/java/com/spendly/repository/`
    - `src/main/java/com/spendly/model/`
- Run command, one per test class written in Step 1:
  `./mvnw test -Dtest=<ClassName>`
- Instruction: Run ONLY the test class(es) written in
  Step 1. Do NOT run the full test suite
  (`./mvnw test` with no `-Dtest` filter). Analyze any
  failures by cross-referencing the test code, the spec,
  and the source files. Classify each failure as a bug
  or a missing feature.

---

## Handoff Rules

- Do NOT start Step 2 until Step 1 is fully complete
- Do NOT attempt to fix any code regardless of what
  the test results show
- Do NOT run any tests beyond the class(es) written in
  Step 1
- Do NOT add new Maven dependencies to `pom.xml` to make
  tests pass
- If spendly-test-writer reports it could not write
  the test file(s), stop and report the reason — do NOT
  proceed to Step 2

---

## Final Output

After both subagents complete, produce a combined
summary:

### Testing Pipeline Report — $ARGUMENTS

**Step 1 — Tests Written**
- List each test written (class#method) with a one-line
  description of which spec requirement it validates

**Step 2 — Test Results**
- Mirror the spendly-test-runner's structured report

**Verdict**
One of:
- ✅ Ready for code review — all tests pass
- ❌ Needs fixes — list the failing tests and their root causes
