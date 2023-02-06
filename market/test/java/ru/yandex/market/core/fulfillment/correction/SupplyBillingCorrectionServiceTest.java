package ru.yandex.market.core.fulfillment.correction;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;

import org.hamcrest.Matcher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.fulfillment.model.BillingServiceType;
import ru.yandex.market.mbi.util.MbiMatchers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

/**
 * {@link SupplyBillingCorrectionService}
 */
class SupplyBillingCorrectionServiceTest extends FunctionalTest {
    private static final Long SUPPLIER_ID = 12L;
    private static final LocalDate START_DATE = LocalDate.of(2019, Month.FEBRUARY, 1);
    private static final LocalDate END_DATE = LocalDate.of(2019, Month.MARCH, 1);
    private static final long UID = 55;
    @Autowired
    private SupplyBillingCorrectionService correctionService;

    private static Matcher<SupplyBillingCorrection> createCorrection(
            long id,
            String shopSku,
            BillingServiceType serviceType,
            Instant correctionTime,
            int amount
    ) {
        return MbiMatchers.<SupplyBillingCorrection>newAllOfBuilder()
                .add(SupplyBillingCorrection::getId, id)
                .add(SupplyBillingCorrection::getUid, UID)
                .add(SupplyBillingCorrection::getLogin, "ya")
                .add(SupplyBillingCorrection::getSupplierId, 12L)
                .add(SupplyBillingCorrection::getShopSku, shopSku)
                .add(SupplyBillingCorrection::getServiceType, serviceType)
                .add(SupplyBillingCorrection::getCorrectionTimestamp, correctionTime)
                .add(SupplyBillingCorrection::getDescription, "comment")
                .add(SupplyBillingCorrection::getAmount, amount)
                .build();
    }

    private static SupplyBillingCorrection createCorrection(BillingServiceType serviceType, int amount) {
        return SupplyBillingCorrection.builder()
                .setUid(UID)
                .setLogin("ya")
                .setSupplierId(12)
                .setShopSku("sku_1")
                .setServiceType(serviceType)
                .setCorrectionTimestamp(getCorrectionTime(2019, 2, 5, 12, 40, 32))
                .setDescription("comment")
                .setAmount(amount)
                .build();
    }

    private static Instant getCorrectionTime(int year, int month, int dayOfMonth, int hour, int minute, int second) {
        return LocalDateTime.of(year, month, dayOfMonth, hour, minute, second)
                .atZone(ZoneId.systemDefault())
                .toInstant();
    }

    @Test
    @DisplayName("Создание корректировки")
    @DbUnitDataSet(
            before = "SupplyBillingCorrectionServiceTest.addCorrection.before.csv",
            after = "SupplyBillingCorrectionServiceTest.addCorrection.after.csv"
    )
    void test_addCorrection() {
        correctionService.addCorrection(
                createCorrection(BillingServiceType.FF_WITHDRAW, -95), UID
        );
        correctionService.addCorrection(
                createCorrection(BillingServiceType.FF_WITHDRAW, -45), UID
        );
        correctionService.addCorrection(
                createCorrection(BillingServiceType.FF_STORAGE_BILLING, 80), UID
        );
    }

    @Test
    @DisplayName("Корректировки биллинга изъятий")
    @DbUnitDataSet(before = "SupplyBillingCorrectionServiceTest.corrections.before.csv")
    void test_getWithdrawCorrection() {
        var corrections = correctionService.getWithdrawCorrections(SUPPLIER_ID, START_DATE, END_DATE);
        assertThat(corrections, hasSize(1));
        assertThat(corrections.get(0), createCorrection(
                1,
                "sku_2",
                BillingServiceType.FF_WITHDRAW,
                getCorrectionTime(2019, 2, 28, 23, 59, 59),
                -67
        ));
    }

    @Test
    @DisplayName("Корректировки биллинга хранения")
    @DbUnitDataSet(before = "SupplyBillingCorrectionServiceTest.corrections.before.csv")
    void test_getStorageCorrection() {
        var corrections = correctionService.getStorageCorrections(SUPPLIER_ID, START_DATE, END_DATE);
        assertThat(corrections, hasSize(1));
        assertThat(corrections.get(0), createCorrection(
                5,
                "sku_1",
                BillingServiceType.FF_STORAGE_BILLING,
                getCorrectionTime(2019, 2, 1, 0, 0, 0),
                -35
        ));
    }
}
