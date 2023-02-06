package ru.yandex.autotests.innerpochta.util.handlers;

/**
 * Created by mabelpines on 13.05.15.
 */
public class FiltersConstants {
    //Handlers:
    public static final String HANDLER_DO_FILTERS_ADD = "do-filters-add";
    public static final String HANDLER_DO_FILTERS_DELETE = "do-filters-delete";
    public static final String HANDLER_FILTERS = "filters";
    public static final String HANDLER_UNSUBSCRIBE_FURITA_FILTERS = "unsubscribe-furita-filters";
    public static final String HANDLER_DO_UNSUBSCRIBE_FURITA_FILTERS_DELETE = "do-unsubscribe-furita-filters-delete";
    public static final String HANDLER_DO_FILTERS_BLACKLIST_ADD = "do-filters-blacklist-add";
    public static final String HANDLER_DO_FILTERS_BLACKLIST = "filters-blacklist";
    public static final String HANDLER_DO_FILTERS_BLACKLIST_REMOVE = "do-filters-blacklist-remove";
    public static final String HANDLER_DO_UNSUBSCRIBE_FURITA_FILTERS_CREATE = "do-unsubscribe-furita-filters-create";
    public static final String HANDLER_DO_FILTERS_WHITELIST = "filters-whitelist";
    public static final String HANDLER_DO_FILTERS_WHITELIST_ADD = "do-filters-whitelist-add";
    public static final String HANDLER_DO_FILTERS_WHITELIST_REMOVE = "do-filters-whitelist-remove";
    //Do-filters-add, filters params
    //attachment params
    public static final String FILTERS_ADD_PARAM_ATTACHMENT = "attachment.0";
    public static final String FILTERS_ADD_PARAM_ATTACHMENT_TO_ALL = ""; //to all letters
    public static final String FILTERS_ADD_PARAM_WITH_ATTACHMENT = "1";
    public static final String FILTERS_ADD_PARAM_NO_ATTACHES = "2"; //letters without attachment

    //clicker params (filter type)
    public static final String FILTERS_ADD_PARAM_CLICKER = "clicker.0";
    public static final String FILTERS_ADD_PARAM_CLICKER_MOVE = "move"; //move_folder
    public static final String FILTERS_ADD_PARAM_CLICKER_MOVEL = "movel"; //move_label

    //field1 params (target objects to impliment conditions)
    public static final String FILTERS_ADD_PARAM_FIELD1 = "field1.0";
    public static final String FILTERS_ADD_PARAM_FIELD1_FROM = "from";
    public static final String FILTERS_ADD_PARAM_FIELD1_TO = "to";
    public static final String FILTERS_ADD_PARAM_FIELD1_CC = "cc";
    public static final String FILTERS_ADD_PARAM_FIELD1_SUBJECT = "subject";
    public static final String FILTERS_ADD_PARAM_FIELD1_BODY = "body";
    public static final String FILTERS_ADD_PARAM_FIELD1_FILENAME = "filename";

    //field2 params (conditions)
    public static final String FILTERS_ADD_PARAM_FIELD2 = "field2.0";
    public static final String FILTERS_ADD_PARAM_FIELD2_COINCIDE = "1"; //совпадает
    public static final String FILTERS_ADD_PARAM_FIELD2_DIFF = "2"; //не совпадает
    public static final String FILTERS_ADD_PARAM_FIELD2_CONTAINS = "3"; //содержит
    public static final String FILTERS_ADD_PARAM_FIELD2_NOT_CONTAINS = "4"; // не содержит

    //field3 params (pattern)
    public static final String FILTERS_ADD_PARAM_FIELD3 = "field3.0";

    //letter params (letter list to implement acondition)
    public static final String FILTERS_ADD_PARAM_LETTER = "letter.0";
    public static final String FILTERS_ADD_PARAM_ALL = "all"; //to all letters
    public static final String FILTERS_APP_PARAM_NOSPAM = "nospam"; //to all letters except spam
    public static final String FILTERS_ADD_PARAM_CLEARSPAM = "clearspam"; //to spam only

    //logic params
    public static final String FILTERS_ADD_PARAM_LOGIC = "logic.0";
    public static final String FILTERS_ADD_PARAM_LOGIC_OR = "0";
    public static final String FILTERS_ADD_PARAM_LOGIC_AND = "1";

    public static final String FILTERS_ADD_PARAM_MOVE_FOLDER = "move_folder.0";
    public static final String FILTERS_ADD_PARAM_MOVE_LABEL = "move_label.0";
    public static final String FILTERS_ADD_PARAM_NAME = "name.0";
    public static final String FILTERS_ADD_PARAM_DEFAULT_NAME = "Моё правило";

    public static final String FILTERS_PARAM_ID = "id.0";
    public static final String UNSUBSCRIBE_FILTERS_PARAM_ID = "filterIds.0";
    public static final String FILTERS_PARAM_EMAIL = "email.0";
    public static final String UNSUBSCRIBE_FILTERS_PARAM_MAILLIST = "maillistList.0";

    //handler query params
    public static final String FILTERS_HANDLER_QUERY_PARAM_1 = "client_name";
    public static final String FILTERS_HANDLER_QUERY_PARAM_2 = "client_version";

    //handler query params values
    public static final String FILTERS_HANDLER_QUERY_PARAM_1_VAL = "SUBSCRIPTIONS";
    public static final String FILTERS_HANDLER_QUERY_PARAM_2_VAL = "2.10.9";

    public static final String CONTENT_TYPE = "application/json";
}
