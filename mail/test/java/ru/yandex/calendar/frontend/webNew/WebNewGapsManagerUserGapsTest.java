package ru.yandex.calendar.frontend.webNew;

import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.MapF;
import ru.yandex.calendar.frontend.bender.WebDateTime;
import ru.yandex.calendar.frontend.webNew.dto.out.UserGapInfo;
import ru.yandex.calendar.frontend.webNew.dto.out.UserWorkMode;
import ru.yandex.calendar.test.auto.db.util.TestUserInfo;

public class WebNewGapsManagerUserGapsTest extends WebNewGapsManagerTestBase {

    @Autowired
    private WebNewGapsManager gapsManager;

    private final DateTimeZone tz = DateTimeZone.forID("Europe/Moscow");
    private TestUserInfo testUser;

    @Override
    public void setup() {
        super.setup();
        testUser = createUser("test-user", 1234L, UserWorkMode.OFFICE);
    }

    @Test
    public void existentUserGapWithEqualRange() {
        createGap("2021-01-01T00:00:00.00+0300", "2021-01-02T00:00:00.00+0300", testUser, tz);
        WebDateTime from = WebDateTime.localDateTime(LocalDateTime.parse("2021-01-01T00:00:00"));
        WebDateTime to = WebDateTime.localDateTime(LocalDateTime.parse("2021-01-02T00:00:00"));
        checkExistentGap(from, to);
    }

    @Test
    public void existentUserGapWithLeftEqualRightIncludeRange() {
        createGap("2021-01-03T00:00:00.00+0300", "2021-01-03T12:00:00.00+0300", testUser, tz);
        WebDateTime from = WebDateTime.localDateTime(LocalDateTime.parse("2021-01-03T00:00:00"));
        WebDateTime to = WebDateTime.localDateTime(LocalDateTime.parse("2021-01-04T00:00:00"));
        checkExistentGap(from, to);
    }

    @Test
    public void existentUserGapWithLeftIncludeRightEqualRange() {
        createGap("2021-01-04T10:00:00.00+0300", "2021-01-05T00:00:00.00+0300", testUser, tz);
        WebDateTime from = WebDateTime.localDateTime(LocalDateTime.parse("2021-01-04T00:00:00"));
        WebDateTime to = WebDateTime.localDateTime(LocalDateTime.parse("2021-01-05T00:00:00"));
        checkExistentGap(from, to);
    }

    @Test
    public void existentUserGapWithLeftAndRightIncludesRange() {
        createGap("2021-01-05T10:00:00.00+0300", "2021-01-05T12:00:00.00+0300", testUser, tz);
        WebDateTime from = WebDateTime.localDateTime(LocalDateTime.parse("2021-01-05T00:00:00"));
        WebDateTime to = WebDateTime.localDateTime(LocalDateTime.parse("2021-01-06T00:00:00"));
        checkExistentGap(from, to);
    }

    @Test
    public void existentUserGapWithLeftExcludeRightEqualRange() {
        createGap("2021-01-05T10:00:00.00+0300", "2021-01-07T00:00:00.00+0300", testUser, tz);
        WebDateTime from = WebDateTime.localDateTime(LocalDateTime.parse("2021-01-06T00:00:00"));
        WebDateTime to = WebDateTime.localDateTime(LocalDateTime.parse("2021-01-07T00:00:00"));
        checkExistentGap(from, to);
    }

    @Test
    public void existentUserGapWithLeftEqualRightExcludeRange() {
        createGap("2021-01-08T00:00:00.00+0300", "2021-01-09T12:00:00.00+0300", testUser, tz);
        WebDateTime from = WebDateTime.localDateTime(LocalDateTime.parse("2021-01-08T00:00:00"));
        WebDateTime to = WebDateTime.localDateTime(LocalDateTime.parse("2021-01-09T00:00:00"));
        checkExistentGap(from, to);
    }

    @Test
    public void existentUserGapWithLeftAndRightExcludeRange() {
        createGap("2021-01-10T12:00:00.00+0300", "2021-01-12T12:00:00.00+0300", testUser, tz);
        WebDateTime from = WebDateTime.localDateTime(LocalDateTime.parse("2021-01-11T00:00:00"));
        WebDateTime to = WebDateTime.localDateTime(LocalDateTime.parse("2021-01-12T00:00:00"));
        checkExistentGap(from, to);
    }

    @Test
    public void nonexistentUserGapWithLeftExcludeRightEqualRange() {
        createGap("2021-02-01T12:00:00.00+0300", "2021-02-02T00:00:00.00+0300", testUser, tz);
        WebDateTime from = WebDateTime.localDateTime(LocalDateTime.parse("2021-02-02T00:00:00"));
        WebDateTime to = WebDateTime.localDateTime(LocalDateTime.parse("2021-02-03T00:00:00"));
        checkNonexistentGap(from, to);
    }

    @Test
    public void nonexistentUserGapWithLeftEqualRightExcludeRange() {
        createGap("2021-02-03T00:00:00.00+0300", "2021-02-03T10:00:00.00+0300", testUser, tz);
        WebDateTime from = WebDateTime.localDateTime(LocalDateTime.parse("2021-02-02T00:00:00"));
        WebDateTime to = WebDateTime.localDateTime(LocalDateTime.parse("2021-02-03T00:00:00"));
        checkNonexistentGap(from, to);
    }

    @Test
    public void nonexistentUserGapWithEventBeforeRange() {
        createGap("2021-02-04T00:00:00.00+0300", "2021-02-04T23:00:00.00+0300", testUser, tz);
        WebDateTime from = WebDateTime.localDateTime(LocalDateTime.parse("2021-02-05T00:00:00"));
        WebDateTime to = WebDateTime.localDateTime(LocalDateTime.parse("2021-02-06T00:00:00"));
        checkNonexistentGap(from, to);
    }

    @Test
    public void nonexistentUserGapWithEventBeforeRange2() {
        createGap("2021-03-04T00:00:00.00+0300", "2021-03-05T00:00:00.00+0300", testUser, tz);
        WebDateTime from = WebDateTime.localDateTime(LocalDateTime.parse("2021-03-06T00:00:00"));
        WebDateTime to = WebDateTime.localDateTime(LocalDateTime.parse("2021-03-07T00:00:00"));
        checkNonexistentGap(from, to);
    }


    @Test
    public void nonexistentUserGapWithEventAfterRange() {
        createGap("2021-02-07T12:00:00.00+0300", "2021-02-08T00:00:00.00+0300", testUser, tz);
        WebDateTime from = WebDateTime.localDateTime(LocalDateTime.parse("2021-02-06T00:00:00"));
        WebDateTime to = WebDateTime.localDateTime(LocalDateTime.parse("2021-02-07T00:00:00"));
        checkNonexistentGap(from, to);
    }

    @Test
    public void nonexistentUserGapWithEventAfterRange2() {
        createGap("2021-03-07T00:00:00.00+0300", "2021-03-08T00:00:00.00+0300", testUser, tz);
        WebDateTime from = WebDateTime.localDateTime(LocalDateTime.parse("2021-03-06T00:00:00"));
        WebDateTime to = WebDateTime.localDateTime(LocalDateTime.parse("2021-03-07T00:00:00"));;
        checkNonexistentGap(from, to);
    }

    private void checkExistentGap(WebDateTime from, WebDateTime to) {
        MapF<String, UserGapInfo> result = gapsManager.getUserGapsByUserLogins(from.toLocalDateTime(), to.toLocalDateTime(), Cf.list(testUser.getLoginRaw()), tz);
        UserGapInfo gapInfo = result.getO(testUser.getLoginRaw()).orElseThrow();

        Assert.assertEquals(gapInfo.workMode, UserWorkMode.OFFICE);
        Assert.assertEquals(1, gapInfo.gaps.size());
    }

    private void checkNonexistentGap(WebDateTime from, WebDateTime to) {
        MapF<String, UserGapInfo> result = gapsManager.getUserGapsByUserLogins(from.toLocalDateTime(), to.toLocalDateTime(), Cf.list(testUser.getLoginRaw()), tz);
        UserGapInfo gapInfo = result.getO(testUser.getLoginRaw()).orElseThrow();

        Assert.assertEquals(gapInfo.workMode, UserWorkMode.OFFICE);
        Assert.assertEquals(0, gapInfo.gaps.size());
    }
}
