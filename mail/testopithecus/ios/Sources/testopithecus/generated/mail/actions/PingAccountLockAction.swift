// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM mail/actions/ping-account-lock-action.ts >>>

import Foundation

open class PingAccountLockAction: MBTAction {
  public static let type: MBTActionType = "PingAccountLock"
  private var accountLock: UserLock
  public init(_ accountLock: UserLock) {
    self.accountLock = accountLock
  }

  @discardableResult
  open func supported(_: YSArray<FeatureID>, _: YSArray<FeatureID>) -> Bool {
    return true
  }

  @discardableResult
  open func canBePerformed(_: App) -> Bool {
    return true
  }

  @discardableResult
  open func perform(_: App, _: App, _ history: MBTHistory) -> MBTComponent {
    accountLock.ping(int64(30 * 1000))
    return history.currentComponent
  }

  @discardableResult
  open func events() -> YSArray<TestopithecusEvent> {
    return YSArray()
  }

  @discardableResult
  open func tostring() -> String {
    return "PingAccountLock"
  }

  @discardableResult
  open func getActionType() -> MBTActionType {
    return PingAccountLockAction.type
  }
}