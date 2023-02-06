import { Int32 } from '../../../../ys/ys';
import { log } from '../../../utils/logger';
import { MarkableRead } from '../../mail-features';
import { MessageListDisplayModel } from '../message-list-display-model';

export class MarkableReadModel implements MarkableRead {
  constructor(private model: MessageListDisplayModel) {
  }

  public markAsRead(order: Int32): void {
    for (const mid of this.model.getMessageThreadByOrder(order)) {
      log(`Marking mid ${mid} as read`)
      this.model.storedMessage(mid).mutableHead.read = true;
    }
  }

  public markAsUnread(order: Int32): void {
    for (const mid of this.model.getMessageThreadByOrder(order)) {
      log(`Marking mid ${mid} as unread`)
      this.model.storedMessage(mid).mutableHead.read = false;
    }
  }
}
