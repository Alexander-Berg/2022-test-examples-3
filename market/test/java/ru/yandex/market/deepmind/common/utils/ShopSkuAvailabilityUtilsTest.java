package ru.yandex.market.deepmind.common.utils;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.deepmind.common.availability.AvailabilityInterval;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.WarehouseUsingType;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Msku;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Warehouse;
import ru.yandex.market.mboc.common.utils.availability.PeriodResponse;

import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.MARSHRUT_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.TOMILINO_ID;


@SuppressWarnings({"checkstyle:MagicNumber", "checkstyle:RegexpSingleline"})
public class ShopSkuAvailabilityUtilsTest {
    public static final Warehouse MARSHRUT = new Warehouse()
        .setId(MARSHRUT_ID).setName("Маршрут (Котельники)")
        .setUsingType(WarehouseUsingType.USE_FOR_FULFILLMENT);
    public static final Warehouse TOMILINO = new Warehouse()
        .setId(TOMILINO_ID).setName("Яндекс.Маркет (Томилино)")
        .setUsingType(WarehouseUsingType.USE_FOR_FULFILLMENT);

    @Before
    public void clearDate() {
        ShopSkuAvailabilityUtils.setPeriodFormingDate(LocalDate.of(2019, 1, 1));
    }

    @After
    public void tearDown() {
        ShopSkuAvailabilityUtils.setPeriodFormingDate(null);
    }

    @Test
    public void testSplitByIntervalsOnEmptyList() {
        var result = ShopSkuAvailabilityUtils.splitByIntervals(List.of());

        Assertions.assertThat(result).isEmpty();
    }

    @Test
    public void testSplitByIntervalOnListWithoutDates() {
        var availability = MatrixAvailabilityUtils.mskuInWarehouse(true, msku(), TOMILINO, null, null, null, null);

        var result = ShopSkuAvailabilityUtils.splitByIntervals(List.of(availability));

        Assertions.assertThat(result)
            .containsOnly(Map.entry(AvailabilityInterval.all(), List.of(availability)));
    }

    @Test
    public void testSplitByIntervalWithDates() {
        var delisted = MatrixAvailabilityUtils.offerDelisted(1, "");
        var mskuTo = MatrixAvailabilityUtils.mskuInWarehouse(true, msku(), TOMILINO,
            null, LocalDate.of(2020, 2, 28), null, null);

        var result = ShopSkuAvailabilityUtils.splitByIntervals(List.of(delisted, mskuTo));

        Assertions.assertThat(result)
            .containsOnly(
                Map.entry(AvailabilityInterval.to("2020-02-28"), List.of(delisted, mskuTo)),
                Map.entry(AvailabilityInterval.from("2020-02-29"), List.of(delisted))
            );
    }

    @Test
    public void testSplitByIntervalWithDatesAndPeriods() {
        // 2 ограничения:
        // 1: msku с 2019-02-01 до 2019-02-28
        // 2: сезон с разрешенной доставкой c 20XX-02-15 по 20XX-03-15 и c 20XX-06-01 по 20XX-07-01
        var msku = MatrixAvailabilityUtils.mskuInWarehouse(false, msku(), MARSHRUT,
            LocalDate.of(2019, 2, 1), LocalDate.of(2019, 2, 28), null, null);
        var season = MatrixAvailabilityUtils.mskuInSeason(
            msku().getId(),
            new Warehouse().setId(0L).setName(""),
            new PeriodResponse(0, 0, "15 февраля", "15 марта", "02-15", "03-15"),
            new PeriodResponse(0, 0, "1 июня", "1 июля", "06-01", "07-01")
        );

        var result = ShopSkuAvailabilityUtils.splitByIntervals(List.of(msku, season));

        Assertions.assertThat(result)
            .containsExactly(
                Map.entry(AvailabilityInterval.to("2019-01-31"), List.of(season)),
                Map.entry(AvailabilityInterval.interval("2019-02-01", "2019-02-14"), List.of(msku, season)),
                Map.entry(AvailabilityInterval.interval("2019-02-15", "2019-02-28"), List.of(msku)),
                Map.entry(AvailabilityInterval.interval("2019-03-01", "2019-03-15"), List.of()),
                Map.entry(AvailabilityInterval.interval("2019-03-16", "2019-05-31"), List.of(season)),
                Map.entry(AvailabilityInterval.interval("2019-06-01", "2019-07-01"), List.of()),
                Map.entry(AvailabilityInterval.from("2019-07-02"), List.of(season))
            );
    }

    private Msku msku() {
        return new Msku().setId(0L).setTitle("").setCategoryId(-1L);
    }
}
