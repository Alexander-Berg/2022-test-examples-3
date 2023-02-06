import { Int32, Int64, Nullable } from '../../../ys/ys'
import { MailboxClientHandler } from '../../client/mailbox-client'
import { currentTimeMs } from '../logging/logging-utils';
import { ArchiveMessage, Spamable } from '../mail-features'
import { MessageListDisplayBackend } from './message-list-display-backend'

export class ArchiveMessageBackend implements ArchiveMessage {
  private lastMoveToSpamTime: Nullable<Int64> = null

  constructor(
    private messageListDisplayBackend: MessageListDisplayBackend,
    private clientsHandler: MailboxClientHandler) {
  }

  public archiveMessage(order: Int32): void {
    const tid = this.messageListDisplayBackend.getThreadMessage(order).tid!
    this.clientsHandler.getCurrentClient().archive('Archive', tid)
    this.lastMoveToSpamTime = currentTimeMs()
  }

  public toastShown(): boolean {
    if (this.lastMoveToSpamTime === null) {
      return false
    }
    return currentTimeMs() - this.lastMoveToSpamTime! < 5000
  }
}
