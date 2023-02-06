package ru.yandex.market.antifraud.orders.service.loyalty.orderCount;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import ru.yandex.market.antifraud.orders.storage.entity.antifraud.YtWalletTransaction;

public class WalletTransactionsFilterTest {

    @Test
    public void promoFilter() {
        var filter = WalletTransactionPromoFilter.builder()
            .promoFilters(List.of("promo1", "promo2"))
            .build();
        Assertions.assertThat(filter.test(YtWalletTransaction.builder()
            .promoKey("promo1, promo3")
            .build())
        ).isTrue();
        Assertions.assertThat(filter.test(YtWalletTransaction.builder()
            .promoKey("promo3, promo4")
            .build())
        ).isFalse();
    }
}
