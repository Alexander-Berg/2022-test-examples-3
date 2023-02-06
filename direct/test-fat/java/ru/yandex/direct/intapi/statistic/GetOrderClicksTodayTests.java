package ru.yandex.direct.intapi.statistic;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.intapi.IntApiException;
import ru.yandex.direct.intapi.entity.statistic.StatisticController;
import ru.yandex.direct.intapi.entity.statistic.model.GetOrderClicksTodayResponse;
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
public class GetOrderClicksTodayTests {
    private TimeProvider timeProvider = new TimeProvider();

    private static final long ORDER_ID = 1L;
    private static final long UNEXISTED_ORDER_ID = 2L;
    private static final long SECOND_ORDER_ID = 3L;
    private static final long BAD_ORDER_ID = 0L;
    private static final long CLICKS_TODAY = 5L;
    private final Long todayTimestamp = timeProvider.instantNow().getEpochSecond();
    private final Long yesterdayTimestamp = timeProvider.instantNow().minus(1, DAYS).getEpochSecond();
    private final Long twoDaysAgoTimestamp = timeProvider.instantNow().minus(2, DAYS).getEpochSecond();


    @Autowired
    private StatisticController statisticController;

    @Autowired
    private StatTablesUtils statTablesUtils;

    @Before
    public void before() {
        //Подготовим записи
        List<OrderStatDayYTRecord> stats = asList(
                new OrderStatDayYTRecord().withOrderID(SECOND_ORDER_ID).withUpdateTime(todayTimestamp)
                        .withIsSearch(true).withClicks(null),
                new OrderStatDayYTRecord().withOrderID(ORDER_ID).withUpdateTime(twoDaysAgoTimestamp)
                        .withIsSearch(true).withClicks(1L),
                new OrderStatDayYTRecord().withOrderID(ORDER_ID).withUpdateTime(yesterdayTimestamp)
                        .withIsSearch(true).withClicks(3L),
                new OrderStatDayYTRecord().withOrderID(ORDER_ID).withUpdateTime(todayTimestamp)
                        .withIsSearch(true).withClicks(CLICKS_TODAY)
        );
        statTablesUtils.bindTableToTmp(ORDERSTATDAY_BS, generatePrefix());
        statTablesUtils.createOrderStatDayTable(YT_LOCAL, stats);
    }

    @Test
    public void getOrderClicksTodayTest() {
        GetOrderClicksTodayResponse result = statisticController.getOrderClicksToday(ORDER_ID);
        GetOrderClicksTodayResponse getOrderClicksTodayResponse = new GetOrderClicksTodayResponse(CLICKS_TODAY);
        assertThat(result).isEqualToComparingFieldByField(getOrderClicksTodayResponse);
    }

    @Test
    public void getOrderClicksTodayNullTest() {
        GetOrderClicksTodayResponse result = statisticController.getOrderClicksToday(UNEXISTED_ORDER_ID);
        GetOrderClicksTodayResponse getOrderClicksTodayResponse = new GetOrderClicksTodayResponse(0L);
        assertThat(result).isEqualToComparingFieldByField(getOrderClicksTodayResponse);
    }

    @Test
    public void getOrderClicksTodayNullYtValuesTest() {
        GetOrderClicksTodayResponse result = statisticController.getOrderClicksToday(SECOND_ORDER_ID);
        GetOrderClicksTodayResponse getOrderClicksTodayResponse = new GetOrderClicksTodayResponse(0L);
        assertThat(result).isEqualToComparingFieldByField(getOrderClicksTodayResponse);
    }

    @Test
    public void getOrderClicksTodayErrorTest() {
        assertThatThrownBy(() -> statisticController.getOrderClicksToday(BAD_ORDER_ID))
                .isInstanceOf(IntApiException.class)
                .hasMessageContaining("\"code\":\"BAD_PARAM\"")
                .hasMessageContaining("\"message\":\"params must be greater than 0\"");
    }
}
