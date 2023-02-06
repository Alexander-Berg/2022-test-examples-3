package ru.yandex.autotests.innerpochta.utils;

import ru.yandex.autotests.innerpochta.wmi.adapter.WmiAdapterUser;
import ru.yandex.junitextensions.rules.loginrule.Credentials;

/**
 * User: alex89
 * Date: 17.05.13
 */
public class WmiApiUtils extends WmiAdapterUser {
    public WmiApiUtils(String mailBoxLogin, String mailBoxPwd) {
        super(mailBoxLogin, mailBoxPwd);
    }

    public static WmiApiUtils inMailbox(String mailBoxLogin, String mailBoxPwd) {
        return new WmiApiUtils(mailBoxLogin, mailBoxPwd);
    }

    public static WmiApiUtils inMailbox(Credentials mailBoxAccount) {
        return new WmiApiUtils(mailBoxAccount.getLogin(), mailBoxAccount.getPassword());
    }
}
