// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM events/event-names.ts >>>

package com.yandex.xplat.eventus

public open class EventNames {
    companion object {
        @JvmStatic var START_WITH_MESSAGE_LIST: String = "START_WITH_MESSAGE_LIST"
        @JvmStatic var START_FROM_MESSAGE_NOTIFICATION: String = "START_FROM_MESSAGE_NOTIFICATION"
        @JvmStatic var START_FROM_WIDGET: String = "START_FROM_WIDGET"
        @JvmStatic var LIST_MESSAGE_OPEN: String = "LIST_MESSAGE_OPEN"
        @JvmStatic var LIST_MESSAGE_DELETE: String = "LIST_MESSAGE_DELETE"
        @JvmStatic var LIST_MESSAGE_OPEN_ACTIONS: String = "LIST_MESSAGE_OPEN_ACTIONS"
        @JvmStatic var LIST_MESSAGE_MARK_AS_READ: String = "LIST_MESSAGE_MARK_AS_READ"
        @JvmStatic var LIST_MESSAGE_MARK_AS_UNREAD: String = "LIST_MESSAGE_MARK_AS_UNREAD"
        @JvmStatic var LIST_MESSAGE_REFRESH: String = "LIST_MESSAGE_REFRESH"
        @JvmStatic var LIST_MESSAGE_WRITE_NEW_MESSAGE: String = "LIST_MESSAGE_WRITE_NEW_MESSAGE"
        @JvmStatic var LIST_MESSAGE_REPLY: String = "LIST_MESSAGE_REPLY"
        @JvmStatic var LIST_MESSAGE_REPLY_ALL: String = "LIST_MESSAGE_REPLY_ALL"
        @JvmStatic var LIST_MESSAGE_FORWARD: String = "LIST_MESSAGE_FORWARD"
        @JvmStatic var LIST_MESSAGE_MARK_AS_IMPORTANT: String = "LIST_MESSAGE_MARK_AS_IMPORTANT"
        @JvmStatic var LIST_MESSAGE_MARK_AS_NOT_IMPORTANT: String = "LIST_MESSAGE_MARK_AS_NOT_IMPORTANT"
        @JvmStatic var LIST_MESSAGE_MARK_AS_SPAM: String = "LIST_MESSAGE_MARK_AS_SPAM"
        @JvmStatic var LIST_MESSAGE_MARK_AS_NOT_SPAM: String = "LIST_MESSAGE_MARK_AS_NOT_SPAM"
        @JvmStatic var LIST_MESSAGE_MOVE_TO_FOLDER: String = "LIST_MESSAGE_MOVE_TO_FOLDER"
        @JvmStatic var LIST_MESSAGE_MARK_AS: String = "LIST_MESSAGE_MARK_AS"
        @JvmStatic var LIST_MESSAGE_ARCHIVE: String = "LIST_MESSAGE_ARCHIVE"
        @JvmStatic var GROUP_MESSAGE_SELECT: String = "GROUP_MESSAGE_SELECT"
        @JvmStatic var GROUP_MESSAGE_DESELECT: String = "GROUP_MESSAGE_DESSELECT"
        @JvmStatic var GROUP_DELETE_SELECTED: String = "GROUP_DELETE_SELECTED"
        @JvmStatic var GROUP_MARK_AS_READ_SELECTED: String = "GROUP_MARK_AS_READ_SELECTED"
        @JvmStatic var GROUP_MARK_AS_UNREAD_SELECTED: String = "GROUP_MARK_AS_UNREAD_SELECTED"
        @JvmStatic var MESSAGE_ACTION_REPLY: String = "MESSAGE_ACTION_REPLY"
        @JvmStatic var MESSAGE_ACTION_REPLY_ALL: String = "MESSAGE_ACTION_REPLY_ALL"
        @JvmStatic var MESSAGE_ACTION_FORWARD: String = "MESSAGE_ACTION_FORWARD"
        @JvmStatic var MESSAGE_ACTION_DELETE: String = "MESSAGE_ACTION_DELETE"
        @JvmStatic var MESSAGE_ACTION_MARK_AS_READ: String = "MESSAGE_ACTION_MARK_AS_READ"
        @JvmStatic var MESSAGE_ACTION_MARK_AS_UNREAD: String = "MESSAGE_ACTION_MARK_AS_UNREAD"
        @JvmStatic var MESSAGE_ACTION_MARK_AS_IMPORTANT: String = "MESSAGE_ACTION_MARK_AS_IMPORTANT"
        @JvmStatic var MESSAGE_ACTION_MARK_AS_NOT_IMPORTANT: String = "MESSAGE_ACTION_MARK_AS_NOT_IMPORTANT"
        @JvmStatic var MESSAGE_ACTION_MARK_AS_SPAM: String = "MESSAGE_ACTION_MARK_AS_SPAM"
        @JvmStatic var MESSAGE_ACTION_MARK_AS_NOT_SPAM: String = "MESSAGE_ACTION_MARK_AS_NOT_SPAM"
        @JvmStatic var MESSAGE_ACTION_MOVE_TO_FOLDER: String = "MESSAGE_ACTION_MOVE_TO_FOLDER"
        @JvmStatic var MESSAGE_ACTION_MARK_AS: String = "MESSAGE_ACTION_MARK_AS"
        @JvmStatic var MESSAGE_ACTION_ARCHIVE: String = "MESSAGE_ACTION_ARCHIVE"
        @JvmStatic var MESSAGE_ACTION_CANCEL: String = "MESSAGE_ACTION_CANCEL"
        @JvmStatic var MESSAGE_VIEW_BACK: String = "MESSAGE_VIEW_BACK"
        @JvmStatic var MESSAGE_VIEW_DELETE: String = "MESSAGE_VIEW_DELETE"
        @JvmStatic var MESSAGE_VIEW_REPLY: String = "MESSAGE_VIEW_REPLY"
        @JvmStatic var MESSAGE_VIEW_REPLY_ALL: String = "MESSAGE_VIEW_REPLY_ALL"
        @JvmStatic var MESSAGE_VIEW_EDIT_DRAFT: String = "MESSAGE_VIEW_EDIT_DRAFT"
        @JvmStatic var MESSAGE_VIEW_EDIT_TEMPLATES: String = "MESSAGE_VIEW_EDIT_TEMPLATES"
        @JvmStatic var MESSAGE_VIEW_OPEN_ACTIONS: String = "MESSAGE_VIEW_OPEN_ACTIONS"
        @JvmStatic var MESSAGE_VIEW_LINK_INTERACT: String = "MESSAGE_VIEW_LINK_INTERACT"
        @JvmStatic var COMPOSE_ADD_RECEIVER: String = "COMPOSE_ADD_RECEIVER"
        @JvmStatic var COMPOSE_REMOVE_RECEIVER: String = "COMPOSE_REMOVE_RECEIVER"
        @JvmStatic var COMPOSE_SET_SUBJECT: String = "COMPOSE_SET_SUBJECT"
        @JvmStatic var COMPOSE_SET_BODY: String = "COMPOSE_SET_BODY"
        @JvmStatic var COMPOSE_EDIT_BODY: String = "COMPOSE_EDIT_BODY"
        @JvmStatic var COMPOSE_ADD_ATTACHMENTS: String = "COMPOSE_ADD_ATTACHMENTS"
        @JvmStatic var COMPOSE_REMOVE_ATTACHMENT: String = "COMPOSE_REMOVE_ATTACHMENT"
        @JvmStatic var COMPOSE_SEND_MESSAGE: String = "COMPOSE_SEND_MESSAGE"
        @JvmStatic var COMPOSE_BACK: String = "COMPOSE_BACK"
        @JvmStatic var COMPOSE_SHOWN: String = "COMPOSE_SHOWN"
        @JvmStatic var COMPOSE_ADD_PHOTOMAIL: String = "COMPOSE_ADD_PHOTOMAIL"
        @JvmStatic var COMPOSE_ADD_SCANS: String = "COMPOSE_ADD_SCANS"
        @JvmStatic var COMPOSE_ADD_SELECTED_SCANS: String = "COMPOSE_ADD_SELECTED_SCANS"
        @JvmStatic var PUSH_MESSAGES_RECEIVED_SHOWN: String = "PUSH_MESSAGES_RECEIVED_SHOWN"
        @JvmStatic var PUSH_SINGLE_MESSAGE_CLICKED: String = "PUSH_MESSAGE_CLICKED"
        @JvmStatic var PUSH_REPLY_MESSAGE_CLICKED: String = "PUSH_REPLY_MESSAGE_CLICKED"
        @JvmStatic var PUSH_SMART_REPLY_MESSAGE_CLICKED: String = "PUSH_SMART_REPLY_MESSAGE_CLICKED"
        @JvmStatic var PUSH_THREAD_CLICKED: String = "PUSH_THREAD_CLICKED"
        @JvmStatic var PUSH_FOLDER_CLICKED: String = "PUSH_FOLDER_CLICKED"
        @JvmStatic var QUICK_REPLY_CLICKED: String = "QUICK_REPLY_CLICKED"
        @JvmStatic var QUICK_REPLY_SMART_REPLY_MESSAGE_CLICKED: String = "QUICK_REPLY_SMART_REPLY_MESSAGE_CLICKED"
        @JvmStatic var QUICK_REPLY_OPEN_COMPOSE: String = "QUICK_REPLY_OPEN_COMPOSE"
        @JvmStatic var QUICK_REPLY_SMART_REPLY_SHOWN: String = "QUICK_REPLY_SMART_REPLY_SHOWN"
        @JvmStatic var QUICK_REPLY_EDIT_BODY: String = "QUICK_REPLY_EDIT_BODY"
        @JvmStatic var QUICK_REPLY_CLOSE_SMART_REPLY_MESSAGE: String = "QUICK_REPLY_CLOSE_SMART_REPLY_MESSAGE"
        @JvmStatic var QUICK_REPLY_CLOSE_ALL_SMART_REPLIES: String = "QUICK_REPLY_CLOSE_ALL_SMART_REPLIES"
        @JvmStatic var QUICK_REPLY_SEND_MESSAGE: String = "QUICK_REPLY_SEND_MESSAGE"
        @JvmStatic var SMART_REPLY_TURNED_OFF_BY_USER: String = "SMART_REPLY_TURNED_OFF_BY_USER"
        @JvmStatic var SMART_REPLY_TURNED_ON_BY_USER: String = "SMART_REPLY_TURNED_ON_BY_USER"
        @JvmStatic var STORIES_SHOWN: String = "STORIES_SHOWN"
        @JvmStatic var STORIES_CLOSE_CLICK: String = "STORIES_CLOSE_CLICK"
        @JvmStatic var STORY_OPEN: String = "STORY_OPEN"
        @JvmStatic var STORY_ACTION_CLICK: String = "STORY_ACTION_CLICK"
        @JvmStatic var STORY_SLIDE_SHOWN: String = "STORY_SLIDE_SHOWN"
        @JvmStatic var STORY_SLIDE_PREVIOUS_CLICK: String = "STORY_SLIDE_PREVIOUS_CLICK"
        @JvmStatic var STORY_SLIDE_NEXT_CLICK: String = "STORY_SLIDE_NEXT_CLICK"
        @JvmStatic var STORY_SLIDE_PAUSE: String = "STORY_SLIDE_PAUSE"
        @JvmStatic var STORY_SLIDE_RESUME: String = "STORY_SLIDE_RESUME"
        @JvmStatic var MODEL_SYNC_MESSAGE_LIST: String = "MODEL_SYNC_MESSAGE_LIST"
        @JvmStatic var ERROR: String = "ERROR"
        @JvmStatic var DEBUG: String = "DEBUG"
        @JvmStatic var THREADING_TURNED_ON: String = "THREADING_TURNED_ON"
        @JvmStatic var THREADING_TURNED_OFF: String = "THREADING_TURNED_OFF"
        @JvmStatic var EDIT_SIGNATURE_OPEN: String = "EDIT_SIGNATURE_OPEN"
        @JvmStatic var EDIT_SIGNATURE: String = "EDIT_SIGNATURE"
        @JvmStatic var EDIT_SIGNATURE_BACK: String = "EDIT_SIGNATURE_BACK"
        @JvmStatic var THEMES_TURNED_ON: String = "THEMES_TURNED_ON"
        @JvmStatic var THEMES_TURNED_OFF: String = "THEMES_TURNED_OFF"
        @JvmStatic var PUSH_NOTIFICATIONS_TURNED_OFF: String = "PUSH_NOTIFICATIONS_TURNED_OFF"
        @JvmStatic var PUSH_NOTIFICATIONS_TURNED_ON: String = "PUSH_NOTIFICATIONS_TURNED_ON"
        @JvmStatic var CLOSE_ACCOUNT_SETTINGS: String = "CLOSE_ACCOUNT_SETTINGS"
        @JvmStatic var OPEN_ACCOUNT_SETTINGS: String = "OPEN_ACCOUNT_SETTINGS"
        @JvmStatic var SEARCH_OPENED: String = "SEARCH_OPENED"
        @JvmStatic var SEARCH_BY_ZERO_SUGGEST: String = "SEARCH_BY_ZERO_SUGGEST"
        @JvmStatic var SUGGEST_CLICK: String = "SUGGEST_CLICK"
        @JvmStatic var SETTINGS_CLEAR_CACHE: String = "SETTINGS_CLEAR_CACHE"
        @JvmStatic var SETTINGS_COMPACT_MODE_ON: String = "SETTINGS_CLEAR_CACHE"
        @JvmStatic var SETTINGS_COMPACT_MODE_OFF: String = "SETTINGS_CLEAR_CACHE"
        @JvmStatic var MULTIACCOUNT_ADD_NEW_ACCOUNT: String = "MULTIACCOUNT_ADD_NEW_ACCOUNT"
        @JvmStatic var MULTIACCOUNT_LOGOUT_FROM_ACCOUNT: String = "MULTIACCOUNT_LOGOUT_FROM_ACCOUNT"
        @JvmStatic var MULTIACCOUNT_SWITCH_TO_ACCOUNT: String = "MULTIACCOUNT_SWITCH_ACCOUNT"
        @JvmStatic var CONTACT_LIST_OPEN: String = "CONTACT_LIST_OPEN"
        @JvmStatic var CONTACT_LIST_CLOSE: String = "CONTACT_LIST_CLOSE"
        @JvmStatic var CONTACT_LIST_CLICK: String = "CONTACT_LIST_CLICK"
        @JvmStatic var CONTACT_LIST_MODE_CHANGE: String = "CONTACT_LIST_MODE_CHANGE"
        @JvmStatic var CONTACT_LIST_CREATE: String = "CONTACT_LIST_CREATE"
        @JvmStatic var CONTACT_LIST_SEARCH: String = "CONTACT_LIST_SEARCH"
        @JvmStatic var CONTACT_CREATE_OPEN: String = "CONTACT_CREATE_OPEN"
        @JvmStatic var CONTACT_CREATE_CLOSE: String = "CONTACT_CREATE_CLOSE"
        @JvmStatic var CONTACT_CREATE_SUBMIT: String = "CONTACT_CREATE_SUBMIT"
        @JvmStatic var CONTACT_DETAILS_OPEN: String = "CONTACT_DETAILS_OPEN"
        @JvmStatic var CONTACT_DETAILS_INIT: String = "CONTACT_DETAILS_INIT"
        @JvmStatic var CONTACT_DETAILS_EDIT: String = "CONTACT_DETAILS_EDIT"
        @JvmStatic var CONTACT_DETAILS_DELETE: String = "CONTACT_DETAILS_DELETE"
        @JvmStatic var CONTACT_DETAILS_CLICK: String = "CONTACT_DETAILS_CLICK"
        @JvmStatic var CONTACT_DETAILS_COPY: String = "CONTACT_DETAILS_COPY"
        @JvmStatic var CONTACT_DETAILS_CLOSE: String = "CONTACT_DETAILS_CLOSE"
        @JvmStatic var CONTACT_EDIT_OPEN: String = "CONTACT_EDIT_OPEN"
        @JvmStatic var CONTACT_EDIT_CLOSE: String = "CONTACT_EDIT_CLOSE"
        @JvmStatic var CONTACT_EDIT_SUBMIT: String = "CONTACT_EDIT_SUBMIT"
        @JvmStatic var ECOMAIL_SERVICE_INIT: String = "ECOMAIL_SERVICE_INIT"
        @JvmStatic var ECOMAIL_SERVICE_OPEN: String = "ECOMAIL_SERVICE_OPEN"
        @JvmStatic var ECOMAIL_SERVICE_CLOSE: String = "ECOMAIL_SERVICE_CLOSE"
        @JvmStatic var PHISHING_OPEN_MESSAGE: String = "PHISHING_OPEN_MESSAGE"
        @JvmStatic var PHISHING_MORE_INFO_CLICK: String = "PHISHING_MORE_INFO_CLICK"
        @JvmStatic var PHISHING_DELETE_MESSAGE: String = "PHISHING_DELETE_MESSAGE"
    }
}
