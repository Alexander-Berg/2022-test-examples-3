// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM tests/group-mode-view-test.ts >>>

import Foundation

open class GroupModeViewTest: RegularYandexTestBase {
  public init() {
    super.init("Переход в режим групповых операций")
  }

  open override func setupSettings(_ settings: TestSettings) {
    settings.iosCase(1064).androidCase(3299)
  }

  open override func prepareMailbox(_ builder: ImapMailboxBuilder) {
    builder.nextMessage("subj1").nextMessage("subj2").nextMessage("subj3").nextMessage("subj4")
  }

  @discardableResult
  open override func regularScenario(_ account: UserAccount) -> TestPlan {
    return TestPlan.yandexLogin(account).then(InitialSelectMessage(0)).then(SelectMessage(2))
  }
}
