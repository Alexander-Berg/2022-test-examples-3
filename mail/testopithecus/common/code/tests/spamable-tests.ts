import { MoveToSpamAction } from '../mail/actions/spamable-actions'
import { ImapMailboxBuilder, ImapMessage } from '../mail/mailbox-preparer'
import { RegularYandexTestBase } from '../mbt/mbt-test'
import { TestPlan } from '../mbt/walk/fixed-scenario-strategy'
import { UserAccount } from '../users/user-pool'

export class MoveToSpamFirstMessageTest extends RegularYandexTestBase {
  constructor() {
    super('first message should be deleted from inbox if move to spam')
  }

  public prepareMailbox(mailbox: ImapMailboxBuilder): void {
    mailbox
      .nextMessage('subj')
      .nextMessage('subj2')
  }

  public regularScenario(account: UserAccount): TestPlan {
    return TestPlan.yandexLogin(account)
      .then(new MoveToSpamAction(0))
      .then(new MoveToSpamAction(0))
  }
}
