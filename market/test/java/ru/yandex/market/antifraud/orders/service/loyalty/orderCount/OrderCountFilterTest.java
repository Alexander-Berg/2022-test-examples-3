package ru.yandex.market.antifraud.orders.service.loyalty.orderCount;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import ru.yandex.market.crm.platform.commons.Uid;
import ru.yandex.market.crm.platform.commons.UidType;
import ru.yandex.market.crm.platform.models.Order;
import ru.yandex.market.crm.platform.models.OrderProperty;

public class OrderCountFilterTest {

    @Test
    public void promoFilter() {
        var filter = OrderCountPromoFilter.builder()
                .propertyName("promoKey")
                .promoFilters(List.of("promo1", "promo2"))
                .build();
        Assertions.assertThat(filter.test(Order.newBuilder()
                .addOrderProperties(OrderProperty.newBuilder().setKey("promoKey").setTextValue("promo1, promo3").build())
                .build())
        ).isTrue();
        Assertions.assertThat(filter.test(Order.newBuilder()
                .addOrderProperties(OrderProperty.newBuilder().setKey("promoKeys").setTextValue("promo1, promo2").build())
                .build())
        ).isFalse();
        Assertions.assertThat(filter.test(Order.newBuilder()
                .addOrderProperties(OrderProperty.newBuilder().setKey("promoKey").setTextValue("promo3, promo4").build())
                .build())
        ).isFalse();
        Assertions.assertThat(filter.test(Order.newBuilder().build())).isFalse();
    }

    @Test
    public void periodFilter() {
        var now = Instant.ofEpochMilli(System.currentTimeMillis());
        var filter = OrderCountPeriodFilter.builder()
                .from(now.minus(Duration.ofDays(2)))
                .to(now)
                .build();
        Assertions.assertThat(filter.test(Order.newBuilder()
                .setCreationDate(now.minus(Duration.ofDays(3)).toEpochMilli())
                .build())
        ).isFalse();
        Assertions.assertThat(filter.test(Order.newBuilder()
                .setCreationDate(now.minus(Duration.ofDays(2)).toEpochMilli())
                .build())
        ).isTrue();
        Assertions.assertThat(filter.test(Order.newBuilder()
                .setCreationDate(now.minus(Duration.ofDays(1)).toEpochMilli())
                .build())
        ).isTrue();
        Assertions.assertThat(filter.test(Order.newBuilder()
                .setCreationDate(now.toEpochMilli())
                .build())
        ).isTrue();
        Assertions.assertThat(filter.test(Order.newBuilder()
                .setCreationDate(now.plus(Duration.ofDays(1)).toEpochMilli())
                .build())
        ).isFalse();
    }

    @Test
    public void mobileFilter() {
        var filter = new OrderCountMobileFilter();
        Assertions.assertThat(filter.test(Order.newBuilder()
                .addUid(Uid.newBuilder().setType(UidType.UUID).setStringValue("uuid").build())
                .addUid(Uid.newBuilder().setType(UidType.CRYPTA_ID1).setStringValue("uuid").build())
                .build())
        ).isTrue();
        Assertions.assertThat(filter.test(Order.newBuilder()
                .addUid(Uid.newBuilder().setType(UidType.CRYPTA_ID1).setStringValue("uuid").build())
                .build())
        ).isFalse();
        Assertions.assertThat(filter.test(Order.newBuilder().build())).isFalse();
    }
}
