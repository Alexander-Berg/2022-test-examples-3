import { range } from '../../../ys/ys'
import { App, MBTAction, MBTComponent, MBTComponentType } from '../../mbt/mbt-abstractions'
import { MBTComponentActions } from '../../mbt/walk/behaviour/user-behaviour'
import { assertBooleanEquals } from '../../utils/assert'
import { log } from '../../utils/logger'
import { max, min } from '../../utils/utils'
import { ArchiveMessageAction } from '../actions/archive-message-action'
import { ClearCache } from '../actions/clear-cache'
import { GoToFolderAction } from '../actions/folder-navigator-actions'
import { SelectMessage } from '../actions/group-mode-actions'
import { MarkAsImportant, MarkAsUnimportant } from '../actions/labeled-actions'
import { MarkAsRead, MarkAsUnread } from '../actions/markable-actions'
import { OpenMessage } from '../actions/open-message'
import { RotatableAction } from '../actions/rotatable-actions'
import { DeleteMessageByShortSwipe } from '../actions/short-swipe-delete-action'
import { MoveToSpamAction } from '../actions/spamable-actions'
import {
  CollapseThreadAction,
  ExpandThreadAction,
  MarkAsReadExpandedAction,
  MarkAsUnreadExpandedAction,
} from '../actions/thread-markable-actions'
import { OpenComposeAction } from '../actions/write-message-actions'
import {
  ArchiveMessageFeature,
  ExpandableThreadsModelFeature,
  FolderNavigatorFeature,
  GroupModeFeature,
  MessageListDisplayFeature, ShortSwipeDeleteFeature,
  SpamableFeature,
} from '../mail-features'
import { DefaultFolderName, Message } from '../model/mail-model'

export class MaillistComponent implements MBTComponent {
  public static readonly type: MBTComponentType = 'MaillistComponent'

  public constructor() {
  }

  public assertMatches(model: App, application: App): void {
    const messageListModel = MessageListDisplayFeature.get.castIfSupported(model)
    const messageListApplication = MessageListDisplayFeature.get.castIfSupported(application)
    if (messageListModel === null || messageListApplication === null) {
      return
    }

    log(`Model and application comparison started`)
    let comparedMessages = 0
    const actualMessages = messageListApplication.getMessageList(10)
    const expectedMessages = messageListModel.getMessageList(10)
    for (const i of range(0, min(max(actualMessages.length, 1), expectedMessages.length))) {
      if (i >= actualMessages.length) {
        throw new Error(`There is expected to be message at position ${i} but there was not`)
      }
      const actual = actualMessages[i]
      const expected = expectedMessages[i]
      log(`№${i}: expected=${expected.tostring()} actual=${actual.tostring()}`)
      if (!Message.matches(expected, actual)) {
        throw new Error(`Messages are different at position ${i} expected=${expected.tostring()} actual=${actual.tostring()}`)
      } else {
        comparedMessages += 1
      }
    }
    // const actualUnreadCounter = messageListApplication.unreadCounter(this.folder);
    // const expectedUnreadCounter = messageListModel.unreadCounter(this.folder);
    // if (actualUnreadCounter !== expectedUnreadCounter) {
    //   throw new Error(`Number of unread messages are different expected=${expectedUnreadCounter} actual=${actualUnreadCounter}`)
    // }

    log(`Message view is ok, compared: ${comparedMessages}`)

    const editorModel = GroupModeFeature.get.castIfSupported(model)
    const editorApplication = GroupModeFeature.get.castIfSupported(application)
    if (editorModel !== null && editorApplication !== null) {
      const modelGroupMode = editorModel.isInGroupMode()
      const applicationGroupMode = editorApplication.isInGroupMode()
      if (modelGroupMode !== applicationGroupMode) {
        throw new Error(`Group mode is different, expected=${modelGroupMode} actual=${applicationGroupMode}`)
      }
      log(`Groupe mode is ok, state: ${modelGroupMode}`)
    }

    let comparedThreads = 0
    const expandableThreadsModel = ExpandableThreadsModelFeature.get.castIfSupported(model)
    const expandableThreadsApplication = ExpandableThreadsModelFeature.get.castIfSupported(application)
    if (expandableThreadsModel !== null && expandableThreadsApplication !== null) {
      const expectedMessages = messageListModel.getMessageList(10)
      for (const threadOrder of range(0, expectedMessages.length)) {
        if (expandableThreadsModel.isExpanded(threadOrder)) {
          comparedThreads += 1
          const modelMessagesInThread = expandableThreadsModel.getMessagesInThread(threadOrder)
          const appMessagesInThread = expandableThreadsApplication.getMessagesInThread(threadOrder)
          for (const messageInThreadOrder of range(0, modelMessagesInThread.length)) {
            const expected = modelMessagesInThread[messageInThreadOrder]
            const actual = appMessagesInThread[messageInThreadOrder]
            if (actual.read !== expected.read) {
              throw new Error(`Messages are different at thread position ${threadOrder}, message position ${messageInThreadOrder} expected=${expected.tostring()} actual=${actual.tostring()}`)
            }
          }
        }
      }
    }

    const spamableModel = SpamableFeature.get.castIfSupported(model)
    const spamableApplication = SpamableFeature.get.castIfSupported(application)
    if (spamableModel !== null && spamableApplication !== null) {
      assertBooleanEquals(spamableModel.toastShown(), spamableApplication.toastShown(), `Toast about move to spam`)
    }

    const archiveMessageModel = ArchiveMessageFeature.get.castIfSupported(model)
    const archiveMessageApplication = ArchiveMessageFeature.get.castIfSupported(application)
    if (archiveMessageModel !== null && archiveMessageApplication !== null) {
      assertBooleanEquals(archiveMessageModel.toastShown(), archiveMessageApplication.toastShown(), `Toast about move to archive`)
    }

    const shortSwipeDeleteModel = ShortSwipeDeleteFeature.get.castIfSupported(model)
    const shortSwipeDeleteApplication = ShortSwipeDeleteFeature.get.castIfSupported(application)
    if (shortSwipeDeleteModel !== null && shortSwipeDeleteApplication !== null) {
      assertBooleanEquals(shortSwipeDeleteModel.toastShown(), shortSwipeDeleteApplication.toastShown(), `Toast about move to trash`)
    }

    log(`Model and application are equal, compared ${comparedMessages} messages and ${comparedThreads} expanded threads`)
  }

  public getComponentType(): string {
    return MaillistComponent.type
  }

  public tostring(): string {
    return `MaillistComponent()`
  }
}

export class AllMaillistActions implements MBTComponentActions {
  public getActions(model: App): MBTAction[] {
    const actions: MBTAction[] = []

    MessageListDisplayFeature.get.performIfSupported(model, (mailboxModel) => {
      const messages = mailboxModel.getMessageList(3)
      for (const i of range(0, messages.length)) {
        actions.push(new MarkAsRead(i))
        actions.push(new MarkAsUnread(i))
        actions.push(new OpenMessage(i))
        actions.push(new ExpandThreadAction(i))
        actions.push(new CollapseThreadAction(i))
        actions.push(new MarkAsReadExpandedAction(i, 0))
        actions.push(new MarkAsUnreadExpandedAction(i, 0))
        actions.push(new MarkAsImportant(i))
        actions.push(new MarkAsUnimportant(i))
        actions.push(new MoveToSpamAction(i))
        actions.push(new SelectMessage(i))
        actions.push(new ArchiveMessageAction(i))
        actions.push(new DeleteMessageByShortSwipe(i))
      }
    })
    FolderNavigatorFeature.get.performIfSupported(model, (mailboxModel) => {
      // TODO mailboxModel.getFoldersList()
      const folders = [DefaultFolderName.inbox, DefaultFolderName.sent, DefaultFolderName.trash, DefaultFolderName.spam]
      for (const folder of folders) {
        actions.push(new GoToFolderAction(folder))
      }
    })
    actions.push(new ClearCache())
    RotatableAction.addActions(actions)
    actions.push(new OpenComposeAction())

    return actions
  }
}
