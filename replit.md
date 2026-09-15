# RailGuard

RailGuard is a dark-first mobile field engineering platform for AI-assisted railway crack detection, inspection evidence, risk analysis, and maintenance verification.

## Run & Operate

- `pnpm --filter @workspace/api-server run dev` — run the API server (port 5000)
- `pnpm run typecheck` — full typecheck across all packages
- `pnpm run build` — typecheck + build all packages
- `pnpm --filter @workspace/api-spec run codegen` — regenerate API hooks and Zod schemas from the OpenAPI spec
- `pnpm --filter @workspace/db run push` — push DB schema changes (dev only)
- Required env: `DATABASE_URL` — Postgres connection string

## Stack

- pnpm workspaces, Node.js 24, TypeScript 5.9
- API: Express 5
- DB: PostgreSQL + Drizzle ORM
- Validation: Zod (`zod/v4`), `drizzle-zod`
- API codegen: Orval (from OpenAPI spec)
- Build: esbuild (CJS bundle)

## Where things live

- `artifacts/railguard/app/` — Expo Router screens and navigation
- `artifacts/railguard/components/RailGuard.tsx` — shared mobile UI primitives, screen registry, and screen renderers
- `artifacts/railguard/constants/colors.ts` — RailGuard semantic color tokens
- `artifacts/railguard/assets/images/icon.png` — RailGuard app icon

## Architecture decisions

- The first build is frontend-only and uses local device persistence for settings and field-workspace state.
- A route-driven screen registry keeps the large inspection product surface consistent without duplicating screen chrome.
- Railway status semantics are always expressed with text and icons in addition to color.
- Expo Router provides the tabbed control surface and deep links into every supporting workflow screen.

## Product

RailGuard covers authentication and onboarding, live inspection capture, AI crack detections, defect history, evidence and engineer verification, railway maps, maintenance tasks, reports, analytics, scheduling, and field-team settings. The primary flow is inspection setup → live inspection → detection result → crack review → measurement/verification → saved inspection.

## User preferences

- Premium, technical, dark-first industrial interface; avoid generic SaaS styling, excessive gradients, and placeholder-looking screens.

## Gotchas

- The mobile app is served by the managed Expo workflow `artifacts/railguard: expo`.

## Pointers

- See the `pnpm-workspace` skill for workspace structure, TypeScript setup, and package details
