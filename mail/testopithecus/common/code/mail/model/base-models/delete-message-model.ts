import { Int32 } from '../../../../ys/ys'
import { DeleteMessage } from '../../mail-features'
import { MessageId } from '../mail-model'
import { MessageListDisplayModel } from '../message-list-display-model'

export class DeleteMessageModel implements DeleteMessage {
  constructor(private model: MessageListDisplayModel) {
  }

  public deleteMessage(order: Int32): void {
    this.deleteMessages(new Set<Int32>([order]))
  }

  public deleteMessages(orders: Set<Int32>): void {
    for (const mid of this.model.getMidsByOrders(orders)) {
      this.model.removeMessage(mid)
    }
  }
}
