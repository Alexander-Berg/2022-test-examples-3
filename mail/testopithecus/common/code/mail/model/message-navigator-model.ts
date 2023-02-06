import { int64 } from '../../../ys/ys';
import { FullMessageView, MarkableRead, MessageNavigator } from '../mail-features';
import { MessageId } from './mail-model';
import { MessageListDisplayModel } from './message-list-display-model';

export class MessageNavigatorModel implements MessageNavigator {
  public openedMessage: MessageId = int64(-1);

  constructor(private markable: MarkableRead, private messageListDisplay: MessageListDisplayModel) {
  }

  public closeMessage(): void {
    this.openedMessage = int64(-1);
  }

  public deleteCurrentMessage(): void {
    const openedMessage = this.openedMessage;
    if (openedMessage === int64(-1)) {
      throw new Error('No opened message!');
    }
    this.closeMessage();
    this.messageListDisplay.removeMessage(openedMessage);
  }

  public openMessage(order: number): void {
    this.markable.markAsRead(order);
    this.openedMessage = this.messageListDisplay.getMessageId(order);
  }

  public getOpenedMessage(): FullMessageView {
    return this.messageListDisplay.storedMessage(this.openedMessage);
  }
}
