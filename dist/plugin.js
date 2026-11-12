var capacitorOdcHomeWidget = (function (exports, core) {
    'use strict';

    /** Error codes returned in `error.code` when a call is rejected. */
    const OdcHomeWidgetErrors = {
        INVALID_KIND: 'OS-PLUG-HWGT-0001',
        INVALID_DATA: 'OS-PLUG-HWGT-0002',
        STORAGE: 'OS-PLUG-HWGT-0003',
        PIN_NOT_SUPPORTED: 'OS-PLUG-HWGT-0004',
        PIN_REFUSED: 'OS-PLUG-HWGT-0005',
        UNEXPECTED: 'OS-PLUG-HWGT-0006',
    };

    const OdcHomeWidget = core.registerPlugin('OdcHomeWidget', {
        web: () => Promise.resolve().then(function () { return web; }).then((m) => new m.OdcHomeWidgetWeb()),
    });

    const MESSAGE = 'Not available on web';
    /** Home screen widgets only exist on Android; every call rejects on web. */
    class OdcHomeWidgetWeb extends core.WebPlugin {
        async setWidgetData(_options) {
            throw this.unavailable(MESSAGE);
        }
        async setWidgetValues(_options) {
            throw this.unavailable(MESSAGE);
        }
        async getWidgetValues() {
            throw this.unavailable(MESSAGE);
        }
        async refreshNow() {
            throw this.unavailable(MESSAGE);
        }
        async setTheme(_options) {
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

    var web = /*#__PURE__*/Object.freeze({
        __proto__: null,
        OdcHomeWidgetWeb: OdcHomeWidgetWeb
    });

    exports.OdcHomeWidget = OdcHomeWidget;
    exports.OdcHomeWidgetErrors = OdcHomeWidgetErrors;

    return exports;

})({}, capacitorExports);
//# sourceMappingURL=plugin.js.map
