import { registerPlugin } from '@capacitor/core';
const OdcHomeWidget = registerPlugin('OdcHomeWidget', {
    web: () => import('./web').then((m) => new m.OdcHomeWidgetWeb()),
});
export * from './definitions';
export { OdcHomeWidget };
//# sourceMappingURL=index.js.map