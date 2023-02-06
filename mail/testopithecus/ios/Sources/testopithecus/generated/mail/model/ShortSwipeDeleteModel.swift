// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM mail/model/short-swipe-delete-model.ts >>>

import Foundation

open class ShortSwipeDeleteModel: ShortSwipeDelete {
  private var lastDeleteMessageTime: Int64!
  private var deleteMessage: DeleteMessageModel
  public init(_ deleteMessage: DeleteMessageModel) {
    self.deleteMessage = deleteMessage
  }

  open func deleteMessageByShortSwipe(_ order: Int32) {
    deleteMessage.deleteMessage(order)
    lastDeleteMessageTime = currentTimeMs()
  }

  @discardableResult
  open func toastShown() -> Bool {
    if lastDeleteMessageTime == nil {
      return false
    }
    return currentTimeMs() - lastDeleteMessageTime! < 5000
  }
}