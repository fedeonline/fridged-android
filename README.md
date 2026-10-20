# fridged-android

Personal Android app for the fridge controlled by
[`fridged`](https://github.com/fedeonline/fridged): current state, temperature
history and remote changes to the target band, all through the Firestore documents
the daemon already publishes. See [`docs/PLAN.md`](docs/PLAN.md).

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

## Build and install

```sh
./gradlew testDebugUnitTest          # unit tests
./gradlew installDebug               # debug build, reads the simulated device fridge-sim
./gradlew installRelease             # signed release, reads the real fridge
```

Debug and release have different application ids and can be installed side by side.

## Owner claim

Firestore rules only allow users with the `owner` custom claim. Set it once with a
service account key (kept outside the repo):

```sh
cd scripts && npm install
GOOGLE_APPLICATION_CREDENTIALS=/path/to/service-account.json node set-owner-claim.mjs you@example.com
```

## License

MIT
