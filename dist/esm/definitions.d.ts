import type { PluginListenerHandle } from '@capacitor/core';
/** The native widget templates. */
export type WidgetKind = 'COUNT' | 'LIST' | 'ACTIONS' | 'INFO' | 'STATS' | 'HERO' | 'ARTICLES' | 'GRID' | 'MESSAGES' | 'TOGGLES' | 'CHECKLIST';
/**
 * Built-in icon names: search, refresh, add, check, check_circle, circle,
 * chevron_left, chevron_right, arrow_up, arrow_down, favorite, star, home, play,
 * camera, upload, menu, lightbulb, tv, sun, cloud, rain, calendar, location,
 * reply, chat, bell, bolt, trending_up, info. Unknown names are hidden.
 */
export type WidgetIcon = string;
/**
 * Image source: an https URL or a data URI (data:image/png;base64,...).
 * The plugin downloads and caches images when you call setWidgetData.
 */
export type WidgetImage = string;
/** Colors as #RRGGBB or #AARRGGBB. Every field is optional. */
export interface WidgetColors {
    background?: string;
    onBackground?: string;
    muted?: string;
    accent?: string;
    onAccent?: string;
    tile?: string;
    onTile?: string;
    tileActive?: string;
    onTileActive?: string;
    /** Up trend in INFO rows. */
    positive?: string;
    /** Down trend in INFO rows. */
    negative?: string;
}
/** Brand theme. Without `dark`, the same colors are used in dark mode. */
export interface WidgetTheme extends WidgetColors {
    dark?: WidgetColors;
}
export interface HeaderButton {
    icon: WidgetIcon;
    deepLink: string;
}
/** Header fields every template accepts. */
export interface WidgetHeader {
    title?: string;
    icon?: WidgetIcon;
    /** Up to 2 icon buttons on the right of the header. */
    buttons?: HeaderButton[];
    /** Opened when the widget background is tapped. */
    deepLink?: string;
    /** Overrides the app-wide theme for this widget. */
    theme?: WidgetTheme;
}
export interface CountWidgetData extends WidgetHeader {
    value: string | number;
    subtitle?: string;
}
export interface ListWidgetData extends WidgetHeader {
    /** First 3 used. A row without deepLink uses the widget's deepLink. */
    items: {
        text: string;
        deepLink?: string;
    }[];
}
export interface ActionsWidgetData extends WidgetHeader {
    /** First 3 used. */
    actions: {
        label: string;
        icon?: WidgetIcon;
        deepLink: string;
    }[];
}
/** Rows of details: weather, stocks, account balances... */
export interface InfoWidgetData extends WidgetHeader {
    headline?: string;
    /** First 5 used. `trend` colors the values and picks an arrow icon. */
    rows: {
        icon?: WidgetIcon;
        label: string;
        sublabel?: string;
        value?: string;
        subvalue?: string;
        trend?: 'up' | 'down' | '';
        deepLink?: string;
    }[];
}
export interface StatsWidgetData extends WidgetHeader {
    /** First 4 used. `progress` is 0-100 and fills the ring. */
    tiles: {
        icon?: WidgetIcon;
        value: string;
        label: string;
        progress?: number;
        deepLink?: string;
    }[];
}
export interface HeroWidgetData extends WidgetHeader {
    image?: WidgetImage;
    overline?: string;
    headline: string;
    subtitle?: string;
}
export interface ArticlesWidgetData extends WidgetHeader {
    /** Scrollable; first 25 used. */
    items: {
        image?: WidgetImage;
        overline?: string;
        meta?: string;
        title: string;
        subtitle?: string;
        deepLink?: string;
    }[];
}
export interface GridWidgetData extends WidgetHeader {
    /** Scrollable 3-column grid; first 25 used. */
    items: {
        image?: WidgetImage;
        title: string;
        subtitle?: string;
        deepLink?: string;
    }[];
}
export interface MessagesWidgetData extends WidgetHeader {
    /** Scrollable; first 25 used. `highlight` uses the active tile colors. */
    messages: {
        sender: string;
        time?: string;
        text: string;
        highlight?: boolean;
        deepLink?: string;
    }[];
}
export interface TogglesWidgetData extends WidgetHeader {
    /** First 6 used, in 2 columns. Tapping a tile flips `on` and fires `widgetToggle`. */
    tiles: {
        id: string;
        icon?: WidgetIcon;
        label: string;
        on: boolean;
        onText?: string;
        offText?: string;
    }[];
}
export interface ChecklistWidgetData extends WidgetHeader {
    /** Scrollable; first 25 used. Tapping a row flips `checked` and fires `widgetToggle`. */
    items: {
        id: string;
        text: string;
        checked: boolean;
        icon?: WidgetIcon;
    }[];
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
export interface SetThemeOptions {
    /** A WidgetTheme as a JSON string; an empty string resets to the default colors. */
    data: string;
}
export interface WidgetTapEvent {
    kind: WidgetKind;
    deepLink: string;
}
export interface WidgetToggleEvent {
    kind: 'TOGGLES' | 'CHECKLIST';
    /** The tile's or item's `id`. */
    id: string;
    /** The new state. */
    on: boolean;
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
     * Saves the data for a widget kind, downloads its images, and redraws every
     * widget of that kind. Resolves once the widgets show the new data.
     */
    setWidgetData(options: SetWidgetDataOptions): Promise<void>;
    /** Sets the brand colors for all widgets and redraws them. */
    setTheme(options: SetThemeOptions): Promise<void>;
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
    /**
     * Fires when a TOGGLES tile or CHECKLIST row is tapped on the home screen.
     * Taps made while the app was closed are delivered when it next starts or resumes.
     */
    addListener(eventName: 'widgetToggle', listener: (event: WidgetToggleEvent) => void): Promise<PluginListenerHandle>;
    /** Removes all listeners for this plugin. */
    removeAllListeners(): Promise<void>;
}
