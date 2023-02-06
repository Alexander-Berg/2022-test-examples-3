import { Int64, Nullable } from '../../../ys/ys'
import { currentTimeMs } from '../logging/logging-utils';
import { ShortSwipeDelete } from '../mail-features'
import { DeleteMessageModel } from './base-models/delete-message-model'

export class ShortSwipeDeleteModel implements ShortSwipeDelete {
  private lastDeleteMessageTime: Nullable<Int64> = null
  constructor(private deleteMessage: DeleteMessageModel) {
  }

  public deleteMessageByShortSwipe(order: number): void {
    this.deleteMessage.deleteMessage(order)
    this.lastDeleteMessageTime = currentTimeMs()
  }

  public toastShown(): boolean {
    if (this.lastDeleteMessageTime === null) {
      return false
    }
    return currentTimeMs() - this.lastDeleteMessageTime! < 5000
  }

}
