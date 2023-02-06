package ru.yandex.market.core.sorting.correction;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.sorting.model.SortingIntakeType;

/**
 * Тест для {@link SortingDailyBillingCorrectionService}.
 */
class SortingSortingDailyBillingCorrectionServiceTest extends FunctionalTest {

    @Autowired
    private SortingDailyBillingCorrectionService sortingDailyBillingCorrectionService;

    @DbUnitDataSet(
            before = "SortingDailyBillingCorrectionServiceTestEmpty.before.csv",
            after = "SortingDailyBillingCorrectionServiceTestEmpty.after.csv"
    )
    @Test
    void testEmpty() {
        sortingDailyBillingCorrectionService.persistDailyBillingCorrections(Collections.emptyList());
    }

    @DbUnitDataSet(
            before = "SortingDailyBillingCorrectionServiceTestPersistList.before.csv",
            after = "SortingDailyBillingCorrectionServiceTestPersistList.after.csv"
    )
    @Test
    void testPersistDailyCorrections() {
        sortingDailyBillingCorrectionService.persistDailyBillingCorrections(getCorrections());
    }

    @DbUnitDataSet(
            before = "SortingDailyBillingCorrectionServiceTestEmpty.before.csv",
            after = "SortingDailyBillingCorrectionServiceTestPersistNegative.after.csv"
    )
    @Test
    void shouldNotThrowExceptionWhenDifferentIntakeTypesGiven() {
        sortingDailyBillingCorrectionService
                .persistDailyBillingCorrections(getDifferentServiceTypeCorrections());
    }

    private List<SortingDailyBillingCorrection> getDifferentServiceTypeCorrections() {
        return List.of(
                SortingDailyBillingCorrection.builder()
                        .setId(1L)
                        .setUid(1L)
                        .setLogin("login1")
                        .setSupplierId(100500L)
                        .setIntakeType(SortingIntakeType.INTAKE)
                        .setCorrectionTimestamp(LocalDate.of(2020, 1, 5).atStartOfDay(ZoneId.systemDefault()).toInstant())
                        .setCorrectedDate(LocalDate.of(2019, 11, 1))
                        .setDescription("mistake1")
                        .setAmount(120000)
                        .build(),
                SortingDailyBillingCorrection.builder()
                        .setId(1L)
                        .setUid(1L)
                        .setLogin("login1")
                        .setSupplierId(100501L)
                        .setIntakeType(SortingIntakeType.SELF_DELIVERY)
                        .setCorrectionTimestamp(LocalDate.of(2020, 1, 5).atStartOfDay(ZoneId.systemDefault()).toInstant())
                        .setCorrectedDate(LocalDate.of(2019, 10, 17))
                        .setDescription("mistake2")
                        .setAmount(25000)
                        .build(),
                SortingDailyBillingCorrection.builder()
                        .setId(1L)
                        .setUid(1L)
                        .setLogin("login1")
                        .setSupplierId(100501L)
                        .setIntakeType(SortingIntakeType.SELF_DELIVERY)
                        .setCorrectionTimestamp(LocalDate.of(2020, 1, 5).atStartOfDay(ZoneId.systemDefault()).toInstant())
                        .setCorrectedDate(LocalDate.of(2019, 10, 18))
                        .setDescription("mistake2")
                        .setAmount(-15000)
                        .build()
        );
    }

    private List<SortingDailyBillingCorrection> getCorrections() {
        return List.of(
                SortingDailyBillingCorrection.builder()
                        .setId(1L)
                        .setUid(1L)
                        .setLogin("login1")
                        .setSupplierId(100500L)
                        .setIntakeType(SortingIntakeType.INTAKE)
                        .setCorrectionTimestamp(LocalDate.of(2020, 1, 5).atStartOfDay(ZoneId.systemDefault()).toInstant())
                        .setCorrectedDate(LocalDate.of(2019, 11, 1))
                        .setDescription("mistake1")
                        .setAmount(120000)
                        .build(),
                SortingDailyBillingCorrection.builder()
                        .setId(1L)
                        .setUid(1L)
                        .setLogin("login1")
                        .setSupplierId(100501L)
                        .setIntakeType(SortingIntakeType.INTAKE)
                        .setCorrectionTimestamp(LocalDate.of(2020, 1, 5).atStartOfDay(ZoneId.systemDefault()).toInstant())
                        .setCorrectedDate(LocalDate.of(2019, 10, 17))
                        .setDescription("mistake2")
                        .setAmount(25000)
                        .build()
        );
    }
}
