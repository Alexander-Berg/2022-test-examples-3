import { Int32 } from '../../../ys/ys';
import { MarkableImportant } from '../mail-features';
import { MessageListDisplayModel } from './message-list-display-model';

export class MarkableImportantModel implements MarkableImportant {
  constructor(private model: MessageListDisplayModel) {
  }

  public markAsImportant(order: Int32): void {
    for (const mid of this.model.getMessageThreadByOrder(order)) {
      this.model.storedMessage(mid).mutableHead.important = true;
    }
  }

  public markAsUnimportant(order: Int32): void {
    for (const mid of this.model.getMessageThreadByOrder(order)) {
      this.model.storedMessage(mid).mutableHead.important = false;
    }
  }
}
