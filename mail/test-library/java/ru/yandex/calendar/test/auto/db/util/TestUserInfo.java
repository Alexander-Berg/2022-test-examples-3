package ru.yandex.calendar.test.auto.db.util;

import lombok.Value;
import ru.yandex.bolts.function.Function;
import ru.yandex.calendar.logic.sharing.participant.ParticipantId;
import ru.yandex.calendar.logic.user.UserInfo;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.inside.passport.login.PassportLogin;
import ru.yandex.misc.email.Email;

@Value
public class TestUserInfo {
    UserInfo userInfo;
    PassportLogin login;
    Email email;
    long defaultLayerId;

    public PassportUid getUid() {
        return userInfo.getUid();
    }

    public ParticipantId getParticipantId() {
        return ParticipantId.yandexUid(getUid());
    }

    public String getLoginRaw() {
        return login.getRawValue();
    }

    public static Function<TestUserInfo, PassportUid> getUidF() {
        return TestUserInfo::getUid;
    }

    public TestUserInfo withSuperUser(boolean superUser) {
        return new TestUserInfo(userInfo.withSuperUserForTest(superUser), login, email, defaultLayerId);
    }
}
