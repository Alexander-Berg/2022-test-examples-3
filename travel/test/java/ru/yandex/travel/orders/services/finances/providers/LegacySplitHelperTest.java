package ru.yandex.travel.orders.services.finances.providers;

import java.util.List;

import org.junit.Test;

import ru.yandex.travel.commons.proto.ProtoCurrencyUnit;
import ru.yandex.travel.orders.entities.finances.FinancialEvent;
import ru.yandex.travel.orders.entities.finances.FinancialEventType;
import ru.yandex.travel.orders.services.finances.proto.EMoneyRefundMode;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.travel.orders.services.finances.FinancialEventService.DEFAULT_MONEY_REFUND_MODE;
import static ru.yandex.travel.orders.services.finances.providers.LegacySplitHelper.calculateLegacyPenaltySplit;
import static ru.yandex.travel.orders.services.finances.providers.LegacySplitHelper.calculatePenaltyPlusMoney;
import static ru.yandex.travel.orders.services.finances.providers.LegacySplitHelper.calculatePenaltyPromoMoney;
import static ru.yandex.travel.orders.services.finances.providers.ProviderTestHelper.fullSplit;
import static ru.yandex.travel.orders.services.finances.providers.ProviderTestHelper.sourcesSplit;
import static ru.yandex.travel.testing.misc.TestBaseObjects.rub;

public class LegacySplitHelperTest {
    @Test
    public void calculateLegacyPenaltySplit_deferredPayment() {
        ServiceBalance balance = new ServiceBalance(List.of(), ProtoCurrencyUnit.RUB);
        assertThat(calculateLegacyPenaltySplit(balance, rub(1_000), DEFAULT_MONEY_REFUND_MODE))
                .isEqualTo(sourcesSplit(1_000, 0, 0));
    }

    @Test
    public void calculateLegacyPenaltySplit_activePayment() {
        FinancialEvent e = FinancialEvent.builder()
                .type(FinancialEventType.PAYMENT)
                .partnerAmount(rub(6_000))
                .feeAmount(rub(0))
                .plusPartnerAmount(rub(2_000))
                .promoCodePartnerAmount(rub(2_000))
                .build();
        ServiceBalance balance = new ServiceBalance(List.of(e), ProtoCurrencyUnit.RUB);
        assertThat(calculateLegacyPenaltySplit(balance, rub(6_500), DEFAULT_MONEY_REFUND_MODE))
                .isEqualTo(sourcesSplit(6_000, 500, 0));
    }

    @Test
    public void testCalculatePenaltyPromoMoney() {
        EMoneyRefundMode mode1 = EMoneyRefundMode.MRM_PROMO_MONEY_FIRST;
        assertThat(calculatePenaltyPromoMoney(rub(10_000), rub(3_000), rub(0), mode1)).isEqualTo(rub(3_000));
        assertThat(calculatePenaltyPromoMoney(rub(10_000), rub(3_000), rub(2_000), mode1)).isEqualTo(rub(1_000));
        assertThat(calculatePenaltyPromoMoney(rub(10_000), rub(3_000), rub(4_000), mode1)).isEqualTo(rub(0));

        EMoneyRefundMode mode2 = EMoneyRefundMode.MRM_USER_MONEY_FIRST;
        assertThat(calculatePenaltyPromoMoney(rub(10_000), rub(3_000), rub(0), mode2)).isEqualTo(rub(3_000));
        assertThat(calculatePenaltyPromoMoney(rub(10_000), rub(3_000), rub(2_000), mode2)).isEqualTo(rub(3_000));
        assertThat(calculatePenaltyPromoMoney(rub(10_000), rub(3_000), rub(7_000), mode2)).isEqualTo(rub(3_000));
        assertThat(calculatePenaltyPromoMoney(rub(10_000), rub(3_000), rub(8_000), mode2)).isEqualTo(rub(2_000));
    }

    @Test
    public void testCalculatePenaltyPlusMoney() {
        FullMoneySplit payment = fullSplit(5_000, 2_000, 2_000, 0, 1_000, 0);

        assertThat(calculatePenaltyPlusMoney(payment, rub(3_000), rub(0))).isEqualTo(rub(0));
        assertThat(calculatePenaltyPlusMoney(payment, rub(2_000), rub(0))).isEqualTo(rub(1_000));
        assertThat(calculatePenaltyPlusMoney(payment, rub(1_500), rub(0))).isEqualTo(rub(1_500));
        assertThat(calculatePenaltyPlusMoney(payment, rub(1_000), rub(0))).isEqualTo(rub(2_000));

        assertThat(calculatePenaltyPlusMoney(payment, rub(500), rub(500))).isEqualTo(rub(2_000));
        assertThat(calculatePenaltyPlusMoney(payment, rub(1_000), rub(500))).isEqualTo(rub(1_500));

        assertThat(calculatePenaltyPlusMoney(payment, rub(500), rub(1_000))).isEqualTo(rub(1_500));
        assertThat(calculatePenaltyPlusMoney(payment, rub(1_000), rub(1_000))).isEqualTo(rub(1_000));
        assertThat(calculatePenaltyPlusMoney(payment, rub(1_500), rub(1_000))).isEqualTo(rub(500));
        assertThat(calculatePenaltyPlusMoney(payment, rub(3_000), rub(1_000))).isEqualTo(rub(0));
        assertThat(calculatePenaltyPlusMoney(payment, rub(5_000), rub(1_000))).isEqualTo(rub(0));
    }
}
