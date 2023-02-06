package ru.yandex.market.core.order.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;

import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.PartnerPriceMarkup;
import ru.yandex.market.checkout.checkouter.order.PartnerPriceMarkupCoefficient;
import ru.yandex.market.checkout.checkouter.order.promo.ItemPromo;
import ru.yandex.market.core.billing.DiscountService;
import ru.yandex.market.core.currency.CurrencyConverterService;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.of;

@ExtendWith(MockitoExtension.class)
class MbiOrderItemTest {
    private static final BigDecimal OI_SUBSIDY = new BigDecimal("111");
    private static final BigDecimal OI_BUYER_DISCOUNT = new BigDecimal("222");
    private static final BigDecimal OI_BUYER_SUBSIDY = new BigDecimal("333");
    private static final BigDecimal OI_BUYER_PRICE_NOMINAL = new BigDecimal("444");
    private static final BigDecimal OI_BUYER_PRICE_BEFORE_DISCOUNT = new BigDecimal("555");
    private static final BigDecimal OI_FEED_PRICE = new BigDecimal("666");
    private static final BigDecimal OI_PRICE = new BigDecimal("777");
    private static final BigDecimal OI_PARTNER_PRICE = new BigDecimal("777");
    private static final PartnerPriceMarkup PARTNER_PRICE_MARKUP = new PartnerPriceMarkup();
    private static final Integer OI_FEE_INT = 888;
    private static final Long OI_SUPPLIER_ID = 1000L;
    private static final Object NO_ID = null;
    private static final LocalDateTime UPDATE_TIME = LocalDateTime.of(2019, 5, 13, 0, 0);

    private static final boolean NOT_GLOBAL = false;
    private static final boolean IS_FULFILMENT_TRUE = true;
    private static final BigDecimal SUBSIDY_TOTAL = BigDecimal.valueOf(33);
    private static final BigDecimal BUYERS_ITEM_COUNT_DISCOUNT = BigDecimal.valueOf(44);
    private static final BigDecimal BUYERS_TOTAL_DISCOUNT = BigDecimal.valueOf(55);

    static {
        PARTNER_PRICE_MARKUP.setUpdateTime(UPDATE_TIME.toInstant(ZoneOffset.UTC).toEpochMilli());
        PARTNER_PRICE_MARKUP.setCoefficients(
                Collections.singletonList(
                        new PartnerPriceMarkupCoefficient("marketMultiplier", new BigDecimal("1.04"))
                )
        );
    }

    @Mock
    private CurrencyConverterService currencyConverterService;
    @Mock
    private DiscountService discountService;

    private static Stream<Arguments> supplierIdForColor() {
        return Stream.of(
                of(
                        "DSBS",
                        Color.WHITE,
                        NO_ID,
                        OI_SUPPLIER_ID,
                        (Consumer<OrderItem>) oi -> oi.setSupplierId(OI_SUPPLIER_ID)
                ),
                of(
                        "Blue",
                        Color.BLUE,
                        OI_SUPPLIER_ID,
                        NO_ID,
                        (Consumer<OrderItem>) oi -> oi.setSupplierId(OI_SUPPLIER_ID)
                ),
                of(
                        "Blue + not specified supplier_id",
                        Color.BLUE,
                        NO_ID,
                        NO_ID,
                        (Consumer<OrderItem>) oi -> {
                        }
                )
        );
    }

    private static Stream<Arguments> getDimensionsErrors() {
        return Stream.of(
                of(
                        "Dimensions sum is negative",
                        MbiOrderItem.builder().setWidth(-1L).setHeight(0L).setDepth(0L).build(),
                        "Dimensions sum must be positive"
                )
        );
    }

    @DisplayName("Ограничения на dimensions")
    @MethodSource("getDimensionsErrors")
    @ParameterizedTest(name = "{0}")
    void test_getDimensionsErrors(
            @SuppressWarnings("unused") String descr,
            MbiOrderItem mbiOrderItem, String expectedMsg) {
        final IllegalStateException ex = Assertions.assertThrows(
                IllegalStateException.class,
                mbiOrderItem::getDimensionsSum
        );
        Assertions.assertEquals(expectedMsg, ex.getMessage());
    }

    @Test
    void testGetBillingPrice() {
        MbiOrderItem mbiOrderItem = MbiOrderItem.builder()
                .setPrice(new BigDecimal("123.456"))
                .build();
        assertThat(mbiOrderItem.getBillingPrice(), is(new BigDecimal("123.456")));
    }

    @Test
    void test_atSupplierWarehouse() {
        assertTrue(Preconditions.checkNotNull(MbiOrderItem.builder()
                .setAtSupplierWarehouse(true)
                .build()
                .isAtSupplierWarehouse()));
    }

    @Test
    void testGetBillingPriceWithSubsidy() {
        MbiOrderItem mbiOrderItem = MbiOrderItem.builder()
                .setPrice(new BigDecimal("123.456"))
                .setSubsidy(new BigDecimal("1000"))
                .setAtSupplierWarehouse(false)
                .build();
        assertThat(mbiOrderItem.getBillingPrice(), is(new BigDecimal("1123.456")));
    }

    @Test
    void testGetDimensionsSum() {
        MbiOrderItem mbiOrderItem = MbiOrderItem.builder()
                .setWidth(1L)
                .setHeight(20L)
                .setDepth(300L)
                .build();

        assertThat(mbiOrderItem.getDimensionsSum(), is(321L));
    }

    @Test
    void testNullDimensionsSum() {
        MbiOrderItem mbiOrderItem = MbiOrderItem.builder()
                .setWidth(0L)
                .setHeight(0L)
                .setDepth(0L)
                .build();

        assertNull(mbiOrderItem.getDimensionsSum());
    }

    @DisplayName("Трансляция поля supplierId")
    @MethodSource("supplierIdForColor")
    @ParameterizedTest(name = "{0}")
    void test_toMbiOrderItemList_supplierId(
            @SuppressWarnings("unused") String descr,
            Color orderColor,
            Long expectedMbiSupplierId,
            Long expectedMbiFfShopId,
            Consumer<OrderItem> consumer
    ) {

        final OrderItem orderItem = prepareSomeOrderItem();
        final Order order = prepareSomeOrder(orderColor);
        consumer.accept(orderItem);

        final Collection<MbiOrderItem> mbiOrderItems = MbiOrderItem.toMbiOrderItemList(
                order,
                ImmutableList.of(orderItem),
                currencyConverterService,
                discountService,
                null);

        final MbiOrderItem mbiOrderItem = mbiOrderItems.iterator().next();
        assertEquals(mbiOrderItem.getFfSupplierId(), expectedMbiSupplierId);
        assertEquals(mbiOrderItem.getFulfilmentShopId(), expectedMbiFfShopId);
    }

    @Test
    void test_toMbiOrderItemList_partnerPriceMarkups() {
        final OrderItem orderItem = prepareSomeOrderItem();
        final Order order = prepareSomeOrder(Color.BLUE);

        final Collection<MbiOrderItem> mbiOrderItems = MbiOrderItem.toMbiOrderItemList(
                order,
                ImmutableList.of(orderItem),
                currencyConverterService,
                discountService,
                null);

        final MbiOrderItem mbiOrderItem = mbiOrderItems.iterator().next();
        assertEquals(OI_PARTNER_PRICE.movePointRight(2), mbiOrderItem.getPartnerPrice());
    }

    @DisplayName("Трансляция полей itemPromo")
    @Test()
    void test_toMbiOrderItemList_promo() {
        final Set<ItemPromo> promos = Set.of(
                ItemPromo.cashbackPromo(BigDecimal.ONE, null, "promo-1"),
                ItemPromo.cashbackPromo(BigDecimal.TEN,  "promo-2",
                        OI_SUPPLIER_ID, BigDecimal.valueOf(1.5), BigDecimal.valueOf(3.5))
        );
        final OrderItem orderItem = prepareSomeOrderItem();
        orderItem.setPromos(promos);
        final Order order = prepareSomeOrder(Color.BLUE);

        final Collection<MbiOrderItem> mbiOrderItems = MbiOrderItem.toMbiOrderItemList(
                order,
                ImmutableList.of(orderItem),
                currencyConverterService,
                discountService,
                null);

        MbiOrderItem mbiOrderItem = mbiOrderItems.iterator().next();

        assertThat(mbiOrderItem.getPromos(), containsInAnyOrder(
                allOf(
                        hasProperty("partnerId", nullValue()),
                        hasProperty("cashbackAccrualAmount", equalTo(100L)),
                        hasProperty("marketCashbackPercent", nullValue()),
                        hasProperty("partnerCashbackPercent", nullValue())
                ),
                allOf(
                        hasProperty("partnerId", equalTo(OI_SUPPLIER_ID)),
                        hasProperty("cashbackAccrualAmount", equalTo(1000L)),
                        hasProperty("marketCashbackPercent", equalTo(BigDecimal.valueOf(1.5))),
                        hasProperty("partnerCashbackPercent", equalTo(BigDecimal.valueOf(3.5)))
                )
        ));

        assertEquals(OI_PARTNER_PRICE.movePointRight(2), mbiOrderItem.getPartnerPrice());
    }

    private Order prepareSomeOrder(Color rgbColor) {
        final Order order = new Order(
                NOT_GLOBAL,
                IS_FULFILMENT_TRUE,
                SUBSIDY_TOTAL,
                BUYERS_ITEM_COUNT_DISCOUNT,
                BUYERS_ITEM_COUNT_DISCOUNT,
                BUYERS_TOTAL_DISCOUNT,
                BUYERS_TOTAL_DISCOUNT
        );
        order.setRgb(rgbColor);
        return order;
    }

    private OrderItem prepareSomeOrderItem() {
        final OrderItem orderItem
                = new OrderItem(
                OI_SUBSIDY,
                OI_BUYER_DISCOUNT,
                OI_BUYER_SUBSIDY,
                OI_BUYER_PRICE_NOMINAL,
                OI_BUYER_PRICE_BEFORE_DISCOUNT,
                OI_FEED_PRICE,
                OI_PARTNER_PRICE,
                PARTNER_PRICE_MARKUP
        );
        orderItem.setCount(1);
        orderItem.setFeeInt(OI_FEE_INT);
        orderItem.setPrice(OI_PRICE);
        return orderItem;
    }

}
