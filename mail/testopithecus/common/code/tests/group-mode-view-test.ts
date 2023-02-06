import { InitialSelectMessage, SelectMessage} from '../mail/actions/group-mode-actions'
import { ImapMailboxBuilder} from '../mail/mailbox-preparer'
import { RegularYandexTestBase, TestSettings } from '../mbt/mbt-test'
import { TestPlan } from '../mbt/walk/fixed-scenario-strategy'
import { UserAccount } from '../users/user-pool'

export class GroupModeViewTest extends RegularYandexTestBase {
  constructor() {
    super('Переход в режим групповых операций')
  }

  public setupSettings(settings: TestSettings): void {
    settings
      .iosCase(1064)
      .androidCase(3299)
  }

  public prepareMailbox(builder: ImapMailboxBuilder): void {
    builder.nextMessage('subj1')
      .nextMessage('subj2')
      .nextMessage('subj3')
      .nextMessage('subj4')
  }

  public regularScenario(account: UserAccount): TestPlan {
    return TestPlan.yandexLogin(account)
      .then(new InitialSelectMessage(0))
      .then(new SelectMessage(2))
  }
}
