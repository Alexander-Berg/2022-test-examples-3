import { MarkAsRead} from '../mail/actions/markable-actions'
import { ImapMailboxBuilder} from '../mail/mailbox-preparer'
import { RegularYandexTestBase, TestSettings } from '../mbt/mbt-test'
import {  TestPlan } from '../mbt/walk/fixed-scenario-strategy'
import { UserAccount } from '../users/user-pool'

export class SwipeToReadTest extends RegularYandexTestBase {
  constructor() {
    super('Swipe to read: непрочитанное письмо в Инбоксе')
  }

  public setupSettings(settings: TestSettings): void {
    settings
      .iosCase(873)
      .androidCase(2299)
  }

  public prepareMailbox(builder: ImapMailboxBuilder): void {
    builder
      .nextMessage('subj')
      .nextMessage('subj1')
  }

  public regularScenario(account: UserAccount): TestPlan {
    return TestPlan.yandexLogin(account)
      .then(new MarkAsRead(0))
  }
}
