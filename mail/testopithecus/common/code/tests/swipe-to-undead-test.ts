import { MarkAsRead, MarkAsUnread } from '../mail/actions/markable-actions'
import { ImapMailboxBuilder, ImapMessage } from '../mail/mailbox-preparer'
import { RegularYandexTestBase, TestSettings } from '../mbt/mbt-test'
import { TestPlan } from '../mbt/walk/fixed-scenario-strategy'
import { UserAccount } from '../users/user-pool'

export class SwipeToUndeadTest extends RegularYandexTestBase {
  constructor() {
    super('Swipe to unread: прочитанное письмо в Инбоксе')
  }

  public setupSettings(settings: TestSettings): void {
    settings
      .iosCase(1062)
      .androidCase(3297)
  }

  public prepareMailbox(builder: ImapMailboxBuilder): void {
    builder.nextMessage('subj')
      .nextMessage('subj1')
  }

  public regularScenario(account: UserAccount): TestPlan {
    return TestPlan.yandexLogin(account)
      .then(new MarkAsRead(0))
      .then(new MarkAsUnread(0))
  }
}
