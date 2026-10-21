# CLAUDE.md

**Before anything else, read `PROGRESS.md` if it exists**: it holds the current feature,
the next task, decisions already made, and open blockers. Work proceeds in interruptible
steps and the state lives there, not in the conversation.

`PROGRESS.md` is a **local file, ignored by git and never published**: it holds details
of the actual installation (Firebase project, accounts, paths). Never add it to a commit,
and never copy such details into tracked files.

After each completed feature: tick the checkbox in `PROGRESS.md`, add a line to its session
log, and commit the code. Commits use the author configured in this repository.

## What this project is

A personal Android app for the fridge controlled by the `fridged` daemon
(`fedeonline/fridged`). It shows the current state, charts the temperature over time and
changes the target band, only by reading and writing the Firestore documents the daemon
already publishes or reads. Nothing changes on the Raspberry Pi. The full plan is in
`docs/PLAN.md`.

## Constraints that must not be violated

- **Never commit credentials.** `google-services.json`, keystores, keystore passwords,
  `.firebaserc` and service account JSON files stay local (see `.gitignore` and
  `docs/PLAN.md` section 9). The repo only holds `google-services.example.json` with fake
  values. Check `git status` before every commit.
- **The data contract is owned by `fridged`.** `docs/` holds the contract and the JSON
  fixtures used by tests. When `fridged` changes a field, updating the contract and the
  fixtures is the explicit sync step between the two repos.
- **The app writes exactly `targetMin` and `targetMax`** to `settings/current`, validated
  with the same rules as the daemon's `check_band` (finite, 0–25 °C, max − min ≥ 0.5).
- **Latest Android only**: `minSdk` = `targetSdk` = latest stable API level. No
  compatibility code for older versions.
- **Logic behind interfaces with fakes**: `FridgeRepository` has an in-memory fake, so the
  whole UI can be developed, previewed and tested without Firebase. `model` stays pure
  Kotlin (no Android, no Firebase, no clock reads: pass the current instant in).
- Debug builds target the simulated device `fridge-sim`; the real `fridge` is used only
  from feature 7 (release).

## Conventions

- **All code, comments and documentation in English.** Conversation with the user may be
  in Italian, but nothing in the repository is.
- **Every user-visible string goes in resources**: English in `values/strings.xml`
  (default), Italian in `values-it/strings.xml`. Add both at the same time.
- One cycle per feature: model and tests first, UI on the fake repository, then Firestore
  wiring tested against `fridge-sim`.
- `./gradlew lint testDebugUnitTest` clean before every commit; when `firebase/` changes,
  `cd firebase && npm test` too. Never deploy rules that have not passed the tests.
