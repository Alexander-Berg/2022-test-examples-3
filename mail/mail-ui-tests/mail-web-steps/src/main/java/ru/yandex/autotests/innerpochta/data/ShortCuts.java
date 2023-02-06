package ru.yandex.autotests.innerpochta.data;

/**
 * User: lanwen
 * Date: 24.10.13
 * Time: 13:23
 */
public enum ShortCuts {
    SETTINGS("/setup"),
    COMPOSE("/compose"),
    COMPOSE_TO("/compose?to="),
    ABOOK("/abook"),
    INBOX("/messages"),
    UNREAD("/messages?extra_cond=unread"),
    FOLDER("/messages?current_folder="),
    MESSAGE("/message?ids="),
    MSG("/msg?ids="),
    SEARCH("/search?request=");

    private String path;

    ShortCuts(String path) {
        this.path = path;
    }

    public String path() {
        return path;
    }
}
