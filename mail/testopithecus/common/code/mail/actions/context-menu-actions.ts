import { Int32, int64 } from '../../../ys/ys'
import { App, FeatureID, MBTAction, MBTActionType, MBTComponent, MBTHistory } from '../../mbt/mbt-abstractions'
import { Testopithecus } from '../logging/events/testopithecus'
import { TestopithecusEvent } from '../logging/testopithecus-event'
import {
  ContextMenu,
  ContextMenuFeature,
  FolderNavigatorFeature,
  MessageListDisplayFeature,
  MessageView,
  MovableToFolderFeature,
} from '../mail-features'
import { MarkAsImportant } from './labeled-actions'
import { MarkAsRead, MarkAsUnread } from './markable-actions'

export abstract class BaseContextMenuAction implements MBTAction {
  constructor(protected order: Int32, private type: MBTActionType) {
  }

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return ContextMenuFeature.get.includedAll(modelFeatures, applicationFeatures)
  }

  public canBePerformed(model: App): boolean {
    const messageListModel = MessageListDisplayFeature.get.forceCast(model)
    const messages = messageListModel.getMessageList(10)
    return this.order < messages.length && this.canBePerformedImpl(messages[this.order])
  }

  public perform(model: App, application: App, history: MBTHistory): MBTComponent {
    this.performImpl(ContextMenuFeature.get.forceCast(model))
    this.performImpl(ContextMenuFeature.get.forceCast(application))
    return history.currentComponent
  }

  public abstract canBePerformedImpl(message: MessageView): boolean

  public abstract performImpl(modelOrApplication: ContextMenu): void

  public abstract events(): TestopithecusEvent[]

  public abstract tostring(): string

  public getActionType(): MBTActionType {
    return this.type
  }
}

export class DeleteMessageFromContextMenuAction extends BaseContextMenuAction {
  public static readonly type: MBTActionType = 'DeleteMessageFromContextMenu'

  constructor(order: Int32) {
    super(order, DeleteMessageFromContextMenuAction.type)
  }

  public performImpl(modelOrApplication: ContextMenu): void {
    modelOrApplication.deleteMessageFromContextMenu(this.order)
  }

  public tostring(): string {
    return `DeleteMessageFromContextMenu(${this.order})`
  }

  public canBePerformedImpl(message: MessageView): boolean {
    return true
  }

  public events(): TestopithecusEvent[] {
    return [
      Testopithecus.messageListEvents.openMessageActions(this.order, int64(-1)),
      Testopithecus.messageActionsEvents.delete(),
    ]
  }
}

export class MarkAsReadFromContextMenuAction extends BaseContextMenuAction {
  public static readonly type: MBTActionType = 'MarkAsReadFromContextMenu'

  constructor(order: Int32) {
    super(order, MarkAsReadFromContextMenuAction.type)
  }

  public performImpl(modelOrApplication: ContextMenu): void {
    modelOrApplication.markAsReadFromContextMenu(this.order)
  }

  public tostring(): string {
    return `MarkAsReadFromContextMenu(${this.order})`
  }

  public canBePerformedImpl(message: MessageView): boolean {
    return MarkAsRead.canMarkRead(message)
  }

  public events(): TestopithecusEvent[] {
    return [
      Testopithecus.messageListEvents.openMessageActions(this.order, int64(-1)),
      Testopithecus.messageActionsEvents.markAsRead(),
    ]
  }
}

export class MarkAsUnreadFromContextMenuAction extends BaseContextMenuAction {
  public static readonly type: MBTActionType = 'MarkAsUnreadFromContextMenu'

  constructor(order: Int32) {
    super(order, MarkAsUnreadFromContextMenuAction.type)
  }

  public performImpl(modelOrApplication: ContextMenu): void {
    modelOrApplication.markAsUnreadFromContextMenu(this.order)
  }

  public tostring(): string {
    return `MarkAsUnreadFromContextMenuAction(${this.order})`
  }

  public canBePerformedImpl(message: MessageView): boolean {
    return MarkAsUnread.canMarkUnread(message)
  }

  public events(): TestopithecusEvent[] {
    return [
      Testopithecus.messageListEvents.openMessageActions(this.order, int64(-1)),
      Testopithecus.messageActionsEvents.markAsUnread(),
    ]
  }
}

export class MarkAsImportantFromContextMenuAction extends BaseContextMenuAction {
  public static readonly type: MBTActionType = 'MarkAsImportantFromContextMenu'

  constructor(order: Int32) {
    super(order, MarkAsImportantFromContextMenuAction.type)
  }

  public performImpl(modelOrApplication: ContextMenu): void {
    modelOrApplication.markAsImportantFromContextMenu(this.order)
  }

  public tostring(): string {
    return `MarkAsImportantFromContextMenuAction(${this.order})`
  }

  public canBePerformedImpl(message: MessageView): boolean {
    return MarkAsImportant.canMarkImportant(message)
  }

  public events(): TestopithecusEvent[] {
    return [
      Testopithecus.messageListEvents.openMessageActions(this.order, int64(-1)),
      Testopithecus.messageActionsEvents.markAsImportant(),
    ]
  }
}

export class MoveToFolderFromContextMenuAction implements MBTAction {
  public static readonly type: MBTActionType = 'MoveToFolderFromContextMenu'

  constructor(private order: Int32, private folderName: string) {
  }

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return FolderNavigatorFeature.get.included(modelFeatures)
      && MovableToFolderFeature.get.included(modelFeatures)
      && ContextMenuFeature.get.includedAll(modelFeatures, applicationFeatures)
  }

  public canBePerformed(model: App): boolean {
    const folderNavigatorModel = FolderNavigatorFeature.get.forceCast(model)
    const folders = folderNavigatorModel.getFoldersList()
    return folders.filter((folder) => folder.name === this.folderName).length > 0 &&
      folderNavigatorModel.getCurrentFolder().name !== this.folderName
  }

  public events(): TestopithecusEvent[] {
    return [
      Testopithecus.messageListEvents.openMessageActions(this.order, int64(-1)),
      Testopithecus.messageActionsEvents.moveToFolder(),
    ]
  }

  public perform(model: App, application: App, history: MBTHistory): MBTComponent {
    ContextMenuFeature.get.forceCast(model).moveToFolderFromContextMenu(this.order, this.folderName)
    ContextMenuFeature.get.forceCast(application).moveToFolderFromContextMenu(this.order, this.folderName)
    return history.currentComponent
  }

  public tostring(): string {
    return `MovableToFolderFromContextMenuAction(${this.order} ${this.folderName})`
  }

  public getActionType(): MBTActionType {
    return MoveToFolderFromContextMenuAction.type
  }
}
