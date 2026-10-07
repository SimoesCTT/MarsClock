# Mars Clock

A Martian timekeeper for Android. No ads, no trackers, no INTERNET permission.

## Features

- Live Mars Coordinated Time (MTC)
- Darian calendar with tasks and notes per sol
- Recurring tasks ("every N sols")
- Reminders at a given MTC time
- Live mission sols for Curiosity and Perseverance
- Jump to any Mars year
- All data stored locally (Room/SQLite)

## Screenshots

(to be added)

## Permissions

| Permission | Why |
|---|---|
| `POST_NOTIFICATIONS` | To show task reminders (Android 13+) |
| `SCHEDULE_EXACT_ALARM` | To fire reminders at the exact MTC time |
| `RECEIVE_BOOT_COMPLETED` | To reschedule reminders after reboot |

**This app does not request the `INTERNET` permission.** It cannot phone home.

## Building

Requires JDK 17+ and Android SDK 34.

    ./gradlew assembleDebug

APK lands at `app/build/outputs/apk/debug/app-debug.apk`.

## License

GPL-3.0-or-later. See [LICENSE](LICENSE).
