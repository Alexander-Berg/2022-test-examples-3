import { Int32 } from '../../../ys/ys';
import { ExpandableThreads, MessageView, ReadOnlyExpandableThreads } from '../mail-features';
import { FullMessage, MessageId } from './mail-model';
import { MessageListDisplayModel } from './message-list-display-model';

export class ReadOnlyExpandableThreadsModel implements ReadOnlyExpandableThreads {
  public expanded: Set<MessageId> = new Set<MessageId>();

  constructor(private messageListDisplay: MessageListDisplayModel) {
  }

  public isExpanded(threadOrder: Int32): boolean {
    const mid = this.messageListDisplay.getMessageId(threadOrder);
    return this.expanded.has(mid);
  }

  public isRead(threadOrder: number, messageOrder: number): boolean {
    return this.getThreadMessage(threadOrder, messageOrder).head.read;
  }

  // TODO: занулять темы тредных писем в этом методе
  public getMessagesInThread(threadOrder: Int32): MessageView[] {
    return this.messageListDisplay.getMessagesInThreadByMid(this.messageListDisplay.getMessageId(threadOrder)).map((mid) => this.messageListDisplay.storedMessage(mid).head)
  }

  public getThreadMessage(threadOrder: Int32, messageOrder: Int32): FullMessage {
    const mid = this.getMessagesInThreadByOrder(threadOrder)[messageOrder];
    return this.messageListDisplay.storedMessage(mid);
  }

  private getMessagesInThreadByOrder(threadOrder: Int32): MessageId[] {
    const mid = this.messageListDisplay.getMessageId(threadOrder);
    return this.messageListDisplay.getMessagesInThreadByMid(mid);
  }
}

export class ExpandableThreadsModel implements ExpandableThreads {
  constructor(private readonlyExpandableThreads: ReadOnlyExpandableThreadsModel, private messageListDisplay: MessageListDisplayModel) {
  }

  public markThreadMessageAsRead(threadOrder: Int32, messageOrder: Int32): void {
    this.readonlyExpandableThreads.getThreadMessage(threadOrder, messageOrder).mutableHead.read = true;
  }

  public markThreadMessageAsUnRead(threadOrder: Int32, messageOrder: Int32): void {
    this.readonlyExpandableThreads.getThreadMessage(threadOrder, messageOrder).mutableHead.read = false;
  }

  public markThreadMessageAsImportant(threadOrder: Int32, messageOrder: Int32): void {
    this.readonlyExpandableThreads.getThreadMessage(threadOrder, messageOrder).mutableHead.important = true;
  }

  public markThreadMessageAsUnimportant(threadOrder: Int32, messageOrder: Int32): void {
    this.readonlyExpandableThreads.getThreadMessage(threadOrder, messageOrder).mutableHead.important = false;
  }

  public expandThread(order: Int32): void {
    const mid = this.messageListDisplay.getMessageId(order);
    this.readonlyExpandableThreads.expanded.add(mid);
  }

  public collapseThread(order: Int32): void {
    const mid = this.messageListDisplay.getMessageId(order);
    this.readonlyExpandableThreads.expanded.delete(mid);
  }
}
