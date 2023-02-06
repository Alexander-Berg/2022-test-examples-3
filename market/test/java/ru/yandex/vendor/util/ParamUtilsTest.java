package ru.yandex.vendor.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import ru.yandex.vendor.stats.PlatformType;
import ru.yandex.vendor.stats.StatisticsScaleType;
import ru.yandex.vendor.stats.modelbids.ClickPaymentType;
import ru.yandex.vendor.stats.modelbids.ModelbidsMetricType;
import ru.yandex.vendor.stats.modelbids.ModelbidsStatsContext;

import static org.junit.Assert.assertEquals;

public class ParamUtilsTest {

    @Test
    void testPackModelbidsFilter() {
        int vendorId = 123;
        List<ModelbidsMetricType> metrics = Arrays.asList(ModelbidsMetricType.CHARGES, ModelbidsMetricType.SHOWS);
        ClickPaymentType clickPaymentType = ClickPaymentType.FREE;
        long from = System.currentTimeMillis() - 24 * 60 * 60 * 1000;
        long to = System.currentTimeMillis();
        StatisticsScaleType scaleType = StatisticsScaleType.MONTH;
        List<Long> models = Arrays.asList(100500L, 100501L);
        List<PlatformType> platforms = Collections.singletonList(PlatformType.DESKTOP);
        boolean detailedByModel = true;
        boolean detailedByCategory = false;

        ModelbidsStatsContext expected = ModelbidsStatsContext.newBuilder()
                .setVendorId(vendorId)
                .setUid(1)
                .setFrom(from)
                .setTo(to)
                .setScaleType(scaleType)
                .setModels(models)
                .setGroups(Collections.emptyList())
                .setCategories(Collections.emptyList())
                .setPlatform(platforms)
                .setClickPaymentType(clickPaymentType)
                .setMetrics(metrics)
                .setDetailedByModel(detailedByModel)
                .setDetailedByCategory(detailedByCategory)
                .build();
        ModelbidsStatsContext actual = ParamUtils.unpack(
                ParamUtils.pack(expected, ModelbidsStatsContext.class),
                ModelbidsStatsContext.class);

        assertEquals("source and unpacked objects should be equal", expected, actual);
    }
}
