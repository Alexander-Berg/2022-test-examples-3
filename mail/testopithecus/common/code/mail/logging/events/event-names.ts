export class EventNames {

  // Application start events
  public static START_WITH_MESSAGE_LIST: string = 'START_WITH_MESSAGE_LIST';
  public static START_FROM_MESSAGE_NOTIFICATION: string = 'START_FROM_MESSAGE_NOTIFICATION';
  public static START_FROM_WIDGET: string = 'START_FROM_WIDGET';

  // Message list events
  public static LIST_MESSAGE_OPEN: string = 'LIST_MESSAGE_OPEN';
  public static LIST_MESSAGE_DELETE: string = 'LIST_MESSAGE_DELETE';
  public static LIST_MESSAGE_OPEN_ACTIONS: string = 'LIST_MESSAGE_OPEN_ACTIONS';
  public static LIST_MESSAGE_MARK_AS_READ: string = 'LIST_MESSAGE_MARK_AS_READ';
  public static LIST_MESSAGE_MARK_AS_UNREAD: string = 'LIST_MESSAGE_MARK_AS_UNREAD';
  public static LIST_MESSAGE_REFRESH: string = 'LIST_MESSAGE_REFRESH';
  public static LIST_MESSAGE_WRITE_NEW_MESSAGE: string = 'LIST_MESSAGE_WRITE_NEW_MESSAGE';

  // Group actions events
  public static GROUP_MESSAGE_SELECT: string = 'GROUP_MESSAGE_SELECT';
  public static GROUP_MESSAGE_DESELECT: string = 'GROUP_MESSAGE_DESSELECT';
  public static GROUP_DELETE_SELECTED: string = 'GROUP_DELETE_SELECTED';
  public static GROUP_MARK_AS_READ_SELECTED: string = 'GROUP_MARK_AS_READ_SELECTED';
  public static GROUP_MARK_AS_UNREAD_SELECTED: string = 'GROUP_MARK_AS_UNREAD_SELECTED';

  // Message actions events
  public static MESSAGE_ACTION_REPLY: string = 'MESSAGE_ACTION_REPLY';
  public static MESSAGE_ACTION_REPLY_ALL: string = 'MESSAGE_ACTION_REPLY_ALL';
  public static MESSAGE_ACTION_FORWARD: string = 'MESSAGE_ACTION_FORWARD';
  public static MESSAGE_ACTION_DELETE: string = 'MESSAGE_ACTION_DELETE';
  public static MESSAGE_ACTION_MARK_AS_READ: string = 'MESSAGE_ACTION_MARK_AS_READ';
  public static MESSAGE_ACTION_MARK_AS_UNREAD: string = 'MESSAGE_ACTION_MARK_AS_UNREAD';
  public static MESSAGE_ACTION_MARK_AS_IMPORTANT: string = 'MESSAGE_ACTION_MARK_AS_IMPORTANT';
  public static MESSAGE_ACTION_MARK_AS_NOT_IMPORTANT: string = 'MESSAGE_ACTION_MARK_AS_NOT_IMPORTANT';
  public static MESSAGE_ACTION_MARK_AS_SPAM: string = 'MESSAGE_ACTION_MARK_AS_SPAM';
  public static MESSAGE_ACTION_MARK_AS_NOT_SPAM: string = 'MESSAGE_ACTION_MARK_AS_NOT_SPAM';
  public static MESSAGE_ACTION_MOVE_TO_FOLDER: string = 'MESSAGE_ACTION_MOVE_TO_FOLDER';
  public static MESSAGE_ACTION_MARK_AS: string = 'MESSAGE_ACTION_MARK_AS';
  public static MESSAGE_ACTION_ARCHIVE: string = 'MESSAGE_ACTION_ARCHIVE';
  public static MESSAGE_ACTION_CANCEL: string = 'MESSAGE_ACTION_CANCEL';

  // Message events
  public static MESSAGE_VIEW_BACK: string = 'MESSAGE_VIEW_BACK';
  public static MESSAGE_VIEW_DELETE: string = 'MESSAGE_VIEW_DELETE';
  public static MESSAGE_VIEW_REPLY: string = 'MESSAGE_VIEW_REPLY';
  public static MESSAGE_VIEW_REPLY_ALL: string = 'MESSAGE_VIEW_REPLY_ALL';
  public static MESSAGE_VIEW_EDIT_DRAFT: string = 'MESSAGE_VIEW_EDIT_DRAFT';
  public static MESSAGE_VIEW_OPEN_ACTIONS: string = 'MESSAGE_VIEW_OPEN_ACTIONS';

  // Compose events
  public static COMPOSE_ADD_RECEIVER: string = 'COMPOSE_ADD_RECEIVER';
  public static COMPOSE_REMOVE_RECEIVER: string = 'COMPOSE_REMOVE_RECEIVER';
  public static COMPOSE_SET_SUBJECT: string = 'COMPOSE_SET_SUBJECT';
  public static COMPOSE_SET_BODY: string = 'COMPOSE_SET_BODY';
  public static COMPOSE_EDIT_BODY: string = 'COMPOSE_EDIT_BODY';
  public static COMPOSE_ADD_ATTACHMENTS: string = 'COMPOSE_ADD_ATTACHMENTS';
  public static COMPOSE_REMOVE_ATTACHMENT: string = 'COMPOSE_REMOVE_ATTACHMENT';
  public static COMPOSE_SEND_MESSAGE: string = 'COMPOSE_SEND_MESSAGE';
  public static COMPOSE_BACK: string = 'COMPOSE_BACK';

  // Pushes events
  public static PUSH_MESSAGES_RECEIVED_SHOWN: string = 'PUSH_MESSAGES_RECEIVED_SHOWN';
  public static PUSH_SINGLE_MESSAGE_CLICKED: string = 'PUSH_MESSAGE_CLICKED';
  public static PUSH_REPLY_MESSAGE_CLICKED: string = 'PUSH_REPLY_MESSAGE_CLICKED';
  public static PUSH_SMART_REPLY_MESSAGE_CLICKED: string = 'PUSH_SMART_REPLY_MESSAGE_CLICKED';
  public static PUSH_THREAD_CLICKED: string = 'PUSH_THREAD_CLICKED';
  public static PUSH_FOLDER_CLICKED: string = 'PUSH_FOLDER_CLICKED';

  // Model sync events
  public static MODEL_SYNC_MESSAGE_LIST: string = 'MODEL_SYNC_MESSAGE_LIST';

  // Additional events
  public static STUB: string = 'STUB';
  public static ERROR: string = 'ERROR';
  public static DEBUG: string = 'DEBUG';

}
