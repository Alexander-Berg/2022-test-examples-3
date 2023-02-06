import { ImapMailboxBuilder} from '../mail/mailbox-preparer'
import { RegularYandexTestBase, TestSettings } from '../mbt/mbt-test'
import { TestPlan } from '../mbt/walk/fixed-scenario-strategy'
import { UserAccount } from '../users/user-pool'

export class InboxTopBarDisplayTest extends RegularYandexTestBase {
  constructor() {
    super('Отображение топ бара в списке писем Инбокса')
  }

  public setupSettings(settings: TestSettings): void {
    settings
      .androidCase(2300)
      .iosCase(874)
  }

  public prepareMailbox(builder: ImapMailboxBuilder): void {
    builder.nextMessage('subj')
  }

  public regularScenario(account: UserAccount): TestPlan {
    return TestPlan.yandexLogin(account)
  }
}
