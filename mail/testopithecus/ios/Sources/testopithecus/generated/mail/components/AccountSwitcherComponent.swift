// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM mail/components/account-switcher-component.ts >>>

import Foundation

open class AccountSwitcherComponent: MBTComponent {
  public static let type: String = "AccountSwitcher"
  @discardableResult
  open func getComponentType() -> MBTComponentType {
    return AccountSwitcherComponent.type
  }

  open func assertMatches(_: App, _: App) {}

  @discardableResult
  open func tostring() -> String {
    return getComponentType()
  }
}

open class AllAccountSwitcherActions: MBTComponentActions {
  private var accounts: YSArray<UserAccount>
  public init(_ accounts: YSArray<UserAccount>) {
    self.accounts = accounts
  }

  @discardableResult
  open func getActions(_ model: App) -> YSArray<MBTAction> {
    let actions: YSArray<MBTAction> = YSArray()
    MultiAccountFeature.get.performIfSupported(model) {
      _ in
      for acc in self.accounts {
        actions.push(SwitchAccountAction(acc.login))
      }
      actions.push(AddNewAccountAction())
    }
    return actions
  }
}