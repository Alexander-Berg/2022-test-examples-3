import { MapJSONItem } from '../mail/logging/json-types'
import { DeleteCurrentMessage, DeleteMessageAction } from '../mail/actions/delete-message';
import { RefreshMessageListAction } from '../mail/actions/message-list-actions';
import { BackToMaillist, OpenMessage } from '../mail/actions/open-message';
import { EventNames } from '../mail/logging/events/event-names';
import { MBTAction } from '../mbt/mbt-abstractions';

export class ActionParser {

  public parseActionFromJson(json: MapJSONItem): MBTAction {
    switch (json.getString('event_name')) {
      // message list actions
      case EventNames.LIST_MESSAGE_OPEN:
        return new OpenMessage(json.getInt32('order')!);
      case EventNames.LIST_MESSAGE_DELETE:
        return new DeleteMessageAction(json.getInt32('order')!);
      case EventNames.LIST_MESSAGE_REFRESH:
        return new RefreshMessageListAction();

      // message actions
      case EventNames.MESSAGE_VIEW_BACK:
        return new BackToMaillist();
      case EventNames.MESSAGE_VIEW_DELETE:
        return new DeleteCurrentMessage();

      default:
        throw new Error(`Unknown action ${json.getString('event_name')}`);
    }
  }

  public parseActionFromString(extrasString: string): MBTAction {
    return this.parseActionFromJson(this.parse(extrasString))
  }

  // noinspection JSMethodCanBeStatic, JSUnusedLocalSymbols
  public parse(extrasString: string): MapJSONItem {
    // TODO
    return new MapJSONItem();
  }

}
