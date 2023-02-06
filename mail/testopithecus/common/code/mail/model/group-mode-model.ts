import { Int32, Nullable } from '../../../ys/ys'
import { requireNonNull } from '../../utils/utils'
import { GroupMode } from '../mail-features'
import { ArchiveMessageModel } from './archive-message-model'
import { DeleteMessageModel } from './base-models/delete-message-model'
import { MarkableReadModel } from './base-models/markable-read-model'
import { MarkableImportantModel } from './label-model'
import { MovableToFolderModel } from './movable-to-folder-model'
import { SpamableModel } from './spamable-model'

export class GroupModeModel implements GroupMode {
  public selectedOrders: Set<Int32> = new Set<Int32>()

  constructor(
    private markableModel: MarkableReadModel,
    private deleteMessageModel: DeleteMessageModel,
    private archive: ArchiveMessageModel,
    private important: MarkableImportantModel,
    private spam: SpamableModel,
    private moveToFolder: MovableToFolderModel,
  ) {
  }

  public getSelectedMessages(): Set<Int32> {
    return this.selectedOrders
  }

  public isInGroupMode(): boolean {
    return this.selectedOrders.size !== 0
  }

  public markAsReadSelectedMessages(): void {
    for (const order of this.selectedOrders.values()) {
      this.markableModel.markAsRead(order)
    }
    this.selectedOrders = new Set<Int32>()
  }

  public markAsUnreadSelectedMessages(): void {
    for (const order of this.selectedOrders.values()) {
      this.markableModel.markAsUnread(order)
    }
    this.selectedOrders = new Set<Int32>()
  }

  public deleteSelectedMessages(): void {
    this.deleteMessageModel.deleteMessages(this.selectedOrders)
    this.selectedOrders = new Set<Int32>()
  }

  public selectMessage(byOrder: Int32): void {
    this.selectedOrders.add(byOrder)
  }

  public archiveSelectedMessages(): void {
    this.archive.archiveMessages(this.selectedOrders)
    this.selectedOrders = new Set<Int32>()
  }

  public markAsImportantSelectedMessages(): void {
    for (const order of this.selectedOrders.values()) {
      this.important.markAsImportant(order)
    }
    this.selectedOrders = new Set<Int32>()
  }

  public markAsNotSpamSelectedMessages(): void {
    this.spam.moveFromSpamMessages(this.selectedOrders)
    this.selectedOrders = new Set<Int32>()
  }

  public markAsSpamSelectedMessages(): void {
    this.spam.moveToSpamMessages(this.selectedOrders)
    this.selectedOrders = new Set<Int32>()
  }

  public markAsUnImportantSelectedMessages(): void {
    for (const order of this.selectedOrders.values()) {
      this.important.markAsUnimportant(order)
    }
    this.selectedOrders = new Set<Int32>()
  }

  public moveToFolderSelectedMessages(folderName: string): void {
    for (const order of this.selectedOrders.values()) {
      this.moveToFolder.moveMessageToFolder(order, folderName)
    }
    this.selectedOrders = new Set<Int32>()
  }

  public unselectAllMessages(): void {
    this.selectedOrders = new Set<Int32>()
  }

  public unselectMessage(byOrder: Int32): void {
    this.selectedOrders.delete(byOrder)
  }

  public copy(): GroupModeModel {
    const copy = new GroupModeModel(
      this.markableModel,
      this.deleteMessageModel,
      this.archive,
      this.important,
      this.spam,
      this.moveToFolder,
      )
    copy.selectedOrders = this.selectedOrders
    return copy
  }

  public initialMessageSelect(byOrder: Int32): void {
    this.selectedOrders = new Set<Int32>([byOrder])
  }
}
