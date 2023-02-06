import { CreateFolderAction, GoToFolderAction } from '../mail/actions/folder-navigator-actions'
import { ImapMailboxBuilder, ImapMessage } from '../mail/mailbox-preparer'
import { DefaultFolderName } from '../mail/model/mail-model'
import { RegularYandexTestBase } from '../mbt/mbt-test'
import { TestPlan } from '../mbt/walk/fixed-scenario-strategy'
import { UserAccount } from '../users/user-pool'

export class ChangeFoldersTest extends RegularYandexTestBase {
  constructor() {
    super('should navigate through folders')
  }

  public prepareMailbox(mailbox: ImapMailboxBuilder): void {
    mailbox.nextMessage('subj')
  }

  public regularScenario(account: UserAccount): TestPlan {
    return TestPlan.yandexLogin(account)
      .then(new GoToFolderAction(DefaultFolderName.spam))
      .then(new GoToFolderAction(DefaultFolderName.inbox))
  }
}

export class CreateFoldersTest extends RegularYandexTestBase {
  constructor() {
    super('should create folders')
  }

  public prepareMailbox(mailbox: ImapMailboxBuilder): void {
    mailbox.nextMessage('subj')
  }

  public regularScenario(account: UserAccount): TestPlan {
    return TestPlan.yandexLogin(account)
      .then(new CreateFolderAction('TestFolder1'))
      .then(new CreateFolderAction('TestFolder2'))
      .then(new CreateFolderAction('TestFolder3'))
  }
}
