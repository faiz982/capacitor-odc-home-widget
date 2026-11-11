/* global capacitorOdcHomeWidget */
const { OdcHomeWidget } = capacitorOdcHomeWidget;

const $ = (sel) => document.querySelector(sel);
const log = (msg) => {
  const line = `${new Date().toLocaleTimeString()}  ${msg}\n`;
  $('#log').textContent = line + $('#log').textContent;
};
const errText = (e) => (e && e.code ? `${e.code}: ${e.message}` : String(e && e.message ? e.message : e));

function buildData(kind) {
  const now = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  if (kind === 'COUNT') {
    return {
      title: 'Open approvals',
      value: Number($('#countValue').value || 0),
      subtitle: `Updated ${now}`,
      deepLink: 'approvals',
    };
  }
  if (kind === 'LIST') {
    const items = $('#listItems')
      .value.split(',')
      .map((s) => s.trim())
      .filter(Boolean)
      .slice(0, 3)
      .map((text, i) => ({ text, deepLink: `tasks/${i + 1}` }));
    return { title: 'My tasks', items, deepLink: 'tasks' };
  }
  return {
    title: 'Quick actions',
    actions: [
      { label: 'New request', deepLink: 'requests/new' },
      { label: 'Approvals', deepLink: 'approvals' },
      { label: 'Scan', deepLink: 'scan' },
    ],
  };
}

async function setWidget(kind) {
  try {
    await OdcHomeWidget.setWidgetData({ kind, data: JSON.stringify(buildData(kind)) });
    log(`${kind}: data saved`);
  } catch (e) {
    log(`${kind}: ${errText(e)}`);
  }
}

async function pin(kind) {
  try {
    const { supported } = await OdcHomeWidget.isPinSupported();
    if (!supported) {
      log('Pinning is not supported by this launcher. Long-press the home screen > Widgets instead.');
      return;
    }
    await OdcHomeWidget.requestPin({ kind });
    log(`${kind}: pin requested`);
  } catch (e) {
    log(`${kind}: ${errText(e)}`);
  }
}

function showTap(event) {
  const box = $('#tap');
  box.style.display = 'block';
  box.textContent = `Opened from the ${event.kind} widget → deep link "${event.deepLink}"`;
  log(`widgetTap ${JSON.stringify(event)}`);
}

async function init() {
  const native = window.Capacitor && window.Capacitor.isNativePlatform();
  $('#status').textContent = native
    ? `Plugin available: ${window.Capacitor.isPluginAvailable('OdcHomeWidget')}`
    : 'Running in a browser: widgets only work in the Android app.';

  document.querySelectorAll('[data-set]').forEach((b) => b.addEventListener('click', () => setWidget(b.dataset.set)));
  document.querySelectorAll('[data-pin]').forEach((b) => b.addEventListener('click', () => pin(b.dataset.pin)));

  $('#counts').addEventListener('click', async () => {
    try {
      const parts = [];
      for (const kind of ['COUNT', 'LIST', 'ACTIONS']) {
        const { count } = await OdcHomeWidget.getInstalledCount({ kind });
        parts.push(`${kind}=${count}`);
      }
      log(`installed: ${parts.join(', ')}`);
    } catch (e) {
      log(errText(e));
    }
  });

  $('#clearAll').addEventListener('click', async () => {
    try {
      for (const kind of ['COUNT', 'LIST', 'ACTIONS']) await OdcHomeWidget.clear({ kind });
      log('all widgets cleared');
    } catch (e) {
      log(errText(e));
    }
  });

  // Registering the listener also delivers a tap that cold-started the app.
  await OdcHomeWidget.addListener('widgetTap', showTap);
}

init();
