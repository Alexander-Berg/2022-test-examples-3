package ru.yandex.autotests.testpers.mail.mon.misc;


import ru.yandex.autotests.lib.junit.rules.login.Credentials;

/**
 * Created by IntelliJ IDEA.
 * User: lanwen
 * Date: 26.03.12
 * Time: 18:52
 *
 * Класс, знающий необходимые параметры аккаунта
 */
public class Account implements Credentials {
    /**
     * Логин
     */
    private String login;
    /**
     * Пароль
     */
    private String pwd;

    private String domain;

    public Account(String login, String pwd) {
        this.login = login;
        this.pwd = pwd;
        this.domain = "yandex.ru";
    }

    @Override
    public String getLogin() {
        return login;
    }

    @Override
    public String getPassword() {
        return pwd;
    }

    public Account domain(String domain) {
        this.domain = domain;
        return this;
    }

    public String getSelfEmail() {
        return login.contains("@") ? login : login + "@" + domain;
    }

    @Override
    public String toString() {
        return getSelfEmail() + " : " + pwd;
    }
}
