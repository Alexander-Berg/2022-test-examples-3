package ru.yandex.direct.intapi.statistic;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.intapi.IntApiException;
import ru.yandex.direct.intapi.entity.statistic.StatisticController;
import ru.yandex.direct.intapi.entity.statistic.model.GetSessionsNumForWeekResponse;
import ru.yandex.direct.intapi.fatconfiguration.FatIntApiTest;
import ru.yandex.direct.intapi.statistic.statutils.OrderStatDayYTRecord;
import ru.yandex.direct.intapi.statistic.statutils.StatTablesUtils;
import ru.yandex.direct.utils.TimeProvider;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.yandex.direct.grid.schema.yt.Tables.ORDERSTATDAY_BS;
import static ru.yandex.direct.intapi.utils.TablesUtils.generatePrefix;
import static ru.yandex.direct.ytwrapper.model.YtCluster.YT_LOCAL;

@FatIntApiTest
@RunWith(SpringJUnit4ClassRunner.class)
public class GetSessionsNumForWeekTests {
    private TimeProvider timeProvider = new TimeProvider();

    private static final long ORDER_ID = 1L;
    private static final long UNEXISTED_ORDER_ID = 2L;
    private static final long SECOND_ORDER_ID = 3L;
    private static final long BAD_ORDER_ID = 0L;
    private static final long EXPECTED_WEEK_SESSION_NUM = 8L;
    private final Long todayTimestamp = timeProvider.instantNow().getEpochSecond();
    private final Long weekAgoTimestamp = timeProvider.instantNow().minus(6, DAYS).getEpochSecond();
    private final Long twoWeekAgoTimestamp = timeProvider.instantNow().minus(14, DAYS).getEpochSecond();


    @Autowired
    private StatisticController statisticController;

    @Autowired
    private StatTablesUtils statTablesUtils;

    @Before
    public void before() {
        //Подготовим записи
        List<OrderStatDayYTRecord> stats = asList(
                new OrderStatDayYTRecord().withOrderID(SECOND_ORDER_ID).withUpdateTime(todayTimestamp)
                        .withIsSearch(true).withSessionNum(null),
                new OrderStatDayYTRecord().withOrderID(ORDER_ID).withUpdateTime(twoWeekAgoTimestamp)
                        .withIsSearch(true).withSessionNum(1L),
                new OrderStatDayYTRecord().withOrderID(ORDER_ID).withUpdateTime(weekAgoTimestamp)
                        .withIsSearch(true).withSessionNum(3L),
                new OrderStatDayYTRecord().withOrderID(ORDER_ID).withUpdateTime(todayTimestamp)
                        .withIsSearch(true).withSessionNum(5L)
        );
        statTablesUtils.bindTableToTmp(ORDERSTATDAY_BS, generatePrefix());
        statTablesUtils.createOrderStatDayTable(YT_LOCAL, stats);
    }

    @Test
    public void getSessionsNumForWeekTest() {
        GetSessionsNumForWeekResponse result = statisticController.getSessionsNumForWeek(ORDER_ID);
        GetSessionsNumForWeekResponse getSessionsNumForWeekResponse =
                new GetSessionsNumForWeekResponse(EXPECTED_WEEK_SESSION_NUM);
        assertThat(result).isEqualToComparingFieldByField(getSessionsNumForWeekResponse);
    }

    @Test
    public void getSessionsNumForWeekNullTest() {
        GetSessionsNumForWeekResponse result = statisticController.getSessionsNumForWeek(UNEXISTED_ORDER_ID);
        GetSessionsNumForWeekResponse getSessionsNumForWeekResponse = new GetSessionsNumForWeekResponse(0L);
        assertThat(result).isEqualToComparingFieldByField(getSessionsNumForWeekResponse);
    }

    @Test
    public void getSessionsNumForWeekNullYtValuesTest() {
        GetSessionsNumForWeekResponse result = statisticController.getSessionsNumForWeek(SECOND_ORDER_ID);
        GetSessionsNumForWeekResponse getSessionsNumForWeekResponse = new GetSessionsNumForWeekResponse(0L);
        assertThat(result).isEqualToComparingFieldByField(getSessionsNumForWeekResponse);
    }

    @Test
    public void getSessionsNumForWeekErrorTest() {
        assertThatThrownBy(() -> statisticController.getSessionsNumForWeek(BAD_ORDER_ID))
                .isInstanceOf(IntApiException.class)
                .hasMessageContaining("\"code\":\"BAD_PARAM\"")
                .hasMessageContaining("\"message\":\"params must be greater than 0\"");
    }
}
