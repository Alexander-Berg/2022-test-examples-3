import { AddToAction, AddToFromSuggestAction, } from '../mail/actions/compose-message-actions'
import { RefreshMessageListAction } from '../mail/actions/message-list-actions'
import { OpenMessage } from '../mail/actions/open-message'
import { OpenComposeAction, SendPreparedAction, } from '../mail/actions/write-message-actions'
import { ImapMailboxBuilder } from '../mail/mailbox-preparer'
import { MBTPlatform, RegularYandexTestBase, TestSettings } from '../mbt/mbt-test'
import { TestPlan } from '../mbt/walk/fixed-scenario-strategy'
import { UserAccount } from '../users/user-pool'

export class SendMessageWithBody extends RegularYandexTestBase {
  constructor() {
    super('should receive message with body')
  }

  public setupSettings(settings: TestSettings): void {
    settings.ignoreOn(MBTPlatform.Android).ignoreOn(MBTPlatform.IOS)
  }

  public prepareMailbox(mailbox: ImapMailboxBuilder): void {
    mailbox.nextMessage('subj')
  }

  public regularScenario(account: UserAccount): TestPlan {
    return TestPlan.yandexLogin(account)
      .then(new OpenComposeAction())
      .then(new AddToAction(account.login))
      // .then(new SetBodyAction('test_body')) TODO: support chromedriver
      .thenChain([new SendPreparedAction(), new RefreshMessageListAction()])
      .then(new OpenMessage(0))
  }

}

export class SendMessageWithToAddedFromSuggestTest extends RegularYandexTestBase {
  constructor() {
    super('should receive message with to added from suggest')
  }

  public setupSettings(settings: TestSettings): void {
    settings.ignoreOn(MBTPlatform.Android).ignoreOn(MBTPlatform.IOS)
  }

  public prepareMailbox(builder: ImapMailboxBuilder): void {
    builder.nextMessage('subj')
  }

  public regularScenario(account: UserAccount): TestPlan {
    return TestPlan.yandexLogin(account)
      .then(new OpenComposeAction())
      .then(new AddToFromSuggestAction(account.login.substr(0, 6)))
      // .then(new SetBodyAction('test_body')) TODO: support chromedriver
      .thenChain([new SendPreparedAction(), new RefreshMessageListAction()])
      .then(new OpenMessage(0))
  }
}
