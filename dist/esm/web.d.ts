import { WebPlugin } from '@capacitor/core';
import type { KindOptions, OdcHomeWidgetPlugin, SetThemeOptions, SetWidgetDataOptions, ValuesOptions, ValuesResult } from './definitions';
/** Home screen widgets only exist on Android; every call rejects on web. */
export declare class OdcHomeWidgetWeb extends WebPlugin implements OdcHomeWidgetPlugin {
    setWidgetData(_options: SetWidgetDataOptions): Promise<void>;
    setWidgetValues(_options: ValuesOptions): Promise<void>;
    getWidgetValues(): Promise<ValuesResult>;
    refreshNow(): Promise<ValuesResult>;
    setTheme(_options: SetThemeOptions): Promise<void>;
    refresh(_options?: KindOptions): Promise<void>;
    clear(_options?: KindOptions): Promise<void>;
    isPinSupported(): Promise<{
        supported: boolean;
    }>;
    requestPin(_options?: KindOptions): Promise<void>;
    getInstalledCount(_options?: KindOptions): Promise<{
        count: number;
    }>;
}
