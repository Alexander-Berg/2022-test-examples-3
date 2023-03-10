// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM mail/model/archive-message-model.ts >>>

import Foundation

open class ArchiveMessageModel: ArchiveMessage {
  private var lastArchiveMessageTime: Int64!
  private var model: MessageListDisplayModel
  public init(_ model: MessageListDisplayModel) {
    self.model = model
  }

  open func archiveMessage(_ order: Int32) {
    archiveMessages(YSSet<Int32>(YSArray(order)))
  }

  open func archiveMessages(_ orders: YSSet<Int32>) {
    let folderToMessages = model.accountDataHandler.getCurrentAccount().folderToMessages
    if !folderToMessages.has(DefaultFolderName.archive) {
      folderToMessages.set(DefaultFolderName.archive, YSSet())
    }
    for mid in model.getMidsByOrders(orders) {
      model.moveMessageToFolder(mid, DefaultFolderName.archive)
    }
    lastArchiveMessageTime = currentTimeMs()
  }

  @discardableResult
  open func toastShown() -> Bool {
    if lastArchiveMessageTime == nil {
      return false
    }
    return currentTimeMs() - lastArchiveMessageTime! < 5000
  }
}
