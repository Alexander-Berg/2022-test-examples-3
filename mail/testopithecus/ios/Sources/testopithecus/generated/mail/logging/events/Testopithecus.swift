// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM mail/logging/events/testopithecus.ts >>>

import Foundation

open class Testopithecus {
  public static var startEvents: StartEvents = StartEvents()
  public static var messageViewEvents: MessageEvents = MessageEvents()
  public static var groupActionsEvents: GroupActionsEvents = GroupActionsEvents()
  public static var messageActionsEvents: MessageActionsEvents = MessageActionsEvents()
  public static var messageListEvents: MessageListEvents = MessageListEvents()
  public static var composeEvents: ComposeEvents = ComposeEvents()
  public static var pushEvents: PushEvents = PushEvents()
  public static var modelSyncEvents: ModelSyncEvents = ModelSyncEvents()
  @discardableResult
  open class func errorEvent(_ reason: String) -> TestopithecusEvent {
    return TestopithecusEvent(EventNames.ERROR, ValueMapBuilder.customEvent("error").addError().addReason(reason))
  }

  @discardableResult
  open class func eventCreationErrorEvent(_ event: String, _ reason: String) -> TestopithecusEvent {
    return TestopithecusEvent(EventNames.ERROR, ValueMapBuilder.customEvent("error").addError().addReason(reason).addEvent(event))
  }

  @discardableResult
  open class func stubEvent() -> TestopithecusEvent {
    return TestopithecusEvent(EventNames.STUB, ValueMapBuilder.customEvent("stub"))
  }

  @discardableResult
  open class func debugEvent(_ value: YSMap<String, JSONItem> = YSMap()) -> TestopithecusEvent {
    return TestopithecusEvent(EventNames.DEBUG, ValueMapBuilder.customEvent("debug", value).addDebug())
  }
}