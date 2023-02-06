package ru.yandex.market.mbo.tms.health;

import java.time.Instant;
import java.time.Period;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.mbo.health.HealthLogger;
import ru.yandex.market.mbo.tms.health.published.guru.GuruCounter;
import ru.yandex.market.mbo.tms.health.published.guru.KpiCategoryStatsReport;

/**
 * @author york
 * @since 11.05.2018
 */
@SuppressWarnings("checkstyle:magicNumber")
public class KpiCategoryStatsReportTest {

    private KpiCategoryStatsReport statsReport;
    private Set<Instant> writtenTimes = new HashSet<>();
    private List<Map<String, Object>> writtenMaps = new ArrayList<>();

    @Before
    public void init() {
        HealthLogger logger = Mockito.mock(HealthLogger.class);
        Mockito.doAnswer(invocation -> {
            writtenTimes.add(invocation.getArgument(0));
            writtenMaps.add(invocation.getArgument(1));
            return null;
        }).when(logger).logTskv(Mockito.any(), Mockito.any());
        statsReport = new KpiCategoryStatsReport(null, //todo: переписать используя моки
            null,
            null,
            null,
            null,
            null,
            logger,
            null,
            null,
            null,
            null) {
            @Override
            protected Map<Long, Long> getModelsCount(Instant prevTime, Instant curTime) {
                return Collections.emptyMap();
            }

            @Override
            protected Map<Long, Long> getOffersCount(Instant prevTime, Instant curTime) {
                return ImmutableMap.<Long, Long>builder()
                    .put(100L, 10010L)
                    .build();
            }
        };
    }

    @Test
    public void testCategoryStatsReport() {
        GuruCounter counter100 = new GuruCounter();
        counter100.setPicturesCount(10);
        counter100.setPublishedOnMarketGuruModelsCount(100);
        counter100.setPublishedOnMarketDel(11);
        counter100.setPublishedOnMarkeNew(12);
        counter100.setPublishedDel(5);
        counter100.setPublishedNew(3);
        counter100.setVendorSourceModels(104);
        counter100.setYangSourceModels(150);
        counter100.setOperatorSourceModels(62);
        counter100.setAutoSourceModels(455);
        counter100.setPublishedGuruModelsCount(34);
        counter100.setPublishedGuruModificationCount(4566);
        GuruCounter counter101 = new GuruCounter();
        Map<Long, GuruCounter> counterMap = new HashMap<>();
        counterMap.put(100L, counter100);
        counterMap.put(101L, counter101);
        Instant start = Instant.now().minus(Period.ofDays(1));
        Instant end = Instant.now().minusMillis(5550);
        statsReport.makeCategoryGuruReports(counterMap, start, end);
        Assert.assertEquals(1, writtenTimes.size());
        Assert.assertEquals(end, writtenTimes.iterator().next());
        Assert.assertEquals(2, writtenMaps.size());

        Map<String, Object> for100 = writtenMaps.get(0);
        Map<String, Object> for101;
        if ((Long) for100.get("category_id") == 100L) {
            for101 = writtenMaps.get(1);
        } else {
            for101 = for100;
            for100 = writtenMaps.get(1);
        }
        compare(counter100, for100);
        compare(counter101, for101);
        Assert.assertEquals(10010L, for100.get("offers_count"));
        Assert.assertEquals(0L, for100.get("models_count"));
        Assert.assertEquals(0L, for101.get("offers_count"));
        Assert.assertEquals(0L, for101.get("models_count"));
    }

    private void compare(GuruCounter counter, Map<String, Object> map) {
        Assert.assertEquals(counter.getPicturesCount(), map.get("guru_models_pictures_count"));

        Assert.assertEquals(counter.getPublishedGuruModelsCount(), map.get("published_guru_models_count"));
        Assert.assertEquals(counter.getPublishedGuruModificationCount(), map.get("published_modifications_count"));
        Assert.assertEquals(counter.getAliasesCount(), map.get("guru_models_aliases_count"));
        Assert.assertEquals(counter.getFilledParamsCount(), map.get("guru_models_filled_params_count"));

        Assert.assertEquals(counter.getPublishedOnMarkeNew(), map.get("published_on_market_new"));
        Assert.assertEquals(counter.getPublishedOnMarketDel(), map.get("published_on_market_del"));

        Assert.assertEquals(counter.getPublishedOnMarketGuruModelsCount(), map.get("published_on_market_count"));
        Assert.assertEquals(counter.getPublishedNew(), map.get("published_models_new"));
        Assert.assertEquals(counter.getPublishedDel(), map.get("published_models_del"));

        Assert.assertEquals(counter.getVendorSourceModels(), map.get("vendor_source_models"));
        Assert.assertEquals(counter.getAutoSourceModels(), map.get("auto_source_models"));
        Assert.assertEquals(counter.getYangSourceModels(), map.get("yang_source_models"));
        Assert.assertEquals(counter.getOperatorSourceModels(), map.get("operator_source_models"));
    }
}
