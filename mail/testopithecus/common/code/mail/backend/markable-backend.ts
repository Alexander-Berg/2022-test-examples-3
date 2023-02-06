import { Int32 } from '../../../ys/ys';
import { MailboxClientHandler } from '../../client/mailbox-client';
import { MarkableRead } from '../mail-features';
import { MessageListDisplayBackend } from './message-list-display-backend';

export class MarkableBackend implements MarkableRead {

  constructor(private mailListDisplayBackend: MessageListDisplayBackend, private clientsHandler: MailboxClientHandler) {
  }

  public markAsRead(order: Int32): void {
    const message = this.mailListDisplayBackend.getThreadMessage(order)
    if (message.tid !== null) {
      this.clientsHandler.getCurrentClient().markThreadAsRead(message.tid!);
    } else {
      this.clientsHandler.getCurrentClient().markMessageAsRead(message.mid);
    }
  }

  public markAsUnread(order: Int32): void {
    const message = this.mailListDisplayBackend.getThreadMessage(order)
    if (message.tid !== null) {
      this.clientsHandler.getCurrentClient().markThreadAsUnread(message.tid!);
    } else {
      this.clientsHandler.getCurrentClient().markMessageAsUnread(message.mid);
    }
  }
}
