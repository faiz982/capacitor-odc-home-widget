# capacitor-odc-home-widget

Android home screen widget for OutSystems Developer Cloud (ODC) mobile apps
(Capacitor stack, MABS 12+). There is one widget, **Widget**, and your app
designs it entirely in JSON: a tree of rows, columns, text, icons, images,
progress rings and buttons on a gradient or image background. Change the JSON
to change the design; no plugin change is needed. The widget draws the last
saved JSON, so it works while the app is closed and survives reboots.

**Android only.** iOS methods reject with "Home widgets are Android-only in v1"
(`isPinSupported` resolves `{ supported: false }`).

## The JSON

Send it with `setWidgetData({ kind: 'WIDGET', data })` (in ODC:
`SetWidgetData(Kind = "WIDGET", DataJson = ...)`). Top-level fields: `layout`
(required), `background`, `deepLink` (tap on empty space) and `theme`.
Keys are matched case-insensitively, so ODC `JSONSerialize` output works.

```json
{
  "deepLink": "approvals",
  "layout": {
    "type": "column", "padding": 16, "gap": 4,
    "children": [
      { "type": "row", "gap": 8, "children": [
        { "type": "icon", "name": "check_circle", "color": "accent" },
        { "type": "text", "text": "Open approvals", "size": 16, "bold": true, "weight": 1 },
        { "type": "icon", "name": "refresh", "color": "muted", "deepLink": "refresh" }
      ]},
      { "type": "spacer" },
      { "type": "text", "text": "12", "size": 40, "bold": true, "color": "accent" },
      { "type": "text", "text": "Updated 15:42", "color": "muted" },
      { "type": "row", "gap": 8, "children": [
        { "type": "button", "text": "Approve all", "icon": "check", "deepLink": "approve", "weight": 1 },
        { "type": "button", "text": "Open", "background": "tile", "color": "onTile", "deepLink": "approvals", "weight": 1 }
      ]}
    ]
  }
}
```

| Element | Purpose | Main fields |
|---|---|---|
| `column` | Stack children vertically | `children`, `gap`, `padding`, `align`, `valign`, `background` (flat) |
| `row` | Place children side by side | same as column |
| `box` | Rounded tile around its children | `children`, `background` (default `tile`), `padding` (default 12) |
| `text` | Text | `text`, `size` (sp), `color`, `bold`, `strike`, `maxLines`, `align` |
| `icon` | Built-in icon | `name`, `color`, `size` |
| `image` | Picture from URL or data URI | `src`, `width`, `height`, `radius` |
| `progress` | Ring 0-100 | `value`, `color`, `icon`, `text`, `size` |
| `spacer` | Space; without `size` it fills the free space | `size` |
| `button` | Pill with label and icon | `text`, `icon`, `background` (default `accent`), `color` |
| `clock` | Live time/date that ticks by itself, even with the app closed | `format` (`HH:mm:ss`, `EEE d MMM`...), `format12`, `format24`, `timeZone`, `size`, `color`, `bold` |
| `timer` | Live stopwatch / countdown | `since` or `until` (epoch ms or ISO date-time), `format`, `size`, `color`, `bold` |

Every element also takes `weight` (fill the remaining space of its row/column),
`padding`, `visible` (true/false or a condition, see below), `deepLink` (tap
opens the app and fires `widgetTap`) and `action` (tap does something without
opening the app, see below).

**Icons** (`icon.name`, `button.icon`, `progress.icon`): search, refresh, add,
check, check_circle, circle, chevron_left, chevron_right, arrow_up, arrow_down,
favorite, star, home, play, camera, upload, menu, lightbulb, tv, sun, cloud,
rain, calendar, location, reply, chat, bell, bolt, trending_up, info.

**Images** (`image.src`, `background.image`): an `https://` URL or a data URI
(`data:image/jpeg;base64,...`, e.g. from ODC Binary Data with
`BinaryDataToBase64`). They are downloaded and scaled once when you call
`setWidgetData`; the widget itself never uses the network.

**Colors** are theme names (`background`, `onBackground`, `muted`, `accent`,
`onAccent`, `tile`, `onTile`, `tileActive`, `onTileActive`, `positive`,
`negative`), which follow your `setTheme` brand colors and dark mode, or fixed
hex values like `#0B57D0`.

**Background** (optional, top level next to `layout`): a full-bleed gradient,
image and/or decoration, drawn at the widget's exact size with rounded corners.

```json
{
  "background": { "gradient": ["#8BE04E", "#1BB8A9"], "angle": 135,
                  "decoration": "rings", "decorationColor": "#40FFFFFF" },
  "layout": { "type": "column", "padding": 16, "children": [
    { "type": "spacer" },
    { "type": "text", "text": "Get started", "size": 16, "bold": true, "color": "#FFFFFF" },
    { "type": "text", "text": "Track your daily activities.", "color": "#E6FFFFFF" }
  ]}
}
```

| Field | Meaning |
|---|---|
| `gradient` | 1-6 hex colors (one color = solid fill) |
| `angle` | CSS-style degrees: 0 bottom→top, 90 left→right, 135 (default) top-left→bottom-right |
| `image` | URL or data URI, center-cropped under the gradient (use colors with alpha, e.g. `#80000000`, as a scrim) |
| `decoration` | `rings` (concentric outlines, top right), `circles`, `waves`, or `none` |
| `decorationColor` | Hex with alpha, default `#40FFFFFF` |

**Limits:** 80 elements, 8 levels deep, 90 tappable elements. Exact sizes for
icons, images, rings and spacers need Android 12+; older phones use defaults
(icon 24dp, image 120dp tall in a column / 56dp in a row, ring 48dp, spacer 8dp).
Android widgets can't show text inputs or animations.

## Live widgets: placeholders, refresh, clocks and buttons

Android widgets can't run HTML or JavaScript, and app code can't run every
second while the app is closed. The plugin covers the same needs natively:

- **Clocks and timers tick on their own**: `clock` and `timer` elements are
  drawn by the launcher and update every second (with `ss` in the format) with
  no battery cost.
- **`{{placeholders}}`** in any string of the JSON are filled from live
  **values**: `{{temp}}`, dot paths `{{current.temperature_2m}}`,
  `{{items.0.name}}`, and filters `{{temp|round}}` (`round`, `floor`, `ceil`,
  `fixed1`, `fixed2`, `upper`, `lower`, `time`, `date`). A string that is only
  one placeholder keeps its type, so `"value": "{{progress}}"` stays a number.
- **Values**: the JSON's `values` are starting values (a key that already
  has a value keeps it, so state survives the app calling `setWidgetData`
  again). After that, `setWidgetValues()` from the app, the background
  `refresh`, `http` responses and state buttons overwrite them. Read them back
  with `getWidgetValues()`; `clear()` resets everything.
- **`refresh`** fetches JSON from your API (for example an ODC REST API
  endpoint) in the background and merges it into the values, while the app is
  closed. Android runs it at most every 15 minutes and may delay it to save
  battery; for anything faster, update from the app with `setWidgetValues`.
  After each refresh `{{_updatedTime}}` (HH:mm) is set; `{{_error}}` holds the
  last failure.
- **`visible`** takes a condition after placeholders are filled:
  `"{{lights}}"`, `"{{page}} == 2"`, `"{{temp}} > 25"`, `"{{status}} != done"`.

### Buttons that work without opening the app

Give any element an `action`:

| `type` | Fields | What a tap does |
|---|---|---|
| `open` | `deepLink` | Opens the app (`widgetTap`), same as `deepLink` |
| `set` | `key`, `value` | Sets a value and redraws |
| `toggle` | `key` | Flips a true/false value |
| `add` | `key`, `by` (default 1), `min`, `max` | Adds to a number (use a negative `by` to subtract) |
| `cycle` | `key`, `values` | Moves to the next value in the list (pages, tabs, modes) |
| `refresh` | | Runs `refresh` now |
| `http` | `url`, `method` (POST), `headers`, `body`, `key`, `merge` (true) | Calls your API in the background; the JSON response is merged into the values |

`set`, `toggle`, `add`, `cycle` and `http` also fire `widgetAction
{ type, id, key, value, ok, error }` in the app: straight away if it is
running, otherwise when it next opens. Add an `id` to an action to tell them apart.

### Example: clock and live weather

Uses the free Open-Meteo API (no key). The clock ticks every second; the
temperature refreshes every 30 minutes, or when the refresh icon is tapped.

```json
{
  "values": { "city": "London" },
  "refresh": { "url": "https://api.open-meteo.com/v1/forecast?latitude=51.51&longitude=-0.13&current=temperature_2m,wind_speed_10m", "minutes": 30 },
  "background": { "gradient": ["#4FACFE", "#0061D5"], "decoration": "circles" },
  "layout": { "type": "column", "padding": 16, "children": [
    { "type": "row", "children": [
      { "type": "clock", "format": "HH:mm:ss", "size": 30, "bold": true, "color": "#FFFFFF", "weight": 1 },
      { "type": "icon", "name": "refresh", "color": "#FFFFFF", "action": { "type": "refresh" } }
    ]},
    { "type": "clock", "format": "EEEE, d MMMM", "color": "#E6FFFFFF" },
    { "type": "spacer" },
    { "type": "row", "gap": 10, "children": [
      { "type": "icon", "name": "sun", "size": 32, "color": "#FFFFFF" },
      { "type": "text", "text": "{{current.temperature_2m|round}}°", "size": 34, "bold": true, "color": "#FFFFFF" },
      { "type": "column", "weight": 1, "children": [
        { "type": "text", "text": "{{city}}", "bold": true, "color": "#FFFFFF" },
        { "type": "text", "text": "Wind {{current.wind_speed_10m|round}} km/h · {{_updatedTime}}", "size": 12, "color": "#CCFFFFFF", "maxLines": 1 }
      ]}
    ]}
  ]}
}
```

### Example: interactive buttons

```json
{
  "values": { "lights": false, "water": 0 },
  "layout": { "type": "column", "padding": 16, "gap": 12, "children": [
    { "type": "row", "gap": 8, "children": [
      { "type": "icon", "name": "lightbulb", "color": "accent" },
      { "type": "text", "text": "Living room", "bold": true, "weight": 1 },
      { "type": "button", "text": "On", "visible": "{{lights}}", "action": { "type": "toggle", "key": "lights", "id": "lights" } },
      { "type": "button", "text": "Off", "background": "tile", "color": "onTile", "visible": "{{lights}} == false", "action": { "type": "toggle", "key": "lights", "id": "lights" } }
    ]},
    { "type": "row", "gap": 8, "children": [
      { "type": "text", "text": "Water {{water}}%", "size": 16, "weight": 1 },
      { "type": "button", "text": "−", "background": "tile", "color": "onTile", "action": { "type": "add", "key": "water", "by": -25, "min": 0 } },
      { "type": "button", "text": "+", "action": { "type": "add", "key": "water", "by": 25, "max": 100, "id": "water" } }
    ]},
    { "type": "progress", "value": "{{water}}", "icon": "favorite", "size": 56 }
  ]}
}
```

## Brand colors

Theme color names in the layout (`accent`, `muted`, `tile`...) resolve to these. Call `setTheme` once (for example at app start); the widget's own `theme` overrides it.

```json
{
  "background": "#FFFFFF", "onBackground": "#1B1B1F", "muted": "#5F6368",
  "accent": "#0B57D0", "onAccent": "#FFFFFF",
  "tile": "#F1F3F4", "onTile": "#1B1B1F", "tileActive": "#D3E3FD", "onTileActive": "#041E49",
  "positive": "#188038", "negative": "#D93025",
  "dark": { "background": "#1F1F1F", "onBackground": "#E3E3E3", "accent": "#A8C7FA" }
}
```

Every field is optional. Without `dark`, the same colors are used in dark mode.
On Android 12+ widgets switch light/dark instantly; on older versions they
switch at the next update.

## API

| Method | Returns |
|---|---|
| `setWidgetData({ kind?, data })` | `void`. `data` is a JSON **string**; resolves after images are downloaded |
| `setTheme({ data })` | `void`. `""` resets to the default colors |
| `setWidgetValues({ data })` | `void`. Merges a JSON object string into the live values and redraws |
| `getWidgetValues()` | `{ data }`: all live values as a JSON string |
| `refreshNow()` | `{ data }`: runs `refresh` now, returns the new values |
| `refresh()` | `void` |
| `clear()` | `void` |
| `isPinSupported()` | `{ supported }` |
| `requestPin()` | `void` (Android 8+, launcher must support it) |
| `getInstalledCount()` | `{ count }` |
| `addListener('widgetTap', cb)` | `cb({ kind, deepLink })` |
| `addListener('widgetAction', cb)` | `cb({ type, id, key, value, ok, error })` |
| `removeAllListeners()` | `void` |

## Error codes

| Code | Meaning |
|---|---|
| `OS-PLUG-HWGT-0001` | `kind` is not `WIDGET` |
| `OS-PLUG-HWGT-0002` | `data` missing, not JSON, bad layout element, or bad color (the message names the path) |
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

## Use in ODC

See [odc-library/ODC_LIBRARY.md](odc-library/ODC_LIBRARY.md).

## Limitations

- One data set: every Widget on the home screen shows the same JSON.
- Clocks and timers tick every second; other data updates when the app sends it, on a button press, or on the background refresh (15 minutes at the fastest).
- No HTML/JavaScript, text inputs, animations or scrolling lists. Images are scaled to 512 px.
- Android only.

## License

MIT
