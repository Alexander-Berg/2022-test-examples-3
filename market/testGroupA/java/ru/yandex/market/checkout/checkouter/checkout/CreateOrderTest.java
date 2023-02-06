package ru.yandex.market.checkout.checkouter.checkout;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Iterables;
import io.qameta.allure.Epic;
import io.qameta.allure.Story;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.allure.Epics;
import ru.yandex.market.checkout.allure.Stories;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.ShopMetaDataBuilder;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.client.CheckouterClientParams;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.Buyer;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.MultiOrder;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderAcceptMethod;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderSortingType;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.checkout.checkouter.shop.PaymentClass;
import ru.yandex.market.checkout.helpers.OrderGetHelper;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.providers.DeliveryResponseProvider;
import ru.yandex.market.checkout.providers.WhiteParametersProvider;
import ru.yandex.market.checkout.test.providers.BuyerProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;
import ru.yandex.market.checkout.util.balance.ShopSettingsHelper;
import ru.yandex.market.common.report.model.FeedOfferId;
import ru.yandex.market.common.report.model.LocalDeliveryOption;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.FF_DELIVERY_SERVICE_ID;

/**
 * @author musachev
 */
public class CreateOrderTest extends AbstractWebTestBase {

    @Autowired
    protected OrderPayHelper orderPayHelper;

    @Autowired
    private OrderGetHelper orderGetHelper;

    /**
     * checkouter-2: Успешное создание заказа, магазин с partner-interface=true
     *
     * @link https://testpalm.yandex-team.ru/testcase/checkouter-2
     */
    @Epic(Epics.CHECKOUT)
    @Story(Stories.CHECKOUT)
    @DisplayName("Успешное создание заказа, магазин с partner-interface=true")
    @Test
    public void createOrderStub() {
        Parameters parameters = BlueParametersProvider.defaultBlueNonFulfilmentOrderParameters();
        parameters.setAcceptMethod(OrderAcceptMethod.WEB_INTERFACE);
        parameters.setDeliveryType(DeliveryType.DELIVERY);
        parameters.setDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET);
        Order createdOrder = orderCreateHelper.createOrder(parameters);

        assertThat(createdOrder.getId(), notNullValue());
        assertEquals(OrderAcceptMethod.WEB_INTERFACE, createdOrder.getAcceptMethod());
        assertEquals(OrderStatus.PENDING, createdOrder.getStatus());
    }

    /**
     * checkouter-12: Успешное создание заказа, магазин с partner-interface=false
     *
     * @link https://testpalm.yandex-team.ru/testcase/checkouter-12
     */
    @Epic(Epics.CHECKOUT)
    @Story(Stories.CHECKOUT)
    @DisplayName("Успешное создание заказа, магазин с partner-interface=false")
    @Test
    public void createOrderApi() {
        Parameters parameters = BlueParametersProvider.defaultBlueNonFulfilmentOrderParameters();
        parameters.setAcceptMethod(OrderAcceptMethod.PUSH_API);
        parameters.setDeliveryType(DeliveryType.DELIVERY);
        parameters.setDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET);
        Order createdOrder = orderCreateHelper.createOrder(parameters);

        assertThat(createdOrder.getId(), notNullValue());
        assertEquals(OrderAcceptMethod.PUSH_API, createdOrder.getAcceptMethod());
        assertEquals(OrderStatus.PROCESSING, createdOrder.getStatus());
    }

    /**
     * checkouter-61: Успешное создание фейкового заказа
     *
     * @link https://testpalm.yandex-team.ru/testcase/checkouter-61
     * <p>
     * Создать заказ с
     * sandbox = true
     * Exception
     * Заказ успешно создан, в ответе “fake”: true
     */
    @Epic(Epics.CHECKOUT)
    @Story(Stories.CHECKOUT)
    @DisplayName("Успешное создание фейкового заказа")
    @Test
    public void createOrderFake() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.setSandbox(true);
        Order createdOrder = orderCreateHelper.createOrder(parameters);

        assertThat(createdOrder.getId(), notNullValue());
        assertEquals(OrderStatus.UNPAID, createdOrder.getStatus());

        assertTrue(createdOrder.isFake());
    }

    /**
     * checkouter-15: Успешное создание предоплатного заказа.
     *
     * @link https://testpalm.yandex-team.ru/testcase/checkouter-15
     */
    @Epic(Epics.CHECKOUT)
    @Story(Stories.CHECKOUT)
    @DisplayName("Успешное создание предоплатного заказа.")
    @Test
    public void createPrepaidOrder() {
        Parameters parameters = BlueParametersProvider.prepaidBlueOrderParameters();
        parameters.getBuiltMultiCart().setPaymentType(PaymentType.PREPAID);
        parameters.getBuiltMultiCart().setPaymentMethod(PaymentMethod.YANDEX);
        parameters.addShopMetaData(
                parameters.getOrder().getShopId(),
                ShopSettingsHelper.getDefaultMeta()
        );

        Order order = orderCreateHelper.createOrder(parameters);

        order = orderService.getOrder(order.getId());

        Assertions.assertEquals(OrderStatus.UNPAID, order.getStatus());
        Assertions.assertEquals(PaymentType.PREPAID, order.getPaymentType());
        Assertions.assertEquals(PaymentMethod.YANDEX, order.getPaymentMethod());
    }

    /**
     * checkouter-15 checkouter-2: Успешное создание предоплатного заказа для ПИ магазина.
     * Оплачиваем его и получаем заказ в PENDING.
     *
     * @link https://testpalm.yandex-team.ru/testcase/checkouter-15
     */
    @Disabled("Flapping test")
    @Test
    public void createPrepaidPiOrderAndPay() {
        Parameters parameters = BlueParametersProvider.defaultBlueNonFulfilmentOrderParameters();
        parameters.setAcceptMethod(OrderAcceptMethod.WEB_INTERFACE);
        parameters.getBuiltMultiCart().setPaymentType(PaymentType.PREPAID);
        parameters.getBuiltMultiCart().setPaymentMethod(PaymentMethod.YANDEX);
        parameters.addShopMetaData(
                parameters.getOrder().getShopId(),
                ShopSettingsHelper.getDefaultMeta()
        );

        Order order = orderCreateHelper.createOrder(parameters);
        orderPayHelper.payForOrder(order);

        order = orderService.getOrder(order.getId());

        Assertions.assertEquals(OrderStatus.PENDING, order.getStatus());
        Assertions.assertEquals(PaymentType.PREPAID, order.getPaymentType());
        Assertions.assertEquals(PaymentMethod.YANDEX, order.getPaymentMethod());
    }

    /**
     * checkouter-65: Успешное создание заказа с кириллическим email.
     */
    @Test
    public void createOrderWithCyrillicMail() {
        String email = "login@почта.рф";

        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.getOrder().getBuyer().setEmail(email);

        Order createdOrder = orderCreateHelper.createOrder(parameters);

        Assertions.assertEquals(email, createdOrder.getBuyer().getEmail());
    }

    /**
     * checkouter-133. Создание заказа со строковым offerId.
     */
    @Epic(Epics.CHECKOUT)
    @Story(Stories.CHECKOUT)
    @DisplayName("Создание заказа со строковым offerId.")
    @Test
    public void createOrderWithAlphaNumericOfferId() {
        String offerId = "abcdef0123456";

        OrderItem orderItem = OrderItemProvider.getOrderItem();
        orderItem.setFeedOfferId(new FeedOfferId(offerId, 1L));

        Order order = OrderProvider.getBlueOrder(o -> o.setItems(Collections.singleton(orderItem)));

        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters(order);

        Order createdOrder = orderCreateHelper.createOrder(parameters);

        Assertions.assertEquals(offerId, Iterables.getOnlyElement(createdOrder.getItems()).getOfferId());
    }

    @Test
    public void filterOutPrepaidPaymentMethodIfProdClassIsOff() {
        Parameters parameters = BlueParametersProvider.prepaidBlueOrderParameters();
        LocalDeliveryOption localDeliveryOption = new LocalDeliveryOption();
        localDeliveryOption.setDeliveryServiceId(FF_DELIVERY_SERVICE_ID);
        localDeliveryOption.setDayFrom(0);
        localDeliveryOption.setDayTo(2);
        localDeliveryOption.setPaymentMethods(
                Set.of(
                        PaymentMethod.YANDEX.name(),
                        PaymentMethod.CARD_ON_DELIVERY.name(),
                        PaymentMethod.CASH_ON_DELIVERY.name()
                )
        );
        localDeliveryOption.setPrice(new BigDecimal("100"));
        FeedOfferId feedOfferId = parameters.getOrder().getItems().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("No items in default order"))
                .getFeedOfferId();
        parameters.setShopId(OrderProvider.FF_ONLY_DELIVERY_SHOP_ID);
        parameters.getOrder().setDelivery(DeliveryResponseProvider.createFFDeliveryDelivery(false));
        parameters.setDeliveryType(DeliveryType.DELIVERY);
        parameters.setDeliveryPartnerType(DeliveryPartnerType.SHOP);
        parameters.setDeliveryServiceId(FF_DELIVERY_SERVICE_ID);
        parameters.getReportParameters().setLocalDeliveryOptions(
                Map.of(feedOfferId, Collections.singletonList(localDeliveryOption))
        );
        parameters.addShopMetaData(
                parameters.getOrder().getShopId(),
                ShopMetaDataBuilder.createTestDefault()
                        .withProdClass(PaymentClass.OFF)
                        .build()
        );
        parameters.getBuiltMultiCart().setPaymentOptions(null);
        parameters.getBuiltMultiCart().setPaymentMethod(null);
        parameters.getBuiltMultiCart().setPaymentType(null);
        parameters.getBuiltMultiCart().getCarts().forEach(c -> {
            c.setPaymentOptions(null);
            c.setPaymentMethod(null);
            c.setPaymentType(null);
        });

        MultiCart cart = orderCreateHelper.cart(parameters);

        assertThat(
                cart.getPaymentOptions(),
                hasItems(PaymentMethod.CARD_ON_DELIVERY, PaymentMethod.CASH_ON_DELIVERY));
        assertThat(
                cart.getCarts().stream().flatMap(c -> c.getPaymentOptions().stream()).collect(Collectors.toSet()),
                hasItems(PaymentMethod.CARD_ON_DELIVERY, PaymentMethod.CASH_ON_DELIVERY));
        assertThat(
                cart.getCarts().stream().flatMap(c -> c.getDeliveryOptions().stream()).flatMap(delivery -> delivery
                        .getPaymentOptions().stream()).collect(Collectors.toSet()),
                hasItems(PaymentMethod.CARD_ON_DELIVERY, PaymentMethod.CASH_ON_DELIVERY));
    }

    @Test
    public void createOrderWithYandexUid() {
        String yandexUid = "the_yandex_uid";
        Buyer buyer = BuyerProvider.getBuyer();
        buyer.setYandexUid(yandexUid);

        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters(buyer);

        Order createdOrder = orderCreateHelper.createOrder(parameters);

        Order orderFromDb = orderService.getOrder(createdOrder.getId());
        assertThat(orderFromDb.getBuyer().getYandexUid(), is(yandexUid));
    }

    @Test
    public void createOrderWithShopOrderId() {
        Parameters parameters = WhiteParametersProvider.simpleWhiteParameters();
        parameters.setReserveOnly(true);
        parameters.setSandbox(true);

        Order order = orderCreateHelper.createOrder(parameters);

        Assertions.assertEquals("100500", order.getDisplayOrderId());
    }

    @Test
    public void doesNotCreateOrderWithBuyerRegionId() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.getBuiltMultiCart().setBuyerRegionId(null);
        parameters.setCheckCartErrors(false);
        parameters.setExpectedCheckoutReturnCode(400);
        parameters.setExpectedCartReturnCode(400);
        parameters.cartResultActions()
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value(
                        allOf(
                                containsString("Missing"),
                                containsString("region"),
                                containsString("id")
                        )
                ));

        orderCreateHelper.cart(parameters);
    }

    @Test
    public void doesNotCreateOrderWithEmptyOrders() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.setExpectedCheckoutReturnCode(400);
        parameters.setMultiCartAction(
                mc -> {
                    mc.setCarts(Collections.emptyList());
                    parameters.setMockPushApi(false);
                }
        );
        parameters.setCheckOrderCreateErrors(false);
        parameters.setErrorMatcher(
                jsonPath("$.status").exists()
        );
        parameters.checkoutResultActions()
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value(
                        allOf(
                                containsString("Missing"),
                                containsString("order")
                        )
                ));
        orderCreateHelper.createOrder(parameters);
    }

    @DisplayName("OrderItem: price дублируется в quantPrice, count в quantity")
    @Test
    public void shouldBeTheSameQuantityCountAndPriceQuantPrice() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();

        var orderItem = parameters.getOrder().getItems().iterator().next();

        Order createdOrder = orderCreateHelper.createOrder(parameters);

        Order orderFromDb = orderService.getOrder(createdOrder.getId());
        OrderItem actualOrderItem = Iterables.getOnlyElement(orderFromDb.getItems());

        BigDecimal expectedQuantity = orderItem.getQuantity();
        expectedQuantity = expectedQuantity.setScale(3);
        BigDecimal expectedQuantPrice = orderItem.getPrice();
        expectedQuantPrice = expectedQuantPrice.setScale(2);

        Assertions.assertEquals(expectedQuantity, actualOrderItem.getQuantity().setScale(3));
        Assertions.assertEquals(expectedQuantity.intValue(), actualOrderItem.getCount());

        Assertions.assertEquals(expectedQuantPrice, actualOrderItem.getQuantPrice());
        Assertions.assertEquals(expectedQuantPrice, actualOrderItem.getPrice());
    }

    @DisplayName("OrderItem: unitInfo передается от репорта и сохраняется в БД")
    @Test
    public void orderItemUnitInfoExistsTest() throws Exception {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();

        String unitInfo = "упаковка";
        parameters.getOrders().get(0).getItems().iterator().next().setUnitInfo(unitInfo);

        MultiCart cart = orderCreateHelper.cart(parameters);
        OrderItem orderItemFromCart = Iterables.getOnlyElement(cart.getCarts().get(0).getItems());

        MultiOrder checkout = orderCreateHelper.checkout(cart, parameters);
        Order order = checkout.getOrders().get(0);

        OrderItem orderItemFromCheckout = Iterables.getOnlyElement(order.getItems());

        Order orderFromDb = orderService.getOrder(order.getId());
        OrderItem actualOrderItem = Iterables.getOnlyElement(orderFromDb.getItems());

        orderStatusHelper.proceedOrderToStatus(orderFromDb, OrderStatus.DELIVERY);

        assertEquals(unitInfo, orderItemFromCart.getUnitInfo());
        assertEquals(unitInfo, orderItemFromCheckout.getUnitInfo());
        assertEquals(unitInfo, actualOrderItem.getUnitInfo());

        long uid = order.getBuyer().getUid();
        mockMvc.perform(
                get("/orders/by-uid/{userId}", uid)
                        .param(CheckouterClientParams.RGB, Color.BLUE.name())
                        .param(CheckouterClientParams.DISABLE_DEFAULT_DATE_RANGE, "true")
                        .param(CheckouterClientParams.SORT, OrderSortingType.BY_DATE.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("orders[0].items[0].unitInfo").value(unitInfo));

        Order orderFromHelper = orderGetHelper.getOrder(order.getId(), ClientInfo.SYSTEM);

        assertEquals(unitInfo, orderFromHelper.getItems().iterator().next().getUnitInfo());
    }


    @DisplayName("item.countStep сохраняется в БД и отдается по хттп")
    @Test
    public void orderItemCountStepExistsTest() throws Exception {
        final int countStep = 2;
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();

        parameters.getOrders().get(0).getItems().iterator().next().setCountStep(countStep);

        Order order = orderCreateHelper.createOrder(parameters);

        Order orderFromDb = orderService.getOrder(order.getId());
        OrderItem actualOrderItem = Iterables.getOnlyElement(orderFromDb.getItems());

        assertEquals(countStep, actualOrderItem.getCountStep());

        mockMvc.perform(
                get("/orders/{id}", order.getId())
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].countStep").value(countStep));
    }
}
