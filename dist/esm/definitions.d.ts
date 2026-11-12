import type { PluginListenerHandle } from '@capacitor/core';
/** The widget kind. There is one: WIDGET, whose whole look comes from the JSON layout. */
export type WidgetKind = 'WIDGET';
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
    positive?: string;
    negative?: string;
}
/** Brand theme. Without `dark`, the same colors are used in dark mode. */
export interface WidgetTheme extends WidgetColors {
    dark?: WidgetColors;
}
/** A theme color name (accent, muted, onBackground, tile...) or a hex color (#RRGGBB / #AARRGGBB). */
export type LayoutColor = string;
/**
 * One element of the layout. Containers (column, row, box) have children;
 * the others are leaves. Sizes are in dp (text size in sp). Exact sizes for
 * icon, image, progress and spacer need Android 12+; older versions use defaults.
 */
export interface LayoutElement {
    type: 'column' | 'row' | 'box' | 'text' | 'icon' | 'image' | 'progress' | 'spacer' | 'button' | 'clock' | 'timer';
    children?: LayoutElement[];
    /** dp for all sides, or [start, top, end, bottom]. */
    padding?: number | [number, number, number, number];
    /** Space between children of a column/row/box, in dp. */
    gap?: number;
    /** > 0 makes the element fill the remaining space of its column/row. */
    weight?: number;
    /** start | center | end (children of a container, or text alignment). */
    align?: 'start' | 'center' | 'end';
    /** top | center | bottom. */
    valign?: 'top' | 'center' | 'bottom';
    /** column/row: flat color; box: rounded tile color; button: pill color. */
    background?: LayoutColor;
    /** text, icon, button content, progress ring. */
    color?: LayoutColor;
    /** text / button label. */
    text?: string;
    /** text size in sp; icon/progress size or spacer length in dp. */
    size?: number;
    bold?: boolean;
    strike?: boolean;
    maxLines?: number;
    /** icon name (see WidgetIcon). */
    name?: WidgetIcon;
    /** button / progress icon. */
    icon?: WidgetIcon;
    /** image source. */
    src?: WidgetImage;
    width?: number;
    height?: number;
    /** image corner radius as a fraction of the shorter side (0-0.5, default 0.12). */
    radius?: number;
    /** progress 0-100. */
    value?: number;
    /** false hides it; also a condition string such as "{{page}} == 2" or "{{temp}} > 25". */
    visible?: boolean | string;
    /** Tapping this element opens the app with this deep link (widgetTap event). */
    deepLink?: string;
    /** What a tap does without opening the app (overrides deepLink). */
    action?: WidgetAction;
    /** clock: date/time pattern (HH:mm:ss, EEE d MMM...); timer: Chronometer format ("%s"). */
    format?: string;
    /** clock: pattern used when the phone is in 24-hour mode. */
    format24?: string;
    /** clock: pattern used when the phone is in 12-hour mode (e.g. "h:mm a"). */
    format12?: string;
    /** clock: IANA time zone, e.g. "Europe/London". Default: the phone's. */
    timeZone?: string;
    /** timer: count up from this time (epoch ms or ISO date-time). */
    since?: number | string;
    /** timer: count down to this time (epoch ms or ISO date-time). */
    until?: number | string;
}
/** A button action that runs on the home screen. */
export type WidgetAction = {
    type: 'open';
    deepLink: string;
    id?: string;
} | {
    type: 'set';
    key: string;
    value: unknown;
    id?: string;
} | {
    type: 'toggle';
    key: string;
    id?: string;
} | {
    type: 'add';
    key: string;
    by?: number;
    min?: number;
    max?: number;
    id?: string;
} | {
    type: 'cycle';
    key: string;
    values: unknown[];
    id?: string;
} | {
    type: 'refresh';
    id?: string;
} | {
    type: 'http';
    url: string;
    method?: string;
    headers?: Record<string, string>;
    body?: unknown;
    /** Store the JSON response under this key (default: merge its fields). */
    key?: string;
    /** Merge the response into the values (default true). */
    merge?: boolean;
    id?: string;
};
/** Background data refresh while the app is closed (Android runs it at most every 15 minutes). */
export interface WidgetRefresh {
    url: string;
    /** Default 30, minimum 15. */
    minutes?: number;
    method?: string;
    headers?: Record<string, string>;
    /** Store the JSON response under this key (default: merge its fields into the values). */
    key?: string;
}
/** Full-bleed background, drawn at the widget's size. */
export interface WidgetBackgroundData {
    /** 1-6 hex colors; one color is a solid fill. */
    gradient?: string[];
    /** CSS-style degrees (0 bottom→top, 90 left→right). Default 135. */
    angle?: number;
    /** Image under the gradient, center-cropped. */
    image?: WidgetImage;
    decoration?: 'none' | 'rings' | 'circles' | 'waves';
    /** Hex with alpha, default #40FFFFFF. */
    decorationColor?: string;
}
/** The data you send (as a JSON string): the whole widget is described here. */
export interface WidgetData {
    layout: LayoutElement;
    /** Starting values for {{placeholders}}. */
    values?: Record<string, unknown>;
    refresh?: WidgetRefresh;
    background?: WidgetBackgroundData;
    /** Tapping empty space opens the app with this deep link. */
    deepLink?: string;
    /** Overrides the app-wide theme for this widget. */
    theme?: WidgetTheme;
}
export interface KindOptions {
    /** Optional; defaults to 'WIDGET'. */
    kind?: WidgetKind;
}
export interface SetWidgetDataOptions extends KindOptions {
    /**
     * A WidgetData object as a JSON string.
     * Keys are matched case-insensitively, so ODC JSONSerialize output works as is.
     */
    data: string;
}
export interface SetThemeOptions {
    /** A WidgetTheme as a JSON string; an empty string resets to the default colors. */
    data: string;
}
export interface ValuesOptions {
    /** A JSON object string, e.g. '{"temp":21}'. */
    data: string;
}
export interface ValuesResult {
    /** All live values as a JSON object string. */
    data: string;
}
/** A widget button press (set / toggle / add / cycle / http). */
export interface WidgetActionEvent {
    type: string;
    id: string;
    key: string;
    /** New value (state actions) or the parsed response (http). */
    value: unknown;
    ok: boolean;
    error: string;
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
     * Saves the widget data, downloads its images, and redraws every widget on
     * the home screen. Resolves once the widgets show the new data.
     */
    setWidgetData(options: SetWidgetDataOptions): Promise<void>;
    /** Merges values into the live values behind {{placeholders}} and redraws. */
    setWidgetValues(options: ValuesOptions): Promise<void>;
    /** The live values, including changes made by widget buttons and background refreshes. */
    getWidgetValues(): Promise<ValuesResult>;
    /** Runs the widget's `refresh` request now; resolves with the new values. */
    refreshNow(): Promise<ValuesResult>;
    /** Sets the brand colors used by theme color names and redraws the widgets. */
    setTheme(options: SetThemeOptions): Promise<void>;
    /** Redraws the widgets from the saved data. */
    refresh(options?: KindOptions): Promise<void>;
    /** Removes the saved data; widgets go back to "Open the app to set up". */
    clear(options?: KindOptions): Promise<void>;
    /** Whether the launcher lets the app pin a widget (Android 8+). */
    isPinSupported(): Promise<{
        supported: boolean;
    }>;
    /** Asks the launcher to add the widget to the home screen. */
    requestPin(options?: KindOptions): Promise<void>;
    /** How many of these widgets are on the home screen. */
    getInstalledCount(options?: KindOptions): Promise<{
        count: number;
    }>;
    /**
     * Fires when the user taps an element that has a deep link. A tap that
     * cold-starts the app is held until the first listener is added.
     */
    addListener(eventName: 'widgetTap', listener: (event: WidgetTapEvent) => void): Promise<PluginListenerHandle>;
    /**
     * Fires for widget buttons that ran without opening the app. Presses made
     * while the app was closed are delivered when it next starts or resumes.
     */
    addListener(eventName: 'widgetAction', listener: (event: WidgetActionEvent) => void): Promise<PluginListenerHandle>;
    /** Removes all listeners for this plugin. */
    removeAllListeners(): Promise<void>;
}
