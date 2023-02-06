import { DeleteCurrentMessage, DeleteMessageAction } from '../mail/actions/delete-message'
import { OpenMessage } from '../mail/actions/open-message'
import { ImapMailboxBuilder, ImapMessage } from '../mail/mailbox-preparer'
import { RegularYandexTestBase, TestSettings } from '../mbt/mbt-test'
import { TestPlan } from '../mbt/walk/fixed-scenario-strategy'
import { UserAccount } from '../users/user-pool'

export class DeleteThreadTest extends RegularYandexTestBase {
  constructor() {
    super('Удаление треда')
  }

  public prepareMailbox(mailbox: ImapMailboxBuilder): void {
    mailbox.nextThread('thread_subj', 4)
  }

  public regularScenario(account: UserAccount): TestPlan {
    return TestPlan.yandexLogin(account)
      .then(new DeleteMessageAction(0))
  }
}

export class DeleteMessageBySwipeTest extends RegularYandexTestBase {
  constructor() {
    super('Удаление письма по short swipe')
  }

  public setupSettings(settings: TestSettings): void {
    settings
      .iosCase(875)
      .androidCase(2301)
  }

  public prepareMailbox(mailbox: ImapMailboxBuilder): void {
    mailbox.nextMessage('subj')
  }

  public regularScenario(account: UserAccount): TestPlan {
    return TestPlan.yandexLogin(account)
      .then(new DeleteMessageAction(0))
  }
}

export class DeleteCurrentMessageTest extends RegularYandexTestBase {
  constructor() {
    super('Удаление открытого письма')
  }

  public setupSettings(settings: TestSettings): void {
    settings
      .androidCase(2046)
      .iosCase(673)
  }

  public prepareMailbox(mailbox: ImapMailboxBuilder): void {
    mailbox.nextMessage('subj')
  }

  public regularScenario(account: UserAccount): TestPlan {
    return TestPlan.yandexLogin(account)
      .then(new OpenMessage(0))
      .then(new DeleteCurrentMessage())
  }
}
