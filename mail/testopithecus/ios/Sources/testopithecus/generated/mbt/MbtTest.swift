// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM mbt/mbt-test.ts >>>

import Foundation

public enum AccountType: String, Codable {
  case Yandex = "YANDEX"
  case Yahoo = "YAHOO"
  case Google = "GMAIL"
  case Mail = "MAIL"
  case Hotmail = "HOTMAIL"
  case Rambler = "RAMBLER"
  case Outlook = "OUTLOOK"
  case Other = "OTHER"
  public func toString() -> String {
    return rawValue
  }
}

public enum MBTPlatform {
  case MobileAPI
  case Android
  case IOS
}

public protocol MBTTest {
  var description: String { get }
  func setupSettings(_ settings: TestSettings) -> Void
  @discardableResult
  func requiredAccounts() -> YSArray<AccountType>
  func prepareMailboxes(_ builders: YSArray<ImapMailboxBuilder>) -> Void
  @discardableResult
  func scenario(_ accounts: YSArray<UserAccount>, _ modelProvider: AppModelProvider!, _ supportedFeatures: YSArray<FeatureID>) -> TestPlan
}

open class AbstractMBTTest: MBTTest {
  public let description: String
  public init(_ description: String) {
    self.description = description
  }

  open func setupSettings(_: TestSettings) {}

  @discardableResult
  open func requiredAccounts() -> YSArray<AccountType> {
    fatalError("Must be overridden in subclasses")
  }

  open func prepareMailboxes(_: YSArray<ImapMailboxBuilder>) {
    fatalError("Must be overridden in subclasses")
  }

  @discardableResult
  open func scenario(_: YSArray<UserAccount>, _: AppModelProvider!, _: YSArray<FeatureID>) -> TestPlan {
    fatalError("Must be overridden in subclasses")
  }
}

open class RegularYandexTestBase: AbstractMBTTest {
  public override init(_ description: String) {
    super.init(description)
  }

  @discardableResult
  open override func requiredAccounts() -> YSArray<AccountType> {
    return YSArray(AccountType.Yandex)
  }

  open override func prepareMailboxes(_ mailboxes: YSArray<ImapMailboxBuilder>) {
    if mailboxes.length != 1 {
      fatalError("Тесты на базе RegularYandexTestBase должны наливать ровно один аккаунт!")
    }
    prepareMailbox(mailboxes[0])
  }

  @discardableResult
  open override func scenario(_ accounts: YSArray<UserAccount>, _: AppModelProvider!, _: YSArray<FeatureID>) -> TestPlan {
    if accounts.length != 1 {
      fatalError("Тесты на базе RegularYandexTestBase должны использовать ровно один аккаунт!")
    }
    return regularScenario(accounts[0])
  }

  @discardableResult
  open func regularScenario(_: UserAccount) -> TestPlan {
    fatalError("Must be overridden in subclasses")
  }

  open func prepareMailbox(_: ImapMailboxBuilder) {
    fatalError("Must be overridden in subclasses")
  }
}

open class TestSettings {
  private var testCaseIds: YSMap<MBTPlatform, Int32> = YSMap<MBTPlatform, Int32>()
  private var logValidationPlatforms: YSSet<MBTPlatform> = YSSet()
  private var ignoredPlatforms: YSSet<MBTPlatform> = YSSet()
  private var currentPlatform: MBTPlatform
  public init(_ currentPlatform: MBTPlatform) {
    self.currentPlatform = currentPlatform
  }

  @discardableResult
  open func setTestCaseId(_ platform: MBTPlatform, _ id: Int32) -> TestSettings {
    testCaseIds.set(platform, id)
    return self
  }

  @discardableResult
  open func androidCase(_ id: Int32) -> TestSettings {
    return setTestCaseId(MBTPlatform.Android, id)
  }

  @discardableResult
  open func iosCase(_ id: Int32) -> TestSettings {
    return setTestCaseId(MBTPlatform.IOS, id)
  }

  @discardableResult
  open func validateLogs(_ platform: MBTPlatform) -> TestSettings {
    logValidationPlatforms.add(platform)
    return self
  }

  @discardableResult
  open func shouldValidateLogs() -> Bool {
    return logValidationPlatforms.has(currentPlatform)
  }

  @discardableResult
  open func ignoreOn(_ platform: MBTPlatform) -> TestSettings {
    ignoredPlatforms.add(platform)
    return self
  }

  @discardableResult
  open func isIgnored() -> Bool {
    return ignoredPlatforms.has(currentPlatform)
  }
}