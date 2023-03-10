// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM mail/model/context-menu-model.ts >>>

import Foundation

open class ContextMenuModel: ContextMenu {
  private var deleteMessage: DeleteMessageModel
  private var importantMessage: MarkableImportantModel
  private var markableRead: MarkableReadModel
  private var movableToFolder: MovableToFolderModel
  public init(_ deleteMessage: DeleteMessageModel, _ importantMessage: MarkableImportantModel, _ markableRead: MarkableReadModel, _ movableToFolder: MovableToFolderModel) {
    self.deleteMessage = deleteMessage
    self.importantMessage = importantMessage
    self.markableRead = markableRead
    self.movableToFolder = movableToFolder
  }

  open func deleteMessageFromContextMenu(_ order: Int32) {
    deleteMessage.deleteMessage(order)
  }

  open func markAsImportantFromContextMenu(_ order: Int32) {
    importantMessage.markAsImportant(order)
  }

  open func markAsUnImportantFromContextMenu(_ order: Int32) {
    importantMessage.markAsUnimportant(order)
  }

  open func markAsReadFromContextMenu(_ order: Int32) {
    markableRead.markAsRead(order)
  }

  open func markAsUnreadFromContextMenu(_ order: Int32) {
    markableRead.markAsUnread(order)
  }

  open func moveToFolderFromContextMenu(_ order: Int32, _ folderName: String) {
    movableToFolder.moveMessageToFolder(order, folderName)
  }
}
