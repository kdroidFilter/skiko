# Repository Guidelines

## Project Structure & Module Organization
- `skiko/` is the main Kotlin Multiplatform library; platform sources sit under `skiko/src` (`commonMain`, `jvmMain`, `androidMain`, `jsMain`, `nativeMain`, etc.) with matching `*Test` trees and screenshot baselines in `skiko/src/jvmTest/screenshots`.
- `skiko/buildSrc` houses custom Gradle tasks for Skia toolchains; `skiko/ci` has build scripts; `skiko/import-generator` provides the JS/Wasm import generator plugin.
- `samples/SkiaAwtSample` is included as a composite build for quick desktop checks; other samples (Android, multiplatform, web) live in `samples/` and build independently.
- `taokt/` contains Tao windowing bindings using Kotlin plus Rust code in `taokt/taokt/src/main/rust`, wired through the `gobleyCargo` plugin.

## Build, Test, and Development Commands
- Build/publish Skiko locally: `./gradlew :skiko:publishToMavenLocal` (requires platform toolchains noted in `DEVELOPMENT.md`; set `SKIA_DIR` to reuse a local Skia checkout).
- General tests: `./gradlew :skiko:check`. Web paths: `./gradlew :skiko:jsBrowserTest` or `:skiko:wasmJsBrowserTest` (Karma + ChromeHeadless).
- UI/screenshot tests: `./gradlew :skiko:awtTest -Dskiko.test.ui.enabled=true` and avoid input while they run.
- Samples: `./gradlew :SkiaAwtSample:run` after publishing Skiko. Tao bindings: `./gradlew :taokt:check`; when editing Rust, run `cargo fmt && cargo test` inside `taokt/taokt`.

## Coding Style & Naming Conventions
- Kotlin: JetBrains defaults (4-space indents, UpperCamelCase types, lowerCamelCase members). Keep `expect/actual` pairs aligned and prefer `@file:OptIn` over scattered annotations.
- Native code: match nearby style in `*.cc`/`*.mm`/`*.h` (brace on next line, snake_case where used). Rust follows standard `cargo fmt`; keep modules and functions snake_case.
- Place platform-specific helpers in their source set and name tests with intent (e.g., `rendersSvgText_onMetal`).

## Testing Guidelines
- JVM uses JUnit/`kotlin.test`; JS/Wasm use Karma/ChromeHeadless; Native/iOS runs through Kotlin/Native. Add coverage in the platform where the change lands; prefer `commonTest` for shared logic.
- Update screenshot baselines only for intentional rendering changes and mention the update in the PR.
- Gate slow or GPU-specific checks behind system properties (see `DEVELOPMENT.md`) and document required flags in test KDoc.

## Commit & Pull Request Guidelines
- Open a YouTrack issue for substantial work and reference it. Use imperative, capitalized commit subjects without trailing periods; avoid merge commits.
- PRs should explain the why, list test commands/platforms, link issues, and include release notes when user-visible. Keep scope tight and pair code with tests when feasible.
- Run relevant Gradle tasks before submitting (`:skiko:check`, UI tests for rendering work, sample runs if demos change).
