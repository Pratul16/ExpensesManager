// swift-tools-version: 5.9
import PackageDescription
import AppleProductTypes

let package = Package(
    name: "Expenses Manager",
    platforms: [
        .iOS("16.0")
    ],
    products: [
        .iOSApplication(
            name: "Expenses Manager",
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
                .landscapeLeft,
                .portraitUpsideDown
            ],
            appCategory: .finance
        )
    ],
    targets: [
        .executableTarget(
            name: "AppModule",
            path: "."
        )
    ]
)
