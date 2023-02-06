package ru.yandex.calendar.frontend.webNew;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Optional;
import java.util.OptionalInt;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Instant;

import ru.yandex.bolts.collection.Option;
import ru.yandex.calendar.frontend.webNew.dto.out.UserWorkMode;
import ru.yandex.calendar.logic.beans.generated.Event;
import ru.yandex.calendar.logic.event.model.EventType;
import ru.yandex.calendar.logic.layer.LayerType;
import ru.yandex.calendar.logic.sharing.Decision;
import ru.yandex.calendar.micro.yt.entity.YtUser;
import ru.yandex.calendar.test.auto.db.util.TestUserInfo;
import ru.yandex.calendar.util.idlent.YandexUser;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.inside.passport.login.PassportLogin;
import ru.yandex.mail.cerberus.LocationId;
import ru.yandex.mail.cerberus.Uid;
import ru.yandex.mail.cerberus.yt.data.YtUserInfo;
import ru.yandex.mail.cerberus.yt.staff.dto.StaffUser;
import ru.yandex.misc.email.Email;

abstract class WebNewGapsManagerTestBase extends WebNewTestBase {

    protected TestUserInfo createUser(String login, long uid, UserWorkMode workMode) {
        String testEmail = login + "-email@yandex-team.ru";
        YandexUser yandexUser = new YandexUser(
                new PassportUid(uid),
                new PassportLogin(login),
                Option.of("test-user-name"),
                Option.of(new Email(testEmail)),
                Option.empty(),
                Option.empty(),
                Option.empty());

        YtUserInfo userInfo = new YtUserInfo(
                uid,
                StaffUser.Affiliation.YANDEX,
                testEmail,
                ZoneId.systemDefault(),
                new LocationId(1), OptionalInt.of(1), "ru", "test", "user", "test", "user", Optional.empty(),
                "test-position", "test-position", StaffUser.Gender.MALE, Optional.empty(), new ArrayList<>(), EnumSet.of(YtUserInfo.Trait.HOMEWORKER), new ArrayList<>(), Optional.empty(), Optional.of(workMode.name()));

        testManager.prepareYandexTeamUser(new YtUser(new Uid(uid), login, userInfo));
        return testManager.prepareYandexUser(yandexUser);
    }

    protected void createGap(String startTs, String endTs, TestUserInfo targetUser, DateTimeZone tz) {
        createGap(startTs, endTs, targetUser, EventType.ABSENCE, tz);
    }
    protected void createGap(String startTs, String endTs, TestUserInfo targetUser, EventType eventType, DateTimeZone tz) {
        Instant startEventTs = DateTime.parse(startTs).toInstant();
        Instant endEventTs = DateTime.parse(endTs).toInstant();

        Event eventOverrides = new Event();
        eventOverrides.setStartTs(startEventTs);
        eventOverrides.setEndTs(endEventTs);
        eventOverrides.setType(eventType);
        eventOverrides.setIsAllDay(true);

        Event absenceEvent = testManager.createDefaultEvent(targetUser.getUid(), "test-absence", eventOverrides);
        long absenceLayerId = testManager.createLayer(targetUser.getUid(), LayerType.ABSENCE);
        testManager.createEventLayer(absenceLayerId, absenceEvent.getId());
        testManager.createEventUser(new PassportUid(targetUser.getUid().getUid()), absenceEvent.getId(), Decision.YES, Option.empty());
    }
}
