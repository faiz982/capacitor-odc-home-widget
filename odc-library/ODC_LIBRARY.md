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
      "source": { "npm": "https://github.com/faiz982/capacitor-odc-home-widget/archive/refs/tags/v0.3.0.tar.gz" }
    }
  },
  "metadata": { "mabs-min": "12.0.0", "name": "Home Widget Plugin", "version": "0.3.0" }
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

## 2. Static entity and structures

**Static entity `WidgetKind`** (Label: Text) with records `COUNT`, `LIST`, `ACTIONS`.
Pass `Entities.WidgetKind.COUNT` etc. to the actions' `Kind` input (Text).

**Structures** (build data with these, then `JSONSerialize`). Attribute names can
stay PascalCase: the plugin matches JSON keys case-insensitively.

| Structure | Attributes |
|---|---|
| `CountWidgetData` | `Title` (Text), `Value` (Text), `Subtitle` (Text), `DeepLink` (Text) |
| `WidgetLinkItem` | `Text` (Text), `DeepLink` (Text) |
| `ListWidgetData` | `Title` (Text), `Items` (WidgetLinkItem List, max 3 used), `DeepLink` (Text) |
| `WidgetActionItem` | `Label` (Text), `DeepLink` (Text) |
| `ActionsWidgetData` | `Title` (Text), `Actions` (WidgetActionItem List, max 3 used) |

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
2. Home screen: three buttons. Each one assigns a structure, runs
   `JSONSerialize` on it, then calls `SetWidgetData(Kind, JSONSerialize.JSON)`.
   Example for COUNT: `Title = "Open approvals"`, `Value = "12"`,
   `Subtitle = "Updated " + FormatDateTime(CurrDateTime(), "HH:mm")`, `DeepLink = "approvals"`.
3. A **Pin widget** button: `IsPinSupported` → if true, `RequestPinWidget`.
4. Drop `WidgetTapListener` on the screen. In `OnWidgetTap`: show a feedback
   message with `DeepLink`, then a Switch on `DeepLink` (for example
   `"approvals"` → Approvals screen).
5. Portal → app → **Mobile distribution** → MABS 12.1 → Android build.

## 6. Upgrading the library to v0.2.0 (11 templates, brand colors, toggles)

1. **Extensibility:** change the tag in the JSON above to `#v0.2.0` (already shown) and publish.
2. **Nothing else is required for the new templates.** `SetWidgetData` already takes any
   `Kind` text and JSON, so `"INFO"`, `"STATS"`, `"HERO"`, `"ARTICLES"`, `"GRID"`,
   `"MESSAGES"`, `"TOGGLES"` and `"CHECKLIST"` work as they are. Build the JSON with your own
   structures and `JSONSerialize`; see the plugin README for every field.
   Add the new names to the `WidgetKind` static entity if you use it.
3. **Optional: `SetWidgetTheme`** client action. Input `ThemeJson` (Text); outputs `Success`, `Error`.
   JavaScript node:

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

   Example `ThemeJson`: `{"accent":"#0B57D0","tileActive":"#D3E3FD","onTileActive":"#041E49"}`.

4. **Optional: toggle events** for `TOGGLES` and `CHECKLIST`. In the `WidgetTapListener` block:
   - Add event `OnWidgetToggle` with inputs `Kind` (Text), `Id` (Text), `IsOn` (Boolean).
   - Add client action `TriggerOnWidgetToggle(Kind, Id, IsOn)` that triggers it.
   - In the `OnReady` JavaScript, after the `widgetTap` listener, add:

```js
if (plugin) {
  if (window.__odcWidgetToggleHandle) { window.__odcWidgetToggleHandle.remove(); }
  plugin.addListener('widgetToggle', function (e) {
    $actions.TriggerOnWidgetToggle(e.kind || '', e.id || '', !!e.on);
  }).then(function (handle) { window.__odcWidgetToggleHandle = handle; });
}
```

   - In `OnDestroy`, also remove `window.__odcWidgetToggleHandle`.
   Taps made while the app was closed arrive when the app next opens.
