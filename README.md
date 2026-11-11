# capacitor-odc-home-widget

Android home screen widgets for OutSystems Developer Cloud (ODC) mobile apps
(Capacitor stack, MABS 12+). The app sends JSON; the widget draws one of three
native templates and opens a deep link when tapped. Widgets read the last saved
data, so they work while the app is closed and survive reboots.

**Android only in v1.** iOS methods reject with "Home widgets are Android-only in v1"
(`isPinSupported` resolves `{ supported: false }`).

| Template | Default size | Shows |
|---|---|---|
| `COUNT` | 2×2 | title, one big value, subtitle |
| `LIST` | 4×2 | title, up to 3 tappable rows |
| `ACTIONS` | 4×1 | title, up to 3 buttons |

## Repository layout

| Path | Contents |
|---|---|
| `src/` | TypeScript API |
| `android/src/main/java/com/odchomewidget/` | Plugin, providers, renderer, storage |
| `android/src/main/res/` | Layouts (`odc_widget_*`), provider info, light/dark colors |
| `ios/` | Stub |
| `example-app/` | Standalone Capacitor app to test on a phone without ODC |
| `odc-library/ODC_LIBRARY.md` | Everything to build the ODC Mobile Library and demo app |

## API

| Method | Returns |
|---|---|
| `setWidgetData({ kind, data })` | `void`. `data` is a JSON **string** |
| `refresh({ kind })` | `void` |
| `clear({ kind })` | `void` |
| `isPinSupported()` | `{ supported }` |
| `requestPin({ kind })` | `void` (Android 8+, launcher must support it) |
| `getInstalledCount({ kind })` | `{ count }` |
| `addListener('widgetTap', cb)` | `cb({ kind, deepLink })` |
| `removeAllListeners()` | `void` |

## Data shapes

Keys are matched case-insensitively (`Title` or `title`).

```json
// COUNT
{ "title": "Open approvals", "value": 12, "subtitle": "Updated 09:30", "deepLink": "approvals" }

// LIST  (first 3 items; a row without deepLink uses the list's deepLink)
{ "title": "My tasks", "deepLink": "tasks",
  "items": [ { "text": "PO #4512", "deepLink": "tasks/1" }, { "text": "Invoice INV-0098" } ] }

// ACTIONS  (first 3 actions)
{ "title": "Quick actions",
  "actions": [ { "label": "New request", "deepLink": "requests/new" }, { "label": "Scan", "deepLink": "scan" } ] }
```

## Error codes

| Code | Meaning |
|---|---|
| `OS-PLUG-HWGT-0001` | `kind` is not COUNT, LIST or ACTIONS |
| `OS-PLUG-HWGT-0002` | `data` missing, not JSON, or wrong shape for the kind |
| `OS-PLUG-HWGT-0003` | Could not save the data |
| `OS-PLUG-HWGT-0004` | Launcher can't pin widgets from the app |
| `OS-PLUG-HWGT-0005` | Launcher refused the pin request |
| `OS-PLUG-HWGT-0006` | Unexpected error |

## Build

```bash
npm install
npm run build
```

Commit `dist/` before tagging: MABS installs the plugin from the Git tag and
does not run the build.

## Try it on a phone without ODC

Needs JDK 21 and the Android SDK (API 35), e.g. via Android Studio.

```bash
cd example-app
npm install
npm run sync
npx cap run android
```

Or `npm run open` to open it in Android Studio. Set data in the app, then
long-press the home screen → Widgets → *Home Widget Demo*.

## Use in ODC

See [odc-library/ODC_LIBRARY.md](odc-library/ODC_LIBRARY.md).

## Troubleshooting

| Symptom | Likely cause | Fix |
|---|---|---|
| Widget missing from the picker | Receiver not merged, or cached plugin version | Check the merged manifest in the build log; bump the tag and rebuild |
| `CheckHomeWidgetPlugin` is false | App on the Cordova stack or MABS < 12 | Set framework to Capacitor, MABS 12.1 |
| "Open the app to set up" forever | `setWidgetData` never ran, or JSON rejected | Read the `Error` output |
| Tap opens a second app copy | Host activity not reused | Report it with the launcher model; a `singleTask` build action is the fallback |
| Stops updating on Xiaomi/Oppo | Battery optimization | The widget still updates whenever the app sets data |

## Limitations

- One data set per template: two LIST widgets show the same list.
- Widgets only change when the app calls `setWidgetData` (no background refresh).
- Android only.

## License

MIT
