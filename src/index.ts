import { registerPlugin } from '@capacitor/core';

import type { OdcHomeWidgetPlugin } from './definitions';

const OdcHomeWidget = registerPlugin<OdcHomeWidgetPlugin>('OdcHomeWidget', {
  web: () => import('./web').then((m) => new m.OdcHomeWidgetWeb()),
});

export * from './definitions';
export { OdcHomeWidget };
