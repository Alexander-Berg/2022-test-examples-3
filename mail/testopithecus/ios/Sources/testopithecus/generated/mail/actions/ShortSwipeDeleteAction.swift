// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM mail/actions/short-swipe-delete-action.ts >>>

import Foundation

open class BaseShortSwipeAction: MBTAction {
  public var order: Int32
  private var type: MBTActionType
  public init(_ order: Int32, _ type: MBTActionType) {
    self.order = order
    self.type = type
  }

  @discardableResult
  open func supported(_ modelFeatures: YSArray<FeatureID>, _ applicationFeatures: YSArray<FeatureID>) -> Bool {
    return ShortSwipeDeleteFeature.get.includedAll(modelFeatures, applicationFeatures)
  }

  @discardableResult
  open func canBePerformed(_ model: App) -> Bool {
    let messageListModel = MessageListDisplayFeature.get.forceCast(model)
    let messages = messageListModel.getMessageList(10)
    return order < messages.length && canBePerformedImpl(messages[self.order])
  }

  @discardableResult
  open func perform(_ model: App, _ application: App, _ history: MBTHistory) -> MBTComponent {
    performImpl(ShortSwipeDeleteFeature.get.forceCast(model))
    performImpl(ShortSwipeDeleteFeature.get.forceCast(application))
    return history.currentComponent
  }

  @discardableResult
  open func canBePerformedImpl(_: MessageView) -> Bool {
    fatalError("Must be overridden in subclasses")
  }

  open func performImpl(_: ShortSwipeDelete) {
    fatalError("Must be overridden in subclasses")
  }

  @discardableResult
  open func events() -> YSArray<TestopithecusEvent> {
    return YSArray(Testopithecus.stubEvent())
  }

  @discardableResult
  open func getActionType() -> MBTActionType {
    return type
  }

  @discardableResult
  open func tostring() -> String {
    fatalError("Must be overridden in subclasses")
  }
}

open class DeleteMessageByShortSwipe: BaseShortSwipeAction {
  public static let type: MBTActionType = "DeleteMessageByShortSwipe"
  public init(_ order: Int32) {
    super.init(order, DeleteMessageByShortSwipe.type)
  }

  @discardableResult
  open override func canBePerformedImpl(_: MessageView) -> Bool {
    return true
  }

  open override func performImpl(_ modelOrApplication: ShortSwipeDelete) {
    modelOrApplication.deleteMessageByShortSwipe(order)
  }

  @discardableResult
  open override func tostring() -> String {
    return "DeleteMessageByShortSwipe(\(order))"
  }
}
