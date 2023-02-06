import { Int32, Int64, Nullable } from '../../../ys/ys'
import { currentTimeMs } from '../logging/logging-utils';
import { ArchiveMessage } from '../mail-features'
import { DefaultFolderName, MessageId } from './mail-model'
import { MessageListDisplayModel } from './message-list-display-model'

export class ArchiveMessageModel implements ArchiveMessage {
  private lastArchiveMessageTime: Nullable<Int64> = null

  constructor(private model: MessageListDisplayModel) {
  }

  public archiveMessage(order: Int32): void {
    this.archiveMessages(new Set<Int32>([order]))
  }

  public archiveMessages(orders: Set<Int32>): void {
    const folderToMessages = this.model.accountDataHandler.getCurrentAccount().folderToMessages

    if (!folderToMessages.has(DefaultFolderName.archive)) {
      folderToMessages.set(DefaultFolderName.archive, new Set())
    }
    for (const mid of this.model.getMidsByOrders(orders)) {
      this.model.moveMessageToFolder(mid, DefaultFolderName.archive)
    }
    this.lastArchiveMessageTime = currentTimeMs()
  }

  public toastShown(): boolean {
    if (this.lastArchiveMessageTime === null) {
      return false
    }
    return currentTimeMs() - this.lastArchiveMessageTime! < 5000
  }

}
