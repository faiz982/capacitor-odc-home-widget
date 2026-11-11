import type { PluginListenerHandle } from '@capacitor/core';
/** The three native widget templates. */
export type WidgetKind = 'COUNT' | 'LIST' | 'ACTIONS';
/** Data for a COUNT widget: one big value with a title and subtitle. */
export interface CountWidgetData {
    title?: string;
    value: string | number;
    subtitle?: string;
    /** Sent back in `widgetTap` when the widget is tapped. */
    deepLink?: string;
}
export interface ListWidgetItem {
    text: string;
    /** Falls back to the list's `deepLink` when empty. */
    deepLink?: string;
}
/** Data for a LIST widget. Only the first 3 items are shown. */
export interface ListWidgetData {
    title?: string;
    items: ListWidgetItem[];
    /** Used when the title or empty space is tapped. */
    deepLink?: string;
}
export interface ActionsWidgetButton {
    label: string;
    deepLink: string;
}
/** Data for an ACTIONS widget. Only the first 3 actions are shown. */
export interface ActionsWidgetData {
    title?: string;
    actions: ActionsWidgetButton[];
}
export interface KindOptions {
    kind: WidgetKind;
}
export interface SetWidgetDataOptions {
    kind: WidgetKind;
    /**
     * The widget data as a JSON string (see the *WidgetData interfaces).
     * Keys are matched case-insensitively, so ODC JSONSerialize output works as is.
     */
    data: string;
}
export interface WidgetTapEvent {
    kind: WidgetKind;
    deepLink: string;
}
/** Error codes returned in `error.code` when a call is rejected. */
export declare const OdcHomeWidgetErrors: {
    readonly INVALID_KIND: "OS-PLUG-HWGT-0001";
    readonly INVALID_DATA: "OS-PLUG-HWGT-0002";
    readonly STORAGE: "OS-PLUG-HWGT-0003";
    readonly PIN_NOT_SUPPORTED: "OS-PLUG-HWGT-0004";
    readonly PIN_REFUSED: "OS-PLUG-HWGT-0005";
    readonly UNEXPECTED: "OS-PLUG-HWGT-0006";
};
export interface OdcHomeWidgetPlugin {
    /**
     * Saves the data for a widget kind and redraws every widget of that kind.
     * The data stays saved, so widgets keep it while the app is closed.
     */
    setWidgetData(options: SetWidgetDataOptions): Promise<void>;
    /** Redraws every widget of a kind from the saved data. */
    refresh(options: KindOptions): Promise<void>;
    /** Removes the saved data; widgets go back to "Open the app to set up". */
    clear(options: KindOptions): Promise<void>;
    /** Whether the launcher lets the app pin a widget (Android 8+). */
    isPinSupported(): Promise<{
        supported: boolean;
    }>;
    /** Asks the launcher to add a widget of this kind to the home screen. */
    requestPin(options: KindOptions): Promise<void>;
    /** How many widgets of this kind are on the home screen. */
    getInstalledCount(options: KindOptions): Promise<{
        count: number;
    }>;
    /**
     * Fires when the user taps a widget that has a deep link. A tap that
     * cold-starts the app is held until the first listener is added.
     */
    addListener(eventName: 'widgetTap', listener: (event: WidgetTapEvent) => void): Promise<PluginListenerHandle>;
    /** Removes all listeners for this plugin. */
    removeAllListeners(): Promise<void>;
}
