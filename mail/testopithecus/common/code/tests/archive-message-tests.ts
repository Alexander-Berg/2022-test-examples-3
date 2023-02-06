import { ArchiveMessageAction } from '../mail/actions/archive-message-action'
import { ImapMailboxBuilder, ImapMessage } from '../mail/mailbox-preparer'
import { RegularYandexTestBase } from '../mbt/mbt-test'
import { TestPlan } from '../mbt/walk/fixed-scenario-strategy'
import { UserAccount } from '../users/user-pool'

export class ArchiveFirstMessageTest extends RegularYandexTestBase {
  constructor() {
    super('first message should be deleted from inbox if move to archive')
  }

  public prepareMailbox(mailbox: ImapMailboxBuilder): void {
    mailbox
      .nextMessage('subj')
      .nextMessage('subj2')
  }

  public regularScenario(account: UserAccount): TestPlan {
    return TestPlan.yandexLogin(account)
      .then(new ArchiveMessageAction(0))
      .then(new ArchiveMessageAction(0))
  }
}
