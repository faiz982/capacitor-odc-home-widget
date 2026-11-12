# HomeWidget: ODC Mobile Library

Everything to paste into ODC Studio to wrap the plugin. Build it as a
**Library** named `HomeWidget`; adding extensibility turns it into a Mobile
Library (only mobile apps can reference it).

## 1. Extensibility tab

Set the mobile framework to **Capacitor**, then paste:

```json
{
  "buildConfigurations": {
    "capacitor": {
      "source": { "npm": "https://github.com/faiz982/capacitor-odc-home-widget/archive/refs/tags/v0.6.0.tar.gz" }
    }
  },
  "metadata": { "mabs-min": "12.0.0", "name": "Home Widget Plugin", "version": "0.6.0" }
}
```

- Always point at a **tag**, never a branch.
- `dist/` is committed in the repo, so MABS needs no TypeScript build.
- ODC Studio validates this JSON as you type. If it flags a key, follow its hint:
  the schema above matches published MABS 12 examples, but I could not confirm it
  in official docs.
- No build action is needed: the plugin's manifest and resources merge
  automatically, and widget taps reuse the running activity
  (`CLEAR_TOP | SINGLE_TOP`).

## 2. Kind and data

There is one widget kind, `WIDGET`. Pass `"WIDGET"` (or an empty text) to every
action's `Kind` input. `DataJson` describes the whole widget: a `layout` tree,
an optional `background`, `deepLink` and `theme`. See the plugin README section
"The JSON" for every field.

Build `DataJson` as a Text expression, or with structures and `JSONSerialize`.
Attribute names can stay PascalCase: the plugin matches JSON keys case-insensitively.

## 3. Client actions (all Public)

Each action has one JavaScript node. Map the node's input/output parameters
to the action's inputs/outputs with the same names.

Every node starts with the same plugin lookup:

```js
var cap = window.Capacitor;
var plugin = cap && cap.isPluginAvailable && cap.isPluginAvailable('OdcHomeWidget')
  ? ((cap.Plugins && cap.Plugins.OdcHomeWidget) || cap.registerPlugin('OdcHomeWidget'))
  : null;
```

### CheckHomeWidgetPlugin
Outputs: `IsAvailable` (Boolean)

```js
var cap = window.Capacitor;
$parameters.IsAvailable = !!(cap && cap.isPluginAvailable && cap.isPluginAvailable('OdcHomeWidget'));
```

### SetWidgetData
Inputs: `Kind` (Text), `DataJson` (Text). Outputs: `Success` (Boolean), `Error` (Text)

```js
var cap = window.Capacitor;
var plugin = cap && cap.isPluginAvailable && cap.isPluginAvailable('OdcHomeWidget')
  ? ((cap.Plugins && cap.Plugins.OdcHomeWidget) || cap.registerPlugin('OdcHomeWidget'))
  : null;

if (!plugin) {
  $parameters.Success = false;
  $parameters.Error = 'Home Widget plugin not available in this build';
  $resolve();
} else {
  plugin.setWidgetData({ kind: $parameters.Kind, data: $parameters.DataJson })
    .then(function () { $parameters.Success = true; $resolve(); })
    .catch(function (e) {
      $parameters.Success = false;
      $parameters.Error = (e && e.code ? e.code + ': ' : '') + (e && e.message ? e.message : String(e));
      $resolve();
    });
}
```

### RefreshWidget / ClearWidget / RequestPinWidget
Inputs: `Kind` (Text). Outputs: `Success` (Boolean), `Error` (Text).
Same as `SetWidgetData`; replace the call with:

| Action | Call |
|---|---|
| `RefreshWidget` | `plugin.refresh({ kind: $parameters.Kind })` |
| `ClearWidget` | `plugin.clear({ kind: $parameters.Kind })` |
| `RequestPinWidget` | `plugin.requestPin({ kind: $parameters.Kind })` |

### IsPinSupported
Outputs: `Supported` (Boolean)

```js
var cap = window.Capacitor;
var plugin = cap && cap.isPluginAvailable && cap.isPluginAvailable('OdcHomeWidget')
  ? ((cap.Plugins && cap.Plugins.OdcHomeWidget) || cap.registerPlugin('OdcHomeWidget'))
  : null;

if (!plugin) { $parameters.Supported = false; $resolve(); }
else {
  plugin.isPinSupported()
    .then(function (r) { $parameters.Supported = !!r.supported; $resolve(); })
    .catch(function () { $parameters.Supported = false; $resolve(); });
}
```

### GetInstalledCount
Inputs: `Kind` (Text). Outputs: `Count` (Integer)

```js
var cap = window.Capacitor;
var plugin = cap && cap.isPluginAvailable && cap.isPluginAvailable('OdcHomeWidget')
  ? ((cap.Plugins && cap.Plugins.OdcHomeWidget) || cap.registerPlugin('OdcHomeWidget'))
  : null;

if (!plugin) { $parameters.Count = 0; $resolve(); }
else {
  plugin.getInstalledCount({ kind: $parameters.Kind })
    .then(function (r) { $parameters.Count = r.count; $resolve(); })
    .catch(function () { $parameters.Count = 0; $resolve(); });
}
```

## 4. Block `WidgetTapListener`

- Event: `OnWidgetTap` with inputs `Kind` (Text) and `DeepLink` (Text), mandatory.
- Client action `TriggerOnWidgetTap(Kind, DeepLink)`: a single **Trigger Event** node for `OnWidgetTap`.
- **OnReady**: JavaScript node:

```js
var cap = window.Capacitor;
var plugin = cap && cap.isPluginAvailable && cap.isPluginAvailable('OdcHomeWidget')
  ? ((cap.Plugins && cap.Plugins.OdcHomeWidget) || cap.registerPlugin('OdcHomeWidget'))
  : null;

if (plugin) {
  // Removes a handle left over from a previous instance of this block.
  if (window.__odcWidgetTapHandle) { window.__odcWidgetTapHandle.remove(); }
  plugin.addListener('widgetTap', function (e) {
    $actions.TriggerOnWidgetTap(e.kind || '', e.deepLink || '');
  }).then(function (handle) { window.__odcWidgetTapHandle = handle; });
}
```

- **OnDestroy**: JavaScript node:

```js
if (window.__odcWidgetTapHandle) {
  window.__odcWidgetTapHandle.remove();
  window.__odcWidgetTapHandle = null;
}
```

A tap that cold-starts the app is held by the plugin until this listener is
added, so put the block on the app's default screen.

## 5. Demo app `HomeWidgetDemo`

1. New mobile app, framework **Capacitor**, reference all public elements of `HomeWidget`.
2. Home screen: a button that calls `SetWidgetData("WIDGET", DataJson)` with a
   layout from the README (for example the "Get started" card).
3. A **Pin widget** button: `IsPinSupported` → if true, `RequestPinWidget`.
4. Drop `WidgetTapListener` on the screen. In `OnWidgetTap`: switch on `DeepLink`
   to open the matching screen.
5. Portal → app → **Mobile distribution** → MABS 12.1 → Android build.

## 6. Optional: `SetWidgetTheme`

Client action with input `ThemeJson` (Text); outputs `Success`, `Error`.
Theme colors are what layout color names (`accent`, `muted`, `tile`...) resolve to.

```js
var cap = window.Capacitor;
var plugin = cap && cap.isPluginAvailable && cap.isPluginAvailable('OdcHomeWidget')
  ? ((cap.Plugins && cap.Plugins.OdcHomeWidget) || cap.registerPlugin('OdcHomeWidget'))
  : null;

if (!plugin) { $parameters.Success = false; $parameters.Error = 'Home Widget plugin not available in this build'; $resolve(); }
else {
  plugin.setTheme({ data: $parameters.ThemeJson })
    .then(function () { $parameters.Success = true; $resolve(); })
    .catch(function (e) { $parameters.Success = false; $parameters.Error = (e && e.code ? e.code + ': ' : '') + (e && e.message ? e.message : String(e)); $resolve(); });
}
```

Example `ThemeJson`: `{"accent":"#0B57D0","tile":"#F1F3F4","dark":{"accent":"#A8C7FA"}}`.

## 7. Upgrading from v0.4 or older

- v0.5.0 removed the 11 templates (COUNT, LIST, ACTIONS, INFO, STATS, HERO,
  ARTICLES, GRID, MESSAGES, TOGGLES, CHECKLIST). Only **Widget** stays in the
  launcher's widget picker; template widgets already on a home screen disappear
  after the update.
- Calls with another `Kind` now fail with `OS-PLUG-HWGT-0001`.
- The `widgetToggle` event no longer fires. If you added the `OnWidgetToggle`
  JavaScript to `WidgetTapListener`, it can stay (it does nothing) or be removed.
- The `WidgetKind` static entity and the COUNT/LIST/ACTIONS structures can be deleted.

## 8. Live widgets (v0.6.0)

Clocks, timers, `{{placeholders}}`, background refresh and buttons need no new
ODC code: they are all in `DataJson` (see the README section "Live widgets").
Add these to use values and button events from the app.

Every JavaScript node below starts with the usual plugin lookup:

```js
var cap = window.Capacitor;
var plugin = cap && cap.isPluginAvailable && cap.isPluginAvailable('OdcHomeWidget')
  ? ((cap.Plugins && cap.Plugins.OdcHomeWidget) || cap.registerPlugin('OdcHomeWidget'))
  : null;
```

### SetWidgetValues
Inputs: `ValuesJson` (Text, e.g. `{"temp":21,"city":"Paris"}`). Outputs: `Success`, `Error`.
Updates only the live values and redraws: cheaper than resending the layout.

```js
if (!plugin) { $parameters.Success = false; $parameters.Error = 'Home Widget plugin not available in this build'; $resolve(); }
else {
  plugin.setWidgetValues({ data: $parameters.ValuesJson })
    .then(function () { $parameters.Success = true; $resolve(); })
    .catch(function (e) { $parameters.Success = false; $parameters.Error = (e && e.code ? e.code + ': ' : '') + (e && e.message ? e.message : String(e)); $resolve(); });
}
```

### GetWidgetValues / RefreshWidgetNow
Outputs: `ValuesJson` (Text), `Success`, `Error`. Same as above with:

| Action | Call |
|---|---|
| `GetWidgetValues` | `plugin.getWidgetValues()` |
| `RefreshWidgetNow` | `plugin.refreshNow()` |

and in `.then`: `function (r) { $parameters.ValuesJson = r.data; $parameters.Success = true; $resolve(); }`.
Use `JSONDeserialize` on `ValuesJson` with your own structure to read it.

### Button event in `WidgetTapListener`
- Add event `OnWidgetAction` with inputs `Type`, `Id`, `Key`, `ValueJson` (Text), `Ok` (Boolean), `Error` (Text).
- Add client action `TriggerOnWidgetAction(...)` with the same inputs that triggers it.
- In `OnReady`, after the `widgetTap` listener:

```js
if (plugin) {
  if (window.__odcWidgetActionHandle) { window.__odcWidgetActionHandle.remove(); }
  plugin.addListener('widgetAction', function (e) {
    $actions.TriggerOnWidgetAction(e.type || '', e.id || '', e.key || '',
      JSON.stringify(e.value === undefined ? null : e.value), !!e.ok, e.error || '');
  }).then(function (handle) { window.__odcWidgetActionHandle = handle; });
}
```

- In `OnDestroy`, also remove `window.__odcWidgetActionHandle`.

Button presses made while the app was closed arrive when it next opens, so the
app can sync them to the server (for example, `Id = "lights"` → update the entity).
