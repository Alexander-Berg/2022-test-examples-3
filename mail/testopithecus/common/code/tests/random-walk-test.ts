import { Int32 } from '../../ys/ys'
import { AddToAction, SetSubjectAction } from '../mail/actions/compose-message-actions'
import { SendMessageAction } from '../mail/actions/write-message-actions'
import { AppendToBody } from '../mail/actions/wysiwyg-actions'
import { ImapMailboxBuilder, ImapMessage } from '../mail/mailbox-preparer'
import { AccountType } from '../mbt/mbt-test'
import { allActionsBehaviour } from '../mbt/walk/behaviour/full-user-behaviour'
import { UserBehaviour } from '../mbt/walk/behaviour/user-behaviour'
import { UserAccount } from '../users/user-pool'
import { Logger } from '../utils/logger'
import { BaseUserBehaviourTest } from './base-user-behaviour-test'

export class RandomWalkTest extends BaseUserBehaviourTest {
  constructor(pathLength: Int32, logger: Logger, seed: Int32) {
    super(`random walk for ${pathLength} steps with seed ${seed}`, pathLength, logger, seed)
  }

  public requiredAccounts(): AccountType[] {
    return [AccountType.Yandex]
  }

  public prepareMailboxes(mailboxes: ImapMailboxBuilder[]): void {
    mailboxes[0].nextMessage('subj')
  }

  public getUserBehaviour(accounts: UserAccount[]): UserBehaviour {
    return allActionsBehaviour(accounts)
      .blacklist(SendMessageAction.type)
      .blacklist(AddToAction.type)
      .blacklist(SetSubjectAction.type)
      .blacklist(AppendToBody.type)
  }
}
