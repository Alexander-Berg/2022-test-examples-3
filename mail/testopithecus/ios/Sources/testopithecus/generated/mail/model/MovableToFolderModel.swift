// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM mail/model/movable-to-folder-model.ts >>>

import Foundation

open class MovableToFolderModel: MovableToFolder {
  public var model: MessageListDisplayModel
  public init(_ model: MessageListDisplayModel) {
    self.model = model
  }

  open func moveMessageToFolder(_ order: Int32, _ folderName: FolderName) {
    moveMessagesToFolder(YSSet<Int32>(YSArray(order)), folderName)
  }

  open func moveMessagesToFolder(_ orders: YSSet<Int32>, _ folderName: FolderName) {
    for mid in model.getMidsByOrders(orders) {
      model.moveMessageToFolder(mid, folderName)
    }
  }
}
