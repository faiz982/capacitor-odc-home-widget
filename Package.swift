// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "CapacitorOdcHomeWidget",
    platforms: [.iOS(.v14)],
    products: [
        .library(
            name: "CapacitorOdcHomeWidget",
            targets: ["OdcHomeWidgetPlugin"])
    ],
    dependencies: [
        .package(url: "https://github.com/ionic-team/capacitor-swift-pm.git", from: "7.0.0")
    ],
    targets: [
        .target(
            name: "OdcHomeWidgetPlugin",
            dependencies: [
                .product(name: "Capacitor", package: "capacitor-swift-pm"),
                .product(name: "Cordova", package: "capacitor-swift-pm")
            ],
            path: "ios/Sources/OdcHomeWidgetPlugin")
    ]
)
