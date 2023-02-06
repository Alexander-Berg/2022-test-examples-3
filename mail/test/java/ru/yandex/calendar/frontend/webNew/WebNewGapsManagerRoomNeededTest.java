package ru.yandex.calendar.frontend.webNew;

import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.calendar.frontend.bender.WebDateTime;
import ru.yandex.calendar.frontend.webNew.dto.out.NeedForRoomInfo;
import ru.yandex.calendar.frontend.webNew.dto.out.UserWorkMode;
import ru.yandex.calendar.logic.event.model.EventType;
import ru.yandex.calendar.test.auto.db.util.TestUserInfo;

public class WebNewGapsManagerRoomNeededTest extends WebNewGapsManagerTestBase {
    @Autowired
    private WebNewGapsManager gapsManager;

    private final DateTimeZone tz = DateTimeZone.forID("Europe/Moscow");
    private TestUserInfo officeUser;
    private TestUserInfo mixedUser;
    private TestUserInfo remoteUser;

    @Override
    public void setup() {
        super.setup();
        officeUser = createUser("office-test-user", 1234L, UserWorkMode.OFFICE);
        mixedUser = createUser("mixed-test-user", 12345L, UserWorkMode.MIXED);
        remoteUser = createUser("remote-test-user", 123456L, UserWorkMode.REMOTE);
    }

    @Test
    public void officeWorkerWithoutGap() {
        WebDateTime from = WebDateTime.localDateTime(LocalDateTime.parse("2021-02-02T00:00:00"));
        WebDateTime to = WebDateTime.localDateTime(LocalDateTime.parse("2021-02-03T00:00:00"));
        NeedForRoomInfo result = gapsManager.isRoomNeeded(from.toLocalDateTime(), to.toLocalDateTime(), Cf.list(officeUser.getLoginRaw()), tz, true);
        boolean isRoomNeeded = result.users.getO(officeUser.getLoginRaw()).get().isRoomNeeded;
        Assert.assertTrue(isRoomNeeded);
    }

    @Test
    public void officeWorkerWithOfficeWorkGap() {
        createGap("2021-02-02T00:00:00.00+0300", "2021-02-03T00:00:00.00+0300", officeUser, EventType.OFFICE_WORK, tz);
        WebDateTime from = WebDateTime.localDateTime(LocalDateTime.parse("2021-02-02T00:00:00"));
        WebDateTime to = WebDateTime.localDateTime(LocalDateTime.parse("2021-02-03T00:00:00"));

        NeedForRoomInfo result = gapsManager.isRoomNeeded(from.toLocalDateTime(), to.toLocalDateTime(), Cf.list(officeUser.getLoginRaw()), tz, true);
        boolean isRoomNeeded = result.users.getO(officeUser.getLoginRaw()).get().isRoomNeeded;
        Assert.assertTrue(isRoomNeeded);
    }

    @Test
    public void officeWorkerWithDutyGap() {
        createGap("2021-02-04T00:00:00.00+0300", "2021-02-05T00:00:00.00+0300", officeUser, EventType.DUTY, tz);
        WebDateTime from = WebDateTime.localDateTime(LocalDateTime.parse("2021-02-04T00:00:00"));
        WebDateTime to = WebDateTime.localDateTime(LocalDateTime.parse("2021-02-05T00:00:00"));

        NeedForRoomInfo result = gapsManager.isRoomNeeded(from.toLocalDateTime(), to.toLocalDateTime(), Cf.list(officeUser.getLoginRaw()), tz, true);
        boolean isRoomNeeded = result.users.getO(officeUser.getLoginRaw()).get().isRoomNeeded;
        Assert.assertTrue(isRoomNeeded);
    }

    @Test
    public void officeWorkerWithAbsenceGap() {
        createGap("2021-02-06T00:00:00.00+0300", "2021-02-07T00:00:00.00+0300", officeUser, EventType.ABSENCE, tz);
        WebDateTime from = WebDateTime.localDateTime(LocalDateTime.parse("2021-02-06T00:00:00"));
        WebDateTime to = WebDateTime.localDateTime(LocalDateTime.parse("2021-02-07T00:00:00"));

        NeedForRoomInfo result = gapsManager.isRoomNeeded(from.toLocalDateTime(), to.toLocalDateTime(), Cf.list(officeUser.getLoginRaw()), tz, true);
        boolean isRoomNeeded = result.users.getO(officeUser.getLoginRaw()).get().isRoomNeeded;
        Assert.assertFalse(isRoomNeeded);
    }

    @Test
    public void mixedWorkerWithoutGap() {
        WebDateTime from = WebDateTime.localDateTime(LocalDateTime.parse("2021-02-08T00:00:00"));
        WebDateTime to = WebDateTime.localDateTime(LocalDateTime.parse("2021-02-09T00:00:00"));

        NeedForRoomInfo result = gapsManager.isRoomNeeded(from.toLocalDateTime(), to.toLocalDateTime(), Cf.list(mixedUser.getLoginRaw()), tz, true);
        boolean isRoomNeeded = result.users.getO(mixedUser.getLoginRaw()).get().isRoomNeeded;
        Assert.assertFalse(isRoomNeeded);
    }

    @Test
    public void mixedAsOfficeWorkerWithoutGap() {
        WebDateTime from = WebDateTime.localDateTime(LocalDateTime.parse("2021-02-08T00:00:00"));
        WebDateTime to = WebDateTime.localDateTime(LocalDateTime.parse("2021-02-09T00:00:00"));

        NeedForRoomInfo result = gapsManager.isRoomNeeded(from.toLocalDateTime(), to.toLocalDateTime(), Cf.list(mixedUser.getLoginRaw()), tz, false);
        boolean isRoomNeeded = result.users.getO(mixedUser.getLoginRaw()).get().isRoomNeeded;
        Assert.assertTrue(isRoomNeeded);
    }

    @Test
    public void mixedWorkerWithOfficeWorkGap() {
        createGap("2021-02-10T00:00:00.00+0300", "2021-02-11T00:00:00.00+0300", mixedUser, EventType.OFFICE_WORK, tz);
        WebDateTime from = WebDateTime.localDateTime(LocalDateTime.parse("2021-02-10T00:00:00"));
        WebDateTime to = WebDateTime.localDateTime(LocalDateTime.parse("2021-02-11T00:00:00"));

        NeedForRoomInfo result = gapsManager.isRoomNeeded(from.toLocalDateTime(), to.toLocalDateTime(), Cf.list(mixedUser.getLoginRaw()), tz, true);
        boolean isRoomNeeded = result.users.getO(mixedUser.getLoginRaw()).get().isRoomNeeded;
        Assert.assertTrue(isRoomNeeded);
    }

    @Test
    public void mixedAsOfficeWorkerWithOfficeWorkGap() {
        createGap("2021-02-10T00:00:00.00+0300", "2021-02-11T00:00:00.00+0300", mixedUser, EventType.OFFICE_WORK, tz);
        WebDateTime from = WebDateTime.localDateTime(LocalDateTime.parse("2021-02-10T00:00:00"));
        WebDateTime to = WebDateTime.localDateTime(LocalDateTime.parse("2021-02-11T00:00:00"));

        NeedForRoomInfo result = gapsManager.isRoomNeeded(from.toLocalDateTime(), to.toLocalDateTime(), Cf.list(mixedUser.getLoginRaw()), tz, false);
        boolean isRoomNeeded = result.users.getO(mixedUser.getLoginRaw()).get().isRoomNeeded;
        Assert.assertTrue(isRoomNeeded);
    }

    @Test
    public void remoteWorkerWithoutGap() {
        WebDateTime from = WebDateTime.localDateTime(LocalDateTime.parse("2021-02-14T00:00:00"));
        WebDateTime to = WebDateTime.localDateTime(LocalDateTime.parse("2021-02-15T00:00:00"));

        NeedForRoomInfo result = gapsManager.isRoomNeeded(from.toLocalDateTime(), to.toLocalDateTime(), Cf.list(remoteUser.getLoginRaw()), tz, true);
        boolean isRoomNeeded = result.users.getO(remoteUser.getLoginRaw()).get().isRoomNeeded;
        Assert.assertFalse(isRoomNeeded);
    }

    @Test
    public void remoteWorkerWithOfficeWorkGap() {
        createGap("2021-02-14T00:00:00.00+0300", "2021-02-15T00:00:00.00+0300", remoteUser, EventType.OFFICE_WORK, tz);
        WebDateTime from = WebDateTime.localDateTime(LocalDateTime.parse("2021-02-14T00:00:00"));
        WebDateTime to = WebDateTime.localDateTime(LocalDateTime.parse("2021-02-15T00:00:00"));

        NeedForRoomInfo result = gapsManager.isRoomNeeded(from.toLocalDateTime(), to.toLocalDateTime(), Cf.list(remoteUser.getLoginRaw()), tz, true);
        boolean isRoomNeeded = result.users.getO(remoteUser.getLoginRaw()).get().isRoomNeeded;
        Assert.assertTrue(isRoomNeeded);
    }
}
