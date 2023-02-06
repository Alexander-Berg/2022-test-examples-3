package ru.yandex.direct.intapi.statistic;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.statistics.model.FraudAndGiftClicks;
import ru.yandex.direct.intapi.IntApiException;
import ru.yandex.direct.intapi.entity.statistic.StatisticController;
import ru.yandex.direct.intapi.entity.statistic.model.order.GetOrdersStatByIntervalRequest;
import ru.yandex.direct.intapi.fatconfiguration.FatIntApiTest;
import ru.yandex.direct.intapi.statistic.statutils.OrderStatFraudYTRecord;
import ru.yandex.direct.intapi.statistic.statutils.StatTablesUtils;
import ru.yandex.direct.utils.TimeProvider;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.yandex.direct.grid.schema.yt.Tables.ORDERSTATFRAUD_BS;
import static ru.yandex.direct.intapi.utils.TablesUtils.generatePrefix;
import static ru.yandex.direct.ytwrapper.model.YtCluster.YT_LOCAL;

/**
 * Тест хочет иметь запущенный локальный YT, поэтому из IDEA просто так не запустится.
 * Рекомендуется запускать на ppcdev через ya make:
 * https://a.yandex-team.ru/arc/trunk/arcadia/direct/jobs/local_yt_ut/README.md
 */
@FatIntApiTest
@RunWith(SpringJUnit4ClassRunner.class)
public class GetFraudClicksTests {
    private TimeProvider timeProvider = new TimeProvider();

    private static final long ORDER_ID = 1L;
    private static final long UNEXISTED_ORDER_ID = 2L;
    private static final long BAD_ORDER_ID = 0L;
    private final Long todayTimestamp = timeProvider.instantNow().getEpochSecond();
    private final Long yesterdayTimestamp = timeProvider.instantNow().minus(1, DAYS).getEpochSecond();
    private final Long twoDaysAgoTimestamp = timeProvider.instantNow().minus(2, DAYS).getEpochSecond();
    private final Long threeDaysAgoTimestamp = timeProvider.instantNow().minus(3, DAYS).getEpochSecond();


    @Autowired
    private StatisticController statisticController;

    @Autowired
    private StatTablesUtils statTablesUtils;


    @Before
    public void before() {
        //Подготовим записи
        List<OrderStatFraudYTRecord> stats = asList(
                new OrderStatFraudYTRecord().withOrderID(ORDER_ID).withUpdateTime(threeDaysAgoTimestamp)
                        .withFraudClicks(null).withGiftClicks(null)
                        .withFraudShowsGeneral(null).withFraudShowsSophisticated(null),
                new OrderStatFraudYTRecord().withOrderID(ORDER_ID).withUpdateTime(twoDaysAgoTimestamp)
                        .withFraudClicks(1L).withGiftClicks(7L)
                        .withFraudShowsGeneral(0L).withFraudShowsSophisticated(1L),
                new OrderStatFraudYTRecord().withOrderID(ORDER_ID).withUpdateTime(yesterdayTimestamp)
                        .withFraudClicks(3L).withGiftClicks(11L)
                        .withFraudShowsGeneral(10L).withFraudShowsSophisticated(2L),
                new OrderStatFraudYTRecord().withOrderID(ORDER_ID).withUpdateTime(todayTimestamp)
                        .withFraudClicks(5L).withGiftClicks(13L)
                        .withFraudShowsGeneral(2L).withFraudShowsSophisticated(0L)
        );

        statTablesUtils.bindTableToTmp(ORDERSTATFRAUD_BS, generatePrefix());
        statTablesUtils.createOrderStatFraudTable(YT_LOCAL, stats);
    }

    @Test
    public void getFraudClicksTest() {
        final GetOrdersStatByIntervalRequest request =
                new GetOrdersStatByIntervalRequest(List.of(ORDER_ID),
                        Instant.ofEpochSecond(twoDaysAgoTimestamp).atZone(ZoneId.systemDefault()).toLocalDate(),
                        Instant.ofEpochSecond(todayTimestamp).atZone(ZoneId.systemDefault()).toLocalDate());
        FraudAndGiftClicks result = statisticController.getFraudClicks(request);
        FraudAndGiftClicks fraudAndGiftClicks = new FraudAndGiftClicks(4L, 18L, 10L, 3L);
        assertThat(result).isEqualToComparingFieldByField(fraudAndGiftClicks);
    }

    @Test
    public void getFraudClicksNullTest() {
        final GetOrdersStatByIntervalRequest request =
                new GetOrdersStatByIntervalRequest(List.of(UNEXISTED_ORDER_ID),
                        Instant.ofEpochSecond(twoDaysAgoTimestamp).atZone(ZoneId.systemDefault()).toLocalDate(),
                        Instant.ofEpochSecond(todayTimestamp).atZone(ZoneId.systemDefault()).toLocalDate());
        FraudAndGiftClicks result = statisticController.getFraudClicks(request);
        FraudAndGiftClicks fraudAndGiftClicks = new FraudAndGiftClicks(0L, 0L, 0L, 0L);
        assertThat(result).isEqualToComparingFieldByField(fraudAndGiftClicks);
    }

    @Test
    public void getFraudClicksNullYtValuesTest() {
        final GetOrdersStatByIntervalRequest request =
                new GetOrdersStatByIntervalRequest(List.of(ORDER_ID),
                        Instant.ofEpochSecond(threeDaysAgoTimestamp).atZone(ZoneId.systemDefault()).toLocalDate(),
                        Instant.ofEpochSecond(twoDaysAgoTimestamp).atZone(ZoneId.systemDefault()).toLocalDate());
        FraudAndGiftClicks result = statisticController.getFraudClicks(request);
        FraudAndGiftClicks fraudAndGiftClicks = new FraudAndGiftClicks(0L, 0L, 0L, 0L);
        assertThat(result).isEqualToComparingFieldByField(fraudAndGiftClicks);
    }

    @Test
    public void getOrdersDaysNumErrorTest() {
        final GetOrdersStatByIntervalRequest request =
                new GetOrdersStatByIntervalRequest(List.of(BAD_ORDER_ID),
                        Instant.ofEpochSecond(twoDaysAgoTimestamp).atZone(ZoneId.systemDefault()).toLocalDate(),
                        Instant.ofEpochSecond(todayTimestamp).atZone(ZoneId.systemDefault()).toLocalDate());
        assertThatThrownBy(() -> statisticController.getFraudClicks(request))
                .isInstanceOf(IntApiException.class)
                .hasMessageContaining("\"success\":false")
                .hasMessageContaining("\"code\":\"DefectIds.MUST_BE_VALID_ID\"");
    }
}
