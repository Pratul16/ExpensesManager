// swift-tools-version: 5.8
import PackageDescription
import AppleProductTypes

let package = Package(
    name: "ExpensesManager",
    platforms: [
        .iOS("16.0")
    ],
    products: [
        .iOSApplication(
            name: "ExpensesManager",
            targets: ["AppModule"],
            bundleIdentifier: "com.pratul.expensesmanager",
            teamIdentifier: "",
            displayVersion: "1.4.0",
            bundleVersion: "7",
            appIcon: .placeholder(icon: .creditCard),
            accentColor: .presetColor(.purple),
            supportedDeviceFamilies: [
                .pad,
                .phone
            ],
            supportedInterfaceOrientations: [
                .portrait,
                .landscapeRight,
                .landscapeLeft
            ],
            appCategory: .finance
        )
    ],
    targets: [
        .executableTarget(
            name: "AppModule",
            path: "Sources"
        )
    ]
)
