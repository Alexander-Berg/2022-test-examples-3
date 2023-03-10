// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM mail/actions/labeled-actions.ts >>>

import Foundation

open class BaseLabelAction: MBTAction {
  public var order: Int32
  private var type: MBTActionType
  public init(_ order: Int32, _ type: MBTActionType) {
    self.order = order
    self.type = type
  }

  @discardableResult
  open func supported(_ modelFeatures: YSArray<FeatureID>, _ applicationFeatures: YSArray<FeatureID>) -> Bool {
    return MarkableImportantFeature.get.includedAll(modelFeatures, applicationFeatures)
  }

  @discardableResult
  open func canBePerformed(_ model: App) -> Bool {
    let messageListModel = MessageListDisplayFeature.get.forceCast(model)
    let messages = messageListModel.getMessageList(10)
    return order < messages.length && canBePerformedImpl(messages[self.order])
  }

  @discardableResult
  open func perform(_ model: App, _ application: App, _ history: MBTHistory) -> MBTComponent {
    performImpl(MarkableImportantFeature.get.forceCast(model))
    performImpl(MarkableImportantFeature.get.forceCast(application))
    return history.currentComponent
  }

  @discardableResult
  open func canBePerformedImpl(_: MessageView) -> Bool {
    fatalError("Must be overridden in subclasses")
  }

  open func performImpl(_: MarkableImportant) {
    fatalError("Must be overridden in subclasses")
  }

  @discardableResult
  open func events() -> YSArray<TestopithecusEvent> {
    fatalError("Must be overridden in subclasses")
  }

  @discardableResult
  open func tostring() -> String {
    fatalError("Must be overridden in subclasses")
  }

  @discardableResult
  open func getActionType() -> MBTActionType {
    return type
  }
}

open class MarkAsImportant: BaseLabelAction {
  public static let type: MBTActionType = "MarkAsImportant"
  public init(_ order: Int32) {
    super.init(order, MarkAsImportant.type)
  }

  @discardableResult
  open class func canMarkImportant(_ message: MessageView) -> Bool {
    return !message.important
  }

  @discardableResult
  open override func canBePerformedImpl(_ message: MessageView) -> Bool {
    return MarkAsImportant.canMarkImportant(message)
  }

  open override func performImpl(_ modelOrApplication: MarkableImportant) {
    return modelOrApplication.markAsImportant(order)
  }

  @discardableResult
  open override func events() -> YSArray<TestopithecusEvent> {
    return YSArray(Testopithecus.messageListEvents.openMessageActions(order, int64(-1)), Testopithecus.messageActionsEvents.markAsImportant())
  }

  @discardableResult
  open override func tostring() -> String {
    return "MarkAsImportant(#\(order))"
  }
}

open class MarkAsUnimportant: BaseLabelAction {
  public static let type: MBTActionType = "MarkAsImportant"
  public init(_ order: Int32) {
    super.init(order, MarkAsUnimportant.type)
  }

  @discardableResult
  open class func canMarkUnimportant(_ message: MessageView) -> Bool {
    return message.important
  }

  @discardableResult
  open override func canBePerformedImpl(_ message: MessageView) -> Bool {
    return MarkAsUnimportant.canMarkUnimportant(message)
  }

  open override func performImpl(_ modelOrApplication: MarkableImportant) {
    return modelOrApplication.markAsUnimportant(order)
  }

  @discardableResult
  open override func events() -> YSArray<TestopithecusEvent> {
    return YSArray(Testopithecus.messageListEvents.openMessageActions(order, int64(-1)), Testopithecus.messageActionsEvents.markAsNotImportant())
  }

  @discardableResult
  open override func tostring() -> String {
    return "MarkAsUnimportant(#\(order))"
  }
}
