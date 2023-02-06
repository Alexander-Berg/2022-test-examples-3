import { Int32, int64 } from '../../../ys/ys'
import { App, FeatureID, MBTAction, MBTActionType, MBTComponent, MBTHistory } from '../../mbt/mbt-abstractions'
import { Testopithecus } from '../logging/events/testopithecus'
import { TestopithecusEvent } from '../logging/testopithecus-event'
import { MarkableRead, MarkableReadFeature, MessageListDisplayFeature, MessageView } from '../mail-features'

export abstract class BaseMarkAction implements MBTAction {
  protected constructor(protected order: Int32) {
  }

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return MessageListDisplayFeature.get.included(modelFeatures)
      && MarkableReadFeature.get.includedAll(modelFeatures, applicationFeatures)
  }

  public canBePerformed(model: App): boolean {
    const messageListModel = MessageListDisplayFeature.get.forceCast(model)
    const messages = messageListModel.getMessageList(10)
    return this.order < messages.length && this.canBePerformedImpl(messages[this.order])
  }

  public perform(model: App, application: App, history: MBTHistory): MBTComponent {
    this.performImpl(MarkableReadFeature.get.forceCast(model))
    this.performImpl(MarkableReadFeature.get.forceCast(application))
    return history.currentComponent
  }

  public abstract events(): TestopithecusEvent[]

  public abstract canBePerformedImpl(message: MessageView): boolean

  public abstract performImpl(modelOrApplication: MarkableRead): void

  public abstract tostring(): string

  public abstract getActionType(): MBTActionType
}

export class MarkAsRead extends BaseMarkAction {
  public static readonly type: MBTActionType = 'MarkAsRead'

  constructor(order: Int32) {
    super(order)
  }

  public static canMarkRead(message: MessageView): boolean {
    return !message.read
  }

  public canBePerformedImpl(message: MessageView): boolean {
    return MarkAsRead.canMarkRead(message)
  }

  public performImpl(modelOrApplication: MarkableRead): void {
    return modelOrApplication.markAsRead(this.order)
  }

  public events(): TestopithecusEvent[] {
    return [Testopithecus.messageListEvents.markMessageAsRead(this.order, int64(-1))]
  }

  public tostring(): string {
    return `MarkAsRead(#${this.order})`
  }

  public getActionType(): MBTActionType {
    return MarkAsRead.type
  }
}

export class MarkAsUnread extends BaseMarkAction {
  public static readonly type: MBTActionType = 'MarkAsUnread'

  constructor(order: Int32) {
    super(order)
  }

  public static canMarkUnread(message: MessageView): boolean {
    return message.read
  }

  public canBePerformedImpl(message: MessageView): boolean {
    return MarkAsUnread.canMarkUnread(message)
  }

  public events(): TestopithecusEvent[] {
    return [Testopithecus.messageListEvents.markMessageAsUnread(this.order, int64(-1))]
  }

  public performImpl(modelOrApplication: MarkableRead): void {
    return modelOrApplication.markAsUnread(this.order)
  }

  public tostring(): string {
    return `MarkAsUnread(#${this.order})`
  }

  public getActionType(): MBTActionType {
    return MarkAsUnread.type
  }
}
