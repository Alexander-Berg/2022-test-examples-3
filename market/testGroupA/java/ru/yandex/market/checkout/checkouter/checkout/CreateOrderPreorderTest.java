package ru.yandex.market.checkout.checkouter.checkout;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.actual.ActualItem;
import ru.yandex.market.checkout.checkouter.cart.ItemChange;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.common.util.UrlBuilder;
import ru.yandex.market.checkout.helpers.ActualizeHelper;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.YandexMarketDeliveryHelper;
import ru.yandex.market.checkout.helpers.utils.ActualizeParameters;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.helpers.utils.configuration.MockConfiguration;
import ru.yandex.market.checkout.providers.ActualItemProvider;
import ru.yandex.market.checkout.pushapi.web.XpathUtils;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;
import ru.yandex.market.checkout.util.GenericMockHelper;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static ru.yandex.market.checkout.checkouter.order.Color.BLUE;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_DELIVERY_SERVICE_ID;

public class CreateOrderPreorderTest extends AbstractWebTestBase {

    @Autowired
    private OrderPayHelper orderPayHelper;
    @Autowired
    private ActualizeHelper actualizeHelper;
    @Autowired
    private YandexMarketDeliveryHelper yandexMarketDeliveryHelper;

    @Test
    public void shouldCreatePreorderTest() {
        OrderItem item = OrderItemProvider.getOrderItem();
        item.setPreorder(true);
        item.setMsku(124L);

        Parameters parameters = new Parameters(OrderProvider.getBlueOrder(o -> {
            o.setItems(Collections.singletonList(item));
        }));
        parameters.setPaymentMethod(PaymentMethod.YANDEX);

        Order createdOrder = orderCreateHelper.createOrder(parameters);
        orderPayHelper.payForOrder(createdOrder);

        Assertions.assertTrue(createdOrder.isPreorder());

        createdOrder = orderService.getOrder(createdOrder.getId());

        Assertions.assertTrue(createdOrder.isPreorder());
        Assertions.assertTrue(createdOrder.getItems().stream().allMatch(OrderItem::isPreorder));
        assertThat(createdOrder.getStatus(), is(OrderStatus.PENDING));
        assertThat(createdOrder.getSubstatus(), is(OrderSubstatus.PREORDER));

        orderUpdateService.updateOrderStatus(createdOrder.getId(), OrderStatus.PROCESSING, ClientInfo.SYSTEM);
        createdOrder = orderService.getOrder(createdOrder.getId());
        assertThat(createdOrder.getStatus(), is(OrderStatus.PROCESSING));
    }

    @Test
    public void shouldRemovePostpaidOptionsForPreorder() {
        OrderItem item = OrderItemProvider.getOrderItem();
        item.setPreorder(true);
        item.setMsku(124L);

        Parameters parameters = new Parameters(OrderProvider.getBlueOrder(o -> {
            o.setItems(Collections.singletonList(item));
        }));
        parameters.setPaymentMethod(PaymentMethod.YANDEX);

        MultiCart cart = orderCreateHelper.cart(parameters);

        assertThat(
                cart.getPaymentOptions(),
                not(hasItems(PaymentMethod.CARD_ON_DELIVERY, PaymentMethod.CASH_ON_DELIVERY))
        );

        for (Delivery deliveryOption : cart.getCarts().get(0).getDeliveryOptions()) {
            assertThat(
                    deliveryOption.getPaymentOptions(),
                    not(hasItems(PaymentMethod.CASH_ON_DELIVERY, PaymentMethod.CARD_ON_DELIVERY))
            );
        }
    }

    @DisplayName("Нельзя смешивать предзаказа и обычные офферы в рамках одного заказа.")
    @Test
    public void shouldNotAllowToMixPreorderAndNotPreorder() {
        OrderItem first = OrderItemProvider.getOrderItem();
        first.setPreorder(true);
        first.setMsku(123L);

        OrderItem second = OrderItemProvider.getOrderItem();
        second.setPreorder(false);
        second.setMsku(125L);


        Order request = OrderProvider.getBlueOrder(o -> {
            o.setItems(Arrays.asList(first, second));
        });

        Parameters parameters = new Parameters(request);
        parameters.setCheckCartErrors(false);
        parameters.cartResultActions()
                .andExpect(
                        jsonPath("$.carts[0].items[?(@.offerId=='%s')].count", first.getOfferId())
                                .value(0)
                )
                .andExpect(
                        jsonPath("$.carts[0].items[?(@.offerId=='%s')].changes[*]", first.getOfferId())
                                .value(hasItems(ItemChange.MISSING.name()))
                );

        orderCreateHelper.cart(parameters);
    }

    @DisplayName("Чекаутер должен возвращать признак preorder у ActualItem")
    @Test
    public void shouldReturnPreorderInActualize() throws Exception {
        ActualItem actualItem = ActualItemProvider.buildActualItem();
        actualItem.setPreorder(true);

        ActualizeParameters actualizeParameters = new ActualizeParameters(actualItem);

        ActualItem actualized = actualizeHelper.actualizeItem(actualizeParameters);

        assertThat(actualized.isPreorder(), is(true));
    }

    @DisplayName("")
    @Test
    public void preorderOrdersShouldNotExpire() throws Exception {
        Instant createdAt = ZonedDateTime.of(
                2028, 7, 3,
                11, 53, 41,
                0,
                ZoneId.systemDefault()).toInstant();

        setFixedTime(createdAt);

        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withColor(BLUE)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withShopId(OrderProvider.SHOP_ID_WITH_SORTING_CENTER)
                .buildParameters();

        parameters.getReportParameters().getOrder().getItems()
                .forEach(oi -> {
                    oi.setCargoTypes(Sets.newHashSet(1, 2, 3));
                    oi.setPreorder(true);
                });
        parameters.setStockStorageMockType(MockConfiguration.StockStorageMockType.PREORDER_OK);

        Order order = orderCreateHelper.createOrder(parameters);
        orderPayHelper.payForOrder(order);

        assertThat(order.isPreorder(), is(true));

        setFixedTime(createdAt.plus(120, ChronoUnit.MINUTES).plusSeconds(1));

        tmsTaskHelper.runExpireOrderTaskV2();

        Order notCancelled = orderService.getOrder(order.getId());
        assertThat(notCancelled.getStatusExpiryDate(), nullValue());
        assertThat(notCancelled.getStatus(), is(OrderStatus.PENDING));
        assertThat(notCancelled.getSubstatus(), is(OrderSubstatus.PREORDER));
    }


    @DisplayName("Чекаутер должен пробрасывать признак preorder в Push-Api")
    @Test
    public void shouldPassPreorderToPushApi() throws Exception {
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withColor(BLUE)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withShopId(OrderProvider.SHOP_ID_WITH_SORTING_CENTER)
                .withPartnerInterface(false)
                .buildParameters();

        parameters.getReportParameters().getOrder().getItems()
                .forEach(oi -> {
                    oi.setCargoTypes(Sets.newHashSet(1, 2, 3));
                    oi.setPreorder(true);
                });
        parameters.setStockStorageMockType(MockConfiguration.StockStorageMockType.PREORDER_OK);

        Order order = orderCreateHelper.createOrder(parameters);

        List<ServeEvent> servedEvents = Lists.newArrayList(GenericMockHelper.servedEvents(pushApiMock));

        assertThat(servedEvents, hasSize(3));

        ServeEvent first = servedEvents.get(0);
        checkCart(order, first);

        ServeEvent second = servedEvents.get(1);
        checkCart(order, second);

        ServeEvent third = servedEvents.get(2);
        checkOrderAccept(order, third);
    }

    private void checkCart(Order order, ServeEvent event) throws Exception {
        UrlBuilder url = UrlBuilder.fromString(event.getRequest().getUrl());
        Assertions.assertEquals("/shops/" + order.getShopId() + "/cart", url.path);
        byte[] body = event.getRequest().getBody();

        XpathUtils.xpath("/cart/@preorder")
                .assertBoolean(body, StandardCharsets.UTF_8.name(), true);

    }

    private void checkOrderAccept(Order order, ServeEvent event) throws Exception {
        UrlBuilder url = UrlBuilder.fromString(event.getRequest().getUrl());
        Assertions.assertEquals("/shops/" + order.getShopId() + "/order/accept", url.path);
        byte[] body = event.getRequest().getBody();

        XpathUtils.xpath("/order/@preorder")
                .assertBoolean(body, StandardCharsets.UTF_8.name(), true);

    }
}
