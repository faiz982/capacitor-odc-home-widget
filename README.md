# capacitor-odc-home-widget

Material 3 style Android home screen widgets for OutSystems Developer Cloud
(ODC) mobile apps (Capacitor stack, MABS 12+). The app sends JSON; the widget
draws one of 11 native templates in your brand colors. Widgets read the last
saved data, so they work while the app is closed and survive reboots.

**Android only.** iOS methods reject with "Home widgets are Android-only in v1"
(`isPinSupported` resolves `{ supported: false }`).

## Templates

| Kind | Default size | Looks like | Scrolls | Taps |
|---|---|---|---|---|
| `COUNT` | 2×2 | One big number | | Opens deep link |
| `LIST` | 4×2 | Up to 3 text rows | | Per row |
| `ACTIONS` | 4×2 | Up to 3 icon shortcut tiles | | Per tile |
| `INFO` | 2×3 | Weather details, stocks: icon, label, value, trend | | Per row |
| `STATS` | 4×2 | Workout-style rings with value and label | | Per tile |
| `HERO` | 4×2 | Story card with a background image | | Whole card |
| `ARTICLES` | 4×3 | News list with thumbnails | ✓ | Per item |
| `GRID` | 4×3 | Episodes / gallery grid with images | ✓ | Per item |
| `MESSAGES` | 4×3 | Reply-style message cards | ✓ | Per message |
| `TOGGLES` | 2×3 | Switch / smart-home tiles (up to 6) | | **Flips on/off in place** |
| `CHECKLIST` | 2×3 | Shopping-list ticks | ✓ | **Ticks in place** |

Every template accepts the same header fields:

```json
{
  "title": "Fortnightly",
  "icon": "star",
  "buttons": [ { "icon": "search", "deepLink": "search" }, { "icon": "refresh", "deepLink": "refresh" } ],
  "deepLink": "home",
  "theme": { "accent": "#0B57D0" }
}
```

Keys are matched case-insensitively, so ODC `JSONSerialize` output (`Title`,
`DeepLink`, ...) works without renaming.

**Icons** (by name): search, refresh, add, check, check_circle, circle,
chevron_left, chevron_right, arrow_up, arrow_down, favorite, star, home, play,
camera, upload, menu, lightbulb, tv, sun, cloud, rain, calendar, location,
reply, chat, bell, bolt, trending_up, info.

**Images** (`image` fields): an `https://` URL or a data URI
(`data:image/jpeg;base64,...`, e.g. from ODC Binary Data with
`BinaryDataToBase64`). They are downloaded and scaled once when you call
`setWidgetData`; the widget itself never uses the network.

## Data per template

```jsonc
// COUNT
{ "title": "Open approvals", "icon": "check_circle", "value": 12, "subtitle": "Updated 09:30", "deepLink": "approvals" }

// LIST (first 3)
{ "title": "My tasks", "deepLink": "tasks", "items": [ { "text": "PO #4512", "deepLink": "tasks/1" } ] }

// ACTIONS (first 3)
{ "title": "Quick actions", "actions": [ { "label": "Scan", "icon": "camera", "deepLink": "scan" } ] }

// INFO (first 5 rows). trend "up"/"down" colors the values and adds an arrow.
{ "title": "Weather", "icon": "sun", "headline": "London, UK",
  "rows": [ { "icon": "rain", "label": "Rain", "value": "56%" },
            { "label": "ELM.35", "sublabel": "7,877.05", "value": "+0.37%", "subvalue": "+29.06", "trend": "up" } ] }

// STATS (first 4). progress 0-100 fills the ring.
{ "title": "Workout", "icon": "favorite",
  "tiles": [ { "icon": "bolt", "value": "866", "label": "Calories", "progress": 70 } ] }

// HERO
{ "title": "Fortnightly", "icon": "star", "image": "https://example.com/farm.jpg",
  "overline": "World · 20 hrs", "headline": "Converting Farmland Into Housing", "subtitle": "...", "deepLink": "article/42" }

// ARTICLES (scrolls, first 25)
{ "title": "Fortnightly", "items": [ { "image": "https://...", "overline": "US", "meta": "14 hrs",
  "title": "Poverty To Empowerment In Chicago", "subtitle": "Green energy may...", "deepLink": "article/1" } ] }

// GRID (scrolls, 3 columns)
{ "title": "Episodes", "icon": "play", "items": [ { "image": "https://...", "title": "The Concern", "subtitle": "S1 E2", "deepLink": "ep/2" } ] }

// MESSAGES (scrolls). highlight uses the active tile colors.
{ "title": "Reply", "icon": "reply", "messages": [ { "sender": "Hari Ansari", "time": "10:09",
  "text": "And then stop for lunch...", "highlight": true, "deepLink": "chat/7" } ] }

// TOGGLES (first 6, 2 columns). id is required.
{ "title": "House", "icon": "home", "tiles": [ { "id": "living", "icon": "lightbulb", "label": "Living Room",
  "on": true, "onText": "On", "offText": "Off" } ] }

// CHECKLIST (scrolls). id is required.
{ "title": "Groceries", "icon": "check", "items": [ { "id": "milk", "text": "Milk", "checked": false } ] }
```

## WIDGET: design any layout from JSON

One kind, `WIDGET`, draws whatever layout the JSON describes. Send it with
`SetWidgetData(Kind = "WIDGET", DataJson = ...)`; change the JSON to change the design.

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

Every element also takes `weight` (fill the remaining space of its row/column),
`padding`, `visible` and `deepLink` (tap opens the app and fires `widgetTap`).

**Colors** are theme names (`background`, `onBackground`, `muted`, `accent`,
`onAccent`, `tile`, `onTile`, `tileActive`, `onTileActive`, `positive`,
`negative`), which follow your `setTheme` brand colors and dark mode, or fixed
hex values like `#0B57D0`.

**Limits:** 80 elements, 8 levels deep, 90 tappable elements. Exact sizes for
icons, images, rings and spacers need Android 12+; older phones use defaults
(icon 24dp, image 120dp tall in a column / 56dp in a row, ring 48dp, spacer 8dp).
Android widgets can't show text inputs or animations.

## Brand colors

Call `setTheme` once (for example at app start); a widget's own `theme` overrides it.

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

## Toggle events

`TOGGLES` tiles and `CHECKLIST` rows change state on the home screen without
opening the app. The plugin saves the new state and fires
`widgetToggle { kind, id, on }`: immediately if the app is running, otherwise
when it next starts or resumes. Update your own data in the handler.

## API

| Method | Returns |
|---|---|
| `setWidgetData({ kind, data })` | `void`. `data` is a JSON **string**; resolves after images are downloaded |
| `setTheme({ data })` | `void`. `""` resets to the default colors |
| `refresh({ kind })` | `void` |
| `clear({ kind })` | `void` |
| `isPinSupported()` | `{ supported }` |
| `requestPin({ kind })` | `void` (Android 8+, launcher must support it) |
| `getInstalledCount({ kind })` | `{ count }` |
| `addListener('widgetTap', cb)` | `cb({ kind, deepLink })` |
| `addListener('widgetToggle', cb)` | `cb({ kind, id, on })` |
| `removeAllListeners()` | `void` |

## Error codes

| Code | Meaning |
|---|---|
| `OS-PLUG-HWGT-0001` | `kind` is not one of the 12 kinds |
| `OS-PLUG-HWGT-0002` | `data` missing, not JSON, wrong shape, bad color, or a toggle/checklist item without `id` |
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

- One data set per template: two ARTICLES widgets show the same list.
- Widgets only change when the app calls `setWidgetData` or the user toggles a tile.
- Lists and grids show at most 25 items; images are scaled to 512 px.
- Android only.

## License

MIT
