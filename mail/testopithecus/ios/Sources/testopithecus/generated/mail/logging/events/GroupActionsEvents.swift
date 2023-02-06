// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM mail/logging/events/group-actions-events.ts >>>

import Foundation

open class GroupActionsEvents {
  @discardableResult
  open func selectMessage(_ order: Int32, _ mid: Int64) -> TestopithecusEvent {
    if order < 0 {
      return Testopithecus.eventCreationErrorEvent(EventNames.GROUP_MESSAGE_SELECT, "Order must be equal or greater then 0. Was: \(order)")
    }
    return TestopithecusEvent(EventNames.GROUP_MESSAGE_DESELECT, ValueMapBuilder.userEvent().addOrder(order).addMid(mid))
  }

  @discardableResult
  open func deselectMessage(_ order: Int32, _ mid: Int64) -> TestopithecusEvent {
    if order < 0 {
      return Testopithecus.eventCreationErrorEvent(EventNames.GROUP_MESSAGE_DESELECT, "Order must be equal or greater then 0. Was: \(order)")
    }
    return TestopithecusEvent(EventNames.GROUP_MESSAGE_DESELECT, ValueMapBuilder.userEvent().addOrder(order).addMid(mid))
  }

  @discardableResult
  open func deleteSelectedMessages() -> TestopithecusEvent {
    return TestopithecusEvent(EventNames.GROUP_DELETE_SELECTED, ValueMapBuilder.userEvent())
  }

  @discardableResult
  open func markAsReadSelectedMessages() -> TestopithecusEvent {
    return TestopithecusEvent(EventNames.GROUP_MARK_AS_READ_SELECTED, ValueMapBuilder.userEvent())
  }

  @discardableResult
  open func markAsUnreadSelectedMessages() -> TestopithecusEvent {
    return TestopithecusEvent(EventNames.GROUP_MARK_AS_UNREAD_SELECTED, ValueMapBuilder.userEvent())
  }
}