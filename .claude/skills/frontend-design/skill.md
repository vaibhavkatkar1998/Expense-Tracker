---
name: spendly-frontend
description: Build and style frontend pages for Spendly, a Java 21 + Spring Boot + Thymeleaf expense tracker (vanilla JS only, no frontend frameworks). Use this whenever the user is implementing one of Spendly's stub routes (profile, expenses/add, expenses/{id}/edit, expenses/{id}/delete, logout), adding or editing a Thymeleaf template, writing page-specific CSS, or wiring up vanilla JS interactivity anywhere in the app. Also use when the user asks to style, redesign, polish, or "make it look better" for any Spendly page, or mentions Spendly, the expense tracker frontend, layout.html, or files under src/main/resources/templates or static/css/js — even if they don't say "frontend" explicitly.
---

# Spendly Frontend Development

Spendly is a personal expense tracker: Java 21 + Spring Boot + Thymeleaf, vanilla JS only, H2 for storage. The project's `CLAUDE.md` already defines the code-organization rules (where controllers/services/repositories live, naming, `th:href`, no new dependencies) — that stays the source of truth for backend and architecture decisions. **This skill covers what CLAUDE.md doesn't**: how to actually build a page that looks and feels consistent with the rest of the app, and the specific workflow for finishing off Spendly's remaining stub routes.

Two things matter more than anything else here:

1. **Match what already exists before adding something new.** Spendly already has real, implemented pages (landing, register, login) and a shared `layout.html`. Those are the ground truth for how this app actually looks — not assumptions, not generic Bootstrap-style defaults. Read them first, every time.
2. **Only build the step that was asked for.** CLAUDE.md's stub-route table is explicit that stub routes shouldn't be implemented ahead of the task that targets them. This skill is no exception — if the user asks for the `profile` page, build the `profile` page, not a preview of `expenses/add` too.

## Workflow for building a stub page

Spendly's remaining stub routes are `GET /logout`, `GET /profile`, `GET /expenses/add`, `GET /expenses/{id}/edit`, and `GET /expenses/{id}/delete`. For whichever one is the active task:

1. **Read before writing.** Open `layout.html`, at least one already-implemented page (`register.html` or `login.html` are the best references since they involve forms), `style.css`, and `landing.css`. Note the existing patterns: how the layout dialect is used (`th:replace` / `th:insert` / `layout:decorate`, whichever this project uses), how forms are structured, how validation errors are shown, how buttons and inputs are classed. Carry those patterns forward rather than inventing new ones — a second form on the site should look like it was built by the same person who built the first one.
2. **Check the controller.** Look at the controller method for this route before writing the template — confirm what model attributes it actually passes in, since the template can only bind to what's there. Remember repository classes may still be empty at this stage of the project; don't assume a repository method exists just because the template needs data from it — flag it if the wiring isn't there yet rather than quietly stubbing around it.
3. **Build the template.** New file in `templates/`, extending `layout.html` the same way the existing pages do. Every internal link and form action uses `th:href` / `th:action` — never a hardcoded path.
4. **Add page-specific CSS only if the page needs rules beyond what's already global.** Check `style.css` first — buttons, inputs, and card styles are likely already defined there from the register/login pages. Only add a new `.css` file for things genuinely specific to this page (e.g. an expense table, a delete-confirmation state), and use the design tokens in `references/design-system.md` for anything new so it stays visually consistent with the rest of the app.
5. **Add vanilla JS only if there's real interactivity** (client-side validation feedback, confirm-before-delete, dynamic totals). Put it in `main.js` alongside whatever's already there, following its existing style (e.g. how it selects elements, whether it uses event delegation). No frameworks, no npm packages, no inline `<script>` blocks scattered across templates unless that's already the established pattern.

## Design system

Spendly doesn't have a documented visual language yet beyond whatever ended up in `style.css`. Read `references/design-system.md` before styling anything — it defines a color palette, spacing scale, typography, and ready-to-use CSS for common pieces (buttons, form fields, cards, tables, badges, alerts) sized for a finance app: calm, trustworthy, easy to scan at a glance.

**Important:** if `style.css` already has colors, fonts, or component styles in it (from the landing/register/login pages), those existing values win — extend them rather than introducing a second, competing palette. The reference file is there to fill gaps (e.g. there's currently no defined style for an expense table or a delete-confirmation button), not to override what's already shipped. If there's a real conflict — say the existing primary color clashes with what a new page needs — point it out to the user rather than silently picking one.

## What each stub route actually needs

A quick sense of what each page is for, so the UI serves the actual task instead of being generic:

- **`/profile`** — likely a read/edit view of the logged-in user's account details. Needs a form (or read + "edit" toggle) using the same input/label patterns as register/login.
- **`/expenses/add`** — a form to create an expense: amount, category, date, description at minimum. This is the first place a category badge or amount-input pattern is needed — check `references/design-system.md` for those.
- **`/expenses/{id}/edit`** — the same form as add, pre-filled with the existing expense's values.
- **`/expenses/{id}/delete`** — typically a confirmation step before an irreversible action, not a silent delete. A confirm dialog (JS `confirm()` is fine for something this simple — no need to build a custom modal unless the user asks) or a dedicated confirmation page fits the "danger action" styling in the design system.
- **`/logout`** — usually no real UI of its own, just a redirect after clearing the session; sanity-check with the user if a template seems to be expected here.

Don't take these descriptions as fixed requirements — they're a starting assumption. If the controller, `schema.sql`, or the user's own description of the task implies something different, follow that instead.