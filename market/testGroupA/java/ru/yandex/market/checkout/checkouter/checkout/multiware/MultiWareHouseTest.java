package ru.yandex.market.checkout.checkouter.checkout.multiware;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.cart.MultiCartTotals;
import ru.yandex.market.checkout.checkouter.delivery.CartDeliveryOptionRefs;
import ru.yandex.market.checkout.checkouter.delivery.CommonDeliveryOption;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.feature.type.common.CollectionFeatureType;
import ru.yandex.market.checkout.checkouter.order.Buyer;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.MultiOrder;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.cipher.CipherService;
import ru.yandex.market.checkout.helpers.YandexMarketDeliveryHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.FulfilmentProvider;
import ru.yandex.market.checkout.test.providers.ActualDeliveryProvider;
import ru.yandex.market.checkout.test.providers.ActualDeliveryProvider.ActualDeliveryBuilder;
import ru.yandex.market.checkout.test.providers.BuyerProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;

import static java.util.stream.Collectors.toSet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static ru.yandex.common.util.ObjectUtils.defaultIfNull;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_DELIVERY_SERVICE_ID;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_SORTING_CENTER_DELIVERY_SERVICE_ID;
import static ru.yandex.market.checkout.test.providers.OrderProvider.SHOP_ID_WITH_SORTING_CENTER;

/**
 * @author sergeykoles
 * Created on: 19.10.18
 */
public class MultiWareHouseTest extends AbstractWebTestBase {

    private static final int WAREHOUSE1_ID = 123;
    private static final int WAREHOUSE2_ID = 221;
    private static final int WAREHOUSE3_ID = 333;
    @Autowired
    private YandexMarketDeliveryHelper yandexMarketDeliveryHelper;
    @Autowired
    @Qualifier("reportCipherService")
    private CipherService cipherService;
    private Parameters multiWhParams;

    private final EnhancedRandom enhancedRandom = EnhancedRandomBuilder.aNewEnhancedRandom();

    @BeforeEach
    public void setUp() throws Exception {
        multiWhParams = createMultiWarehouseParams();

        checkouterFeatureWriter.writeValue(CollectionFeatureType.ENABLED_FETCHERS, Set.of());
    }

    @AfterEach
    public void cleanup() {
        stockStorageConfigurer.resetMappings();
        stockStorageConfigurer.resetRequests();
    }

    @Test
    @DisplayName("Проверяем работоспособность ручки /cart с несколькими корзинами")
    public void testMultiCart() {
        stockStorageConfigurer.resetRequests();
        MultiCart multiCart = orderCreateHelper.cart(multiWhParams);
        assertThat(multiCart.getCarts(), hasSize(2));
        Set<Pair<Long, Integer>> cartKeys = multiCart.getCarts().stream()
                .map(o -> Pair.of(o.getShopId(), getWareHouseFromOrder(o)))
                .collect(toSet());
        assertThat(cartKeys, hasSize(2));
        assertThat(cartKeys,
                containsInAnyOrder(
                        Pair.of(SHOP_ID_WITH_SORTING_CENTER, WAREHOUSE1_ID),
                        Pair.of(SHOP_ID_WITH_SORTING_CENTER, WAREHOUSE2_ID)
                )
        );
        assertThat(
                multiCart.getCarts().stream()
                        .map(Order::getDeliveryOptions)
                        .map(List::size)
                        .collect(toSet()),
                containsInAnyOrder(3, 4)
        );

        assertThat(stockStorageConfigurer.getServeEvents().size(), equalTo(1));
    }

    @Test
    @DisplayName("Проверяем итоговые суммы multicart.totals на /cart и /checkout")
    public void testMultiCartAndCheckoutAggregates() {
        BigDecimal buyerItemsTotal = BigDecimal.valueOf(1000d);
        BigDecimal buyerDeliveryTotal = BigDecimal.valueOf(200d);
        BigDecimal buyerTotal = buyerItemsTotal.add(buyerDeliveryTotal);

        // имитируем фронт. Сходим в карт, спросим насчёт доставок и возьмём первые попавшиеся.
        MultiCart multiCart = orderCreateHelper.cart(multiWhParams);
        multiCart.getCarts().forEach(
                c -> multiWhParams.getBuiltMultiCart().getCartByLabel(c.getLabel())
                        .ifPresent(
                                paramCart -> {
                                    Delivery delivery = c.getDeliveryOptions().iterator().next();
                                    delivery.setBuyerAddress(paramCart.getDelivery().getBuyerAddress());
                                    delivery.setRegionId(multiWhParams.getBuiltMultiCart().getBuyerRegionId());
                                    paramCart.setDelivery(delivery);
                                }
                        )
        );

        // идём в /cart с актуализированной доставкой, чтобы получить и проверить цены.
        multiWhParams.setMultiCartChecker(mc -> {
            MultiCartTotals multiCartTotals = mc.getTotals();
            assertThat(multiCartTotals.getBuyerItemsTotal(), comparesEqualTo(buyerItemsTotal));
            assertThat(multiCartTotals.getBuyerDeliveryTotal(), comparesEqualTo(buyerDeliveryTotal));
            assertThat(multiCartTotals.getBuyerTotal(), comparesEqualTo(buyerTotal));
        });

        // делаем чекаут и проверяем, что с ценами всё ок
        MultiOrder multiOrder = orderCreateHelper.createMultiOrder(multiWhParams);
        MultiCartTotals multiOrderTotals = multiOrder.getTotals();
        assertThat(multiOrderTotals.getBuyerItemsTotal(), comparesEqualTo(buyerItemsTotal));
        assertThat(multiOrderTotals.getBuyerDeliveryTotal(), comparesEqualTo(buyerDeliveryTotal));
        assertThat(multiOrderTotals.getBuyerTotal(), comparesEqualTo(buyerTotal));

        assertThat(stockStorageConfigurer.getServeEvents()
                .stream()
                .filter(req -> req.getRequest().getUrl().equals("/order/getAvailableAmounts"))
                //3 обращения
                .count(), equalTo(3L));
    }

    @Test
    @DisplayName("Проверяем общие для все корзин типы доставки")
    public void testCommonDeliveryTypes() {
        // к одной из корзин добавим самовывоз
        multiWhParams = createMultiWarehouseParams(actualDeliveryBuilder ->
                        actualDeliveryBuilder.addPickup(MOCK_SORTING_CENTER_DELIVERY_SERVICE_ID),
                // к другой - ничего
                null
        );
        MultiCart multiCart = orderCreateHelper.cart(multiWhParams);

        List<CommonDeliveryOption> commonDeliveryOptions = multiCart.getCommonDeliveryOptions();
        // общая только курьерка (в этом тесте)
        assertThat(
                commonDeliveryOptions.stream()
                        .map(CommonDeliveryOption::getType)
                        .collect(Collectors.toList()),
                containsInAnyOrder(DeliveryType.DELIVERY)
        );
        // проверяем, что для каждого общего типа доставки
        commonDeliveryOptions.forEach(
                cdo -> {
                    List<CartDeliveryOptionRefs> cartDeliveryOptionRefs = cdo.getCartDeliveryOptionRefs();
                    // есть ссылки на все корзины
                    assertThat(
                            cartDeliveryOptionRefs.stream()
                                    .map(CartDeliveryOptionRefs::getCartLabel)
                                    .collect(Collectors.toList()),
                            containsInAnyOrder(
                                    multiCart.getCarts().stream()
                                            .map(Order::getLabel)
                                            .toArray(String[]::new)
                            )
                    );
                    // и для каждой ссылки на корзину
                    cartDeliveryOptionRefs
                            .forEach(cdor -> {
                                // проверяем, что все хэши соответствуют тем,
                                // что есть в корзине с данным лейбелом и данным общим типом доставки
                                List<String> deliveryHashes = cdor.getDeliveryHashes();
                                assertThat(deliveryHashes, not(empty()));
                                assertThat(
                                        deliveryHashes,
                                        containsInAnyOrder(
                                                multiCart.getCartByLabel(cdor.getCartLabel())
                                                        .orElseThrow(
                                                                () -> new RuntimeException("No cart: "
                                                                        + cdor.getCartLabel())
                                                        )
                                                        .getDeliveryOptions().stream()
                                                        .filter(d -> d.getType() == cdo.getType())
                                                        .map(Delivery::getHash)
                                                        .toArray(String[]::new)
                                        )
                                );
                            });
                }
        );
    }


    @Test
    @DisplayName("Проверяем общие для всех пикапов точки самовывоза")
    public void testCommonOutletIds() {
        List<Long> outletIdsFirst = Arrays.asList(12312301L, 12312302L, 12312304L);
        List<Long> outletIdsSecond = Arrays.asList(12312301L, 12312302L, 12312303L);
        Set<Long> expectedCommondOutletIds = new HashSet<>(outletIdsFirst);
        expectedCommondOutletIds.retainAll(outletIdsSecond);
        // добавим самовывоз
        multiWhParams = createMultiWarehouseParams(actualDeliveryBuilder ->
                        actualDeliveryBuilder.addPickup(MOCK_DELIVERY_SERVICE_ID, 1, outletIdsFirst),
                actualDeliveryBuilder ->
                        actualDeliveryBuilder.addPickup(MOCK_DELIVERY_SERVICE_ID, 1, outletIdsSecond)
        );
        MultiCart multiCart = orderCreateHelper.cart(multiWhParams);

        List<CommonDeliveryOption> commonDeliveryOptions = multiCart.getCommonDeliveryOptions();
        // общая опция только самовывоз (в этом тесте)
        assertThat(
                commonDeliveryOptions.stream()
                        .filter(cdo -> cdo.getType() == DeliveryType.PICKUP)
                        .flatMap(cdo -> cdo.getCommonOutletIds().stream())
                        .collect(Collectors.toSet()),
                containsInAnyOrder(expectedCommondOutletIds.toArray())
        );
    }

    @Test
    @DisplayName("Проверяем общие для всех пикапов точки самовывоза когда общих нет")
    public void testEmptyCommonOutletIds() {
        List<Long> outletIdsFirst = Arrays.asList(12312301L, 12312302L);
        List<Long> outletIdsSecond = Arrays.asList(12312303L);
        // добавим самовывоз
        multiWhParams = createMultiWarehouseParams(actualDeliveryBuilder ->
                        actualDeliveryBuilder.addPickup(MOCK_DELIVERY_SERVICE_ID, 1, outletIdsFirst),
                actualDeliveryBuilder ->
                        actualDeliveryBuilder.addPickup(MOCK_DELIVERY_SERVICE_ID, 1, outletIdsSecond)
        );

        Parameters warehouse3Params = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withActualDelivery(
                        ActualDeliveryProvider.builder()
                                .addPickup(MOCK_DELIVERY_SERVICE_ID, 1, Arrays.asList(12312301L, 12312302L, 12312303L))
                                .build()
                )
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withDeliveryServiceId(MOCK_SORTING_CENTER_DELIVERY_SERVICE_ID)
                .withColor(Color.BLUE)
                .withOrder(OrderProvider.getBlueOrder(o -> {
                    @Nonnull OrderItem orderItem = OrderItemProvider.buildOrderItem("100500");
                    orderItem.setSku(FulfilmentProvider.OTHER_TEST_SKU);
                    orderItem.setMsku(FulfilmentProvider.OTHER_TEST_MSKU);
                    orderItem.setShopSku(FulfilmentProvider.OTHER_TEST_SHOP_SKU);
                    o.setItems(Collections.singletonList(orderItem));
                    patchOrder(o, "wh3_", WAREHOUSE3_ID);
                }))
                .buildParameters();

        multiWhParams.addOrder(warehouse3Params);
        multiWhParams.setPaymentMethod(PaymentMethod.YANDEX);
        stockStorageConfigurer.resetRequests();
        MultiCart multiCart = orderCreateHelper.cart(multiWhParams);

        List<CommonDeliveryOption> commonDeliveryOptions = multiCart.getCommonDeliveryOptions();
        // общая опция только самовывоз (в этом тесте)
        assertThat(
                commonDeliveryOptions.stream()
                        .filter(cdo -> cdo.getType() == DeliveryType.PICKUP)
                        .collect(Collectors.toList()),
                empty()
        );

        assertThat(stockStorageConfigurer.getServeEvents().size(), equalTo(1));
    }


    @Test
    @Disabled
    @DisplayName("Проверяем возможность передавать различных покупателей для заказов мультикорзины")
    @SuppressWarnings("checkstyle:HiddenField")
    public void testDifferentBuyersForMultiOrder() {
        Parameters multiWhParams = createMultiWarehouseParams();

        multiWhParams.getBuiltMultiCart().getCarts().forEach(
                c -> c.setBuyer(createRandomBuyer())
        );

        // а у третьего заказа пусть покупатель будет из мультикарта (дефолтный)
        Parameters warehouse3Params = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withActualDelivery(
                        ActualDeliveryProvider.builder()
                                .addDelivery(MOCK_SORTING_CENTER_DELIVERY_SERVICE_ID)
                                .withDayFrom(7)
                                .withDayTo(8)
                                .addDelivery(MOCK_SORTING_CENTER_DELIVERY_SERVICE_ID)
                                .withDayFrom(8)
                                .withDayTo(9)
                                .build()
                )
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withDeliveryServiceId(MOCK_SORTING_CENTER_DELIVERY_SERVICE_ID)
                .withColor(Color.BLUE)
                .withOrder(OrderProvider.getBlueOrder(o -> {
                    @Nonnull OrderItem orderItem = OrderItemProvider.buildOrderItem("100500");
                    orderItem.setSku(FulfilmentProvider.OTHER_TEST_SKU);
                    orderItem.setMsku(FulfilmentProvider.OTHER_TEST_MSKU);
                    orderItem.setShopSku(FulfilmentProvider.OTHER_TEST_SHOP_SKU);
                    o.setItems(Collections.singletonList(orderItem));
                    patchOrder(o, "wh3_", WAREHOUSE3_ID);
                }))
                .buildParameters();

        multiWhParams.addOrder(warehouse3Params);
        multiWhParams.setPaymentMethod(PaymentMethod.YANDEX);

        multiWhParams.setMultiCartAction(
                mc -> mc.getCarts().forEach(
                        c -> multiWhParams.getBuiltMultiCart().getCartByLabel(c.getLabel()).ifPresent(
                                srcCart -> c.setBuyer(srcCart.getBuyer())
                        )
                )
        );
        MultiOrder multiOrder = orderCreateHelper.createMultiOrder(multiWhParams);
        assertThat(multiOrder.getOrders(), hasSize(3));

        List<Order> sourceOrders = multiWhParams.getOrders();
        sourceOrders.forEach(
                o -> {
                    String label = o.getLabel();
                    Buyer expectedBuyer = defaultIfNull(o.getBuyer(), multiWhParams.getBuiltMultiCart().getBuyer());
                    Order createOrder = multiOrder.getCartByLabel(label).get();
                    Buyer createOrderBuyer = createOrder.getBuyer();
                    assertThat(createOrderBuyer.getFirstName(), equalTo(expectedBuyer.getFirstName()));
                    assertThat(createOrderBuyer.getMiddleName(), equalTo(expectedBuyer.getMiddleName()));
                    assertThat(createOrderBuyer.getLastName(), equalTo(expectedBuyer.getLastName()));
                }
        );
    }

    private Buyer createRandomBuyer() {
        Buyer randomBuyer = enhancedRandom.nextObject(Buyer.class);
        Buyer buyer = BuyerProvider.getBuyer();
        buyer.setFirstName(randomBuyer.getFirstName());
        buyer.setLastName(randomBuyer.getLastName());
        buyer.setMiddleName(randomBuyer.getMiddleName());
        buyer.setRegionId(null);
        buyer.setUid(null);
        buyer.setMuid(null);
        buyer.setUuid(null);
        return buyer;
    }

    private Parameters createMultiWarehouseParams() {
        return createMultiWarehouseParams(null, null);
    }

    private Parameters createMultiWarehouseParams(Consumer<ActualDeliveryBuilder> actualDelivery1Override,
                                                  Consumer<ActualDeliveryBuilder> actualDelivery2Override) {
        ActualDeliveryBuilder actualDeliveryBuilder1 = ActualDeliveryProvider.builder()
                .addDelivery(MOCK_SORTING_CENTER_DELIVERY_SERVICE_ID)
                .withDayFrom(7)
                .withDayTo(8)
                .addDelivery(MOCK_SORTING_CENTER_DELIVERY_SERVICE_ID)
                .withDayFrom(8)
                .withDayTo(9)
                .addDelivery(MOCK_SORTING_CENTER_DELIVERY_SERVICE_ID)
                .withDayFrom(10)
                .withDayTo(12);
        if (actualDelivery1Override != null) {
            actualDelivery1Override.accept(actualDeliveryBuilder1);
        }
        Parameters warehouse1Params = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withActualDelivery(
                        actualDeliveryBuilder1
                                .build()
                )
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withDeliveryServiceId(MOCK_SORTING_CENTER_DELIVERY_SERVICE_ID)
                .withColor(Color.BLUE)
                .withOrder(OrderProvider.getBlueOrder(o -> {
                    @Nonnull OrderItem orderItem = OrderItemProvider.getOrderItem();
                    orderItem.setSku(FulfilmentProvider.TEST_SKU);
                    orderItem.setMsku(FulfilmentProvider.TEST_MSKU);
                    orderItem.setShopSku(FulfilmentProvider.TEST_SHOP_SKU);
                    o.setItems(Collections.singletonList(orderItem));
                    patchOrder(o, "wh1_", WAREHOUSE1_ID);
                }))
                .buildParameters();
        ActualDeliveryBuilder actualDeliveryBuilder2 = ActualDeliveryProvider.builder()
                .addDelivery(MOCK_SORTING_CENTER_DELIVERY_SERVICE_ID)
                .withDayFrom(7)
                .withDayTo(8)
                .addDelivery(MOCK_SORTING_CENTER_DELIVERY_SERVICE_ID)
                .withDayFrom(8)
                .withDayTo(9)
                .addDelivery(MOCK_SORTING_CENTER_DELIVERY_SERVICE_ID)
                .withDayFrom(12)
                .withDayTo(100)
                .addDelivery(MOCK_SORTING_CENTER_DELIVERY_SERVICE_ID)
                .withDayFrom(13)
                .withDayTo(15);
        if (actualDelivery2Override != null) {
            actualDelivery2Override.accept(actualDeliveryBuilder2);
        }
        Parameters warehouse2Params = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withActualDelivery(
                        actualDeliveryBuilder2
                                .build()
                )
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withDeliveryServiceId(MOCK_SORTING_CENTER_DELIVERY_SERVICE_ID)
                .withColor(Color.BLUE)
                .withOrder(OrderProvider.getBlueOrder(o -> {
                    @Nonnull OrderItem anotherOrderItem = OrderItemProvider.getAnotherOrderItem();
                    anotherOrderItem.setSku(FulfilmentProvider.ANOTHER_TEST_SKU);
                    anotherOrderItem.setMsku(FulfilmentProvider.ANOTHER_TEST_MSKU);
                    anotherOrderItem.setShopSku(UUID.randomUUID().toString());
                    o.setItems(Collections.singletonList(anotherOrderItem));
                    patchOrder(o, "wh2_", WAREHOUSE2_ID);
                }))
                .buildParameters();
        warehouse1Params.addOrder(warehouse2Params);
        warehouse1Params.setPaymentMethod(PaymentMethod.YANDEX);

        return warehouse1Params;
    }

    private Integer getWareHouseFromOrder(Order o) {
        Set<Integer> warehouseIds = o.getItems().stream()
                .map(OrderItem::getWarehouseId)
                .collect(toSet());

        return warehouseIds.size() == 1 ? warehouseIds.iterator().next() : null;
    }

    private void patchOrder(Order order, String wareMd5Prefix, int warehouseId) {
        order.getItems().forEach(oi -> {
            patchOrderItemWareMd5(oi, wareMd5Prefix);
            oi.setWarehouseId(warehouseId);
        });
    }

    private void patchOrderItemWareMd5(OrderItem oi, String prefix) {
        String wareMd5 = prefix + oi.getWareMd5();
        oi.setWareMd5(wareMd5);
        OrderItemProvider.patchShowInfo(oi, cipherService);
    }
}
