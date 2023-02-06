import {
  DeleteMessageFromContextMenuAction, MarkAsImportantFromContextMenuAction,
  MarkAsReadFromContextMenuAction, MarkAsUnreadFromContextMenuAction,
} from '../../code/mail/actions/context-menu-actions';
import { DeleteMessageAction } from '../../code/mail/actions/delete-message';
import {
  DeleteSelectedMessages,
  MarkAsReadSelectedMessages,
  MarkAsUnreadSelectedMessages,
} from '../../code/mail/actions/group-mode-actions';
import { MarkAsRead, MarkAsUnread } from '../../code/mail/actions/markable-actions';
import { RefreshMessageListAction } from '../../code/mail/actions/message-list-actions';
import { BackToMaillist, OpenMessage } from '../../code/mail/actions/open-message';
import { EventNames } from '../../code/mail/logging/events/event-names';
import { TestopithecusEvent } from '../../code/mail/logging/testopithecus-event';
import { MBTAction } from '../../code/mbt/mbt-abstractions';
import { Nullable } from '../../ys/ys';
import { ParsingUtils } from './parsing-utils'

export class ActionParser {

  private context: Map<string, any> = new Map<string, any>()

  public getAction(event: TestopithecusEvent): Nullable<MBTAction> {
    if (event.getAttributes().has('start_event')) {
      return null
    }
    switch (event.name) {
      case EventNames.LIST_MESSAGE_OPEN:
        return new OpenMessage(ParsingUtils.demandOrder(event))
      case EventNames.LIST_MESSAGE_MARK_AS_UNREAD:
        return new MarkAsUnread(ParsingUtils.demandOrder(event))
      case EventNames.LIST_MESSAGE_MARK_AS_READ:
        return new MarkAsRead(ParsingUtils.demandOrder(event))
      case EventNames.LIST_MESSAGE_DELETE:
        return new DeleteMessageAction(ParsingUtils.demandOrder(event))
      case EventNames.LIST_MESSAGE_OPEN_ACTIONS:
        this.insert('order', ParsingUtils.demandOrder(event))
        return null
      case EventNames.LIST_MESSAGE_REFRESH:
        return new RefreshMessageListAction()
      case EventNames.LIST_MESSAGE_WRITE_NEW_MESSAGE:
        throw new Error('Unsupported event')

      case EventNames.GROUP_MESSAGE_SELECT:
        throw new Error('Unsupported event')
      case EventNames.GROUP_MESSAGE_DESELECT:
        throw new Error('Unsupported event')
      case EventNames.GROUP_MARK_AS_READ_SELECTED:
        return new MarkAsReadSelectedMessages()
      case EventNames.GROUP_MARK_AS_UNREAD_SELECTED:
        return new MarkAsUnreadSelectedMessages()
      case EventNames.GROUP_DELETE_SELECTED:
        return new DeleteSelectedMessages()

      case EventNames.MESSAGE_ACTION_REPLY:
        throw new Error('Unsupported event')
      case EventNames.MESSAGE_ACTION_REPLY_ALL:
        throw new Error('Unsupported event')
      case EventNames.MESSAGE_ACTION_FORWARD:
        throw new Error('Unsupported event')
      case EventNames.MESSAGE_ACTION_DELETE:
        return new DeleteMessageFromContextMenuAction(this.extract('order'))
      case EventNames.MESSAGE_ACTION_MARK_AS_READ:
        return new MarkAsReadFromContextMenuAction(this.extract('order'))
      case EventNames.MESSAGE_ACTION_MARK_AS_UNREAD:
        return new MarkAsUnreadFromContextMenuAction(this.extract('order'))
      case EventNames.MESSAGE_ACTION_MARK_AS_IMPORTANT:
        return new MarkAsImportantFromContextMenuAction(this.extract('order'))
      case EventNames.MESSAGE_ACTION_MARK_AS_NOT_IMPORTANT:
        throw new Error('Unsupported event')
      case EventNames.MESSAGE_ACTION_MARK_AS_SPAM:
        throw new Error('Unsupported event')
      case EventNames.MESSAGE_ACTION_MARK_AS_NOT_SPAM:
        throw new Error('Unsupported event')
      case EventNames.MESSAGE_ACTION_ARCHIVE:
        throw new Error('Unsupported event')
      case EventNames.MESSAGE_ACTION_MARK_AS:
        throw new Error('Unsupported event')
      case EventNames.MESSAGE_ACTION_MOVE_TO_FOLDER:
        throw new Error('Unsupported event')
      case EventNames.MESSAGE_ACTION_CANCEL:
        throw new Error('Unsupported event')

      case EventNames.MESSAGE_VIEW_BACK:
        return new BackToMaillist()
    }
    throw new Error(`Unknown event ${event.name}`)
  }

  private insert(name: string, value: any) {
    this.context.set(name, value)
  }

  private extract(name: string): any {
    const result = this.context.get(name)
    this.context.delete(name)
    return result
  }

}
