package ru.yandex.autotests.innerpochta.util.handlers;

import java.util.Arrays;
import java.util.List;

/**
 * @author mabelpines
 */
public class HandlersParamNameConstants {
    public static final String PARAM_MODEL_0 = "_model.0";
    public static final String PARAM_MODEL_1 = "_model.1";
    public static final String PARAM_HANDLER = "_handlers";
    public static final String PARAM_SHARED_0 = "shared.0";
    public static final String PARAM_EXP = "_exp";
    public static final String PARAM_EEXP = "_eexp";

    //Status names
    public static final String STATUS_ON = "on";
    public static final String STATUS_OFF = "off";
    public static final String STATUS_TRUE = "true";
    public static final String STATUS_FALSE = "false";
    public static final String STATUS_YES = "yes";
    public static final String STATUS_NO = "no";
    public static final String EMPTY_STR = "";
    public static final boolean TRUE = true;
    public static final boolean FALSE = false;
    public static final String STATUS_1 = "1";
    public static final String UNDEFINED = "undefined";

    public static final String LIST_SIGNS = "signs";
    public static final String SETTINGS = "settings";

    public static final int DEFAULT_PAGE_LETTERS_AMOUNT = 30;
    public static final int MAX_PAGE_LETTERS_AMOUNT = 200;
    public static final String DISABLED_ADV = "4133250064000";
    public static final int ENABLED_ADV = 0;

    public static final List<String> ALIAS_LIST =
        Arrays.asList("ya.ru", "yandex.by", "yandex.com", "yandex.kz", "yandex.ru", "yandex.ua");

    public static final String[] LANGS = new String[]{"Ru", "En", "Ua", "By", "Tr"};

    //Folder names
    public static final String INBOX = "inbox";
    public static final String ARCHIVE = "archive";
    public static final String DRAFT = "draft";
    public static final String SENT = "sent";
    public static final String TRASH = "trash";
    public static final String SPAM = "spam";
    public static final String TEMPLATE = "template";
    public static final String OUTBOX = "outbox";
    public static final String REPLY_LATER = "reply_later";
    public static final String TRASH_RU = "Удалённые";
    public static final String SPAM_RU = "Спам";
    public static final String INBOX_RU = "Входящие";
    public static final String ARCHIVE_RU = "Архив";
    public static final String DRAFT_RU = "Черновики";
    public static final String TRASH_RU_TOUCH = "Удаленные";

    public static final String COMPOSE_SMALL = "small";
    public static final String COMPOSE_LARGE = "large";
}
