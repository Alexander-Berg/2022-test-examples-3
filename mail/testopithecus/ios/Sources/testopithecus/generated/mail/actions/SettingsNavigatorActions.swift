// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM mail/actions/settings-navigator-actions.ts >>>

import Foundation

open class OpenSettingsAction: MBTAction {
  public static let type: MBTActionType = "OpenSettings"
  @discardableResult
  open func canBePerformed(_: App) -> Bool {
    return true
  }

  @discardableResult
  open func events() -> YSArray<TestopithecusEvent> {
    return YSArray(Testopithecus.stubEvent())
  }

  @discardableResult
  open func perform(_ model: App, _ application: App, _: MBTHistory) -> MBTComponent {
    let modelImpl = SettingsNavigatorFeature.get.forceCast(model)
    let appImpl = SettingsNavigatorFeature.get.forceCast(application)
    modelImpl.openSettings()
    appImpl.openSettings()
    return SettingsComponent()
  }

  @discardableResult
  open func supported(_ modelFeatures: YSArray<FeatureID>, _ applicationFeatures: YSArray<FeatureID>) -> Bool {
    return SettingsNavigatorFeature.get.includedAll(modelFeatures, applicationFeatures)
  }

  @discardableResult
  open func tostring() -> String {
    return "OpenSettings"
  }

  @discardableResult
  open func getActionType() -> String {
    return OpenSettingsAction.type
  }
}
