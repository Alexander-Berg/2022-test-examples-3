package ru.yandex.mail.common.credentials;

import ru.yandex.mail.common.properties.Scopes;

import static ru.yandex.mail.common.properties.CoreProperties.props;

public class Account {
    public static String login(String login) {
        return login;
    }

    public static String password(String password) {
        return password;
    }

    public static String uid(String uid) {
        return uid;
    }

    public static String domain(String domain) {
        return domain;
    }

    public static Account of(String login, String password, String uid) {
        return new Account(login, password, uid, "yandex.ru", null);
    }

    public static Account of(String login, String password, String uid, BbResponse sessionId) {
        return new Account(login, password, uid, "yandex.ru", sessionId);
    }

    public static Account of(String login, String password, String uid, String domain) {
        return new Account(login, password, uid, domain, null);
    }

    private String uid;
    private String login;
    private String password;
    private String domain;
    private BbResponse fakeBbSessionId;
    private String tvmTicket;

    private Account(String login, String password, String uid, String domain, BbResponse fakeBbSessionId) {
        this.login = login;
        this.password = password;
        this.uid = uid;
        this.domain = domain;
        this.fakeBbSessionId = fakeBbSessionId;
    }

    public String uid() {
        return uid;
    }

    public String login() {
        return login;
    }

    public String email() {
        if (props().scope() == Scopes.INTRANET_PRODUCTION) {
            return login() + "@yandex-team.ru";
        }
        return login.contains("@") ? login : login + "@" + domain;
    }

    public String email(String domain) {
        return login() + "@" + domain;
    }

    public String password() {
        return password;
    }

    public String tvmTicket() {
        return tvmTicket;
    }

    public void setTvmTicket(String ticket) {
        tvmTicket = ticket;
    }

    public String fakeBbSessionId() {
        return fakeBbSessionId.toString();
    }
}
