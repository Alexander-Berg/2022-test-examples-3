package ru.yandex.market.billing.payment.services;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;

@ParametersAreNonnullByDefault
class DomesticPayoutFromAccrualServiceTest extends FunctionalTest {
    @Autowired
    private DomesticPayoutFromAccrualService domesticPayoutFromAccrualService;

    @Test
    @DisplayName("Генерация payout на основе accrual")
    @DbUnitDataSet(
            before = "DomesticPayoutFromAccrualServiceTest.testGeneratePayoutsByDomesticAccruals.before.csv",
            after = "DomesticPayoutFromAccrualServiceTest.testGeneratePayoutsByDomesticAccruals.after.csv"
    )
    void testGeneratePayoutsByDomesticAccruals() {
        domesticPayoutFromAccrualService.generatePayoutsByAccruals();
    }

    @Test
    @DisplayName("Бросаем исключение, если установлена опция использования списка partner_id без самих partner_id")
    @DbUnitDataSet(
            before = "DomesticPayoutFromAccrualServiceTest.testAllowedPartnerIdsWithoutPartnerIds.before.csv"
    )
    void testThrowExceptionOnPartnerIdsMissing() {
        Exception exception = Assertions.assertThrows(
                IllegalStateException.class,
                domesticPayoutFromAccrualService::generatePayoutsByAccruals
        );
        Assertions.assertEquals(
                "ENABLE_ALLOWED_ONLY_PARTNER is set without actual ALLOWED_PARTNER_IDS", exception.getMessage()
        );
    }

    @Test
    @DisplayName("Генерация payout только для выбранных партнёров")
    @DbUnitDataSet(
            before = "DomesticPayoutFromAccrualServiceTest.testProcessOnlyAllowedPartnerIds.before.csv",
            after = "DomesticPayoutFromAccrualServiceTest.testProcessOnlyAllowedPartnerIds.after.csv"
    )
    void testProcessOnlyAllowedPartnerIds() {
        domesticPayoutFromAccrualService.generatePayoutsByAccruals();
    }
}
