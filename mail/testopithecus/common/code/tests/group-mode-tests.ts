import { Nullable } from '../../ys/ys'
import {
  DeleteSelectedMessages, InitialSelectMessage,
  MarkAsReadSelectedMessages,
  MarkAsUnreadSelectedMessages, SelectMessage,
} from '../mail/actions/group-mode-actions'
import { MarkAsRead } from '../mail/actions/markable-actions'
import { OpenMessage } from '../mail/actions/open-message'
import { ImapMailboxBuilder, ImapMessage } from '../mail/mailbox-preparer'
import { RegularYandexTestBase } from '../mbt/mbt-test'
import { TestPlan } from '../mbt/walk/fixed-scenario-strategy'
import { UserAccount } from '../users/user-pool'

export class GroupMarkAsReadDifferentMessagesTest extends RegularYandexTestBase {
  constructor() {
    super('should mark as unread selected messages if one is read and one is unread')
  }

  public prepareMailbox(mailbox: ImapMailboxBuilder): void {
    mailbox
      .nextMessage('subj1')
      .nextMessage('subj2')
  }

  public regularScenario(account: UserAccount): TestPlan {
    return TestPlan.yandexLogin(account)
      .then(new MarkAsRead(0))
      .then(new InitialSelectMessage(0))
      .then(new SelectMessage(1))
      .then(new MarkAsReadSelectedMessages())
  }
}

export class GroupMarkAsReadMessagesTest extends RegularYandexTestBase {
  constructor() {
    super('should mark as unread selected messages if one is read and one is unread')
  }

  public prepareMailbox(mailbox: ImapMailboxBuilder): void {
    mailbox
      .nextMessage('subj1')
      .nextMessage('subj2')
  }

  public regularScenario(account: UserAccount): TestPlan {
    return TestPlan.yandexLogin(account)
      .then(new MarkAsRead(0))
      .then(new MarkAsRead(1))
      .then(new InitialSelectMessage(0))
      .then(new SelectMessage(1))
      .then(new MarkAsUnreadSelectedMessages())
  }
}

export class CanOpenMessageAfterGroupActionTest extends RegularYandexTestBase {
  constructor() {
    super('should be able to open message after group action')
  }

  public prepareMailbox(mailbox: ImapMailboxBuilder): void {
    mailbox
      .nextMessage('subj1')
      .nextMessage('subj2')
  }

  public regularScenario(account: UserAccount): TestPlan {
    return TestPlan.yandexLogin(account)
      .then(new MarkAsRead(0))
      .then(new MarkAsRead(1))
      .then(new InitialSelectMessage(0))
      .then(new SelectMessage(1))
      .then(new MarkAsUnreadSelectedMessages())
      .then(new OpenMessage(0))
  }
}

export class GroupDeleteMessagesTest extends RegularYandexTestBase {
  constructor() {
    super('should delete selected messages')
  }

  public prepareMailbox(mailbox: ImapMailboxBuilder): void {
    mailbox
      .nextMessage('subj1')
      .nextMessage('subj2')
      .nextMessage('subj3')
      .nextMessage('subj4')
  }

  public regularScenario(account: UserAccount): TestPlan {
    return TestPlan.yandexLogin(account)
      .then(new InitialSelectMessage(0))
      .then(new SelectMessage(1))
      .then(new DeleteSelectedMessages())
      .then(new InitialSelectMessage(0))
      .then(new SelectMessage(1))
      .then(new DeleteSelectedMessages())
  }
}
