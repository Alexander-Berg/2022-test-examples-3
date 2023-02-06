import { MoveToFolderAction } from '../mail/actions/movable-to-folder-actions'
import { ImapMailboxBuilder, ImapMessage } from '../mail/mailbox-preparer'
import { DefaultFolderName } from '../mail/model/mail-model'
import { RegularYandexTestBase } from '../mbt/mbt-test'
import { TestPlan } from '../mbt/walk/fixed-scenario-strategy'
import { UserAccount } from '../users/user-pool'

export class MoveToFolderTest extends RegularYandexTestBase {
  constructor() {
    super('should move messages to folders')
  }

  public prepareMailbox(mailbox: ImapMailboxBuilder): void {
    mailbox.nextMessage('subj')
  }

  public regularScenario(account: UserAccount): TestPlan {
    return TestPlan.yandexLogin(account)
      .then(new MoveToFolderAction(0, DefaultFolderName.spam))
  }
}
