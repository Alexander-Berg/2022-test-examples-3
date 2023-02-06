package ru.yandex.market.loyalty.core.model.wallet;

import org.junit.Test;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;

import ru.yandex.market.loyalty.core.model.promo.CashbackPromoBuilder;
import ru.yandex.market.loyalty.core.utils.PromoUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.samePropertyValuesAs;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author <a href="mailto:maratik@yandex-team.ru">Marat Bukharov</a>
 */
public class YandexWalletTransactionTest {
    @Test
    public void shouldBuildFromCopyAllFields() {
        YandexWalletTransaction from = new YandexWalletTransaction(
                0L,
                1L,
                2,
                BigDecimal.valueOf(3),
                null,
                YandexWalletTransactionStatus.CONFIRMED,
                "test-message",
                4,
                Timestamp.from(Instant.now()),
                "test-purchase-token",
                1111L,
                1001L,
                1002L,
                1003L,
                "productId",
                YandexWalletTransactionPriority.LOW,
                "TRREF",
                2,
                LocalDateTime.now(),
                100L,
                YandexWalletRefundTransactionStatus.PROCESSED,
                LocalDateTime.now(),
                LocalDateTime.now(),
                "unique_key",
                1L,
                "1",
                null,
                null);
        YandexWalletTransaction to = from.buildFrom().build();
        assertThat(to, samePropertyValuesAs(from));
    }

    @Test
    public void shouldBuildPartnerCompensationFieldsCorrectly() {
        CashbackPromoBuilder cashbackPromoBuilder = PromoUtils.Cashback.defaultFixed(BigDecimal.ONE);
        cashbackPromoBuilder.setBudgetSourcePartner("mastercard");
        cashbackPromoBuilder.setBudgetSourcePartnerCompensationRate(100);
        cashbackPromoBuilder.setBudgetSourcePartnerCompensationVat(20);

        var result = YandexWalletTransactionPayload.builder()
                .setPartnerCompensation(cashbackPromoBuilder.basePromo(), BigDecimal.valueOf(120L))
                .build();

        assertNotNull(result);
        assertThat(result, allOf(
                hasProperty("partnerCompensationName", equalTo("mastercard")),
                hasProperty("partnerCompensationRate", equalTo(100L)),
                hasProperty("partnerCompensationAmount", comparesEqualTo(BigDecimal.valueOf(12000, 2))),
                hasProperty("partnerCompensationVatRate", equalTo(20L)),
                hasProperty("partnerCompensationVatAmount", comparesEqualTo(BigDecimal.valueOf(2000, 2)))
        ));
    }
}
