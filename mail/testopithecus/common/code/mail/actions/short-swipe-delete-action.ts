import { Int32 } from '../../../ys/ys'
import { App, FeatureID, MBTAction, MBTActionType, MBTComponent, MBTHistory } from '../../mbt/mbt-abstractions'
import { Testopithecus } from '../logging/events/testopithecus'
import { TestopithecusEvent } from '../logging/testopithecus-event'
import {
  ContextMenuFeature, MessageListDisplayFeature,
  MessageView,
  ShortSwipeDelete,
  ShortSwipeDeleteFeature,
} from '../mail-features'

export abstract class BaseShortSwipeAction implements MBTAction {
  constructor(protected order: Int32, private type: MBTActionType) {
  }

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return ShortSwipeDeleteFeature.get.includedAll(modelFeatures, applicationFeatures)
  }

  public canBePerformed(model: App): boolean {
    const messageListModel = MessageListDisplayFeature.get.forceCast(model)
    const messages = messageListModel.getMessageList(10)
    return this.order < messages.length && this.canBePerformedImpl(messages[this.order])
  }

  public perform(model: App, application: App, history: MBTHistory): MBTComponent {
    this.performImpl(ShortSwipeDeleteFeature.get.forceCast(model))
    this.performImpl(ShortSwipeDeleteFeature.get.forceCast(application))
    return history.currentComponent
  }

  public abstract canBePerformedImpl(message: MessageView): boolean

  public abstract performImpl(modelOrApplication: ShortSwipeDelete): void

  public events(): TestopithecusEvent[] {
    return [Testopithecus.stubEvent()]
  }

  public getActionType(): MBTActionType {
    return this.type
  }

  public abstract tostring(): string
}

export class DeleteMessageByShortSwipe extends BaseShortSwipeAction {
  public static readonly type: MBTActionType = 'DeleteMessageByShortSwipe'

  constructor(order: Int32) {
    super(order, DeleteMessageByShortSwipe.type)
  }

  public canBePerformedImpl(message: MessageView): boolean {
    return true
  }

  public performImpl(modelOrApplication: ShortSwipeDelete): void {
    modelOrApplication.deleteMessageByShortSwipe(this.order)
  }

  public tostring(): string {
    return `DeleteMessageByShortSwipe(${this.order})`
  }
}
