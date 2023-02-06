import { MoveToFolderFromContextMenuAction } from '../mail/actions/context-menu-actions'
import { GoToFolderAction } from '../mail/actions/folder-navigator-actions'
import { ImapMailboxBuilder} from '../mail/mailbox-preparer'
import { RegularYandexTestBase, TestSettings } from '../mbt/mbt-test'
import { TestPlan } from '../mbt/walk/fixed-scenario-strategy'
import { UserAccount } from '../users/user-pool'

export class MoveToFolderFromContextMenuTest extends RegularYandexTestBase {
  constructor() {
    super('Short swipe по письму в инбоксе: Move to folder')
  }

  public setupSettings(settings: TestSettings): void {
    settings
      .iosCase(1065)
      .androidCase(2039)
  }

  public prepareMailbox(builder: ImapMailboxBuilder): void {
    builder
      .nextMessage('subj')
      .switchFolder('AutoTestFolder')
      .nextMessage('AutoTestSubj')
  }

  public regularScenario(account: UserAccount): TestPlan {
    return TestPlan.yandexLogin(account)
      .then(new MoveToFolderFromContextMenuAction(0, 'AutoTestFolder'))
      .then(new GoToFolderAction('AutoTestFolder'))
  }
}
