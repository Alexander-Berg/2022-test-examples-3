package ru.yandex.autotests.innerpochta.data;

/**
 * @author oleshko
 */

public enum Languages {
    RU("ru"),
    UK("uk"),
    EN("en"),
    KK("kk"),
    BE("be"),
    TT("tt"),
    AZ("az"),
    TR("tr");

    private String lang;

    Languages(String path) {
        this.lang = path;
    }

    public String lang() {
        return lang;
    }
}
