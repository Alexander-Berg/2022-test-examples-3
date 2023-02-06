// swift-tools-version:5.0
// The swift-tools-version declares the minimum version of Swift required to build this package.
import PackageDescription

let package = Package(
  name: "Testopithecus",
  platforms: [.iOS(.v11)],
  dependencies: [
  ],
  targets: [
    .target(
      name: "Testopithecus",
      dependencies: []
    ),
    .testTarget(
      name: "TestopithecusTests",
      dependencies: ["Testopithecus"]
    )
  ],
  swiftLanguageVersions: [.v5]
)
