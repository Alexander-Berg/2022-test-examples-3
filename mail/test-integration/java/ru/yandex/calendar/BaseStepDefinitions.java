package ru.yandex.calendar;

import lombok.val;
import org.joda.time.DateTime;
import ru.yandex.calendar.logic.user.UserInfo;
import ru.yandex.calendar.logic.user.UserManager;
import ru.yandex.calendar.util.idlent.YandexUser;
import ru.yandex.inside.passport.login.PassportLogin;
import ru.yandex.misc.time.MoscowTime;

import javax.inject.Inject;

public class BaseStepDefinitions {
    protected static final DateTime NOW = MoscowTime.dateTime(2020, 2, 14, 22, 0);

    @Inject
    UserManager userManager;

    protected YandexUser getUser(String login) {
        val uid = userManager.getUidByLoginForTest(PassportLogin.cons(login));
        return userManager.getUserByUid(uid)
            .toOptional()
            .orElseThrow(() -> new IllegalArgumentException("User " + login + " not found"));
    }

    protected UserInfo getUserInfo(String login) {
        val uid = getUser(login).getUid();
        return userManager.getUserInfo(uid);
    }
}
