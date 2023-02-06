import { Int32, Int64, Nullable } from '../../../ys/ys'
import { currentTimeMs } from '../logging/logging-utils'
import { Spamable } from '../mail-features'
import { MarkableReadModel } from './base-models/markable-read-model'
import { DefaultFolderName, MessageId } from './mail-model'
import { MessageListDisplayModel } from './message-list-display-model'

export class SpamableModel implements Spamable {
  private lastSpamActionTime: Nullable<Int64> = null

  constructor(private model: MessageListDisplayModel, private markableModel: MarkableReadModel) {
  }

  public moveToSpam(order: Int32): void {
    this.moveToSpamMessages(new Set<Int32>([order]))
  }

  public moveToSpamMessages(orders: Set<Int32>): void {
    const mids: MessageId[] = []
    for (const order of orders.values()) {
      this.markableModel.markAsRead(order) // Разная логика в iOS приложении и на Web-е
      this.model.getMessageThreadByOrder(order).forEach((mid) => mids.push(mid))
    }
    for (const mid of mids) {
      this.model.moveMessageToFolder(mid, DefaultFolderName.spam)
    }
    this.lastSpamActionTime = currentTimeMs()
  }

  public toastShown(): boolean {
    if (this.lastSpamActionTime === null) {
      return false
    }
    return currentTimeMs() - this.lastSpamActionTime! < 5000
  }

  public moveFromSpam(order: Int32): void {
    this.moveFromSpamMessages(new Set<Int32>([order]))
  }

  public moveFromSpamMessages(orders: Set<Int32>): void {
    for (const mid of this.model.getMidsByOrders(orders)) {
      this.model.moveMessageToFolder(mid, DefaultFolderName.inbox)
    }
    this.lastSpamActionTime = currentTimeMs()
  }
}
