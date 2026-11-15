# Study Flow – Android (v2.5.4)

Native Android version of the Study Flow web app, written in Kotlin with Jetpack Compose and Material 3.

## Open & run
1. Open this folder in **Android Studio** (Ladybug or newer). Let Gradle sync (it downloads Gradle 8.9 and the dependencies).
2. Pick a phone or emulator (Android 8.0 / API 26 or higher) and press **Run**.
   Or from a terminal: `./gradlew assembleDebug` → `app/build/outputs/apk/debug/app-debug.apk`.

## What's in it
- Same 8 sections as the web app: Dashboard, Assignments, Courses, Attendance, Routine, Calendar, Focus Timer, Settings.
- Navigation: an icon-only rail; the menu button expands it with labels and it collapses again after you pick a section.
- Routine: **Day** view (swipe between days) and **Week** grid (scrolls both ways), switched with a toggle.
- Semesters, subtasks, course colours, merge/replace import – all as on the web.
- Real file attachments on tasks (copied into app storage, tap the chip to open).
- Reminders: 15 minutes before each routine session, daily 8:00 AM "due today" alert, focus-timer alert.
- Backups use the same JSON format as the web version, so they can be moved either way
  (attached files are not part of a backup).

## Branding
`branding/` holds the app icon as SVG and a 512 px PNG (red and black layers on a warm off-white background (#F4F0E9, same as the Subscription Tracker icon)).

## Notes
- Data is stored in the app's private storage as `studyflow_data.json`.
- "Cross-Platform Sync" is still the simulated placeholder from the web version.
- For on-time class reminders, allow exact alarms from Settings → Notifications.
- Font: Plus Jakarta Sans (SIL Open Font License, see FONT_LICENSE_OFL.txt).
