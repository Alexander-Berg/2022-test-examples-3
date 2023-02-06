import { DeleteMessageAction } from '../mail/actions/delete-message'
import { MarkAsRead } from '../mail/actions/markable-actions'
import { ImapMailboxBuilder, ImapMessage } from '../mail/mailbox-preparer'
import { RegularYandexTestBase } from '../mbt/mbt-test'
import { TestPlan } from '../mbt/walk/fixed-scenario-strategy'
import { UserAccount } from '../users/user-pool'

export class MySuperTest extends RegularYandexTestBase {
  constructor() {
    super('should mark as unread selected messages if one is read and one is unread')
  }

  public prepareMailbox(mailbox: ImapMailboxBuilder): void {
    mailbox
      .nextMessage('subj1')
      .nextMessage('subj2')
      .switchFolder('Spam')
      .nextMessage('i bad')
      .switchFolder('MySuperPapka')
      .nextMessage('super mamka')
  }

  public regularScenario(account: UserAccount): TestPlan {
    return TestPlan.yandexLogin(account)
      .then(new MarkAsRead(0))
      .then(new DeleteMessageAction(0))
      .then(new MarkAsRead(0))
  }
}
