import { Int32, int64 } from '../../../ys/ys'
import { App, FeatureID, MBTAction, MBTActionType, MBTComponent, MBTHistory } from '../../mbt/mbt-abstractions'
import { Testopithecus } from '../logging/events/testopithecus'
import { TestopithecusEvent } from '../logging/testopithecus-event'
import { MarkableImportant, MarkableImportantFeature, MessageListDisplayFeature, MessageView } from '../mail-features'

export abstract class BaseLabelAction implements MBTAction {
  constructor(protected order: Int32, private type: MBTActionType) {
  }

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return MarkableImportantFeature.get.includedAll(modelFeatures, applicationFeatures)
  }

  public canBePerformed(model: App): boolean {
    const messageListModel = MessageListDisplayFeature.get.forceCast(model)
    const messages = messageListModel.getMessageList(10)
    return this.order < messages.length && this.canBePerformedImpl(messages[this.order])
  }

  public perform(model: App, application: App, history: MBTHistory): MBTComponent {
    this.performImpl(MarkableImportantFeature.get.forceCast(model))
    this.performImpl(MarkableImportantFeature.get.forceCast(application))
    return history.currentComponent
  }

  public abstract canBePerformedImpl(message: MessageView): boolean

  public abstract performImpl(modelOrApplication: MarkableImportant): void

  public abstract events(): TestopithecusEvent[]

  public abstract tostring(): string

  public getActionType(): MBTActionType {
    return this.type
  }
}

export class MarkAsImportant extends BaseLabelAction {
  public static readonly type: MBTActionType = 'MarkAsImportant'

  constructor(order: Int32) {
    super(order, MarkAsImportant.type)
  }

  public static canMarkImportant(message: MessageView): boolean {
    return !message.important
  }

  public canBePerformedImpl(message: MessageView): boolean {
    return MarkAsImportant.canMarkImportant(message)
  }

  public performImpl(modelOrApplication: MarkableImportant): void {
    return modelOrApplication.markAsImportant(this.order)
  }

  public events(): TestopithecusEvent[] {
    return [
      Testopithecus.messageListEvents.openMessageActions(this.order, int64(-1)),
      Testopithecus.messageActionsEvents.markAsImportant(),
    ]
  }

  public tostring(): string {
    return `MarkAsImportant(#${this.order})`
  }
}

export class MarkAsUnimportant extends BaseLabelAction {
  public static readonly type: MBTActionType = 'MarkAsImportant'

  constructor(order: Int32) {
    super(order, MarkAsUnimportant.type)
  }

  public static canMarkUnimportant(message: MessageView): boolean {
    return message.important
  }

  public canBePerformedImpl(message: MessageView): boolean {
    return MarkAsUnimportant.canMarkUnimportant(message)
  }

  public performImpl(modelOrApplication: MarkableImportant): void {
    return modelOrApplication.markAsUnimportant(this.order)
  }

  public events(): TestopithecusEvent[] {
    return [
      Testopithecus.messageListEvents.openMessageActions(this.order, int64(-1)),
      Testopithecus.messageActionsEvents.markAsNotImportant(),
    ]
  }

  public tostring(): string {
    return `MarkAsUnimportant(#${this.order})`
  }
}
