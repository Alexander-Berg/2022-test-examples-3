import { MailboxClientHandler } from '../../client/mailbox-client';
import { DeleteMessage } from '../mail-features';
import { MessageListDisplayBackend } from './message-list-display-backend';

export class DeleteMessageBackend implements DeleteMessage {
  constructor(
    private messageListDisplayBackend: MessageListDisplayBackend,
    private clientsHandler: MailboxClientHandler) {}

  public deleteMessage(order: number): void {
    const fid = this.messageListDisplayBackend.getCurrentFolderId()
    const tid = this.messageListDisplayBackend.getThreadMessage(order).tid!
    this.clientsHandler.getCurrentClient().removeMessageByThreadId(fid, tid)
  }
}
