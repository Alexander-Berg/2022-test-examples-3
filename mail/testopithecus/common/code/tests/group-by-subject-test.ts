import {
  CloseAccountSettingsAction,
  OpenAccountSettingsAction,
} from '../mail/actions/account-navigator-settings-actions'
import { GoToFolderAction } from '../mail/actions/folder-navigator-actions'
import { SwitchOffThreadingAction} from '../mail/actions/group-by-subject-action'
import { OpenSettingsAction } from '../mail/actions/settings-navigator-actions'
import { ImapMailboxBuilder, ImapMessage } from '../mail/mailbox-preparer'
import { DefaultFolderName } from '../mail/model/mail-model'
import { RegularYandexTestBase } from '../mbt/mbt-test'
import { TestPlan } from '../mbt/walk/fixed-scenario-strategy'
import { UserAccount } from '../users/user-pool'

export class GroupBySubjectTest extends RegularYandexTestBase {
  constructor() {
    super('Изменение настройки тредного режима')
  }

  public prepareMailbox(mailbox: ImapMailboxBuilder): void {
    mailbox
      .nextMessage('subj1')
      .nextMessage('subj2')
      .nextMessage('subj2')
      .nextMessage('subj2')
      .nextMessage('subj1')
      .nextMessage('subj3')
  }

  public regularScenario(account: UserAccount): TestPlan {
    return TestPlan.yandexLogin(account)
      .then(new OpenSettingsAction())
      .then(new OpenAccountSettingsAction(0))
      .then(new SwitchOffThreadingAction())
      .then(new CloseAccountSettingsAction())
      .then(new GoToFolderAction(DefaultFolderName.inbox))
      .then(new OpenSettingsAction())
      .then(new OpenAccountSettingsAction(0))
      .then(new CloseAccountSettingsAction())
      .then(new GoToFolderAction(DefaultFolderName.inbox))
  }
}
