import Foundation
import Capacitor

/// iOS stub. Home screen widgets are Android-only in v1: a WidgetKit widget needs
/// its own Widget Extension target, which ODC build actions cannot add.
@objc(OdcHomeWidgetPlugin)
public class OdcHomeWidgetPlugin: CAPPlugin, CAPBridgedPlugin {
    public let identifier = "OdcHomeWidgetPlugin"
    public let jsName = "OdcHomeWidget"
    public let pluginMethods: [CAPPluginMethod] = [
        CAPPluginMethod(name: "setWidgetData", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "setTheme", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "refresh", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "clear", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "isPinSupported", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "requestPin", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getInstalledCount", returnType: CAPPluginReturnPromise)
    ]

    private let message = "Home widgets are Android-only in v1"

    @objc func setWidgetData(_ call: CAPPluginCall) { call.unimplemented(message) }
    @objc func setTheme(_ call: CAPPluginCall) { call.unimplemented(message) }
    @objc func refresh(_ call: CAPPluginCall) { call.unimplemented(message) }
    @objc func clear(_ call: CAPPluginCall) { call.unimplemented(message) }
    @objc func isPinSupported(_ call: CAPPluginCall) { call.resolve(["supported": false]) }
    @objc func requestPin(_ call: CAPPluginCall) { call.unimplemented(message) }
    @objc func getInstalledCount(_ call: CAPPluginCall) { call.unimplemented(message) }
}
