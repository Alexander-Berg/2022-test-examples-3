import { Int32, Int64, Nullable } from '../../../ys/ys'
import { MailboxClientHandler } from '../../client/mailbox-client'
import { requireNonNull } from '../../utils/utils';
import { currentTimeMs } from '../logging/logging-utils'
import { Spamable } from '../mail-features'
import { MessageListDisplayBackend } from './message-list-display-backend'

export class SpamableBackend implements Spamable {
  private lastSpamActionTime: Nullable<Int64> = null

  constructor(
    private messageListDisplayBackend: MessageListDisplayBackend,
    private clientsHandler: MailboxClientHandler) {
  }

  public moveToSpam(order: Int32): void {
    const fid = this.messageListDisplayBackend.getCurrentFolderId()

    const tid = this.messageListDisplayBackend.getThreadMessage(order).tid!
    this.clientsHandler.getCurrentClient().moveToSpam(fid, tid)
    this.lastSpamActionTime = currentTimeMs()
  }

  public toastShown(): boolean {
    if (this.lastSpamActionTime === null) {
      return false
    }
    return currentTimeMs() - this.lastSpamActionTime! < 5000
  }

  public moveFromSpam(order: number): void {
    const fid = this.messageListDisplayBackend.getInbox().fid

    const tid = requireNonNull(
      this.messageListDisplayBackend.getThreadMessage(order).tid,
      'message must have tid!',
    )
    this.clientsHandler.getCurrentClient().moveThreadToFolder(fid, tid)
    this.lastSpamActionTime = currentTimeMs()
  }
}
