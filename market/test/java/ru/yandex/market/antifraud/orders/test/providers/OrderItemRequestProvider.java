package ru.yandex.market.antifraud.orders.test.providers;

import java.math.BigDecimal;
import java.util.Random;

import javax.validation.constraints.NotNull;

import ru.yandex.market.antifraud.orders.web.dto.checkouter.OrderItemRequestDto;

public abstract class OrderItemRequestProvider {
    private static final long DEFAULT_TEST_FEED_ID = 1L;
    private static final String DEFAULT_TEST_SHOP_SKU = "shop_sku_test";
    private static final Long DEFAULT_TEST_MODEL_ID = 13334267L;
    private static final Integer DEFAULT_TEST_CATEGORY_ID = 123;
    private static final BigDecimal DEFAULT_TEST_PRICE = BigDecimal.valueOf(250L);
    private static final Integer DEFAULT_TEST_COUNT = 1;
    private static Random random = new Random(100500L);

    @NotNull
    public static OrderItemRequestDto getOrderItem() {
        return getPreparedBuilder().build();
    }

    public static OrderItemRequestDto.OrderItemRequestDtoBuilder getPreparedBuilder() {
        return OrderItemRequestDto.builder()
                .id(getTestId())
                .feedId(DEFAULT_TEST_FEED_ID)
                .shopSku(DEFAULT_TEST_SHOP_SKU)
                .modelId(DEFAULT_TEST_MODEL_ID)
                .categoryId(DEFAULT_TEST_CATEGORY_ID)
                .price(DEFAULT_TEST_PRICE)
                .count(DEFAULT_TEST_COUNT);
    }

    @NotNull
    public static OrderItemRequestDto buildOrderItem(String offerId, Long itemId, int count) {
        return new OrderItemRequestDto(itemId,
                DEFAULT_TEST_FEED_ID,
                offerId,
                null,
                DEFAULT_TEST_SHOP_SKU,
                null,
                DEFAULT_TEST_MODEL_ID,
                DEFAULT_TEST_CATEGORY_ID,
                DEFAULT_TEST_PRICE,
                count,
                null);
    }

    @NotNull
    public static OrderItemRequestDto buildOrderItem(String offerId) {
        return buildOrderItem(offerId, null, 1);
    }

    private static long getTestId() {
        return Integer.valueOf(random.nextInt(Integer.MAX_VALUE)).longValue();
    }
}
