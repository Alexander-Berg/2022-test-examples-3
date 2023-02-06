import { Int32, int64 } from '../../../ys/ys'
import { App, FeatureID, MBTAction, MBTActionType, MBTComponent, MBTHistory } from '../../mbt/mbt-abstractions'
import { Testopithecus } from '../logging/events/testopithecus'
import { TestopithecusEvent } from '../logging/testopithecus-event'
import {
  ContextMenu,
  ContextMenuFeature,
  FolderNavigatorFeature,
  MessageListDisplayFeature, Spamable,
  SpamableFeature,
} from '../mail-features'
import { FolderNavigatorModel } from '../model/folder-navigator-model'
import { DefaultFolderName } from '../model/mail-model'

export abstract class BaseSpamAction implements MBTAction {

  constructor(protected order: Int32, private type: MBTActionType) {
  }

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return MessageListDisplayFeature.get.included(modelFeatures)
      && FolderNavigatorFeature.get.included(modelFeatures)
      && SpamableFeature.get.includedAll(modelFeatures, applicationFeatures)
  }

  public canBePerformed(model: App): boolean {
    const messageListDisplayModel = MessageListDisplayFeature.get.forceCast(model)
    const messages = messageListDisplayModel.getMessageList(this.order + 1)
    return messages.length > this.order && this.canBePerformedImpl(model)
  }

  public perform(model: App, application: App, history: MBTHistory): MBTComponent {
    this.performImpl(SpamableFeature.get.forceCast(model))
    this.performImpl(SpamableFeature.get.forceCast(application))
    return history.currentComponent
  }

  public abstract performImpl(modelOrApplication: Spamable): void

  public abstract canBePerformedImpl(model: App): boolean

  public abstract tostring(): string

  public events(): TestopithecusEvent[] {
    return [
      Testopithecus.messageListEvents.openMessageActions(this.order, int64(-1)),
    ]
  }

  public abstract getActionType(): MBTActionType
}

export class MoveFromSpamAction extends BaseSpamAction {
  public static readonly type: MBTActionType = 'MoveFromSpam'

  constructor(order: number) {
    super(order, MoveFromSpamAction.type)
  }

  public getActionType(): MBTActionType {
    return MoveFromSpamAction.type
  }

  public performImpl(modelOrApplication: Spamable): void {
    modelOrApplication.moveFromSpam(this.order)
  }

  public tostring(): string {
    return `${MoveFromSpamAction.type}(#${this.order})`
  }

  public events(): TestopithecusEvent[] {
    const events = super.events()
    events.push(Testopithecus.messageActionsEvents.markAsSpam())
    return events
  }

  public canBePerformedImpl(model: App): boolean {
    const folderNavigatorModel = FolderNavigatorFeature.get.forceCast(model)
    return folderNavigatorModel.getCurrentFolder().name === DefaultFolderName.spam
  }
}

export class MoveToSpamAction extends BaseSpamAction {
  public static readonly type: MBTActionType = 'MoveToSpam'

  constructor(order: number) {
    super(order, MoveToSpamAction.type)
  }

  public getActionType(): MBTActionType {
    return MoveToSpamAction.type
  }

  public performImpl(modelOrApplication: Spamable): void {
    modelOrApplication.moveToSpam(this.order)
  }

  public tostring(): string {
    return `${MoveToSpamAction.type}(#${this.order})`
  }

  public events(): TestopithecusEvent[] {
    const events = super.events()
    events.push(Testopithecus.messageActionsEvents.markAsNotSpam())
    return events
  }

  public canBePerformedImpl(model: App): boolean {
    const folderNavigatorModel = FolderNavigatorFeature.get.forceCast(model)
    return folderNavigatorModel.getCurrentFolder().name !== DefaultFolderName.spam
  }
}
