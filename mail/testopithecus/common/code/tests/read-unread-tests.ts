import { MarkAsRead, MarkAsUnread } from '../mail/actions/markable-actions'
import { BackToMaillist, OpenMessage } from '../mail/actions/open-message'
import { ExpandThreadAction } from '../mail/actions/thread-markable-actions'
import { ImapMailboxBuilder } from '../mail/mailbox-preparer'
import { MBTPlatform, RegularYandexTestBase, TestSettings } from '../mbt/mbt-test'
import { TestPlan } from '../mbt/walk/fixed-scenario-strategy'
import { UserAccount } from '../users/user-pool'

export class MarkUnreadAfterReadTest extends RegularYandexTestBase {
  constructor() {
    super('should able to mark unread after read')
  }

  public setupSettings(settings: TestSettings): void {
    settings
      .validateLogs(MBTPlatform.Android)
      .validateLogs(MBTPlatform.IOS)
  }

  public prepareMailbox(mailbox: ImapMailboxBuilder): void {
    mailbox.nextMessage('subj')
  }

  public regularScenario(account: UserAccount): TestPlan {
    return TestPlan.yandexLogin(account)
      .then(new MarkAsRead(0))
      .then(new MarkAsUnread(0))
  }
}

export class ReadMessageAfterOpeningTest  extends RegularYandexTestBase {
  constructor() {
    super('should read message after opening')
  }

  public prepareMailbox(mailbox: ImapMailboxBuilder): void {
    mailbox.nextMessage('subj')
  }

  public regularScenario(account: UserAccount): TestPlan {
    return TestPlan.yandexLogin(account)
      .then(new OpenMessage(0))
      .then(new BackToMaillist())
      .then(new MarkAsUnread(0))
  }
}

export class MarkAllThreadMessagesReadByMarkingMainMessageTest extends RegularYandexTestBase {
  constructor() {
    super('should mark all thread messages read by marking main message')
  }

  public prepareMailbox(mailbox: ImapMailboxBuilder): void {
    mailbox
      .nextMessage('subj')
      .nextMessage('subj')
  }

  public regularScenario(account: UserAccount): TestPlan {
    return TestPlan.yandexLogin(account)
      .then(new MarkAsRead(0))
      .then(new ExpandThreadAction(0))
  }
}
