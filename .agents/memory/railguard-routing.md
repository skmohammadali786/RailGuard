---
name: RailGuard route resolution
description: Expo preview routing behavior for RailGuard's field workflow pages.
---

Use explicit Expo Router files for user-facing RailGuard pages instead of relying on the single-segment catch-all route for primary navigation. The preview can resolve the catch-all inconsistently and show the splash or blank output for valid-looking paths.

**Why:** Direct links to dynamic screen paths failed in the Expo preview even though the shared screen registry contained the matching content.

**How to apply:** When adding a user-visible RailGuard destination, create a dedicated file under `artifacts/railguard/app/` and pass the intended screen key to `RailGuardScreen`; keep the catch-all only as a fallback.