package ru.yandex.market.mbo.tms.goldenmatrix;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.CypressNodeType;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.ytree.YTreeStringNode;
import ru.yandex.market.mbo.utils.DateFormatUtils;
import ru.yandex.market.mbo.yt.TestYt;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author kravchenko-aa
 * @date 2019-07-24
 */
public class RotateYtSessionsExecutorTest {

    private static final YPath YT_ROOT_PATH = YPath.simple("//home/market/test/mbo/msku_parameters");

    private RotateYtSessionsExecutor rotateYtSessionsExecutor;

    private Yt yt;

    @Before
    public void setUp() {
        yt = new TestYt();
        rotateYtSessionsExecutor = new RotateYtSessionsExecutor(yt, YT_ROOT_PATH, "TestTableName");
    }

    @Test
    @SuppressWarnings("checkstyle:MagicNumber")
    public void testRotation() throws Exception {
        LocalDateTime now = LocalDateTime.now();

        // Last month sessions. Take all of them.
        LocalDateTime lastMonthStart = now.minusMonths(1);
        List<String> lastMonthSessions = Arrays.asList(
            createSession(now),
            createSession(now.minusHours(3)),
            createSession(now.minusHours(6)),
            createSession(lastMonthStart.plusMinutes(1)),
            createSession(lastMonthStart.plusHours(1)),
            createSession(lastMonthStart.plusDays(2))
        );

        List<String> allSessions = new ArrayList<>(lastMonthSessions);
        List<String> sessionsToPreserve = new ArrayList<>(lastMonthSessions);

        // Some random monday almost year ago. Take sessions weekly.
        LocalDateTime lastYearStart = now.minusYears(1).plusMonths(1).with(DayOfWeek.MONDAY).withHour(0).withMinute(0);
        allSessions.addAll(Arrays.asList(
            createSession(lastYearStart.plusMinutes(1)), // the first one - take it!
            createSession(lastYearStart.plusHours(1)), // same week - ignored
            createSession(lastYearStart.plusDays(1)), // same week - ignored
            createSession(lastYearStart.plusMinutes(1).plusWeeks(1)), // next week - take it!
            createSession(lastYearStart.plusMinutes(1).plusWeeks(2)) // next week - take it!
        ));
        sessionsToPreserve.addAll(Arrays.asList(
            createSession(lastYearStart.plusMinutes(1)),
            createSession(lastYearStart.plusMinutes(1).plusWeeks(1)),
            createSession(lastYearStart.plusMinutes(1).plusWeeks(2))
        ));

        // Some random beginning of a month almost five years ago. Take sessions monthly.
        LocalDateTime last5YearsStart = now.minusYears(5).plusMonths(3).withDayOfMonth(1).withHour(0).withMinute(0);
        allSessions.addAll(Arrays.asList(
            createSession(last5YearsStart.plusMinutes(1)), // the first one in a month - take it!
            createSession(last5YearsStart.plusDays(1)), // same month - ignored
            createSession(last5YearsStart.plusWeeks(1)), // ...
            createSession(last5YearsStart.plusMonths(2)), // new month, take it!
            createSession(last5YearsStart.plusMinutes(1).plusMonths(3)) // another new month - take it!
        ));
        sessionsToPreserve.addAll(Arrays.asList(
            createSession(last5YearsStart.plusMinutes(1)),
            createSession(last5YearsStart.plusMonths(2)),
            createSession(last5YearsStart.plusMinutes(1).plusMonths(3))
        ));

        // Dinosaur era sessions - totally ignored.
        allSessions.addAll(Arrays.asList(
            createSession(now.minusYears(6)),
            createSession(now.minusYears(10))
        ));
        allSessions.forEach(backup ->
            yt.cypress().create(YT_ROOT_PATH.child(backup), CypressNodeType.TABLE, true));

        rotateYtSessionsExecutor.doRealJob(null);

        List<String> actuallyPreserved = yt.cypress().list(YT_ROOT_PATH).stream()
            .map(YTreeStringNode::getValue)
            .collect(Collectors.toList());
        assertThat(actuallyPreserved).containsExactlyInAnyOrderElementsOf(sessionsToPreserve);
    }

    private String createSession(LocalDateTime dateTime) {
        return DateFormatUtils.DEFAULT_SESSION_FORMAT.format(dateTime);
    }
}
