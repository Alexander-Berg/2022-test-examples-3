import { Int32, int64, Nullable } from '../../../ys/ys'
import { BaseSimpleAction } from '../../mbt/base-simple-action'
import {
  App,
  Feature,
  FeatureID,
  MBTAction,
  MBTActionType,
  MBTComponent,
  MBTHistory,
} from '../../mbt/mbt-abstractions'
import { filterByOrders } from '../../utils/utils'
import { GroupOperationsComponent } from '../components/group-operations-component'
import { MaillistComponent } from '../components/maillist-component'
import { MessageComponent } from '../components/message-component'
import { Testopithecus } from '../logging/events/testopithecus'
import { TestopithecusEvent } from '../logging/testopithecus-event'
import {
  FolderNavigatorFeature,
  GroupMode,
  GroupModeFeature,
  MessageListDisplayFeature,
  MessageView,
} from '../mail-features'
import { DefaultFolderName, Folder, FolderName } from '../model/mail-model'

export abstract class BaseGroupModeAction implements MBTAction {
  constructor(private type: MBTActionType) {
  }

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return GroupModeFeature.get.includedAll(modelFeatures, applicationFeatures)
  }

  public canBePerformed(model: App): boolean {
    const groupModeModel = GroupModeFeature.get.forceCast(model)
    const messageListModel = MessageListDisplayFeature.get.forceCast(model)
    const foldersModel = FolderNavigatorFeature.get.forceCast(model)
    const messages = messageListModel.getMessageList(10)
    const selectedMessageOrders = groupModeModel.getSelectedMessages()
    if (selectedMessageOrders === null) {
      return false
    }
    const selectedMessages: MessageView[] = []
    for (const order of selectedMessageOrders.values()) {
      selectedMessages.push(messages[order])
    }
    const currentFolder = foldersModel.getCurrentFolder()

    return this.canBePerformedImpl(selectedMessages, selectedMessageOrders, currentFolder)
  }

  public perform(model: App, application: App, history: MBTHistory): MBTComponent {
    this.performImpl(GroupModeFeature.get.forceCast(model))
    this.performImpl(GroupModeFeature.get.forceCast(application))
    return new MaillistComponent()
  }

  public abstract canBePerformedImpl(messages: MessageView[], selectedOrders: Set<Int32>, currentFolder: Folder): boolean

  public abstract performImpl(modelOrApplication: GroupMode): void

  public abstract events(): TestopithecusEvent[]

  public abstract tostring(): string

  public getActionType(): MBTActionType {
    return this.type
  }
}

export abstract class BaseMarkSelectedMessages implements MBTAction {
  constructor(private type: MBTActionType) {
  }

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return MessageListDisplayFeature.get.included(modelFeatures)
      && GroupModeFeature.get.includedAll(modelFeatures, applicationFeatures)
  }

  public canBePerformed(model: App): boolean {
    const messageListModel = MessageListDisplayFeature.get.forceCast(model)
    const groupModeModel = GroupModeFeature.get.forceCast(model)
    const selectedMessageOrders = groupModeModel.getSelectedMessages()
    if (selectedMessageOrders === null) {
      return false
    }
    const messages = messageListModel.getMessageList(10)
    const unreadCount = filterByOrders(messages, selectedMessageOrders).filter((message) => !message.read).length
    return this.canBePerformedImpl(unreadCount)
  }

  public perform(model: App, application: App, history: MBTHistory): MBTComponent {
    this.performImpl(GroupModeFeature.get.forceCast(model))
    this.performImpl(GroupModeFeature.get.forceCast(application))
    return new MaillistComponent()
  }

  public abstract events(): TestopithecusEvent[]

  public abstract canBePerformedImpl(selectedUnreadCount: Int32): boolean

  public abstract performImpl(modelOrApplication: GroupMode): void

  public tostring(): string {
    return this.getActionType()
  }

  public getActionType(): MBTActionType {
    return this.type
  }
}

export class InitialSelectMessage extends BaseSimpleAction<GroupMode, MaillistComponent> {
  public static readonly type: MBTActionType = 'InitialSelectMessages'

  constructor(private order: Int32) {
    super(SelectMessage.type)
  }

  public requiredFeature(): Feature<GroupMode> {
    return GroupModeFeature.get
  }

  public canBePerformedImpl(model: GroupMode): boolean {
    return !model.isInGroupMode()
  }

  public events(): TestopithecusEvent[] {
    return [Testopithecus.groupActionsEvents.selectMessage(this.order, int64(-1))]
  }

  public performImpl(modelOrApplication: GroupMode, currentComponent: MaillistComponent): MBTComponent {
    modelOrApplication.initialMessageSelect(this.order)
    return new GroupOperationsComponent()
  }

  public tostring(): string {
    return `InitialSelectMessage(${this.order})`
  }
}

export class SelectMessage extends BaseSimpleAction<GroupMode, GroupOperationsComponent> {
  public static readonly type: MBTActionType = 'SelectMessages'

  constructor(private order: Int32) {
    super(SelectMessage.type)
  }

  public requiredFeature(): Feature<GroupMode> {
    return GroupModeFeature.get
  }

  public canBePerformedImpl(model: GroupMode): boolean {
    return model.isInGroupMode()
  }

  public events(): TestopithecusEvent[] {
    return [Testopithecus.groupActionsEvents.selectMessage(this.order, int64(-1))]
  }

  public performImpl(modelOrApplication: GroupMode, currentComponent: GroupOperationsComponent): MBTComponent {
    modelOrApplication.selectMessage(this.order)
    return new GroupOperationsComponent()
  }

  public tostring(): string {
    return `SelectMessages(${this.order})`
  }
}

export class MarkAsReadSelectedMessages extends BaseMarkSelectedMessages {
  public static readonly type: MBTActionType = 'MarkAsReadSelectedMessages'

  constructor() {
    super(MarkAsReadSelectedMessages.type)
  }

  public canBePerformedImpl(selectedUnreadCount: Int32): boolean {
    return selectedUnreadCount > 0
  }

  public performImpl(modelOrApplication: GroupMode): void {
    modelOrApplication.markAsReadSelectedMessages()
  }

  public events(): TestopithecusEvent[] {
    return [Testopithecus.groupActionsEvents.markAsReadSelectedMessages()]
  }
}

export class MarkAsUnreadSelectedMessages extends BaseMarkSelectedMessages {
  public static readonly type: MBTActionType = 'MarkAsUnreadSelectedMessages'

  constructor() {
    super(MarkAsUnreadSelectedMessages.type)
  }

  public canBePerformedImpl(selectedUnreadCount: Int32): boolean {
    return selectedUnreadCount === 0
  }

  public performImpl(modelOrApplication: GroupMode): void {
    modelOrApplication.markAsUnreadSelectedMessages()
  }

  public events(): TestopithecusEvent[] {
    return [Testopithecus.groupActionsEvents.markAsUnreadSelectedMessages()]
  }
}

export class DeleteSelectedMessages implements MBTAction {
  public static readonly type: MBTActionType = 'DeleteSelectedMessages'

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return MessageListDisplayFeature.get.included(modelFeatures)
      && GroupModeFeature.get.includedAll(modelFeatures, applicationFeatures)
  }

  public canBePerformed(model: App): boolean {
    return true
  }

  public perform(model: App, application: App, history: MBTHistory): MBTComponent {
    GroupModeFeature.get.forceCast(model).deleteSelectedMessages()
    GroupModeFeature.get.forceCast(application).deleteSelectedMessages()
    return new MaillistComponent()
  }

  public events(): TestopithecusEvent[] {
    return [Testopithecus.groupActionsEvents.deleteSelectedMessages()]
  }

  public tostring(): string {
    return this.getActionType()
  }

  public getActionType(): MBTActionType {
    return DeleteSelectedMessages.type
  }
}

export class MarkImportantSelectedAction extends BaseGroupModeAction {

  public static readonly type: MBTActionType = 'MarkImportantSelected'

  constructor() {
    super(MarkImportantSelectedAction.type)
  }

  public canBePerformedImpl(messages: MessageView[], selectedOrders: Set<Int32>, currentFolder: Folder): boolean {
    return messages.map((m) => !m.important).includes(true)
  }

  public events(): TestopithecusEvent[] {
    return []
  }

  public performImpl(modelOrApplication: GroupMode): void {
    modelOrApplication.markAsImportantSelectedMessages()
  }

  public tostring(): string {
    return MarkImportantSelectedAction.type
  }
}

export class MarkUnimportantSelectedAction extends BaseGroupModeAction {

  public static readonly type: MBTActionType = 'MarkUnimportantSelected'

  constructor() {
    super(MarkUnimportantSelectedAction.type)
  }

  public canBePerformedImpl(messages: MessageView[], selectedOrders: Set<Int32>, currentFolder: Folder): boolean {
    return messages.map((m) => !m.important).includes(false)
  }

  public events(): TestopithecusEvent[] {
    return []
  }

  public performImpl(modelOrApplication: GroupMode): void {
    modelOrApplication.markAsUnImportantSelectedMessages()
  }

  public tostring(): string {
    return MarkUnimportantSelectedAction.type
  }
}

export class MarkSpamSelectedAction extends BaseGroupModeAction {

  public static readonly type: MBTActionType = 'MarkSpamSelected'

  constructor() {
    super(MarkSpamSelectedAction.type)
  }

  public canBePerformedImpl(messages: MessageView[], selectedOrders: Set<Int32>, currentFolder: Folder): boolean {
    return currentFolder.name !== DefaultFolderName.spam
  }

  public events(): TestopithecusEvent[] {
    return []
  }

  public performImpl(modelOrApplication: GroupMode): void {
    modelOrApplication.markAsSpamSelectedMessages()
  }

  public tostring(): string {
    return MarkSpamSelectedAction.type
  }
}

export class MarkNotSpamSelectedAction extends BaseGroupModeAction {

  public static readonly type: MBTActionType = 'MarkNotSpamSelected'

  constructor() {
    super(MarkNotSpamSelectedAction.type)
  }

  public canBePerformedImpl(messages: MessageView[], selectedOrders: Set<Int32>, currentFolder: Folder): boolean {
    return currentFolder.name === DefaultFolderName.spam
  }

  public events(): TestopithecusEvent[] {
    return []
  }

  public performImpl(modelOrApplication: GroupMode): void {
    modelOrApplication.markAsNotSpamSelectedMessages()
  }

  public tostring(): string {
    return MarkNotSpamSelectedAction.type
  }
}

export class MoveToFolderSelectedMessagesAction implements MBTAction {
  public static readonly type: MBTActionType = 'MoveToFolderSelectedMessages'

  constructor(private folderName: FolderName) {
  }

  public canBePerformed(model: App): boolean {
    return FolderNavigatorFeature.get.castIfSupported(model)!.getCurrentFolder().name !== this.folderName
  }

  public events(): TestopithecusEvent[] {
    return []
  }

  public getActionType(): string {
    return MoveToFolderSelectedMessagesAction.type
  }

  public perform(model: App, application: App, history: MBTHistory): MBTComponent {
    GroupModeFeature.get.forceCast(model).moveToFolderSelectedMessages(this.folderName)
    GroupModeFeature.get.forceCast(application).moveToFolderSelectedMessages(this.folderName)
    return new MessageComponent()
  }

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return MessageListDisplayFeature.get.included(modelFeatures)
      && GroupModeFeature.get.includedAll(modelFeatures, applicationFeatures)

  }

  public tostring(): string {
    return `${MoveToFolderSelectedMessagesAction.type}(${this.folderName})`
  }

}

export class UnselectMessageAction extends BaseGroupModeAction {

  public static readonly type: MBTActionType = 'UnselectMessages'

  constructor(private order: Int32) {
    super(UnselectMessageAction.type)
  }

  public canBePerformedImpl(messages: MessageView[], selectedOrders: Set<Int32>, currentFolder: Folder): boolean {
    return selectedOrders.has(this.order)
  }

  public events(): TestopithecusEvent[] {
    return []
  }

  public perform(model: App, application: App, history: MBTHistory): MBTComponent {
    this.performImpl(GroupModeFeature.get.forceCast(model))
    this.performImpl(GroupModeFeature.get.forceCast(application))
    if (GroupModeFeature.get.forceCast(model).getSelectedMessages() === null) {
      return new MessageComponent()
    } else {
      return history.currentComponent
    }
  }

  public performImpl(modelOrApplication: GroupMode): void {
    modelOrApplication.unselectMessage(this.order)
  }

  public tostring(): string {
    return `${UnselectMessageAction.type}(${this.order})`
  }
}

export class UnselectAllMessagesAction extends BaseGroupModeAction {

  public static readonly type: MBTActionType = 'UnselectAllMessages'

  constructor() {
    super(UnselectAllMessagesAction.type)
  }

  public canBePerformedImpl(messages: MessageView[], selectedOrders: Set<Int32>, currentFolder: Folder): boolean {
    return true
  }

  public events(): TestopithecusEvent[] {
    return []
  }

  public performImpl(modelOrApplication: GroupMode): void {
    modelOrApplication.unselectAllMessages()
  }

  public tostring(): string {
    return UnselectAllMessagesAction.type
  }
}
