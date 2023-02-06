package ru.yandex.market.billing.fulfillment.tariffs;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.billing.config.OldFirstPartySuppliersIds;
import ru.yandex.market.billing.core.order.model.ValueType;
import ru.yandex.market.billing.fulfillment.OrderType;
import ru.yandex.market.billing.fulfillment.orders.commission.OrderFeeFinder;
import ru.yandex.market.billing.model.billing.BillingServiceType;
import ru.yandex.market.billing.order.model.BillingUnit;
import ru.yandex.market.billing.service.environment.EnvironmentService;
import ru.yandex.market.core.order.model.MbiOrderItemPromo;
import ru.yandex.market.core.order.model.OrderItemForBilling;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.billing.fulfillment.tariffs.TariffValueMatcher.hasBillingUnit;
import static ru.yandex.market.billing.fulfillment.tariffs.TariffValueMatcher.hasMaxValue;
import static ru.yandex.market.billing.fulfillment.tariffs.TariffValueMatcher.hasMinValue;
import static ru.yandex.market.billing.fulfillment.tariffs.TariffValueMatcher.hasValue;
import static ru.yandex.market.billing.fulfillment.tariffs.TariffValueMatcher.hasValueType;
import static ru.yandex.market.billing.model.billing.BillingServiceType.FEE;
import static ru.yandex.market.checkout.checkouter.order.promo.PromoType.CASHBACK;
import static ru.yandex.market.test.order.OrderTestData.orderItemPromoBuilder;
import static ru.yandex.market.test.order.OrderTestData.orderPromo;

public class PriceAndTariffValueServiceTest {

    private static final long SUPPLIER_ID = 1L;
    private static final long SUPPLIER_ID_MIN_FEE = 2L;

    //хардкодим здесь параллельно сервису, чтобы тест сломался при случайном изменении константы в коде
    static final long PLEER_RU_SUPPLIER_ID = 1007095L;
    static final long PLEER_RU_SUPPLIER_ID_2 = 1194319L;

    private static final OrderItemForBilling ORDER_ITEM_FOR_BILLING = getOrderItemForBillingBuilder().build();
    private static final OrderItemForBilling ORDER_ITEM_FOR_BILLING_MIN_FEE = getOrderItemForBillingBuilder()
            .withSupplierId(SUPPLIER_ID_MIN_FEE)
            .build();
    private static final OrderItemForBilling ORDER_ITEM_FOR_BILLING_CASH_ONLY = getOrderItemForBillingBuilder()
            .withCashOnlyOrder(true).build();
    private static final OrderItemForBilling ORDER_ITEM_FOR_BILLING_PLEER_RU_CASH_ONLY = getOrderItemForBillingBuilder()
            .withCashOnlyOrder(true).withSupplierId(PLEER_RU_SUPPLIER_ID).build();
    private static final OrderItemForBilling ORDER_ITEM_FOR_BILLING_PLEER_RU_NO_CASH_ONLY =
            getOrderItemForBillingBuilder()
                    .withCashOnlyOrder(false).withSupplierId(PLEER_RU_SUPPLIER_ID_2).build();
    private static final OrderItemForBilling ORDER_ITEM_FOR_BILLING_CASH_ONLY_MIN_FEE = getOrderItemForBillingBuilder()
            .withCashOnlyOrder(true)
            .withSupplierId(SUPPLIER_ID_MIN_FEE)
            .build();

    private static final LocalDate MARCH_15_2022 = LocalDate.of(2022, Month.MARCH, 15);

    private OrderFeeFinder orderFeeFinder;
    private FulfillmentTariffService fulfillmentTariffService;

    private PriceAndTariffValueService service;
    private EnvironmentService environmentService;
    private OldFirstPartySuppliersIds oldFirstPartySuppliersIds;

    private static OrderItemForBilling.Builder getOrderItemForBillingBuilder() {
        return new OrderItemForBilling.Builder()
                .withDimensionsSum(3L)
                .withDepth(1L)
                .withWidth(1L)
                .withHeight(1L)
                .withWeight(1L)
                .withRealLength(100L)
                .withRealHeight(100L)
                .withRealWidth(100L)
                .withRealWeight(BigDecimal.valueOf(0.1D))
                .withBillingPrice(BigDecimal.valueOf(310))
                .withSupplierId(SUPPLIER_ID)
                .withCategoryId(90589)
                .withItemId(0L)
                .withOrderId(0L)
                .withOrderCreatedAt(LocalDate.of(2021, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant())
                .withFeedId(0L)
                .withOfferId("")
                .withPrice(BigDecimal.ZERO)
                .withCount(0)
                .withDropshipItem(false)
                .withDSBSItem(false)
                .withClickAndCollectItem(false)
                .withCrossdock(false)
                .withExpressOrder(false)
                .withCashOnlyOrder(false);
    }

    private static Stream<Arguments> loyaltyParticipationFeeTestData() {
        var orderItemBuilder = getOrderItemForBillingBuilder()
                .withOrderCreatedAt(LocalDate.of(2021, 9, 1).atStartOfDay(ZoneId.systemDefault()).toInstant())
                .withPrice(new BigDecimal(10000L));
        var orderPromo = orderPromo(null, CASHBACK, "promo-1");

        return Stream.of(
                Arguments.of(
                        false,
                        orderItemBuilder
                                .withIsLoyaltyProgramPartner(true)
                                .build(),
                        // кэшбэк начислялся, партнерский тариф 10%
                        List.of(orderItemPromoBuilder(orderPromo)
                                .setCashbackAccrualAmount(100)
                                .setPartnerId(SUPPLIER_ID)
                                .setPartnerCashbackPercent(BigDecimal.TEN)
                                .build()),
                        priceAndTariffValue(BigDecimal.valueOf(1000L), 1000)
                ),
                Arguments.of(
                        false,
                        orderItemBuilder
                                .withIsLoyaltyProgramPartner(true)
                                .build(),
                        // кэшбэк начислялся, но платит Маркет (партнерский тариф 0%)
                        List.of(orderItemPromoBuilder(orderPromo)
                                .setCashbackAccrualAmount(100)
                                .setPartnerId(SUPPLIER_ID)
                                .setPartnerCashbackPercent(BigDecimal.ZERO)
                                .build()),
                        priceAndTariffValue(BigDecimal.ZERO, 0)
                ),
                Arguments.of(
                        false,
                        orderItemBuilder
                                .withIsLoyaltyProgramPartner(true)
                                .build(),
                        // кэшбэк 100 баллов, партнерский тариф 10%, Маркет тоже 10%, но мы биллим от цены
                        List.of(orderItemPromoBuilder(orderPromo)
                                .setCashbackAccrualAmount(100)
                                .setPartnerId(SUPPLIER_ID)
                                .setMarketCashbackPercent(BigDecimal.TEN)
                                .setPartnerCashbackPercent(BigDecimal.TEN)
                                .build()),
                        // партнер компенсирует 10% от цены, а не половину от начисленного кэшбэка
                        priceAndTariffValue(BigDecimal.valueOf(1000L), 1000)
                ),
                Arguments.of(
                        // если есть хотя бы один айтем с полностью заполненным промо
                        false,
                        orderItemBuilder
                                .withIsLoyaltyProgramPartner(null)
                                .build(),
                        // кэшбэк начислен, партнерский тариф 10%
                        List.of(orderItemPromoBuilder(orderPromo)
                                .setCashbackAccrualAmount(100)
                                .setPartnerId(SUPPLIER_ID)
                                .setPartnerCashbackPercent(BigDecimal.TEN)
                                .build()),
                        priceAndTariffValue(BigDecimal.valueOf(1000L), 1000)
                ),
                Arguments.of(
                        false,
                        orderItemBuilder
                                .withIsLoyaltyProgramPartner(false)
                                .build(),
                        // начислено 100 баллов кэшбэка, платят Маркет и партнер пополам
                        List.of(
                                // кэшбэк начислен, партнерский тариф 3.5%
                                orderItemPromoBuilder(orderPromo)
                                        .setCashbackAccrualAmount(100)
                                        .setPartnerId(SUPPLIER_ID)
                                        .setPartnerCashbackPercent(BigDecimal.valueOf(3.5))
                                        .build(),
                                // кэшбэк начислен, партнерский кэшбэк 1.5%
                                orderItemPromoBuilder(orderPromo)
                                        .setCashbackAccrualAmount(200)
                                        .setPartnerId(SUPPLIER_ID)
                                        .setPartnerCashbackPercent(BigDecimal.valueOf(1.5))
                                        .build()
                        ),
                        // партнер компенсирует 100 * (3.5 + 1.5) = 5 рублей
                        // тариф - сумма процентов по всем акциям: (3.5 + 1.5) * 100
                        priceAndTariffValue(BigDecimal.valueOf(500L), 500)
                ),
                Arguments.of(
                        false,
                        orderItemBuilder.build(),
                        // начислено 100 баллов кэшбэка, платят Маркет и партнер пополам
                        List.of(
                                // кэшбэк начислен, партнерский тариф 3.5%
                                orderItemPromoBuilder(orderPromo)
                                        .setCashbackAccrualAmount(1000000000) // пофиг, сколько балов начислили
                                        .setPartnerId(SUPPLIER_ID)
                                        .setMarketCashbackPercent(BigDecimal.valueOf(15)) //пофиг сколько платит Маркет
                                        .setPartnerCashbackPercent(BigDecimal.valueOf(3.5))
                                        .build(),
                                // кэшбэк начислен, но акция не мерча, а кого-то другого
                                orderItemPromoBuilder(orderPromo)
                                        .setCashbackAccrualAmount(200)
                                        // партнер - "виновник" акции - не мерч
                                        .setPartnerId(42L)
                                        .setPartnerCashbackPercent(BigDecimal.valueOf(1.5))
                                        .build()
                        ),
                        // партнер компенсирует только первую акцию
                        priceAndTariffValue(BigDecimal.valueOf(350L), 350)
                )
        );
    }

    private static PriceAndTariffValue priceAndTariffValue(BigDecimal price, int tariffValue) {
        return new PriceAndTariffValue(price, price, Currency.RUR,
                new TariffValue(tariffValue, ValueType.RELATIVE, BillingUnit.ITEM));
    }

    @BeforeEach
    public void init() throws PriceAndTariffValueServiceException {
        orderFeeFinder = mock(OrderFeeFinder.class);
        doReturn(1).when(orderFeeFinder).findFee(anyLong(), anyLong(), eq(BillingServiceType.REF_MIN_PRICE_DISCOUNT),
                eq(false));
        doReturn(200).when(orderFeeFinder).findFee(anyLong(), anyLong(), any(BillingServiceType.class), eq(false));
        doReturn(1L).when(orderFeeFinder).findMinFee(anyLong(), eq(SUPPLIER_ID), any(BillingServiceType.class));
        doReturn(19L).when(orderFeeFinder).findMinFee(anyLong(), eq(SUPPLIER_ID_MIN_FEE),
                any(BillingServiceType.class));
        doReturn(400).when(orderFeeFinder).findCashOnlyFee(anyLong(), anyLong(), any(BillingServiceType.class));
        doReturn(0).when(orderFeeFinder).findCashOnlyFee(anyLong(), eq(PLEER_RU_SUPPLIER_ID),
                any(BillingServiceType.class));
        doReturn(0).when(orderFeeFinder).findCashOnlyFee(anyLong(), eq(PLEER_RU_SUPPLIER_ID_2),
                any(BillingServiceType.class));
        fulfillmentTariffService = mock(FulfillmentTariffService.class);

        service = mock(PriceAndTariffValueService.class);
        doReturn(orderFeeFinder).when(service).getOrderFeeFinder(isNull(), isNull(), isNull());
        doReturn(fulfillmentTariffService).when(service).getFulfillmentTariffService(isNull(), isNull());
        doCallRealMethod().when(service).getRawAmount(any(TariffValue.class), any(BigDecimal.class));
        doCallRealMethod().when(service).getRawAmountBeforeMin(any(TariffValue.class), any(BigDecimal.class));
        doCallRealMethod().when(service).getForOrderItem(
                any(LocalDate.class), any(BillingServiceType.class), any(OrderItemForBilling.class),
                any(), anyBoolean(), anyMap(), anyMap());
        doCallRealMethod()
                .when(service)
                .getForFulfillmentService(any(LocalDate.class), any(BillingServiceType.class),
                        any(OrderItemForBilling.class),
                        anyMap(), anyMap());

        doCallRealMethod().when(service).getForFee(
                any(OrderItemForBilling.class), any(), any(BillingServiceType.class), any(), anyBoolean()
        );
        doCallRealMethod()
                .when(service)
                .init(isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), any(), isNull(), any());
        environmentService = mock(EnvironmentService.class);
        when(environmentService.getBooleanValue(eq("market.mbi-billing.cash_only_fee_enabled"),
                anyBoolean())).thenReturn(true);
        oldFirstPartySuppliersIds = mock(OldFirstPartySuppliersIds.class);
        when(oldFirstPartySuppliersIds.getYndxStationSubscriptionSupplierId()).thenReturn(2476913L);
        doCallRealMethod().when(service).getBillingPrice(any(), any(), any());
        doCallRealMethod().when(service).isBillingByBatch(any(), any(), any());
        doReturn(true).when(service).isBillingByBatchIsEnabled();
        service.init(null, null, null, null, null, null, null, environmentService, null, oldFirstPartySuppliersIds);
    }

    @Test
    @DisplayName("Обилливание fee для заказа.")
    public void testGetForFee() {
        PriceAndTariffValue actual = service.getForFee(ORDER_ITEM_FOR_BILLING, emptyList(),
                FEE, MARCH_15_2022, false);

        assertNotNull(actual);
        assertThat(actual.getPrice(), comparesEqualTo(new BigDecimal("6.2")));
        assertEquals(200, actual.getTariffValue().getValue());
        assertEquals(ValueType.RELATIVE, actual.getTariffValue().getValueType());
        assertEquals(BillingUnit.ITEM, actual.getTariffValue().getBillingUnit());
        assertEquals(1, actual.getTariffValue().getMinValue().longValue());
        verify(orderFeeFinder, times(1)).findFee(anyLong(), anyLong(), any(BillingServiceType.class), eq(false));
    }

    @Test
    @DisplayName("Обилливание fee для cash-only заказа.")
    public void testCashOnlyFee() {
        PriceAndTariffValue actualCashOnly = service.getForFee(ORDER_ITEM_FOR_BILLING_CASH_ONLY, emptyList(),
                FEE, MARCH_15_2022, false);

        assertNotNull(actualCashOnly);
        assertThat(actualCashOnly.getPrice(), comparesEqualTo(new BigDecimal("18.6")));
        assertEquals(600, actualCashOnly.getTariffValue().getValue());
        assertEquals(ValueType.RELATIVE, actualCashOnly.getTariffValue().getValueType());
        assertEquals(BillingUnit.ITEM, actualCashOnly.getTariffValue().getBillingUnit());
        assertEquals(1, actualCashOnly.getTariffValue().getMinValue().longValue());
        verify(orderFeeFinder, times(1)).findFee(anyLong(), anyLong(), any(BillingServiceType.class), eq(false));
    }

    @Test
    @DisplayName("MinFee перекрывает сумму от cash-only fee.")
    public void testCashOnlyFeeMinFee() {
        PriceAndTariffValue actualCashOnly = service.getForFee(ORDER_ITEM_FOR_BILLING_CASH_ONLY_MIN_FEE, emptyList(),
                FEE, MARCH_15_2022, false);

        PriceAndTariffValue actualOrdinary = service.getForFee(ORDER_ITEM_FOR_BILLING_MIN_FEE, emptyList(),
                FEE, MARCH_15_2022, false);

        assertNotNull(actualCashOnly);
        // с применением minFee суммы одинаковые
        assertThat(actualCashOnly.getPrice(), comparesEqualTo(new BigDecimal("19")));
        assertThat(actualOrdinary.getPrice(), comparesEqualTo(new BigDecimal("19")));
        // до применения minFee биллим правильно cash-only и обычный заказ
        assertThat(actualCashOnly.getPriceBeforeMin(), comparesEqualTo(new BigDecimal("18.6")));
        assertThat(actualOrdinary.getPriceBeforeMin(), comparesEqualTo(new BigDecimal("6.2")));
        assertEquals(600, actualCashOnly.getTariffValue().getValue());
        assertEquals(200, actualOrdinary.getTariffValue().getValue());
        assertEquals(ValueType.RELATIVE, actualCashOnly.getTariffValue().getValueType());
        assertEquals(BillingUnit.ITEM, actualCashOnly.getTariffValue().getBillingUnit());
        assertEquals(19, actualCashOnly.getTariffValue().getMinValue().longValue());
        assertEquals(19, actualOrdinary.getTariffValue().getMinValue().longValue());
        verify(orderFeeFinder, times(2)).findFee(anyLong(), anyLong(), any(BillingServiceType.class), eq(false));
        verify(orderFeeFinder, times(1)).findCashOnlyFee(anyLong(), anyLong(), any(BillingServiceType.class));
    }

    @Test
    @DisplayName("Проверяем пониженный процент для Pleer.ru")
    void testPleerRuCashOnlyFee() {
        PriceAndTariffValue actualCashOnly = service.getForFee(ORDER_ITEM_FOR_BILLING_PLEER_RU_CASH_ONLY, emptyList(),
                FEE, MARCH_15_2022, false);

        assertNotNull(actualCashOnly);
        assertThat(actualCashOnly.getPrice(), comparesEqualTo(new BigDecimal("6.2000")));
        assertEquals(200, actualCashOnly.getTariffValue().getValue());
        assertEquals(ValueType.RELATIVE, actualCashOnly.getTariffValue().getValueType());
        assertEquals(BillingUnit.ITEM, actualCashOnly.getTariffValue().getBillingUnit());
        // вернется 0 как моковое значение
        assertEquals(0, actualCashOnly.getTariffValue().getMinValue().longValue());
        verify(orderFeeFinder, times(1)).findFee(anyLong(), anyLong(), any(BillingServiceType.class), eq(false));
        verify(orderFeeFinder, times(1)).findCashOnlyFee(anyLong(), anyLong(), any(BillingServiceType.class));
    }

    @Test
    @DisplayName("Не берем комиссию cash-only с Pleer.ru если были другие способы оплаты.")
    void testPleerRuNoCashOnlyFeeWhenDifferentPaymentMethodsGiven() {
        PriceAndTariffValue actualCashOnly = service.getForFee(ORDER_ITEM_FOR_BILLING_PLEER_RU_NO_CASH_ONLY,
                emptyList(), FEE, MARCH_15_2022, false);

        assertNotNull(actualCashOnly);
        assertThat(actualCashOnly.getPrice(), comparesEqualTo(new BigDecimal("6.2")));
        assertEquals(200, actualCashOnly.getTariffValue().getValue());
        assertEquals(ValueType.RELATIVE, actualCashOnly.getTariffValue().getValueType());
        assertEquals(BillingUnit.ITEM, actualCashOnly.getTariffValue().getBillingUnit());
        // вернется 0 как моковое значение
        assertEquals(0, actualCashOnly.getTariffValue().getMinValue().longValue());
        verify(orderFeeFinder, times(1)).findFee(anyLong(), anyLong(), any(BillingServiceType.class), eq(false));
        verify(orderFeeFinder, times(0)).findCashOnlyFee(anyLong(), anyLong(), any(BillingServiceType.class));
    }

    @Test
    @DisplayName("Проверка корректности расчёта ff тарифа")
    public void testGetForFulfillmentService() throws PriceAndTariffValueServiceException {
        doReturn(
                Optional.of(new TariffValue(3, ValueType.ABSOLUTE, BillingUnit.ITEM))
        ).when(fulfillmentTariffService).getTariff(
                any(LocalDate.class), any(BillingServiceType.class), any(OrderType.class), anyLong(), anyInt(),
                anyLong(), anyLong(), any(), any()
        );

        LocalDate billingDate = LocalDate.of(2018, 1, 1);

        PriceAndTariffValue actual = service.getForFulfillmentService(
                billingDate,
                BillingServiceType.FF_PROCESSING,
                ORDER_ITEM_FOR_BILLING,
                emptyMap(),
                emptyMap()
        );

        assertNotNull(actual);
        assertThat(actual.getPrice(), comparesEqualTo(new BigDecimal(3)));
        assertThat(actual.getTariffValue(), allOf(
                hasValue(3),
                hasValueType(ValueType.ABSOLUTE),
                hasBillingUnit(BillingUnit.ITEM)
        ));
        verify(fulfillmentTariffService)
                .getTariff(any(LocalDate.class), eq(BillingServiceType.FF_PROCESSING),
                        eq(OrderType.FULFILLMENT), anyLong(), anyInt(), anyLong(), anyLong(), isNull(), isNull());
        verify(service).getRawAmount(any(TariffValue.class), any(BigDecimal.class));
    }

    @Test
    @DisplayName("Проверка корректности расчёта relative ff тарифа")
    public void testGetForRelativeFulfillmentService() throws PriceAndTariffValueServiceException {
        doReturn(
                Optional.of(new TariffValue(1000, ValueType.RELATIVE, BillingUnit.ITEM))
        ).when(fulfillmentTariffService).getTariff(
                any(LocalDate.class), any(BillingServiceType.class), any(OrderType.class),
                anyLong(), anyInt(), anyLong(), anyLong(), any(), any()
        );

        LocalDate billingDate = LocalDate.of(2018, 1, 1);

        PriceAndTariffValue actual = service.getForFulfillmentService(
                billingDate,
                BillingServiceType.FF_PROCESSING,
                ORDER_ITEM_FOR_BILLING,
                emptyMap(),
                emptyMap()
        );

        assertNotNull(actual);
        assertThat(actual.getPrice(), comparesEqualTo(new BigDecimal("31")));
        assertThat(actual.getTariffValue(), allOf(
                hasValue(1000),
                hasValueType(ValueType.RELATIVE),
                hasMinValue(null),
                hasBillingUnit(BillingUnit.ITEM),
                hasMaxValue(null)
        ));
        verify(fulfillmentTariffService)
                .getTariff(any(LocalDate.class), eq(BillingServiceType.FF_PROCESSING), eq(OrderType.FULFILLMENT),
                        anyLong(), anyInt(), anyLong(), anyLong(), isNull(), isNull());
        verify(service).getRawAmount(any(TariffValue.class), any(BigDecimal.class));
    }

    @ParameterizedTest(name = "{index}")
    @MethodSource("loyaltyParticipationFeeTestData")
    public void testGetForLoyaltyParticipationFee(
            boolean useOldCalculationAlgorithm,
            OrderItemForBilling orderItem,
            List<MbiOrderItemPromo> promos,
            PriceAndTariffValue expectation
    ) {
        PriceAndTariffValue actual = service.getForFee(
                orderItem, promos, BillingServiceType.LOYALTY_PARTICIPATION_FEE, MARCH_15_2022, false
        );

        Assertions.assertNotNull(actual);
        assertAll(
                () -> assertThat("calculated price: ", actual.getPrice(), comparesEqualTo(expectation.getPrice())),
                () -> Assertions.assertEquals(
                        expectation.getTariffValue().getValue(), actual.getTariffValue().getValue(), "tariff.value: "),
                () -> Assertions.assertEquals(
                        expectation.getTariffValue().getValueType(), actual.getTariffValue().getValueType(),
                        "tariff.valueType: "),
                () -> Assertions.assertEquals(
                        expectation.getTariffValue().getBillingUnit(), actual.getTariffValue().getBillingUnit(),
                        "tariff.billingUnit: ")
        );
        if (useOldCalculationAlgorithm) {
            verify(orderFeeFinder, times(1))
                    .findFee(90589L, SUPPLIER_ID, BillingServiceType.LOYALTY_PARTICIPATION_FEE, false);
        }
    }

    @Test
    @DisplayName("Loyalty_participation_fee промо-тариф.")
    public void testLoyaltyParticipationFee() {
        var orderItem = getOrderItemForBillingBuilder()
                .withOrderCreatedAt(LocalDate.of(2021, 9, 30).atStartOfDay(ZoneId.systemDefault()).toInstant())
                .withPrice(new BigDecimal("10000"))
                //проверяем что для участвующих в программе лояльности и
                // одновременно cash-only заказов не будет +4% комиссии
                .withIsLoyaltyProgramPartner(true)
                .withCashOnlyOrder(true)
                .build();
        var orderPromo = orderPromo(null, CASHBACK, "promo-1");
        var itemPromo = orderItemPromoBuilder(orderPromo)
                .setCashbackAccrualAmount(100)
                .setPartnerId(null)
                .setMarketCashbackPercent(BigDecimal.TEN)
                .setPartnerCashbackPercent(BigDecimal.TEN)
                .build();

        PriceAndTariffValue forFee = service.getForFee(orderItem, List.of(itemPromo),
                BillingServiceType.LOYALTY_PARTICIPATION_FEE, MARCH_15_2022, false);
        Assertions.assertEquals(200, forFee.getTariffValue().getValue());
    }

    @Test
    @DisplayName("Не корректный промо-тариф для loyalty_participation_fee.")
    public void testGetForLoyaltyParticipationFee_invalidPromoInfo() {
        var orderItem = getOrderItemForBillingBuilder()
                .withOrderCreatedAt(LocalDate.of(2021, 9, 30).atStartOfDay(ZoneId.systemDefault()).toInstant())
                .withPrice(new BigDecimal("10000"))
                .build();
        var orderPromo = orderPromo(null, CASHBACK, "promo-1");
        var itemPromo = orderItemPromoBuilder(orderPromo)
                .setCashbackAccrualAmount(100)
                .setPartnerId(SUPPLIER_ID)
                .setMarketCashbackPercent(BigDecimal.TEN)
                .setPartnerCashbackPercent(null)
                .build();

        assertThrows(PriceAndTariffValueServiceException.class, () -> service.getForFee(
                orderItem, List.of(itemPromo), BillingServiceType.LOYALTY_PARTICIPATION_FEE, MARCH_15_2022, false
        ));
    }

    @Test
    @DisplayName("Проверка корректности расчёта relative ff тарифа с минимальной стоимостью")
    public void testGetForRelativeWithMinFulfillmentService() throws PriceAndTariffValueServiceException {
        doReturn(
                Optional.of(new TariffValue(1000, ValueType.RELATIVE, BillingUnit.ITEM, 3000L, 10000L))
        ).when(fulfillmentTariffService).getTariff(
                any(LocalDate.class), any(BillingServiceType.class), any(OrderType.class),
                anyLong(), anyInt(), anyLong(), anyLong(), any(), any()
        );

        LocalDate billingDate = LocalDate.of(2018, 1, 1);

        PriceAndTariffValue actual = service.getForFulfillmentService(
                billingDate,
                BillingServiceType.FF_PROCESSING,
                ORDER_ITEM_FOR_BILLING,
                emptyMap(),
                emptyMap()
        );

        assertNotNull(actual);
        assertThat(actual.getPrice(), comparesEqualTo(new BigDecimal("3000")));
        assertThat(actual.getTariffValue(), allOf(
                hasValue(1000),
                hasValueType(ValueType.RELATIVE),
                hasMinValue(3000L),
                hasBillingUnit(BillingUnit.ITEM),
                hasMaxValue(10000L)
        ));
        verify(fulfillmentTariffService)
                .getTariff(any(LocalDate.class), eq(BillingServiceType.FF_PROCESSING), eq(OrderType.FULFILLMENT),
                        anyLong(), anyInt(), anyLong(), anyLong(), any(), any());
        verify(service).getRawAmount(any(TariffValue.class), any(BigDecimal.class));
    }

    @Test
    @DisplayName("Подсчитать сумму за единицу товара")
    public void testGetRawAmount() {
        for (ValueType valueType : ValueType.values()) {
            BigDecimal rawAmount = service.getRawAmount(
                    new TariffValue(199, valueType, BillingUnit.ITEM),
                    new BigDecimal(799)
            );
            if (ValueType.ABSOLUTE == valueType) {
                assertEquals(new BigDecimal(199), rawAmount);
            } else {
                assertEquals(new BigDecimal(159001).movePointLeft(4), rawAmount);
            }
        }
    }

    @Test
    @DisplayName("Тест на проверку того, что itemId сохранится в env, если для него не найдется real dimensions")
    public void testRealDimensionSum() {

        LocalDate billingDate = LocalDate.of(2020, 1, 13);
        doReturn(
                Optional.of(new TariffValue(3, ValueType.ABSOLUTE, BillingUnit.ITEM))
        ).when(fulfillmentTariffService).getTariff(
                any(LocalDate.class), any(BillingServiceType.class), any(OrderType.class),
                anyLong(), anyInt(), anyLong(), anyLong(), any(), any()
        );

        OrderItemForBilling orderItem = getOrderItemForBillingBuilder()
                .withRealHeight(null)
                .build();
        service.getForFulfillmentService(
                billingDate,
                FEE,
                orderItem,
                emptyMap(),
                emptyMap()
        );

        verify(environmentService)
                .addValueWithoutReplace(eq("market.mbi-billing.missing_real_dimensions_sum"),
                        argThat(value -> value.equals("0")));
    }

    @Test
    @DisplayName("Тест на проверку того, что itemId сохранится в env, если для него не найдется real weiht")
    public void testRealWeightSum() {
        LocalDate billingDate = LocalDate.of(2020, 1, 13);
        doReturn(
                Optional.of(new TariffValue(3, ValueType.ABSOLUTE, BillingUnit.ITEM)))
                .when(fulfillmentTariffService).getTariff(any(LocalDate.class), any(BillingServiceType.class),
                        any(OrderType.class), anyLong(), anyInt(), anyLong(), anyLong(), any(), any()
                );

        OrderItemForBilling orderItem = getOrderItemForBillingBuilder()
                .withRealWeight(null)
                .build();

        service.getForFulfillmentService(
                billingDate,
                FEE,
                orderItem,
                emptyMap(),
                emptyMap()
        );

        verify(environmentService)
                .addValueWithoutReplace(eq("market.mbi-billing.missing_real_weight"),
                        argThat(value -> value.equals("0")));
    }


    @DisplayName("Проверка расчета цены по батчам/квантам")
    @ParameterizedTest(name = "[{index}] {4}")
    @MethodSource("testBillingPriceWithBatchesData")
    void testBillingPriceWithBatches(
            OrderItemForBilling item,
            BillingServiceType billingServiceType,
            LocalDate billingDate,
            BigDecimal expectedValue,
            String description
    ) {
        BigDecimal actualBillingPrice = service.getBillingPrice(
                item,
                billingServiceType,
                billingDate
        );

        Assertions.assertEquals(
                0,
                actualBillingPrice.compareTo(expectedValue),
                "Actual : " + actualBillingPrice + ", expected : " + expectedValue
        );
    }

    @ParameterizedTest(name = "[{index}] Billing service type {0} by batch  is true")
    @EnumSource(value = BillingServiceType.class, mode = EnumSource.Mode.INCLUDE, names = {
            "FEE", "FEE_CANCELLATION",
            "DELIVERY_TO_CUSTOMER", "DELIVERY_TO_CUSTOMER_CANCELLATION",
            "FF_PROCESSING",
            "CANCELLED_ORDER_FEE",
            "EXPRESS_DELIVERED", "EXPRESS_DELIVERED_CANCELLATION",
            "CROSSREGIONAL_DELIVERY", "CROSSREGIONAL_DELIVERY_CANCELLATION"
    })
    void testIsBillingByBatchesIsTrue(BillingServiceType billingServiceType) {
        OrderItemForBilling item = createItemMocked(10, 5, 20);
        Assertions.assertTrue(service.isBillingByBatch(billingServiceType, MARCH_15_2022, item));
    }

    @ParameterizedTest(name = "[{index}] Billing service type by batch {0} is false")
    @EnumSource(value = BillingServiceType.class, mode = EnumSource.Mode.EXCLUDE, names = {
            "FEE", "FEE_CANCELLATION",
            "DELIVERY_TO_CUSTOMER", "DELIVERY_TO_CUSTOMER_CANCELLATION",
            "FF_PROCESSING",
            "CANCELLED_ORDER_FEE",
            "EXPRESS_DELIVERED", "EXPRESS_DELIVERED_CANCELLATION",
            "CROSSREGIONAL_DELIVERY", "CROSSREGIONAL_DELIVERY_CANCELLATION"
    })
    void testIsBillingByBatchesIsFalse(BillingServiceType billingServiceType) {
        OrderItemForBilling item = createItemMocked(10, 5, 20);
        Assertions.assertFalse(service.isBillingByBatch(billingServiceType, MARCH_15_2022, item));
    }

    @Test
    void fairTestWithBatches() {
        TariffValue tariffValue = new TariffValue(
                10,
                ValueType.RELATIVE, // 10% тариф, минималка - 30
                BillingUnit.ORDER,
                30L,
                null
        );

        OrderItemForBilling itemWithBatch = createItemMocked(12, 6, 10000);
        OrderItemForBilling itemWithoutBatch = createItemMocked(12, null, 10000);

        BigDecimal billingPriceWithBatch = service.getBillingPrice(
                itemWithBatch,
                FEE,
                MARCH_15_2022
        );
        BigDecimal billingPriceWithoutBatch = service.getBillingPrice(
                itemWithoutBatch,
                FEE,
                MARCH_15_2022
        );

        PriceAndTariffValue priceAndTariffValueWithBatch = new PriceAndTariffValue(
                service.getRawAmount(tariffValue, billingPriceWithBatch),
                service.getRawAmountBeforeMin(tariffValue, billingPriceWithBatch),
                Currency.RUR,
                tariffValue
        );

        PriceAndTariffValue priceAndTariffValueWithoutBatch = new PriceAndTariffValue(
                service.getRawAmount(tariffValue, billingPriceWithoutBatch),
                service.getRawAmountBeforeMin(tariffValue, billingPriceWithoutBatch),
                Currency.RUR,
                tariffValue
        );

        // тут возвращаем цену биллинга одного батча (т.к. цена биллинга батча больше минималки, то возвращаем эту цену)
        // и соответственно в будущем мы с мерча возьмем 60 * 2 (кол-во батчей) = 120
        Assertions.assertTrue(BigDecimal.valueOf(60).compareTo(priceAndTariffValueWithBatch.getPrice()) == 0);
        // тут нет батчей, поэтому возвращаем минималку (т.к. она больше цены биллинга одного товара)
        // и соответственно в будущем мы с мерча возьмем 30 * 12 (кол-во товаров) = 360
        Assertions.assertTrue(BigDecimal.valueOf(30).compareTo(priceAndTariffValueWithoutBatch.getPrice()) == 0);
    }

    private static Stream<Arguments> testBillingPriceWithBatchesData() {
        return Stream.of(
                Arguments.of(
                        createItemMocked(12, 1, 10),
                        FEE,
                        MARCH_15_2022,
                        BigDecimal.valueOf(10),
                        "Честно биллим 12 квантов, каждый из которых , т.к. батч сайз = 1"
                ),
                Arguments.of(
                        createItemMocked(12, 12, 10),
                        FEE,
                        MARCH_15_2022,
                        BigDecimal.valueOf(120),
                        "Честно биллим 1 квант по цене 120, т.к. батч сайз = 12"
                ),
                Arguments.of(
                        createItemMocked(6, 12, 10),
                        FEE,
                        MARCH_15_2022,
                        BigDecimal.valueOf(60),
                        "Биллим один квант, в котором лежит min(batchSize, count) товаров"
                ),
                Arguments.of(
                        createItemMocked(12, 1, 10),
                        BillingServiceType.DELIVERY_TO_CUSTOMER,
                        MARCH_15_2022,
                        BigDecimal.valueOf(10),
                        "Тоже самое, только для другой услги"
                ),
                Arguments.of(
                        createItemMocked(12, 1, 10),
                        FEE,
                        LocalDate.of(2022, Month.MARCH, 14),
                        BigDecimal.valueOf(10),
                        "Не проходит по дате биллинга, поэтому биллим по старому"
                ),
                Arguments.of(
                        createItemMocked(12, 1, 10),
                        BillingServiceType.DELIVERY_TO_CUSTOMER_RETURN,
                        MARCH_15_2022,
                        BigDecimal.valueOf(10),
                        "Не проходит по услуге, поэтому биллим по старому"
                ),
                Arguments.of(
                        createItemMocked(12, null, 10),
                        FEE,
                        MARCH_15_2022,
                        BigDecimal.valueOf(10),
                        "Не проходит по batch size = null, поэтому биллим по старому"
                )
        );
    }

    static OrderItemForBilling createItemMocked(int count, Integer batchSize, int price) {
        OrderItemForBilling item = mock(OrderItemForBilling.class);
        doReturn(count).when(item).getCount();
        doReturn(batchSize).when(item).getBatchSize();
        doReturn(BigDecimal.valueOf(price)).when(item).getBillingPrice();
        return item;
    }

}
