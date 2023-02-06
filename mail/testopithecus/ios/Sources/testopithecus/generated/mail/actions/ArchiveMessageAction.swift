// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM mail/actions/archive-message-action.ts >>>

import Foundation

open class ArchiveMessageAction: MBTAction {
  public static let type: MBTActionType = "ArchiveMessage"
  private var order: Int32
  public init(_ order: Int32) {
    self.order = order
  }

  @discardableResult
  open func supported(_ modelFeatures: YSArray<FeatureID>, _ applicationFeatures: YSArray<FeatureID>) -> Bool {
    return MessageListDisplayFeature.get.included(modelFeatures) && FolderNavigatorFeature.get.included(modelFeatures) && ArchiveMessageFeature.get.includedAll(modelFeatures, applicationFeatures)
  }

  @discardableResult
  open func canBePerformed(_ model: App) -> Bool {
    let messageListDisplayModel = MessageListDisplayFeature.get.forceCast(model)
    let messages = messageListDisplayModel.getMessageList(order + 1)
    let currentFolder = FolderNavigatorFeature.get.forceCast(model).getCurrentFolder()
    return currentFolder.name != DefaultFolderName.archive && order < messages.length
  }

  @discardableResult
  open func perform(_ model: App, _ application: App, _ history: MBTHistory) -> MBTComponent {
    ArchiveMessageFeature.get.forceCast(model).archiveMessage(order)
    ArchiveMessageFeature.get.forceCast(application).archiveMessage(order)
    return history.currentComponent
  }

  @discardableResult
  open func getActionType() -> MBTActionType {
    return ArchiveMessageAction.type
  }

  @discardableResult
  open func tostring() -> String {
    return "MoveToArchive(#\(order))"
  }

  @discardableResult
  open func events() -> YSArray<TestopithecusEvent> {
    return YSArray(Testopithecus.messageListEvents.openMessageActions(order, int64(-1)), Testopithecus.messageActionsEvents.archive())
  }
}