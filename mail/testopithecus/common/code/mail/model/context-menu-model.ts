import { Int32 } from '../../../ys/ys';
import { ContextMenu } from '../mail-features';
import { DeleteMessageModel } from './base-models/delete-message-model';
import { MarkableReadModel } from './base-models/markable-read-model';
import { MarkableImportantModel } from './label-model';
import { MovableToFolderModel } from './movable-to-folder-model';

export class ContextMenuModel implements ContextMenu {
  constructor(
    private deleteMessage: DeleteMessageModel,
    private importantMessage: MarkableImportantModel,
    private markableRead: MarkableReadModel,
    private movableToFolder: MovableToFolderModel,
  ) {
  }

  public deleteMessageFromContextMenu(order: number): void {
    this.deleteMessage.deleteMessage(order)
  }

  public markAsImportantFromContextMenu(order: number): void {
    this.importantMessage.markAsImportant(order)
  }

  public markAsUnImportantFromContextMenu(order: number): void {
    this.importantMessage.markAsUnimportant(order)
  }

  public markAsReadFromContextMenu(order: Int32): void {
    this.markableRead.markAsRead(order)
  }

  public markAsUnreadFromContextMenu(order: Int32): void {
    this.markableRead.markAsUnread(order)
  }

  public moveToFolderFromContextMenu(order: Int32, folderName: string): void {
    this.movableToFolder.moveMessageToFolder(order, folderName)
  }

}
