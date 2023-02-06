package ru.yandex.market.checkout.checkouter.checkout;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.google.common.collect.Iterables;
import io.qameta.allure.Epic;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;
import io.qameta.allure.junit4.Tag;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.allure.Epics;
import ru.yandex.market.checkout.allure.Stories;
import ru.yandex.market.checkout.allure.Tags;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.BasicOrder;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderAcceptMethod;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderPropertyType;
import ru.yandex.market.checkout.checkouter.order.OrderSearchRequest;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.order.PagedOrders;
import ru.yandex.market.checkout.checkouter.order.SupplierType;
import ru.yandex.market.checkout.checkouter.order.VatType;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.validation.DimensionsMissingError;
import ru.yandex.market.checkout.checkouter.validation.ValidationResult;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.providers.FFDeliveryProvider;
import ru.yandex.market.checkout.test.providers.ActualDeliveryProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;
import ru.yandex.market.checkout.util.balance.ShopSettingsHelper;
import ru.yandex.market.checkout.util.matching.CheckoutErrorMatchers;
import ru.yandex.market.checkout.util.report.ItemInfo;
import ru.yandex.market.common.report.model.ActualDeliveryOption;
import ru.yandex.market.common.report.model.ActualDeliveryResult;
import ru.yandex.market.common.report.model.FeedOfferId;

import static java.math.BigDecimal.valueOf;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasLength;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.common.util.collections.CollectionUtils.first;
import static ru.yandex.market.checkout.checkouter.cart.ItemChange.MISSING;
import static ru.yandex.market.checkout.checkouter.client.CheckouterClientParams.CLIENT_ROLE;
import static ru.yandex.market.checkout.providers.BlueParametersProvider.defaultBlueOrderParameters;
import static ru.yandex.market.checkout.providers.FulfilmentProvider.FF_SHOP_ID;
import static ru.yandex.market.checkout.providers.FulfilmentProvider.TEST_SHOP_SKU;
import static ru.yandex.market.checkout.providers.FulfilmentProvider.TEST_SKU;
import static ru.yandex.market.checkout.providers.FulfilmentProvider.TEST_WAREHOUSE_ID;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_DELIVERY_SERVICE_ID;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_SORTING_CENTER_DELIVERY_SERVICE_ID;
import static ru.yandex.market.checkout.test.providers.OrderProvider.SHOP_ID_WITH_SORTING_CENTER;

public class CheckoutFulfilmentTest extends AbstractWebTestBase {

    @Autowired
    protected OrderPayHelper orderPayHelper;

    @Epic(Epics.CHECKOUT)
    @Story(Stories.CART)
    @Story(Stories.CHECKOUT)
    @DisplayName("Проверяем создание заказа через фф и появление новых полей")
    @Tag(Tags.FULFILMENT)
    @Test
    //https://testpalm.yandex-team.ru/testcase/checkouter-43
    public void createFfOrder() {
        Parameters parameters = defaultBlueOrderParameters();
        parameters.setPaymentMethod(PaymentMethod.YANDEX);
        parameters.setWarehouseForAllItems(TEST_WAREHOUSE_ID);

        Order order = orderCreateHelper.createOrder(parameters);

        checkOrderInDb(order);
    }

    @Test
    public void tryCreateOrderWithHundredItems() {
        Parameters parameters = defaultBlueOrderParameters();
        parameters.setPaymentMethod(PaymentMethod.YANDEX);
        OrderItem item = parameters.getOrder().getItems().iterator().next();
        for (int cnt = 0; cnt < 100; cnt++) {
            OrderItem newItem = item.clone();
            newItem.setOfferId(newItem.getOfferId() + "_" + cnt);
            newItem.setFeedOfferId(new FeedOfferId(newItem.getOfferId(), newItem.getFeedId()));
            newItem.setMsku(newItem.getMsku() + cnt);
            parameters.getOrder().addItem(newItem);
        }
        parameters.getOrder().getItems().forEach(oi -> oi.setWarehouseId(null));
        parameters.setErrorMatcher(CheckoutErrorMatchers.tooManyItems);
        parameters.setExpectedCartReturnCode(400);

        orderCreateHelper.cart(parameters);
    }

    @Test
    public void createOrderWithExperiments() throws Exception {
        final String experiments = "rearr-factors=fair-common-dimensions-algo=1";

        Parameters parameters = defaultBlueOrderParameters();
        parameters.setPaymentMethod(PaymentMethod.YANDEX);
        parameters.setExperiments(experiments);
        parameters.setWarehouseForAllItems(TEST_WAREHOUSE_ID);

        Order order = orderCreateHelper.createOrder(parameters);

        checkOrderInDb(order);

        Assertions.assertEquals(experiments, orderService.getOrder(order.getId())
                .getProperty(OrderPropertyType.EXPERIMENTS));


        mockMvc.perform(get("/orders/{orderId}", order.getId())
                        .param(CLIENT_ROLE, "SYSTEM"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.properties.experiments").value(experiments));
    }


    @Test
    public void createOrderWithOversizedExperiments() throws Exception {
        final String experiments = StringUtils.repeat("a", 2048);

        Parameters parameters = defaultBlueOrderParameters();
        parameters.setPaymentMethod(PaymentMethod.YANDEX);
        parameters.setExperiments(experiments);
        parameters.setWarehouseForAllItems(TEST_WAREHOUSE_ID);

        Order order = orderCreateHelper.createOrder(parameters);

        checkOrderInDb(order);

        String property = orderService.getOrder(order.getId())
                .getProperty(OrderPropertyType.EXPERIMENTS);
        assertThat(property, hasLength(2000));

        mockMvc.perform(get("/orders/{orderId}", order.getId())
                        .param(CLIENT_ROLE, "SYSTEM"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.properties.experiments").value(hasLength(2000)));
    }

    @Test
    public void createOrderWithTestBuckets() throws Exception {
        final String testBuckets = "buckets";

        Parameters parameters = defaultBlueOrderParameters();
        parameters.setPaymentMethod(PaymentMethod.YANDEX);
        parameters.setTestBuckets(testBuckets);
        parameters.setWarehouseForAllItems(TEST_WAREHOUSE_ID);

        Order order = orderCreateHelper.createOrder(parameters);

        checkOrderInDb(order);

        Assertions.assertEquals(testBuckets, orderService.getOrder(order.getId())
                .getProperty(OrderPropertyType.TEST_BUCKETS));


        mockMvc.perform(get("/orders/{orderId}", order.getId())
                        .param(CLIENT_ROLE, "SYSTEM"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.properties.testBuckets").value(testBuckets));
    }

    @Test
    public void dontSaveEmptyExperiments() throws Exception {
        Parameters parameters = defaultBlueOrderParameters();
        parameters.setPaymentMethod(PaymentMethod.YANDEX);
        parameters.setWarehouseForAllItems(TEST_WAREHOUSE_ID);

        Order order = orderCreateHelper.createOrder(parameters);

        checkOrderInDb(order);

        Assertions.assertNull(orderService.getOrder(order.getId()).getProperty(OrderPropertyType.EXPERIMENTS));

        mockMvc.perform(get("/orders/{orderId}", order.getId())
                        .param(CLIENT_ROLE, "SYSTEM"))
                .andExpect(status().isOk());
    }

    @Test
    public void createOrderWithGenericProperty() {
        Parameters parameters = defaultBlueOrderParameters();
        parameters.getOrder().addProperty(OrderPropertyType.IGNORE_ORDER_IN_NOTIFIER.create(null, true));
        parameters.getOrder().addProperty(OrderPropertyType.YANDEX_PLUS.create(null, false));

        Order order = orderCreateHelper.createOrder(parameters);

        order = orderService.getOrder(order.getId());
        assertEquals(Boolean.TRUE, order.getProperty(OrderPropertyType.IGNORE_ORDER_IN_NOTIFIER));
        assertNotEquals(Boolean.TRUE, order.getProperty(OrderPropertyType.IGNORE_ORDER_IN_MDB));
        assertNotEquals(Boolean.TRUE, order.getProperty(OrderPropertyType.YANDEX_PLUS));
    }

    @Tag(Tags.FULFILMENT)
    @Epic(Epics.CHECKOUT)
    @DisplayName("Проверяем автопереход ФФ заказов с флагом dontCall=true в PROCESSING")
    @Issue("MARKETCHECKOUT-4363")
    @Test
    public void createFfOrderToProcessing() {
        //checkouter-146
        Order paidOrder = createAndPayFFOrder();

        Assertions.assertEquals(true, paidOrder.isFulfilment(), "Order from DB after pay should be FF");
        Assertions.assertEquals(OrderStatus.PROCESSING, paidOrder
                .getStatus(), "Order from DB after pay should be in PROCESSING");
        Assertions.assertEquals(VatType.VAT_20_120, paidOrder.getDelivery().getVat());
        Assertions.assertEquals(VatType.VAT_20_120, paidOrder.getItems().iterator().next().getVat());
    }

    private Order createAndPayFFOrder() {
        Parameters parameters = defaultBlueOrderParameters();
        parameters.setPaymentMethod(PaymentMethod.YANDEX);
        parameters.setAcceptMethod(OrderAcceptMethod.WEB_INTERFACE);
        parameters.getBuyer().setDontCall(true);
        parameters.setWarehouseForAllItems(TEST_WAREHOUSE_ID);

        Order order = orderCreateHelper.createOrder(parameters);

        checkOrderInDb(order);

        orderPayHelper.payForOrder(order);

        return orderService.getOrder(order.getId());
    }

    @Tag(Tags.FULFILMENT)
    @Epic(Epics.CHECKOUT)
    @DisplayName("Проверяем создание заказа через фф и появление новых полей")
    @Test
    //https://testpalm.yandex-team.ru/testcase/checkouter-43
    public void createPartialFfOrder() {
        Parameters parameters = defaultBlueOrderParameters();
        parameters.setPaymentMethod(PaymentMethod.YANDEX);
        OrderItem ffITem = Iterables.getOnlyElement(parameters.getOrder().getItems());
        addCustomItem(parameters, "111", item -> {
            item.setSku(TEST_SKU);
            item.setSupplierId(FF_SHOP_ID);
            parameters.getReportParameters().overrideItemInfo(item.getFeedOfferId()).setFulfilment(
                    new ItemInfo.Fulfilment(FF_SHOP_ID, TEST_SKU, TEST_SHOP_SKU, null, false)
            );
        });
        parameters.setCheckCartErrors(false);

        MultiCart cart = orderCreateHelper.cart(parameters);
        assertThat(cart.getCarts().get(0).getItem(ffITem.getFeedOfferId()).getChanges(), hasItem(MISSING));
    }

    @Tag(Tags.FULFILMENT)
    @Epic(Epics.CHECKOUT)
    @DisplayName("Создание постоплатного ФФ заказа")
    @Test
    public void createFfPostpaidOrder() {
        Parameters parameters = defaultBlueOrderParameters();
        parameters.setPaymentMethod(PaymentMethod.CASH_ON_DELIVERY);
        parameters.setWarehouseForAllItems(TEST_WAREHOUSE_ID);
        parameters.getReportParameters().setDeliveryVat(VatType.VAT_10);

        Order order = orderCreateHelper.createOrder(parameters);

        checkOrderInDb(order);
        order = orderService.getOrder(order.getId());
        Assertions.assertEquals(VatType.VAT_20, order.getDelivery().getVat());
        Assertions.assertEquals(VatType.VAT_20, order.getItems().iterator().next().getVat());
    }

    @Tag(Tags.FULFILMENT)
    @Epic(Epics.CHECKOUT)
    @DisplayName("Создание предоплатного синего ФФ заказа без VAT")
    @Test
    public void createFFPrepaidOrderNoVat() {
        Parameters parameters = defaultBlueOrderParameters();
        parameters.setPaymentMethod(PaymentMethod.YANDEX);
        parameters.setColor(Color.BLUE);
        parameters.getReportParameters().setItemVat(null);
        parameters.setCheckCartErrors(false);

        parameters.cartResultActions()
                .andDo(log())
                .andExpect(jsonPath("$.validationErrors[*].code").value(hasItem("INVALID_FINANCE_DATA")))
                .andExpect(jsonPath("$.validationErrors[*].severity")
                        .value(hasItem(ValidationResult.Severity.ERROR.name())));

        orderCreateHelper.cart(parameters);
    }

    @Disabled("Этот тест не валиден для синего ФФ, т.к. VAT на доставку проставляется всегда, " +
            "см ru/yandex/market/checkout/checkouter/actualization/actualizers/DeliveryOptionsActualizer.java:118")
    @Tag(Tags.FULFILMENT)
    @Epic(Epics.CHECKOUT)
    @DisplayName("Создание предоплатного синего ФФ заказа без VAT")
    @Test
    public void createFFNoDeliveryVat() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.setPaymentMethod(PaymentMethod.YANDEX);
        parameters.setColor(Color.BLUE);
        parameters.getReportParameters().setDeliveryVat(null);
        parameters.setShopId(SHOP_ID_WITH_SORTING_CENTER);
        parameters.getOrder().getDelivery().setDeliveryServiceId(MOCK_SORTING_CENTER_DELIVERY_SERVICE_ID);

        ActualDeliveryResult actualDeliveryResult =
                Iterables.getOnlyElement(parameters.getReportParameters().getActualDelivery().getResults());
        actualDeliveryResult.setDelivery(Collections.singletonList(buildDeliveryOption()));
        parameters.setCheckCartErrors(false);

        MultiCart cart = orderCreateHelper.cart(parameters);
        assertThat(cart.getValidationErrors(), hasSize(1));
        assertThat(cart.getValidationErrors().get(0).getCode(), is("INVALID_FINANCE_DATA"));
        assertThat(cart.getValidationErrors().get(0).getSeverity(), is(ValidationResult.Severity.ERROR));
    }

    private ActualDeliveryOption buildDeliveryOption() {
        ActualDeliveryOption actualDeliveryOption = new ActualDeliveryOption();
        actualDeliveryOption.setDeliveryServiceId(MOCK_SORTING_CENTER_DELIVERY_SERVICE_ID);
        actualDeliveryOption.setCost(BigDecimal.TEN);
        actualDeliveryOption.setDayFrom(0);
        actualDeliveryOption.setDayTo(2);
        actualDeliveryOption.setCurrency(Currency.RUR);
        actualDeliveryOption.setShipmentDay(1);
        actualDeliveryOption.setPaymentMethods(Collections.singleton(PaymentMethod.YANDEX.name()));
        return actualDeliveryOption;
    }

    @Tag(Tags.FULFILMENT)
    @Epic(Epics.CHECKOUT)
    @DisplayName("Создание постоплатного синего ФФ заказа без VAT")
    @Test
    public void createFfPostpaidOrderNoVat() {
        Parameters parameters = defaultBlueOrderParameters();
        parameters.setPaymentMethod(PaymentMethod.CASH_ON_DELIVERY);
        parameters.getReportParameters().setItemVat(null);
        parameters.setCheckCartErrors(false);

        parameters.cartResultActions()
                .andDo(log())
                .andExpect(jsonPath("$.validationErrors[*].code").value(hasItem("INVALID_FINANCE_DATA")))
                .andExpect(jsonPath("$.validationErrors[*].severity")
                        .value(hasItem(ValidationResult.Severity.ERROR.name())));

        orderCreateHelper.cart(parameters);
    }

    @Tag(Tags.FULFILMENT)
    @Epic(Epics.CHECKOUT)
    @Story(Stories.CART)
    @DisplayName("Попытка создания фейквого ФФ заказа должна заканчиваться успешно, " +
            " на сток такой заказ не должен влиять")
    @Test
    //https://testpalm.yandex-team.ru/testcase/checkouter-43
    public void tryCreateFakeFfOrder() {
        Parameters parameters = defaultBlueOrderParameters();
        parameters.setSandbox(true);
        parameters.setCheckCartErrors(true);
        parameters.setExpectedCartReturnCode(200);
        parameters.setWarehouseForAllItems(TEST_WAREHOUSE_ID);

        MultiCart cart = orderCreateHelper.cart(parameters);
        Assertions.assertNotNull(cart.getCarts(), "Should have valid carts");

        pushApiMock.resetRequests();


        Order order = orderCreateHelper.createOrder(parameters);
        Assertions.assertTrue(order.isFake());
        Assertions.assertTrue(pushApiMock
                .getAllServeEvents()
                .stream()
                .filter(e -> e.getRequest().getUrl().toLowerCase().contains("accept"))
                .collect(Collectors.toList()).isEmpty()
        );


        checkOrderInDb(order);
    }

    @Tag(Tags.FULFILMENT)
    @Epic(Epics.CHECKOUT)
    @Story(Stories.CART)
    @DisplayName("Попытка создания ФФ заказа должна заканчиваться успешно, " +
            " должен вызываться push-api/ резервация")
    @Test
    public void tryCreateNotFakeFfOrder() {
        Parameters parameters = defaultBlueOrderParameters();
        parameters.setCheckCartErrors(true);
        parameters.setExpectedCartReturnCode(200);
        parameters.setWarehouseForAllItems(TEST_WAREHOUSE_ID);

        MultiCart cart = orderCreateHelper.cart(parameters);
        Assertions.assertNotNull(cart.getCarts(), "Should have valid carts");

        pushApiMock.resetRequests();

        Order order = orderCreateHelper.createOrder(parameters);
        Assertions.assertFalse(order.isFake());
        Assertions.assertTrue(pushApiMock
                .getAllServeEvents()
                .stream()
                .filter(e -> e.getRequest().getUrl().toLowerCase().contains("accept"))
                .collect(Collectors.toList()).isEmpty()
        );

        checkOrderInDb(order);
    }


    @Tag(Tags.FULFILMENT)
    @Epic(Epics.CHECKOUT)
    @Story(Stories.CART)
    @DisplayName("Создание ФФ заказа с fulfilmentShopId без предоплаты")
    @Test
    //https://testpalm.yandex-team.ru/testcase/checkouter-80
    public void createPrePayFfOrderWithPrepayFulfilmentShopId() {
        Parameters parameters = defaultBlueOrderParameters();
        parameters.setPaymentMethod(PaymentMethod.YANDEX);
        // Реальный fulfilment магазин
        parameters.addShopMetaData(FF_SHOP_ID, ShopSettingsHelper.getPostpayMeta());
        parameters.turnOffErrorChecks();

        MultiCart cart = orderCreateHelper.cart(parameters);
        //т.к. сейчас для ФФ постоплату выкидываем, а настройки пушатся без предоплаты
        //все доставки остаются без опций оплат
        //проверяем что нет предоплаты у всех доставок
        cart.getCarts().stream().flatMap(o -> o.getDeliveryOptions().stream()).forEach(
                delOpt -> assertTrue(!delOpt.getPaymentOptions().contains(PaymentMethod.YANDEX), "Should not have " +
                        "YANDEX prepay method")
        );
    }

    @Tag(Tags.FULFILMENT)
    @Epic(Epics.CHECKOUT)
    @DisplayName("НеСоздание ФФ заказа с разными fulfilmentShopId")
    @Test
    //https://testpalm.yandex-team.ru/testcase/checkouter-79
    public void createPrePayFfOrderWithTwoFulfilmentShopIds() {
        Parameters parameters = defaultBlueOrderParameters();
        addCustomItem(parameters, "222", item -> {
            item.setSku(TEST_SKU);
            item.setShopSku(TEST_SHOP_SKU);
            item.setSupplierId(FF_SHOP_ID);
            parameters.getReportParameters().overrideItemInfo(item.getFeedOfferId()).setFulfilment(
                    new ItemInfo.Fulfilment(FF_SHOP_ID, TEST_SKU, TEST_SHOP_SKU)
            );
        });
        // Другой реальный fulfilment магазин
        parameters.addShopMetaData(FF_SHOP_ID, ShopSettingsHelper.getPostpayMeta());
        parameters.turnOffErrorChecks();
        FFDeliveryProvider.setFFDeliveryParameters(parameters);
        parameters.setDeliveryServiceId(null);

        MultiCart cart = orderCreateHelper.cart(parameters);

        cart.getCarts().forEach(delOpt ->
                assertTrue(delOpt.getDeliveryOptions().stream()
                                .noneMatch(opt -> opt.getPaymentOptions().contains(PaymentMethod.YANDEX)),
                        "Should not have any valid delivery options"));
    }


    @Tag(Tags.FULFILMENT)
    @Epic(Epics.CHECKOUT)
    @Story(Stories.CHECKOUT)
    @DisplayName("Успешное создание фулфилмент-заказа с предоплатным fulfilmentShopId")
    @Test
    //https://testpalm.yandex-team.ru/testcase/checkouter-76
    public void createPrePayFfOrderWithPostPayShopId() {
        Parameters parameters = defaultBlueOrderParameters();
        parameters.setPaymentMethod(PaymentMethod.YANDEX);
        // Виртуальный магазин
        parameters.addShopMetaData(parameters.getOrder().getShopId(), ShopSettingsHelper.getPostpayMeta());
        parameters.setWarehouseForAllItems(TEST_WAREHOUSE_ID);

        Order order = orderCreateHelper.createOrder(parameters);

        checkOrderInDb(order);
    }

    @Tag(Tags.FULFILMENT)
    @Epic(Epics.CHECKOUT)
    @Story(Stories.CHECKOUT)
    @DisplayName("Сохранение весогабаритов корзины")
    @Test
    public void actualDeliveryBasketDimensionsTest() {
        Parameters parameters = defaultBlueOrderParameters();
        parameters.setDeliveryType(DeliveryType.PICKUP);
        parameters.getReportParameters()
                .setActualDelivery(
                        ActualDeliveryProvider.builder()
                                .addPickup(MOCK_DELIVERY_SERVICE_ID)
                                .addWeight(valueOf(1.5678))
                                .addDimensions(List.of(valueOf(20.4), valueOf(10L), valueOf(15.5)))
                                .build()
                );
        Order order = orderCreateHelper.createOrder(parameters);

        assertThat(order.getDelivery().getParcels(), hasSize(1));
        assertThat(order.getDelivery().getParcels().get(0).getWeight(), equalTo(1568L));
        assertThat(order.getDelivery().getParcels().get(0).getWidth(), equalTo(10L));
        assertThat(order.getDelivery().getParcels().get(0).getHeight(), equalTo(16L));
        assertThat(order.getDelivery().getParcels().get(0).getDepth(), equalTo(21L));
    }

    @Tag(Tags.FULFILMENT)
    @Epic(Epics.CHECKOUT)
    @Story(Stories.CHECKOUT)
    @DisplayName("Сохранение весогабаритов корзины (report вернул недостаточно измерений)")
    @Test
    public void actualDeliveryWrongBasketDimensionsTest() {
        Parameters parameters = defaultBlueOrderParameters();
        parameters.setDeliveryType(DeliveryType.PICKUP);
        parameters.getReportParameters().setActualDelivery(
                ActualDeliveryProvider.builder()
                        .addPickup(MOCK_DELIVERY_SERVICE_ID)
                        .addWeight(valueOf(1.5678))
                        .addDimensions(List.of(BigDecimal.valueOf(10L), BigDecimal.valueOf(15.5)))
                        .build()
        );
        Order order = orderCreateHelper.createOrder(parameters);

        assertThat(order.getDelivery().getParcels(), hasSize(1));
        assertThat(order.getDelivery().getParcels().get(0).getWeight(), equalTo(1568L));
        assertThat(order.getDelivery().getParcels().get(0).getWidth(), equalTo(0L));
        assertThat(order.getDelivery().getParcels().get(0).getHeight(), equalTo(0L));
        assertThat(order.getDelivery().getParcels().get(0).getDepth(), equalTo(0L));
    }

    @Tag(Tags.FULFILMENT)
    @Epic(Epics.GET_ORDER)
    @DisplayName("Проверяем поиск заказов по флагу фф")
    @Test
    public void searchFfOrder() {
        Parameters parameters = defaultBlueOrderParameters();
        parameters.setPaymentMethod(PaymentMethod.YANDEX);

        Order order = orderCreateHelper.createOrder(parameters);

        OrderSearchRequest searchRequest = new OrderSearchRequest();
        searchRequest.fulfillment = true;
        searchRequest.orderIds = singletonList(order.getId());
        PagedOrders orders = orderService.getOrders(searchRequest, ClientInfo.SYSTEM);
        assertThat(orders.getPager().getTotal(), is(1));
        assertThat(first(orders.getItems()).getId(), is(order.getId()));

        searchRequest.fulfillment = false;
        orders = orderService.getOrders(searchRequest, ClientInfo.SYSTEM);
        assertThat(orders.getPager().getTotal(), is(0));
    }

    @Tag(Tags.FULFILMENT)
    @Epic(Epics.CHECKOUT)
    @Story(Stories.CHECKOUT)
    @Test
    public void weightAndDimensionsStorage() {
        Parameters parameters = defaultBlueOrderParameters();
        parameters.setPaymentMethod(PaymentMethod.YANDEX);
        parameters.setupFulfillment(new ItemInfo.Fulfilment(FF_SHOP_ID, TEST_SKU, TEST_SHOP_SKU));
        parameters.setWeight(valueOf(30));
        parameters.setDimensions("10", "20", "30");
        Order order = orderCreateHelper.createOrder(parameters);
        order.getItems().forEach(item -> {
                    assertThat(item.getWeight(), is(30000L));
                    assertThat(item.getWidth(), is(10L));
                    assertThat(item.getHeight(), is(20L));
                    assertThat(item.getDepth(), is(30L));
                }
        );
    }

    @Tag(Tags.FULFILMENT)
    @Epic(Epics.CHECKOUT)
    @Story(Stories.CHECKOUT)
    @Test
    public void roundDimensionsStorage() {
        Parameters parameters = defaultBlueOrderParameters();
        parameters.setPaymentMethod(PaymentMethod.YANDEX);
        parameters.setupFulfillment(new ItemInfo.Fulfilment(FF_SHOP_ID, TEST_SKU, TEST_SHOP_SKU));
        parameters.setWeight(valueOf(30));
        parameters.setDimensions("10.4", "20.5", "0.1");
        Order order = orderCreateHelper.createOrder(parameters);
        order.getItems().forEach(item -> {
                    assertThat(item.getWeight(), is(30000L));
                    assertThat(item.getWidth(), is(10L));
                    assertThat(item.getHeight(), is(21L));
                    assertThat(item.getDepth(), is(1L));
                }
        );
    }

    @Tag(Tags.FULFILMENT)
    @Epic(Epics.CHECKOUT)
    @Story(Stories.CART)
    @DisplayName("Ошибка валидации при отсутствии веса в fulfilment-заказе")
    @Test
    public void weightMissingValidationTest() {
        Parameters parameters = defaultBlueOrderParameters();
        parameters.hideWeight(true);
        parameters.getOrder().getItems().iterator().next().setWeight(0L);
        getCartAndCheckValidationErrors(parameters, DimensionsMissingError.CODE);
    }

    @Tag(Tags.FULFILMENT)
    @Epic(Epics.CHECKOUT)
    @Story(Stories.CART)
    @DisplayName("Ошибка валидации при отрицательном значении веса в fulfilment-заказе")
    @Test
    public void weightIsNegativeValidationTest() {
        Parameters parameters = defaultBlueOrderParameters();
        parameters.setWeight(BigDecimal.valueOf(-1));
        getCartAndCheckValidationErrors(parameters, DimensionsMissingError.CODE);
    }

    @Tag(Tags.FULFILMENT)
    @Epic(Epics.CHECKOUT)
    @Story(Stories.CART)
    @DisplayName("Ошибка валидации при отсутствии габаритов в fulfilment-заказе")
    @Test
    public void dimensionsMissingValidationTest() {
        Parameters parameters = defaultBlueOrderParameters();
        parameters.setupFulfillment(new ItemInfo.Fulfilment(FF_SHOP_ID, TEST_SKU, TEST_SHOP_SKU));
        parameters.hideDimensions(true);
        getCartAndCheckValidationErrors(parameters, DimensionsMissingError.CODE);
    }

    @Tag(Tags.FULFILMENT)
    @Epic(Epics.CHECKOUT)
    @Story(Stories.CART)
    @DisplayName("Ошибка валидации при отрицательном значении габаритов в fulfilment-заказе")
    @Test
    public void dimensionsAreNegativeValidationTest() {
        Parameters parameters = defaultBlueOrderParameters();
        parameters.setupFulfillment(new ItemInfo.Fulfilment(FF_SHOP_ID, TEST_SKU, TEST_SHOP_SKU));
        parameters.setDimensions("-10", "10", "10");
        getCartAndCheckValidationErrors(parameters, DimensionsMissingError.CODE);
    }

    @Test
    public void pendingFulfilmentStatusExpiryTest() throws Exception {
        freezeTimeAt("2000-01-01T18:00:00Z");
        Order order = OrderProvider.getFulfilmentOrder();
        long orderId = orderCreateService.createOrder(order, ClientInfo.SYSTEM);
        orderUpdateService.reserveOrder(orderId, "667", order.getDelivery());
        Order updatedOrder = orderUpdateService.updateOrderStatus(orderId, OrderStatus.PENDING, OrderSubstatus
                .AWAIT_CONFIRMATION);

        assertThat("Incorrect PENDING order status expiry date",
                updatedOrder.getStatusExpiryDate(),
                nullValue());
    }

    @Test
    public void checkSupplierTypeIsReturned() {
        Parameters parameters = defaultBlueOrderParameters();
        parameters.setPaymentMethod(PaymentMethod.YANDEX);
        parameters.setSupplierTypeForAllItems(SupplierType.FIRST_PARTY);
        Order order = orderCreateHelper.createOrder(parameters);
        assertThat(
                order.getItems().stream()
                        .findAny()
                        .orElseThrow(() -> new RuntimeException("No items!"))
                        .getSupplierType(),
                not(nullValue()));
    }

    @Tag(Tags.FULFILMENT)
    @Epic(Epics.CHECKOUT)
    @Story(Stories.CART)
    @Test
    @DisplayName("Не должны помечать заказ с MISSING офферами как ff=true")
    public void shouldNotMarkEmptyOrderAsFF() {
        Parameters parameters = defaultBlueOrderParameters();
        OrderItem item = Iterables.getOnlyElement(parameters.getOrder().getItems());
        parameters.getReportParameters().overrideItemInfo(item.getFeedOfferId()).setHideOffer(true);
        parameters.setCheckCartErrors(false);

        MultiCart cart = orderCreateHelper.cart(parameters);

        Assertions.assertTrue(cart.getCarts().stream().noneMatch(BasicOrder::isFulfilment), "cart.fulfilment should " +
                "be false if no items are found");
    }


    private void getCartAndCheckValidationErrors(Parameters parameters, String... errorCodes) {
        parameters.setCheckCartErrors(false);
        MultiCart mc = orderCreateHelper.cart(parameters);
        assertThat(mc.getValidationErrors(), both(not(empty())).and(not(nullValue())));
        assertThat(mc.getValidationErrors(), hasSize(errorCodes.length));
        for (String errorCode : errorCodes) {
            assertThat(
                    mc.getValidationErrors().stream()
                            .map(ValidationResult::getCode)
                            .collect(Collectors.toList()),
                    hasItem(errorCode)
            );
        }
    }

    private void checkOrderInDb(Order order) {
        assertThat(order.getId(), notNullValue());
        assertThat(order.isFulfilment(), is(true));
        Order orderFromDb = orderService.getOrder(order.getId());
        assertThat(orderFromDb.isFulfilment(), is(true));
        assertThat(
                orderFromDb.getItems().stream()
                        .allMatch(orderItem
                                -> orderItem.getSupplierId() != null &&
                                TEST_WAREHOUSE_ID.equals(orderItem.getWarehouseId())),
                is(true)
        );
    }

    private void addCustomItem(Parameters parameters, String offerId, Consumer<OrderItem> orderTuner) {
        parameters.configureMultiCart(multiCart -> {
            OrderItem orderItem = OrderItemProvider.buildOrderItem(offerId);
            orderTuner.accept(orderItem);
            multiCart.getCarts().get(0).addItem(orderItem);
        });
    }
}
