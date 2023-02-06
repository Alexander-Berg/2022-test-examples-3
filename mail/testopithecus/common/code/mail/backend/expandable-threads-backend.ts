import { Int32 } from '../../../ys/ys';
import { MailboxClientHandler } from '../../client/mailbox-client';
import { MessageMeta } from '../../client/message/message-meta';
import { ExpandableThreads } from '../mail-features';
import { MessageListDisplayBackend } from './message-list-display-backend';

export class ExpandableThreadsBackend implements ExpandableThreads {
  constructor(
    private messageListDisplayBackend: MessageListDisplayBackend,
    private clientsHandler: MailboxClientHandler) {}

  public collapseThread(order: Int32): void {
    return
  }

  public expandThread(order: Int32): void {
    return
  }

  public markThreadMessageAsRead(threadOrder: Int32, messageOrder: Int32): void {
    const message = this.getMessageInThread(threadOrder, messageOrder);
    this.clientsHandler.getCurrentClient().markMessageAsRead(message.mid);
  }

  public markThreadMessageAsUnRead(threadOrder: Int32, messageOrder: Int32): void {
    const message = this.getMessageInThread(threadOrder, messageOrder);
    this.clientsHandler.getCurrentClient().markMessageAsUnread(message.mid);
  }

  private getMessageInThread(threadOrder: Int32, messageOrder: Int32): MessageMeta {
    const thread = this.messageListDisplayBackend.getThreadMessage(threadOrder);
    const messages = this.clientsHandler.getCurrentClient().getMessagesInThread(thread.tid!, messageOrder + 1);
    return messages[messageOrder];
  }
}
