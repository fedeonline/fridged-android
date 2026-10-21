# fridged-android — plan

Status: implemented. Features 1–8 are done (notifications still deferred, see
section 2); section 12 records where the implementation refined this plan.
Data contract: [`data-contract.md`](data-contract.md), derived from
`fedeonline/fridged` at commit `db8e66a`.

## 1. Goal

A personal Android app for the fridge controlled by `fridged`:

- see the current state of the fridge;
- chart the temperature over time;
- change the target band (`targetMin` / `targetMax`) remotely.

The app only reads and writes Firestore documents that the daemon already
publishes or reads. Nothing changes on the Raspberry Pi.

## 2. Decisions taken

| Topic | Decision |
| --- | --- |
| Users | Single user (Fede). No sign-up, no multi-user features. |
| Distribution | Not published on the Play Store; sideloaded (`adb install` or APK copy). |
| Repository | Separate public repo `fedeonline/fridged-android`, next to `fridged`. |
| Secrets | None in the repo (see section 9). |
| Authentication | Firebase Authentication, email + password. User created by hand in the console. |
| Sign-up | Disabled via Identity Platform ("User actions" → uncheck "Enable create"). Free on the Spark plan. |
| Authorization | Security rules grant access only to users with an `owner` custom claim, set once by a local script. No UID in the repo. |
| Development data | Debug builds target the simulated device `fridge-sim` (`fridged --simulate`). |
| Android version | Latest Android only: `minSdk` = `targetSdk` = latest stable API level at setup time. No compatibility code for older versions. |
| UI language | English and Italian, following the system language; strings in `values/` (English, default) and `values-it/`. Per-app language selection from system settings comes for free. |
| Notifications | Not needed for now; maybe later. healthchecks.io keeps covering stale data and overrun fault. |
| Stale data | Status is shown as stale when `time` is more than 15 minutes old (three missed upload rounds). |
| Settings document | The app writes exactly `targetMin` and `targetMax` (see 3.3). |
| Charts | Vico. |

## 3. Data contract (from fridged)

The maintained version of this section, with fixtures, is
[`data-contract.md`](data-contract.md); it supersedes the summary below (for example,
`targetMin`/`targetMax` are null in samples spooled before the band was recorded).

All documents live under `devices/{deviceId}/`. Real device id: `fridge`;
simulated: `fridge-sim`.

### 3.1 `status/current` (read)

The latest sample, written in the same commit as the samples, every
upload round (300 s).

| Field | Type | Meaning |
| --- | --- | --- |
| `time` | timestamp | sample time (RFC 3339) |
| `timeMs` | int64 (as string) | same, ms since epoch |
| `clockSynced` | bool | NTP was synced when the sample was taken |
| `timeCorrected` | bool | `timeMs` re-dated after NTP sync |
| `bootId`, `uptimeMs` | string, int64 | stable identity of the sample |
| `temperature` | double or null | °C; null when no reading in that tick |
| `relay` | string | real relay state after protections: `ON` / `OFF` |
| `wanted` | string | state requested by the controller |
| `reason` | string | controller reason: `aboveMax`, `atOrBelowMin`, `inBand`, `stale`, `safeOn`, `safeOff` |
| `mode` | string | `NORMAL` / `SAFE` |
| `overrunFault` | bool | compressor ran too long without reaching the band |
| `targetMin`, `targetMax` | double | band currently applied by the daemon |

Notes:

- When `wanted != relay`, the compressor protections are holding the
  command (min on/off time or forced pause). The hold reason itself is not
  published.
- Samples are uploaded only once the Pi clock is synced, so `time` in
  Firestore is always a synced (possibly corrected) time.

### 3.2 `samples/{bootId}-{uptimeMs:012}` (read)

One document per sample, same fields as `status/current`. One every
5 minutes: 288 per day, about 2016 per week. Ids are stable (retries
overwrite). History queries filter and order by `time`.

### 3.3 `settings/current` (write)

| Field | Type |
| --- | --- |
| `targetMin` | double |
| `targetMax` | double |

Daemon behaviour:

- read after every successful upload, so a change takes effect within
  about 5 minutes (longer if uploads fail);
- valid document → applied, saved locally on the Pi, survives reboots;
- missing document (404) → daemon reverts to the band in its config file;
- invalid document → ignored, current band kept, healthchecks.io pinged
  with `/fail`;
- validation (`check_band`): finite values, both within 0–25 °C,
  `targetMax - targetMin >= 0.5`.

Verified in `settings.rs` (commit `35bb832`): `parse` reads only
`fields.targetMin` and `fields.targetMax` (as `doubleValue` or
`integerValue`) and ignores any other field. Extra fields would therefore
be harmless to the daemon. The app still writes exactly these two fields,
and the security rules enforce it; adding fields later only needs a rules
change.

## 4. Security rules

The daemon uses a service account through the REST API and is not
affected by security rules, so the rules only concern the app.

Behaviour:

- every access requires `request.auth.token.owner == true`;
- `status` and `samples`: read only, no writes;
- `settings/current`: read, write and delete; writes must contain exactly
  `targetMin` and `targetMax`, both numbers, within 0–25,
  difference >= 0.5;
- everything else: denied.

The rules live in the repo (`firebase/`) and are tested against the
Firestore emulator before being deployed:

- owner can read status and samples;
- authenticated user without the claim cannot read anything;
- unauthenticated access is denied;
- invalid bands (out of range, too narrow, extra fields, wrong types) are
  rejected;
- nobody can write `status` or `samples`.

## 5. Architecture

Stack: Kotlin, Jetpack Compose, Firebase Android SDK (Auth + Firestore),
Vico for charts. MVVM with ViewModels
exposing observable UI state.

Single Gradle module `app`, organized in packages:

| Package | Responsibility |
| --- | --- |
| `model` | `Sample` (also the shape of `status/current`), `Band`, band validation (same rules as `check_band`), freshness logic, history stats. Pure code, unit tested. |
| `data` | `FridgeRepository` interface; Firestore implementation; in-memory fake for previews and tests. |
| `auth` | email/password sign-in, session state, sign-out. |
| `ui` | one folder per screen, each with its ViewModel; theme. |
| `widget` | home-screen widget (Glance) and its refresh worker. |

Same philosophy as the daemon: logic behind interfaces with fakes, so the
whole UI can be developed and tested without Firebase.

Firestore offline persistence is enabled; history queries fetch only
samples newer than the latest one already cached.

## 6. Screens

Sign-in (only when signed out), then a bottom navigation with three
sections.

**Status** (home)

- Banners, only when relevant: overrun fault, SAFE mode, stale data
  (`time` older than 15 minutes).
- Large temperature, with "updated N min ago" below.
- Compressor card: ON/OFF, human-readable reason, "held by protections"
  when `wanted != relay`.
- Current band.
- Mini chart of the last 6 hours; tapping it opens History.

**History**

- Range selector: 6 h / 24 h / 7 days.
- Chart: temperature curve, shaded target band, compressor-on strips under
  the curve, gaps where `temperature` is null, SAFE periods highlighted.
- Stats for the range: min, max, mean, compressor duty cycle.

**Band**

- Current band.
- Min/max steppers on a 0.5 °C grid, with immediate validation messages
  (see section 12).
- Save → state "waiting for the fridge" until `status/current` reports the
  same band, then "applied at HH:MM".
- "Restore defaults" → deletes `settings/current`; confirmed when the
  applied band changes, or after two newer statuses (the daemon's default is
  not published).

**Settings** (small menu)

- Sign out.
- Debug builds only: device selector `fridge` / `fridge-sim`.

## 7. Features and definition of done

| # | Feature | Done when | Done |
| --- | --- | --- | --- |
| 1 | Firebase and project setup | App builds, installs with `adb`, sign-in works in debug and release; sign-up disabled; owner claim set. | ✓ |
| 2 | Security rules | Rules tests pass in the emulator; rules deployed. | ✓ |
| 3 | Model and fake repository | Models, band validation and freshness covered by unit tests; Compose previews run on fake data. | ✓ |
| 4 | Status screen | Against `fridge-sim` it updates in real time and banners appear when they should. | ✓ |
| 5 | History | 24 h chart loads from cache on second open without re-reading all samples. | ✓ |
| 6 | Band | A change made in the app is applied by the simulated daemon and shown as confirmed. | ✓ |
| 7 | Release | APK signed with the personal keystore, installed on the phone, pointing at the real fridge. | ✓ |
| 8 | Extras (later) | Home-screen widget. Notifications deferred (see section 2). | widget ✓ |

The SAFE-mode and overrun banners are covered by unit tests and previews only:
the simulator cannot produce those states on demand.

Features 1–4 already make a usable app.

## 8. Development workflow

- One cycle per feature: model and tests first, UI on the fake repository,
  then Firestore wiring tested against `fridge-sim`.
- The real fridge is used only from feature 7.
- `CLAUDE.md` in the repo with the same conventions as `fridged`: code and
  docs in English, local `PROGRESS.md` (git-ignored), commit after each
  feature, never commit credentials.
- `docs/` holds this plan, the data contract and JSON fixtures of status and
  samples used by tests. When `fridged` changes a field, updating the
  contract and fixtures is the explicit sync step between the two repos.

## 9. Secrets and repo hygiene

Not in the repo:

| Item | Where it lives |
| --- | --- |
| `google-services.json` | local only; repo has `google-services.example.json` with fake values |
| Release keystore | outside the project directory, backed up |
| Keystore passwords | `~/.gradle/gradle.properties`; release build fails clearly if missing |
| `.firebaserc` (project id) | local only; README documents `firebase use --add` |
| Service account JSON (for the claim script) | local only, never in the repo |

`.gitignore` is written in the first commit, before any Firebase file is
added. A gitleaks pre-commit hook (`.githooks/pre-commit`, enabled with
`git config core.hooksPath .githooks`) refuses commits that add secrets, and
blocks them if gitleaks is not installed.

Repository layout:

```
fridged-android/
├── .githooks/  pre-commit secret scanner
├── app/        Android project
├── firebase/   security rules, CLI config, rules tests
├── scripts/    owner-claim script
├── docs/       plan, data contract, JSON fixtures
├── CLAUDE.md
└── README.md
```

## 10. Costs

Spark (free) plan is expected to be enough:

- Authentication: email/password free (limits far above one user).
- Firestore: free quota of 50k reads/day. A full 7-day history is about
  2016 reads without cache; with offline persistence and incremental
  queries, normal use is far below the quota.
- Notifications via Cloud Functions would require the Blaze plan: one
  reason they are deferred.

## 11. Open questions

1. ~~Identity Platform upgrade: confirm in the console that it stays free on
   Spark before enabling it.~~ Resolved: the project stays on Spark after the
   upgrade; sign-up is disabled ("Enable create" unchecked).

## 12. Implementation notes

Choices made while building, not visible in the sections above:

- **Application ids**: `io.github.fedeonline.fridged` (release) and
  `io.github.fedeonline.fridged.debug` (debug, orange launcher icon), so both
  install side by side; both are registered in the same Firebase project. The
  Firebase Android API key is restricted to these two apps and their signing
  certificates.
- **Release keystore** created during feature 1, to verify release sign-in
  early, rather than in feature 7.
- **Band on a 0.5 °C grid**: `check_band` compares `max - min >= 0.5` in IEEE
  doubles, so some one-decimal pairs that look 0.5 apart are rejected
  (`4.1 - 3.6 = 0.49999999999999956`). Steppers on a 0.5 grid only produce
  values exact in binary, so the app never offers a band the daemon refuses.
- **Incremental history**: samples come from the Firestore cache, plus a live
  server query from 30 minutes before the latest cached sample (late uploads).
  How far back the cache is complete is stored per device; a wider range is
  read whole once. The listener includes metadata changes, otherwise the
  server's confirmation of unchanged cached data never arrives.
- **Charts**: the curve breaks at missing readings and at holes longer than
  15 minutes (daemon offline); the band shading follows the band recorded in
  each sample. The Status sparkline uses the same data.
- **Widget**: Glance, refreshed by WorkManager every 15 minutes (Android's
  minimum), when added, when the app opens, on sign-out and on a device switch;
  one document read per refresh. It shows the sample's clock time rather than
  "N min ago", which would go stale between refreshes.
- **Debug device switch**: the choice is remembered; the top bar always names
  the device, since debug can also change the real fridge's band.
- **Simulator for development** runs on a development machine, not on the Pi
  (see README).
- **Visual design**: always dark with a fixed pastel palette (`ui/theme/Palette.kt`),
  Manrope (SIL OFL, licence in `app/src/main/assets/licenses/`). Chosen from three
  mockups ("dial", "pastel cards", "instrument"): the dial won. The Status dial shows
  the band as a mint arc and the temperature as a needle on a 10 °C scale centred on
  the band; Band shows the same dial with blue (min) and peach (max) knobs; the widget
  draws it as a bitmap, since Glance cannot draw arcs, and shrinks when narrow.
  Colors carry meaning: mint band/OK, blue compressor/minimum, peach maximum/stale,
  lilac curves and SAFE, pastel red faults.
