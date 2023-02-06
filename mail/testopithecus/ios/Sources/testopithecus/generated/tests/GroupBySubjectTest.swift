// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM tests/group-by-subject-test.ts >>>

import Foundation

open class GroupBySubjectTest: RegularYandexTestBase {
  public init() {
    super.init("Изменение настройки тредного режима")
  }

  open override func prepareMailbox(_ mailbox: ImapMailboxBuilder) {
    mailbox.nextMessage("subj1").nextMessage("subj2").nextMessage("subj2").nextMessage("subj2").nextMessage("subj1").nextMessage("subj3")
  }

  @discardableResult
  open override func regularScenario(_ account: UserAccount) -> TestPlan {
    return TestPlan.yandexLogin(account).then(OpenSettingsAction()).then(OpenAccountSettingsAction(0)).then(SwitchOffThreadingAction()).then(CloseAccountSettingsAction()).then(GoToFolderAction(DefaultFolderName.inbox)).then(OpenSettingsAction()).then(OpenAccountSettingsAction(0)).then(CloseAccountSettingsAction()).then(GoToFolderAction(DefaultFolderName.inbox))
  }
}