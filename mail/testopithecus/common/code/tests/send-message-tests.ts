import { RefreshMessageListAction } from '../mail/actions/message-list-actions'
import { BackToMaillist, OpenMessage } from '../mail/actions/open-message'
import { ExpandThreadAction } from '../mail/actions/thread-markable-actions'
import { OpenComposeAction} from '../mail/actions/write-message-actions'
import { ReplyMessageAction, SendMessageAction } from '../mail/actions/write-message-actions'
import { ImapMailboxBuilder} from '../mail/mailbox-preparer'
import { RegularYandexTestBase } from '../mbt/mbt-test'
import { TestPlan } from '../mbt/walk/fixed-scenario-strategy'
import { UserAccount } from '../users/user-pool'

export class ReceiveMessageTest extends RegularYandexTestBase {
  constructor() {
    super('should receive message')
  }

  public prepareMailbox(mailbox: ImapMailboxBuilder): void {
    mailbox.nextMessage('subj')
  }

  public regularScenario(account: UserAccount): TestPlan {
    return TestPlan.yandexLogin(account)
      .then(new OpenComposeAction())
      .thenChain([
        new SendMessageAction(`${account.login}@yandex.ru`, 'test_message'),
        new RefreshMessageListAction(),
      ])
      .then(new ExpandThreadAction(0))
  }
}

export class ReplyMessageTest  extends RegularYandexTestBase {
  constructor() {
    super('should reply message')
  }

  public prepareMailbox(builder: ImapMailboxBuilder): void {
    builder.nextMessage('subj')
  }

  public regularScenario(account: UserAccount): TestPlan {
    return TestPlan.yandexLogin(account)
      .then(new OpenMessage(0))
      .then(new ReplyMessageAction())
      .then(new BackToMaillist())
      .then(new RefreshMessageListAction())
  }
}
