package ru.yandex.market.api.partner.controllers.stats.model.view;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;

import org.junit.jupiter.api.Test;

import ru.yandex.market.api.partner.controllers.serialization.BaseOldSerializationTest;
import ru.yandex.market.api.partner.controllers.stats.model.MainStatsItem;
import ru.yandex.market.core.stats.ng.model.CampaignStatsItem;
import ru.yandex.market.core.stats.ng.model.OrdersStats;

import static java.util.Collections.emptyMap;

/**
 * @author zoom
 */
class MainStatsItemSerializationTest extends BaseOldSerializationTest {

    @Test
    void shouldSerializeCpaFee() {
        Date date = Date.from(LocalDateTime.of(2016, 1, 1, 0, 0).atOffset(ZoneOffset.UTC).toInstant());
        CampaignStatsItem delegate = new CampaignStatsItem(
                date,
                1,
                2,
                BigInteger.valueOf(11),
                BigInteger.valueOf(22),
                emptyMap(),
                new OrdersStats(
                        2L,
                        BigDecimal.TEN,
                        BigDecimal.ONE,
                        1L,
                        BigDecimal.TEN,
                        BigDecimal.valueOf(3010L)));
        MainStatsItem item = new MainStatsItem(delegate);
        getChecker().testSerialization(
                item,
                "{\n" +
                        "  \"date\": \"2016-01-01\",\n" +
                        "  \"placeGroup\": 1,\n" +
                        "  \"clicks\": 2,\n" +
                        "  \"spending\": 0.11,\n" +
                        "  \"shows\": \"22\",\n" +
                        "  \"createdOrders\": 2,\n" +
                        "  \"createdOrdersGmv\": 0.10,\n" +
                        "  \"createdOrdersCpaSpending\": 0.01,\n" +
                        "  \"acceptedOrders\": 1,\n" +
                        "  \"acceptedOrdersGmv\": 0.10,\n" +
                        "  \"acceptedOrdersCpaSpending\": 30.10\n" +
                        "}",
                "<main-stats-item \n" +
                        "        date=\"2016-01-01\" \n" +
                        "        place-group=\"1\" \n" +
                        "        clicks=\"2\" \n" +
                        "        spending=\"0.11\" \n" +
                        "        shows=\"22\" \n" +
                        "        created-orders=\"2\" \n" +
                        "        created-orders-gmv=\"0.10\" \n" +
                        "        created-orders-cpa-spending=\"0.01\" \n" +
                        "        accepted-orders=\"1\" \n" +
                        "        accepted-orders-gmv=\"0.10\" \n" +
                        "        accepted-orders-cpa-spending=\"30.10\"/>");
    }

    @Test
    void shouldNotSerializeNullCpaFee() {
        Date date = Date.from(LocalDateTime.of(2016, 1, 1, 0, 0).atOffset(ZoneOffset.UTC).toInstant());
        CampaignStatsItem delegate = new CampaignStatsItem(
                date,
                1,
                2,
                BigInteger.valueOf(11),
                BigInteger.valueOf(22),
                emptyMap(),
                null);
        MainStatsItem item = new MainStatsItem(delegate);
        getChecker().testSerialization(
                item,
                "{\n" +
                        "  \"date\": \"2016-01-01\",\n" +
                        "  \"placeGroup\": 1,\n" +
                        "  \"clicks\": 2,\n" +
                        "  \"shows\": \"22\",\n" +
                        "  \"spending\": 0.11\n" +
                        "}",
                "<main-stats-item \n" +
                        "        date=\"2016-01-01\" \n" +
                        "        place-group=\"1\" \n" +
                        "        clicks=\"2\" \n" +
                        "        shows=\"22\" \n" +
                        "        spending=\"0.11\"/>");
    }

}