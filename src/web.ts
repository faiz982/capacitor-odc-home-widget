import { WebPlugin } from '@capacitor/core';

import type { KindOptions, OdcHomeWidgetPlugin, SetThemeOptions, SetWidgetDataOptions } from './definitions';

const MESSAGE = 'Not available on web';

/** Home screen widgets only exist on Android; every call rejects on web. */
export class OdcHomeWidgetWeb extends WebPlugin implements OdcHomeWidgetPlugin {
  async setWidgetData(_options: SetWidgetDataOptions): Promise<void> {
    throw this.unavailable(MESSAGE);
  }

  async setTheme(_options: SetThemeOptions): Promise<void> {
    throw this.unavailable(MESSAGE);
  }

  async refresh(_options?: KindOptions): Promise<void> {
    throw this.unavailable(MESSAGE);
  }

  async clear(_options?: KindOptions): Promise<void> {
    throw this.unavailable(MESSAGE);
  }

  async isPinSupported(): Promise<{ supported: boolean }> {
    return { supported: false };
  }

  async requestPin(_options?: KindOptions): Promise<void> {
    throw this.unavailable(MESSAGE);
  }

  async getInstalledCount(_options?: KindOptions): Promise<{ count: number }> {
    throw this.unavailable(MESSAGE);
  }
}
