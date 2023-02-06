package ru.yandex.direct.intapi.statistic;


import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.intapi.IntApiException;
import ru.yandex.direct.intapi.entity.statistic.StatisticController;
import ru.yandex.direct.intapi.entity.statistic.model.order.GetOrdersDaysNumResponse;
import ru.yandex.direct.intapi.entity.statistic.model.order.GetOrdersStatByIntervalRequest;
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
public class GetOrdersDaysNumTests {
    private TimeProvider timeProvider = new TimeProvider();

    private static final long ORDER_ID = 1L;
    private static final long UNEXISTED_ORDER_ID = 2L;
    private static final long BAD_ORDER_ID = 0L;
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
                new OrderStatDayYTRecord().withOrderID(ORDER_ID).withUpdateTime(twoDaysAgoTimestamp).withIsSearch(true),
                new OrderStatDayYTRecord().withOrderID(ORDER_ID).withUpdateTime(yesterdayTimestamp).withIsSearch(true),
                new OrderStatDayYTRecord().withOrderID(ORDER_ID).withUpdateTime(todayTimestamp).withIsSearch(true)
        );
        statTablesUtils.bindTableToTmp(ORDERSTATDAY_BS, generatePrefix());
        statTablesUtils.createOrderStatDayTable(YT_LOCAL, stats);
    }

    @Test
    public void getOrdersDaysNumTest() {
        final GetOrdersStatByIntervalRequest request =
                new GetOrdersStatByIntervalRequest(List.of(ORDER_ID),
                        Instant.ofEpochSecond(twoDaysAgoTimestamp).atZone(ZoneId.systemDefault()).toLocalDate(),
                        Instant.ofEpochSecond(todayTimestamp).atZone(ZoneId.systemDefault()).toLocalDate());
        GetOrdersDaysNumResponse result = statisticController.getOrdersDaysNum(request);
        GetOrdersDaysNumResponse getOrdersDaysNumResponse = new GetOrdersDaysNumResponse(2L);
        assertThat(result).isEqualToComparingFieldByField(getOrdersDaysNumResponse);

    }

    @Test
    public void getOrdersDaysNumNullTest() {
        final GetOrdersStatByIntervalRequest request =
                new GetOrdersStatByIntervalRequest(List.of(UNEXISTED_ORDER_ID),
                        Instant.ofEpochSecond(twoDaysAgoTimestamp).atZone(ZoneId.systemDefault()).toLocalDate(),
                        Instant.ofEpochSecond(todayTimestamp).atZone(ZoneId.systemDefault()).toLocalDate());
        GetOrdersDaysNumResponse result = statisticController.getOrdersDaysNum(request);
        GetOrdersDaysNumResponse getOrdersDaysNumResponse = new GetOrdersDaysNumResponse(0L);
        assertThat(result).isEqualToComparingFieldByField(getOrdersDaysNumResponse);
    }

    @Test
    public void getOrdersDaysNumErrorTest() {
        final GetOrdersStatByIntervalRequest request =
                new GetOrdersStatByIntervalRequest(List.of(BAD_ORDER_ID),
                        Instant.ofEpochSecond(twoDaysAgoTimestamp).atZone(ZoneId.systemDefault()).toLocalDate(),
                        Instant.ofEpochSecond(todayTimestamp).atZone(ZoneId.systemDefault()).toLocalDate());
        assertThatThrownBy(() -> statisticController.getOrdersDaysNum(request))
                .isInstanceOf(IntApiException.class)
                .hasMessageContaining("\"success\":false")
                .hasMessageContaining("\"code\":\"DefectIds.MUST_BE_VALID_ID\"");
    }
}
