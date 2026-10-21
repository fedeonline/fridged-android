# fridged-android

Personal Android app for the fridge controlled by
[`fridged`](https://github.com/fedeonline/fridged): current state, temperature
history and remote changes to the target band, all through the Firestore documents
the daemon already publishes. See [`docs/PLAN.md`](docs/PLAN.md) and the
[data contract](docs/data-contract.md).

## What it does

- **Status**: current temperature, compressor state and why, the band in force,
  warnings for stale data, SAFE mode and compressor overrun, and the last 6 hours.
- **History**: 6 h / 24 h / 7 days chart with the target band, compressor-on
  periods, SAFE periods and gaps; min, max, average and compressor duty cycle.
- **Band**: change the target band in 0.5 °C steps or go back to the daemon's
  default; the app waits until the fridge reports it applied.
- **Widget**: temperature and compressor state on the home screen, refreshed every
  15 minutes; tap to open the app.
- English and Italian, following the system language.

## Requirements

- Android Studio with the Android 17 (API 37) SDK platform; the app targets the
  latest Android only (`minSdk` = `targetSdk` = 37).
- JDK 17 or newer.
- A Firebase project with Authentication (email/password) and Cloud Firestore,
  shared with `fridged`.

## Local setup

Nothing secret is in this repository. Each machine needs:

1. **`app/google-services.json`**: download it from the Firebase console
   (Project settings → Your apps). It must contain both Android apps,
   `io.github.fedeonline.fridged` and `io.github.fedeonline.fridged.debug`.
   `google-services.example.json` shows the expected shape with fake values.
2. **Release signing** (only for `assembleRelease`): in `~/.gradle/gradle.properties`

   ```properties
   fridged.keystore.file=/absolute/path/outside/the/repo/fridged-release.jks
   fridged.keystore.password=...
   fridged.key.alias=fridged
   fridged.key.password=...
   ```

   The release build fails with a clear message if any of these is missing.

3. **Secret scanner** (blocks commits that add keys or passwords):

   ```sh
   brew install gitleaks
   git config core.hooksPath .githooks
   ```

## Build and install

```sh
./gradlew testDebugUnitTest          # unit tests
./gradlew installDebug               # debug build, reads the simulated device fridge-sim
./gradlew installRelease             # signed release, reads the real fridge
```

Debug and release have different application ids and can be installed side by side.
In the debug build, the ⋮ menu switches between `fridge` and `fridge-sim`; the top bar
always shows which one is in use.

## Developing against the simulator

Debug builds read `fridge-sim`, written by the daemon in simulation mode. Run it on a
development machine from the [`fridged`](https://github.com/fedeonline/fridged) repo,
with a copy of `deploy/config.example.toml` where, under `[firestore]`:

```toml
project_id = "<your-firebase-project>"
credentials_path = "/path/to/the/daemon/service-account.json"   # outside both repos
upload_interval_secs = 60   # faster rounds while developing (default 300)
```

```sh
cargo run -p fridged -- --config /path/to/fridged-sim.toml --simulate -v
```

`--simulate` uploads as `<device_id>-sim`, keeps its spool in the temp directory,
never drives the GPIO and sends no healthcheck pings. Stop it with Ctrl+C: the app
then shows the stale-data warning after 15 minutes, as it should.

## Firestore security rules

Rules live in `firebase/firestore.rules` and are tested against the Firestore emulator
(needs Java 11+). Tests use the demo project `demo-fridged`, never a real one.

```sh
cd firebase && npm install
npm test                                           # emulator + rules tests
npx firebase login                                 # once per machine
npx firebase use --add                             # once: pick the project (writes .firebaserc, git-ignored)
npm run deploy:rules
```

## Owner claim

Firestore rules only allow users with the `owner` custom claim. Set it once with a
service account key (kept outside the repo):

```sh
cd scripts && npm install
GOOGLE_APPLICATION_CREDENTIALS=/path/to/service-account.json node set-owner-claim.mjs you@example.com
```

## Repository layout

```
.githooks/  pre-commit secret scanner (gitleaks)
app/        Android app: model, data, auth, ui, widget
docs/       plan, data contract, JSON fixtures used by the tests
firebase/   Firestore rules, emulator tests, CLI config
scripts/    owner-claim script
```

## License

MIT
