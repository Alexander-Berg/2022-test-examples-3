package ru.yandex.chemodan.app.telemost.tools;

import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.telemost.services.model.PassportOrYaTeamUid;
import ru.yandex.chemodan.app.telemost.services.model.User;
import ru.yandex.misc.time.MoscowTime;

public class UserHelper {
    public static User createTestUserForUid(PassportOrYaTeamUid uid) {
        return new User(uid, Option.of("test"), Option.of("test"), Option.of(false), false, false,
                MoscowTime.TZ, "ru");
    }

    public static  User createTestStaffUserForUid(PassportOrYaTeamUid uid) {
        return new User(uid, Option.of("test"), Option.of("test"), Option.of(false), true, false,
                MoscowTime.TZ, "ru");
    }
}
