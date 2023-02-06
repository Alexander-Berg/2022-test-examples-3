package ru.yandex.market.mbo.tms.health;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import ru.yandex.market.mbo.health.HealthLogger;
import ru.yandex.market.mbo.tms.health.blue.BlueMarketMetricsCounter;
import ru.yandex.market.mbo.tms.health.blue.BlueMarketMetricsReportService;
import ru.yandex.market.mbo.tms.health.sessions.Scale;

import java.time.Instant;
import java.time.Period;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author york
 * @since 11.05.2018
 */
@SuppressWarnings("checkstyle:magicNumber")
public class BlueMarketMetricsReportServiceTest {

    private BlueMarketMetricsReportService reportService;
    private Set<Instant> writtenDates = new HashSet<>();
    private List<Map<String, Object>> writtenMaps = new ArrayList<>();

    @Before
    public void init() {
        HealthLogger logger = Mockito.mock(HealthLogger.class);
        Mockito.doAnswer(invocation -> {
            writtenDates.add(invocation.getArgument(0));
            writtenMaps.add(invocation.getArgument(1));
            return null;
        }).when(logger).logTskv(Mockito.any(), Mockito.any());
        reportService = new BlueMarketMetricsReportService(logger);
    }

    @Test
    public void testCategoryStatsReport() {
        BlueMarketMetricsCounter counter100 = new BlueMarketMetricsCounter();
        counter100.setModelsPublishedOnlyOnBlueCount(1);
        counter100.setModificationsPublishedOnlyOnBlueCount(2);
        counter100.setTotalSkuCount(3);
        counter100.setSkuPublishedOnWhiteCount(4);
        counter100.setSkuPublishedOnBlueCount(5);
        counter100.setSkuPublishedOnBlueAndWhiteCount(6);
        counter100.setTotalSkusInWarehouse(7);
        counter100.setModelsPublishedOnlyOnBlueCountNew(8);
        counter100.setModificationsPublishedOnlyOnBlueCountNew(9);
        counter100.setTotalSkuCountNew(10);
        counter100.setSkuPublishedOnWhiteCountNew(11);
        counter100.setSkuPublishedOnBlueCountNew(12);
        counter100.setSkuPublishedOnBlueAndWhiteCountNew(13);
        counter100.setModelsPublishedOnlyOnBlueCountDel(14);
        counter100.setModificationsPublishedOnlyOnBlueCountDel(15);
        counter100.setTotalSkuCountDel(16);
        counter100.setSkuPublishedOnWhiteCountDel(17);
        counter100.setSkuPublishedOnBlueCountDel(18);
        counter100.setSkuPublishedOnBlueAndWhiteCountDel(19);
        counter100.setGuruEntitiesPublishedOnWhiteCountCur(20);

        BlueMarketMetricsCounter counter101 = new BlueMarketMetricsCounter();
        Map<Long, BlueMarketMetricsCounter> counterMap = new HashMap<>();
        counterMap.put(100L, counter100);
        counterMap.put(101L, counter101);
        Instant start = Instant.now().minus(Period.ofDays(1));
        reportService.logHealth(counterMap, start, Scale.DAY);
        Assert.assertEquals(1, writtenDates.size());
        Assert.assertEquals(start, writtenDates.iterator().next());
        Assert.assertEquals(2, writtenMaps.size());

        Map<String, Object> for100 = writtenMaps.get(0);
        Map<String, Object> for101 = writtenMaps.get(1);

        compare(counter100, for100);
        compare(counter101, for101);

        Assert.assertEquals("DAY", for100.get("calculated_scale"));
        Assert.assertEquals("DAY", for101.get("calculated_scale"));
    }

    private void compare(BlueMarketMetricsCounter counter, Map<String, Object> map) {

        boolean isOnlyForBlue =
            counter.getModelsPublishedOnlyOnBlueCount() + counter.getModificationsPublishedOnlyOnBlueCount() > 0
                && counter.getGuruEntitiesPublishedOnWhiteCountCur() == 0;

        Assert.assertEquals(counter.getModelsPublishedOnlyOnBlueCount(), map.get("published_models_on_blue_count"));
        Assert.assertEquals(counter.getModificationsPublishedOnlyOnBlueCount(),
            map.get("published_modifications_on_blue_count"));
        Assert.assertEquals(counter.getTotalSkuCount(), map.get("total_sku_count"));
        Assert.assertEquals(counter.getSkuPublishedOnBlueCount(), map.get("sku_on_blue_count"));
        Assert.assertEquals(counter.getSkuPublishedOnBlueAndWhiteCount(), map.get("sku_on_blue_and_white_count"));
        Assert.assertEquals(counter.getTotalSkusInWarehouse(), map.get("sku_in_warehouse_count"));
        Assert.assertEquals(isOnlyForBlue ? 1 : 0,  map.get("categories_exclusively_for_blue_count"));
        Assert.assertEquals(counter.getModelsPublishedOnlyOnBlueCountNew(),
            map.get("published_models_on_blue_count_new"));
        Assert.assertEquals(counter.getModificationsPublishedOnlyOnBlueCountNew(),
            map.get("published_modifications_on_blue_count_new"));
        Assert.assertEquals(counter.getTotalSkuCountNew(), map.get("total_sku_count_new"));
        Assert.assertEquals(counter.getSkuPublishedOnBlueCountNew(), map.get("sku_on_blue_count_new"));
        Assert.assertEquals(counter.getSkuPublishedOnBlueAndWhiteCountNew(),
            map.get("sku_on_blue_and_white_count_new"));
        Assert.assertEquals(counter.getModelsPublishedOnlyOnBlueCountDel(),
            map.get("published_models_on_blue_count_del"));
        Assert.assertEquals(counter.getModificationsPublishedOnlyOnBlueCountDel(),
            map.get("published_modifications_on_blue_count_del"));
        Assert.assertEquals(counter.getTotalSkuCountDel(), map.get("total_sku_count_del"));
        Assert.assertEquals(counter.getSkuPublishedOnBlueCountDel(), map.get("sku_on_blue_count_del"));
        Assert.assertEquals(counter.getSkuPublishedOnBlueAndWhiteCountDel(),
            map.get("sku_on_blue_and_white_count_del"));
    }
}
