package ru.yandex.market.replenishment.autoorder.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;
import ru.yandex.market.replenishment.autoorder.exception.UserWarningException;
import ru.yandex.market.replenishment.autoorder.model.InterWarehouseRecommendationsFilter;
import ru.yandex.market.replenishment.autoorder.model.TruckLimits;
import ru.yandex.market.replenishment.autoorder.model.dto.InterWarehouseRecommendationsFilterDTO;
import ru.yandex.market.replenishment.autoorder.model.dto.SetToZeroFilterDTO;
import ru.yandex.market.replenishment.autoorder.utils.TestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class InterWarehouseServiceTest extends FunctionalTest {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Autowired
    private InterWarehouseService interWarehouseService;

    @Test
    @DbUnitDataSet(before = "InterWarehouseServiceTest_generateTrucks.before_clean.csv",
        after = "InterWarehouseServiceTest_generateTrucks.after.csv")
    public void generateTrucksTest() {
        // @NOTE: потенциально тут можно теперь мочить даты через TimeService, но переделывать тест из-за этого
        // немного смысла
        LocalDate today = LocalDate.now();
        assertEquals(today, timeService.getNowDate());

        InterWarehouseRecommendationsFilter filter = new InterWarehouseRecommendationsFilter();
        filter.setDateFrom(DATE_FORMATTER.format(today.plusDays(5)));
        filter.setDateTo(DATE_FORMATTER.format(today.plusDays(15)));

        TruckLimits truckLimits = new TruckLimits();
        truckLimits.setVolume(10);
        truckLimits.setWeight(1);
        truckLimits.setSuppliers(30);
        interWarehouseService.generateTrucks(filter, truckLimits);
    }

    @Test
    @DbUnitDataSet(before = "InterWarehouseServiceTest_generateTrucks.before_over_weight.csv")
    public void generateOverWeightTrucksTest() {
        InterWarehouseRecommendationsFilter filter = new InterWarehouseRecommendationsFilter();
        filter.setDateFrom(DATE_FORMATTER.format(LocalDate.now().plusDays(5)));
        filter.setDateTo(DATE_FORMATTER.format(LocalDate.now().plusDays(15)));

        TruckLimits truckLimits = new TruckLimits();
        truckLimits.setVolume(10);
        truckLimits.setWeight(1);
        truckLimits.setSuppliers(30);

        UserWarningException err = assertThrows(
            UserWarningException.class,
            () -> interWarehouseService.generateTrucks(filter, truckLimits)
        );

        assertEquals("Вес SSKU 000123.1055 больше грузоподъемности грузовика", err.getMessage());
    }

    @Test
    @DbUnitDataSet(before = "InterWarehouseServiceTest_generateTrucks.before_over_volume.csv")
    public void generateOverVolumeTrucksTest() {
        InterWarehouseRecommendationsFilter filter = new InterWarehouseRecommendationsFilter();
        filter.setDateFrom(DATE_FORMATTER.format(LocalDate.now().plusDays(5)));
        filter.setDateTo(DATE_FORMATTER.format(LocalDate.now().plusDays(15)));

        TruckLimits truckLimits = new TruckLimits();
        truckLimits.setVolume(10);
        truckLimits.setWeight(1);
        truckLimits.setSuppliers(30);

        UserWarningException err = assertThrows(
            UserWarningException.class,
            () -> interWarehouseService.generateTrucks(filter, truckLimits)
        );

        assertEquals("Объем SSKU 000123.1055 больше вместимости грузовика", err.getMessage());
    }

    @Test
    @DbUnitDataSet(before = "InterWarehouseServiceTest_setToZero.before.csv",
        after = "InterWarehouseServiceTest_setToZero.after_both.csv")
    public void setToZeroWithBothTest() {
        // @NOTE: если присутствуют и категории, и MSKU, должно удаляться и по категориям, и по MSKU
        SetToZeroFilterDTO setToZeroFilter = new SetToZeroFilterDTO();
        setToZeroFilter.setMskus(Arrays.asList(100510L, 100511L));
        setToZeroFilter.setCategories(Arrays.asList(120L, 122L));

        interWarehouseService.setToZero(setToZeroFilter, "pushkin");
    }

    @Test
    @DbUnitDataSet(before = "InterWarehouseServiceTest_setToZero.before.csv",
        after = "InterWarehouseServiceTest_setToZeroByMsku.after.csv")
    public void setToZeroByMskuOnly() {
        SetToZeroFilterDTO setToZeroFilter = new SetToZeroFilterDTO();
        setToZeroFilter.setMskus(Arrays.asList(100510L, 100511L));

        interWarehouseService.setToZero(setToZeroFilter, "pushkin");
    }

    @Test
    @DbUnitDataSet(before = "InterWarehouseServiceTest_setToZero.before.csv",
        after = "InterWarehouseServiceTest_setToZeroByCategory.after.csv")
    public void setToZeroByCategoryOnly() {
        SetToZeroFilterDTO setToZeroFilter = new SetToZeroFilterDTO();
        setToZeroFilter.setCategories(Arrays.asList(120L, 122L));

        interWarehouseService.setToZero(setToZeroFilter, "pushkin");
    }

    @Test
    @DbUnitDataSet(before = "InterWarehouseServiceTest_setToZero.before.csv",
        after = "InterWarehouseServiceTest_setToZero.after_empty_filter.csv")
    public void setToZeroWithEmptyFilter() {
        SetToZeroFilterDTO setToZeroFilter = new SetToZeroFilterDTO();
        setToZeroFilter.setCategories(Collections.emptyList());
        setToZeroFilter.setMskus(Collections.emptyList());

        interWarehouseService.setToZero(setToZeroFilter, "pushkin");
    }

    @Test
    @DbUnitDataSet(before = "InterWarehouseServiceTest_exportToTM.before.csv",
        after = "InterWarehouseServiceTest_exportToTM.after.csv")
    public void testExportInterWarehouseRecommendationsToTM() {
        TestUtils.mockTimeService(timeService, LocalDateTime.parse("2020-10-10T10:00:00.000"));

        InterWarehouseRecommendationsFilterDTO exportFilter = new InterWarehouseRecommendationsFilterDTO();
        exportFilter.setTruckIdList(List.of(1L, 2L, 3L));

        interWarehouseService.exportInterWarehouseRecommendationsToTM(exportFilter, "pushkin");
    }
}
