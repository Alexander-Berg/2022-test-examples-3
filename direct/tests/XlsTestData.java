package ru.yandex.autotests.direct.tests;

public enum XlsTestData {
    CLIENT_WITH_TEXT_CAMPAIGN_STAT("spb-tester-sq-stat",  28208715L),
    CLIENT_WITH_SEARCH_QUERIES_STAT("spb-tester-sq-stat", 28208715L);
    private String cid;
    private String login;

    XlsTestData(String login, Long cid) {
        this.login = login;
        this.cid = String.valueOf(cid);
    }

    public String getCid() {
        return cid;
    }

    public String getLogin() {
        return login;
    }
}
