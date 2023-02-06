// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM tests/swipe-to-undead-test.ts >>>

import Foundation

open class SwipeToUndeadTest: RegularYandexTestBase {
  public init() {
    super.init("Swipe to unread: прочитанное письмо в Инбоксе")
  }

  open override func setupSettings(_ settings: TestSettings) {
    settings.iosCase(1062).androidCase(3297)
  }

  open override func prepareMailbox(_ builder: ImapMailboxBuilder) {
    builder.nextMessage("subj").nextMessage("subj1")
  }

  @discardableResult
  open override func regularScenario(_ account: UserAccount) -> TestPlan {
    return TestPlan.yandexLogin(account).then(MarkAsRead(0)).then(MarkAsUnread(0))
  }
}