import { WebPlugin } from '@capacitor/core';
import type { KindOptions, OdcHomeWidgetPlugin, SetWidgetDataOptions } from './definitions';
/** Home screen widgets only exist on Android; every call rejects on web. */
export declare class OdcHomeWidgetWeb extends WebPlugin implements OdcHomeWidgetPlugin {
    setWidgetData(_options: SetWidgetDataOptions): Promise<void>;
    refresh(_options: KindOptions): Promise<void>;
    clear(_options: KindOptions): Promise<void>;
    isPinSupported(): Promise<{
        supported: boolean;
    }>;
    requestPin(_options: KindOptions): Promise<void>;
    getInstalledCount(_options: KindOptions): Promise<{
        count: number;
    }>;
}
