import { WebPlugin } from '@capacitor/core';
const MESSAGE = 'Not available on web';
/** Home screen widgets only exist on Android; every call rejects on web. */
export class OdcHomeWidgetWeb extends WebPlugin {
    async setWidgetData(_options) {
        throw this.unavailable(MESSAGE);
    }
    async refresh(_options) {
        throw this.unavailable(MESSAGE);
    }
    async clear(_options) {
        throw this.unavailable(MESSAGE);
    }
    async isPinSupported() {
        return { supported: false };
    }
    async requestPin(_options) {
        throw this.unavailable(MESSAGE);
    }
    async getInstalledCount(_options) {
        throw this.unavailable(MESSAGE);
    }
}
//# sourceMappingURL=web.js.map