// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM mail/model/expandable-threads-model.ts >>>

import Foundation

open class ReadOnlyExpandableThreadsModel: ReadOnlyExpandableThreads {
  public var expanded: YSSet<MessageId> = YSSet<MessageId>()
  private var messageListDisplay: MessageListDisplayModel
  public init(_ messageListDisplay: MessageListDisplayModel) {
    self.messageListDisplay = messageListDisplay
  }

  @discardableResult
  open func isExpanded(_ threadOrder: Int32) -> Bool {
    let mid = messageListDisplay.getMessageId(threadOrder)
    return expanded.has(mid)
  }

  @discardableResult
  open func isRead(_ threadOrder: Int32, _ messageOrder: Int32) -> Bool {
    return getThreadMessage(threadOrder, messageOrder).head.read
  }

  @discardableResult
  open func getMessagesInThread(_ threadOrder: Int32) -> YSArray<MessageView> {
    return messageListDisplay.getMessagesInThreadByMid(messageListDisplay.getMessageId(threadOrder)).map {
      mid in
      self.messageListDisplay.storedMessage(mid).head
    }
  }

  @discardableResult
  open func getThreadMessage(_ threadOrder: Int32, _ messageOrder: Int32) -> FullMessage {
    let mid = getMessagesInThreadByOrder(threadOrder)[messageOrder]
    return messageListDisplay.storedMessage(mid)
  }

  @discardableResult
  private func getMessagesInThreadByOrder(_ threadOrder: Int32) -> YSArray<MessageId> {
    let mid = messageListDisplay.getMessageId(threadOrder)
    return messageListDisplay.getMessagesInThreadByMid(mid)
  }
}

open class ExpandableThreadsModel: ExpandableThreads {
  private var readonlyExpandableThreads: ReadOnlyExpandableThreadsModel
  private var messageListDisplay: MessageListDisplayModel
  public init(_ readonlyExpandableThreads: ReadOnlyExpandableThreadsModel, _ messageListDisplay: MessageListDisplayModel) {
    self.readonlyExpandableThreads = readonlyExpandableThreads
    self.messageListDisplay = messageListDisplay
  }

  open func markThreadMessageAsRead(_ threadOrder: Int32, _ messageOrder: Int32) {
    readonlyExpandableThreads.getThreadMessage(threadOrder, messageOrder).mutableHead.read = true
  }

  open func markThreadMessageAsUnRead(_ threadOrder: Int32, _ messageOrder: Int32) {
    readonlyExpandableThreads.getThreadMessage(threadOrder, messageOrder).mutableHead.read = false
  }

  open func markThreadMessageAsImportant(_ threadOrder: Int32, _ messageOrder: Int32) {
    readonlyExpandableThreads.getThreadMessage(threadOrder, messageOrder).mutableHead.important = true
  }

  open func markThreadMessageAsUnimportant(_ threadOrder: Int32, _ messageOrder: Int32) {
    readonlyExpandableThreads.getThreadMessage(threadOrder, messageOrder).mutableHead.important = false
  }

  open func expandThread(_ order: Int32) {
    let mid = messageListDisplay.getMessageId(order)
    readonlyExpandableThreads.expanded.add(mid)
  }

  open func collapseThread(_ order: Int32) {
    let mid = messageListDisplay.getMessageId(order)
    readonlyExpandableThreads.expanded.delete(mid)
  }
}
