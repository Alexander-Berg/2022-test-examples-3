import { MarkAsImportant, MarkAsUnimportant } from '../mail/actions/labeled-actions'
import { ExpandThreadAction } from '../mail/actions/thread-markable-actions'
import { ImapMailboxBuilder, ImapMessage } from '../mail/mailbox-preparer'
import { RegularYandexTestBase } from '../mbt/mbt-test'
import { TestPlan } from '../mbt/walk/fixed-scenario-strategy'
import { UserAccount } from '../users/user-pool'

export class MarkAsImportantTest extends RegularYandexTestBase {
  constructor() {
    super('should label as unimportant after labelling as important')
  }

  public prepareMailbox(mailbox: ImapMailboxBuilder): void {
    mailbox.nextMessage('subj')
  }

  public regularScenario(account: UserAccount): TestPlan {
    return TestPlan.yandexLogin(account)
      .then(new MarkAsImportant(0))
      .then(new MarkAsUnimportant(0))
  }
}

export class LabelAllThreadMessagesImportantByLabellingMainMessageTest extends RegularYandexTestBase {
  constructor() {
    super('should label all thread messages important by labelling main message')
  }

  public prepareMailbox(mailbox: ImapMailboxBuilder): void {
    mailbox
      .nextMessage('subj')
      .nextMessage('subj')
  }

  public regularScenario(account: UserAccount): TestPlan {
    return TestPlan.yandexLogin(account)
      .then(new MarkAsImportant(0))
      .then(new ExpandThreadAction(0))
  }
}
