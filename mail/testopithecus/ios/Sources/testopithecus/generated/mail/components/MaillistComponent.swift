// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM mail/components/maillist-component.ts >>>

import Foundation

open class MaillistComponent: MBTComponent {
  public static let type: MBTComponentType = "MaillistComponent"
  public init() {}

  open func assertMatches(_ model: App, _ application: App) {
    let messageListModel: MessageListDisplay! = MessageListDisplayFeature.get.castIfSupported(model)
    let messageListApplication: MessageListDisplay! = MessageListDisplayFeature.get.castIfSupported(application)
    if messageListModel == nil || messageListApplication == nil {
      return
    }
    log("Model and application comparison started")
    var comparedMessages = 0
    let actualMessages = messageListApplication.getMessageList(10)
    let expectedMessages = messageListModel.getMessageList(10)
    for i in stride(from: 0, to: min(max(actualMessages.length, 1), expectedMessages.length), by: 1) {
      if i >= actualMessages.length {
        fatalError("There is expected to be message at position \(i) but there was not")
      }
      let actual = actualMessages[i]
      let expected = expectedMessages[i]
      log("№\(i): expected=\(expected.tostring()) actual=\(actual.tostring())")
      if !Message.matches(expected, actual) {
        fatalError("Messages are different at position \(i) expected=\(expected.tostring()) actual=\(actual.tostring())")
      } else {
        comparedMessages += 1
      }
    }
    log("Message view is ok, compared: \(comparedMessages)")
    let editorModel: GroupMode! = GroupModeFeature.get.castIfSupported(model)
    let editorApplication: GroupMode! = GroupModeFeature.get.castIfSupported(application)
    if editorModel != nil, editorApplication != nil {
      let modelGroupMode = editorModel.isInGroupMode()
      let applicationGroupMode = editorApplication.isInGroupMode()
      if modelGroupMode != applicationGroupMode {
        fatalError("Group mode is different, expected=\(modelGroupMode) actual=\(applicationGroupMode)")
      }
      log("Groupe mode is ok, state: \(modelGroupMode)")
    }
    var comparedThreads = 0
    let expandableThreadsModel: ReadOnlyExpandableThreads! = ExpandableThreadsModelFeature.get.castIfSupported(model)
    let expandableThreadsApplication: ReadOnlyExpandableThreads! = ExpandableThreadsModelFeature.get.castIfSupported(application)
    if expandableThreadsModel != nil, expandableThreadsApplication != nil {
      let expectedMessages = messageListModel.getMessageList(10)
      for threadOrder in stride(from: 0, to: expectedMessages.length, by: 1) {
        if expandableThreadsModel.isExpanded(threadOrder) {
          comparedThreads += 1
          let modelMessagesInThread = expandableThreadsModel.getMessagesInThread(threadOrder)
          let appMessagesInThread = expandableThreadsApplication.getMessagesInThread(threadOrder)
          for messageInThreadOrder in stride(from: 0, to: modelMessagesInThread.length, by: 1) {
            let expected = modelMessagesInThread[messageInThreadOrder]
            let actual = appMessagesInThread[messageInThreadOrder]
            if actual.read != expected.read {
              fatalError("Messages are different at thread position \(threadOrder), message position \(messageInThreadOrder) expected=\(expected.tostring()) actual=\(actual.tostring())")
            }
          }
        }
      }
    }
    let spamableModel: Spamable! = SpamableFeature.get.castIfSupported(model)
    let spamableApplication: Spamable! = SpamableFeature.get.castIfSupported(application)
    if spamableModel != nil, spamableApplication != nil {
      assertBooleanEquals(spamableModel.toastShown(), spamableApplication.toastShown(), "Toast about move to spam")
    }
    let archiveMessageModel: ArchiveMessage! = ArchiveMessageFeature.get.castIfSupported(model)
    let archiveMessageApplication: ArchiveMessage! = ArchiveMessageFeature.get.castIfSupported(application)
    if archiveMessageModel != nil, archiveMessageApplication != nil {
      assertBooleanEquals(archiveMessageModel.toastShown(), archiveMessageApplication.toastShown(), "Toast about move to archive")
    }
    let shortSwipeDeleteModel: ShortSwipeDelete! = ShortSwipeDeleteFeature.get.castIfSupported(model)
    let shortSwipeDeleteApplication: ShortSwipeDelete! = ShortSwipeDeleteFeature.get.castIfSupported(application)
    if shortSwipeDeleteModel != nil, shortSwipeDeleteApplication != nil {
      assertBooleanEquals(shortSwipeDeleteModel.toastShown(), shortSwipeDeleteApplication.toastShown(), "Toast about move to trash")
    }
    log("Model and application are equal, compared \(comparedMessages) messages and \(comparedThreads) expanded threads")
  }

  @discardableResult
  open func getComponentType() -> String {
    return MaillistComponent.type
  }

  @discardableResult
  open func tostring() -> String {
    return "MaillistComponent()"
  }
}

open class AllMaillistActions: MBTComponentActions {
  @discardableResult
  open func getActions(_ model: App) -> YSArray<MBTAction> {
    let actions: YSArray<MBTAction> = YSArray()
    MessageListDisplayFeature.get.performIfSupported(model) {
      mailboxModel in
      let messages = mailboxModel.getMessageList(3)
      for i in stride(from: 0, to: messages.length, by: 1) {
        actions.push(MarkAsRead(i))
        actions.push(MarkAsUnread(i))
        actions.push(OpenMessage(i))
        actions.push(ExpandThreadAction(i))
        actions.push(CollapseThreadAction(i))
        actions.push(MarkAsReadExpandedAction(i, 0))
        actions.push(MarkAsUnreadExpandedAction(i, 0))
        actions.push(MarkAsImportant(i))
        actions.push(MarkAsUnimportant(i))
        actions.push(MoveToSpamAction(i))
        actions.push(SelectMessage(i))
        actions.push(ArchiveMessageAction(i))
        actions.push(DeleteMessageByShortSwipe(i))
      }
    }
    FolderNavigatorFeature.get.performIfSupported(model) {
      _ in
      let folders = YSArray(DefaultFolderName.inbox, DefaultFolderName.sent, DefaultFolderName.trash, DefaultFolderName.spam)
      for folder in folders {
        actions.push(GoToFolderAction(folder))
      }
    }
    actions.push(ClearCache())
    RotatableAction.addActions(actions)
    actions.push(OpenComposeAction())
    return actions
  }
}
