package ru.yandex.autotests.innerpochta.data;

/**
 * User: lanwen
 * Date: 24.10.13
 * Time: 13:23
 */
public enum QuickFragments {
    /* LIZA FRAGMENTS */
    INBOX("inbox"),
    MESSAGE("message"),
    SENT("sent"),
    TRASH("trash"),
    SPAM("spam"),
    THREAD("thread"),
    DRAFT("draft"),
    TEMPLATE("template"),
    SETTINGS("setup"),
    SUBSCRIPTIONS("lenta"),
    COMPOSE("compose"),
    OUTBOX("outbox"),
    DONE("done"),
    UNREAD("unread"),
    ATTACHMENTS("attachments"),
    IMPORTANT("important"),
    FOLDER("folder"),
    LABEL("label"),
    SEARCH("search"),
    SEARCH_OPTIONS("search-options"),
    CONTACTS("contacts"),
    LITE_CONTACTS("abook"),
    ARCHIVE("archive"),
    SETTINGS_FILTERS("setup/filters"),
    SETTINGS_COLLECTORS("setup/collectors"),
    SETTINGS_FILTERS_CREATE("setup/filters-create"),
    SETTINGS_FILTERS_CREATE_SIMPLE("setup/filters-create-simple"),
    SETTINGS_FILTERS_CREATE_SIMPLE_LABEL("setup/filters-create-simple/label"),
    SETTINGS_FILTERS_CREATE_SIMPLE_DELETE("setup/filters-create-simple/delete"),
    SETTINGS_FOLDERS("setup/folders"),
    SETTINGS_SECURITY("setup/security"),
    SETTINGS_SENDER("setup/sender"),
    SETTINGS_INTERFACE("setup/interface"),
    SETTINGS_INTERFACE_2PANE("setup/interface/2pane"),
    SETTINGS_INTERFACE_3PANE_VERTICAL("setup/interface/3pane-vertical"),
    SETTINGS_INTERFACE_3PANE_HORIZONTAL("setup/interface/3pane-horizontal"),
    SETTINGS_OTHER("setup/other"),
    SETTINGS_ABOOK("setup/abook"),
    SETTINGS_CLIENT("setup/client"),
    SETTINGS_JOURNAL("setup/journal"),
    SETTINGS_TODO("setup/todo"),
    SETTINGS_DOMAIN("setup/beautiful-email"),
    INBOX_THREAD("inbox/thread"),
    MSG_FRAGMENT("message/%s"),
    COMPOSE_MSG_FRAGMENT("compose/%s"),
    SETTINGS_FILTERS_EDIT("setup/filters-create/id=%s"),
    TAB_TRIPS("categories/trips"),
    MOBILE_PROMO("mobile"),
    /* TOUCH FRAGMENTS */
    PASSPORT("passport"),
    FOLDER_ID("folder/%s"),
    FOLDERS("folders"),
    MESSAGES("messages/%s"),
    THREAD_ID("/thread/%s"),
    INBOX_FOLDER("folder/1"),
    SETTINGS_TOUCH("settings"),
    SEARCH_TOUCH("search?"),
    FEEDBACK("feedback"),
    LABEL_ID("label/%s"),
    SEARCH_RQST("request=%s"),
    CHAT("/chat/%s"),
    RELEVANT_TAB("tab/relevant"),
    NEWS_TAB("tab/news"),
    SOCIAL_TAB("tab/social"),
    NEWS_TAB_WEB("tabs/news"),
    SOCIAL_TAB_WEB("tabs/social"),
    SETTINGS_TOUCH_PART("settings/%s"),
    SETTINGS_BACKUP("setup/backup"),
    RESTORED("restored");

    private String fragment;

    QuickFragments(String fragment) {
        this.fragment = fragment;
    }

    public String fragment(String... addToTheEnd) {
        return String.format(this.fragment, (Object[]) addToTheEnd);
    }

    public String makeUrlPart(String... addToTheEnd) {
        String ConstructedURL = fragment(addToTheEnd);
        return String.format("#%s", ConstructedURL);
    }

    public String makeTouchUrlPart(String... addToTheEnd) {
        String constructedURL = fragment(addToTheEnd);
        return String.format("/touch/%s", constructedURL);
    }
}
