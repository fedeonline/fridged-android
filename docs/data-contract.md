# Data contract with fridged

The Firestore documents this app reads and writes, as produced and consumed by the
`fridged` daemon (`fedeonline/fridged`).

Derived from `fridged` at commit `db8e66a` (no Rust changes since `35bb832`). Sources:

- `fridged/src/sample.rs`: the `Sample` record and its enums;
- `fridged/src/firestore/value.rs`: how a sample becomes Firestore fields;
- `fridged/src/firestore/client.rs`: document paths, one commit for samples + status;
- `fridged/src/settings.rs`: parsing of `settings/current`;
- `fridged/src/config/validate.rs`: `check_band`, `SETPOINT_RANGE`, `MIN_DEAD_BAND`;
- `fridged/src/control.rs`, `fridged/src/relay/mod.rs`: `Reason`, `RelayState`.

**Sync step**: when `fridged` changes any of the above, update this file, the fixtures
in `fixtures/`, and the code and tests that depend on them, in the same commit.

## Paths

All documents live under `devices/{deviceId}/` in the `(default)` database.
Device ids: `fridge` (real), `fridge-sim` (`fridged --simulate`).

| Path | App access |
| --- | --- |
| `status/current` | read |
| `samples/{bootId}-{uptimeMs:012}` | read |
| `settings/current` | read, write, delete |

The daemon writes the samples of an upload round and `status/current` in one commit,
so the status never runs ahead of the samples. Rounds run every `upload_interval_secs`
(default 300 s), one sample per round. Samples are uploaded only once the Pi clock is
NTP-synced, so `time` is always a synced (possibly re-dated) time.

## Sample fields (`status/current` and `samples/*`)

`status/current` is a copy of the latest sample of the round.

| Field | Firestore type | Android SDK type | Notes |
| --- | --- | --- | --- |
| `time` | timestamp | `Timestamp` | millisecond precision |
| `timeMs` | integer | `Long` | same instant as `time` |
| `clockSynced` | boolean | `Boolean` | |
| `timeCorrected` | boolean | `Boolean` | `time` re-dated after NTP sync |
| `bootId` | string | `String` | |
| `uptimeMs` | integer | `Long` | |
| `temperature` | double or **null** | `Double?` | null: no reading on that tick |
| `relay` | string | `String` | `ON` / `OFF`, after compressor protections |
| `wanted` | string | `String` | `ON` / `OFF`, requested by the controller |
| `reason` | string | `String` | `aboveMax`, `atOrBelowMin`, `inBand`, `stale`, `safeOn`, `safeOff` |
| `mode` | string | `String` | `NORMAL` / `SAFE` |
| `overrunFault` | boolean | `Boolean` | |
| `targetMin` | double or **null** | `Double?` | null only in samples spooled before the band was recorded |
| `targetMax` | double or **null** | `Double?` | as `targetMin` |

`wanted != relay` means the compressor protections (minimum on/off time, forced
pause) are holding the command; the hold reason is not published.

## Settings (`settings/current`)

| Field | Firestore type |
| --- | --- |
| `targetMin` | double, or integer (the console stores whole numbers as integers) |
| `targetMax` | double, or integer |

- Read by the daemon after every successful upload: a change applies within about one
  round.
- Missing document: the daemon goes back to the band in its config file.
- Invalid document: ignored, current band kept, healthchecks.io pinged with `/fail`.
- Other fields are ignored by the daemon; the security rules forbid them anyway.

### Band validation (`check_band`)

Both values finite, both within `0..=25` °C, and `targetMax - targetMin >= 0.5`,
computed in IEEE 754 doubles. The app (`model/Band.kt`) and the security rules use the
same rules and the same arithmetic.

## Fixtures

`fixtures/` holds documents in Firestore REST JSON, the exact shape `value.rs` writes.
Unit tests decode them into the maps the Android SDK returns and run them through the
same mapping code the Firestore repository uses.

| File | Case |
| --- | --- |
| `status-cooling.json` | normal cooling, relay ON |
| `status-held.json` | `wanted` ON, `relay` OFF: held by protections |
| `status-safe-no-reading.json` | SAFE mode, `temperature` null |
| `status-overrun.json` | overrun fault |
| `sample-legacy-no-band.json` | old sample: band null, `timeCorrected` true |
| `settings-double.json` | band written by the app |
| `settings-console-integers.json` | band typed in the console as whole numbers |
