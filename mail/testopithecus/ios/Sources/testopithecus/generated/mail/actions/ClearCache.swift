// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM mail/actions/clear-cache.ts >>>

import Foundation

open class ClearCache: BaseSimpleAction<Settings, MBTComponent> {
  public static let type: MBTActionType = "ClearCache"
  public init() {
    super.init(ClearCache.type)
  }

  @discardableResult
  open override func requiredFeature() -> Feature<Settings> {
    return SettingsFeature.get
  }

  @discardableResult
  open override func performImpl(_ modelOrApplication: Settings, _ currentComponent: MBTComponent) -> MBTComponent {
    modelOrApplication.clearCache()
    return currentComponent
  }

  @discardableResult
  open override func events() -> YSArray<TestopithecusEvent> {
    return YSArray(Testopithecus.stubEvent())
  }

  @discardableResult
  open override func tostring() -> String {
    return getActionType()
  }
}