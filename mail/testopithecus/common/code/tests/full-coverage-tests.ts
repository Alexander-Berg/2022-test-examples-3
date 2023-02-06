import { GoToFolderAction } from '../mail/actions/folder-navigator-actions'
import { MoveToFolderAction } from '../mail/actions/movable-to-folder-actions'
import { RotateToLandscape } from '../mail/actions/rotatable-actions'
import { OpenComposeAction, ReplyMessageAction, SendMessageAction } from '../mail/actions/write-message-actions'
import { ImapMailboxBuilder, ImapMessage } from '../mail/mailbox-preparer'
import { AccountType } from '../mbt/mbt-test'
import { allActionsBehaviour } from '../mbt/walk/behaviour/full-user-behaviour'
import { UserBehaviour } from '../mbt/walk/behaviour/user-behaviour'
import { ActionLimitsStrategy } from '../mbt/walk/limits/action-limits-strategy'
import { PersonalActionLimits } from '../mbt/walk/limits/personal-action-limits'
import { UserAccount } from '../users/user-pool'
import { Logger } from '../utils/logger'
import { FullCoverageBaseTest } from './base-user-behaviour-test'

export class NoComposeFullCoverageTest extends FullCoverageBaseTest {
  constructor(logger: Logger) {
    super('should cover all application without compose', logger)
  }

  public requiredAccounts(): AccountType[] {
    return [AccountType.Yandex]
  }

  public prepareMailboxes(mailboxes: ImapMailboxBuilder[]): void {
    mailboxes[0].nextMessage('subj')
  }

  public getUserBehaviour(userAccounts: UserAccount[]): UserBehaviour {
    return allActionsBehaviour(userAccounts)
      .blacklist(OpenComposeAction.type)
      .blacklist(RotateToLandscape.type)
  }

  public getActionLimits(): ActionLimitsStrategy {
    return new PersonalActionLimits(15)
      .setLimit(ReplyMessageAction.type, 2)
      .setLimit(SendMessageAction.type, 2)
      .setLimit(GoToFolderAction.type, 1)
      .setLimit(MoveToFolderAction.type, 1)
  }
}
