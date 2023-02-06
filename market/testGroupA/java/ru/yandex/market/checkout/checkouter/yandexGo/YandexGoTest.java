package ru.yandex.market.checkout.checkouter.yandexGo;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.CheckouterYandexGoClient;
import ru.yandex.market.checkout.checkouter.client.YandexGoOrderParams;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryFeature;
import ru.yandex.market.checkout.checkouter.order.BasicOrder;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.yandexGo.YandexGoOrder;
import ru.yandex.market.checkout.checkouter.order.yandexGo.YandexGoOrders;
import ru.yandex.market.checkout.checkouter.request.RequestClientInfo;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.common.util.collections.CollectionUtils.isEmpty;

public class YandexGoTest extends AbstractWebTestBase {

    private List<Order> expressOrders;
    private List<Order> nonExpressOrders;
    private RequestClientInfo requestClientInfo;

    @Autowired
    private CheckouterYandexGoClient checkouterYandexGoClient;

    @BeforeEach
    void setUp() {
        expressOrders = new ArrayList<>();
        Parameters parameters = BlueParametersProvider.blueNonFulfilmentOrderWithExpressDelivery();
        expressOrders.add(orderCreateHelper.createOrder(parameters));
        var clientInfo = expressOrders.get(0).getUserClientInfo();
        requestClientInfo = new RequestClientInfo(clientInfo.getRole(), clientInfo.getId());

        nonExpressOrders = new ArrayList<>();
        Parameters nonExpressParams = BlueParametersProvider.defaultBlueOrderParameters();
        nonExpressOrders.add(orderCreateHelper.createOrder(nonExpressParams));

        LocalDateTime localDateTime = LocalDateTime.now().plusDays(35);
        setFixedTime(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
        Parameters secondParameters = BlueParametersProvider.blueNonFulfilmentOrderWithExpressDelivery();
        secondParameters.getBuyer().setUid(clientInfo.getUid());
        expressOrders.add(orderCreateHelper.createOrder(secondParameters));
        nonExpressOrders.add(orderCreateHelper.createOrder(nonExpressParams));

        localDateTime = LocalDateTime.now().plusDays(70);
        setFixedTime(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
        Parameters thirdParameters = BlueParametersProvider.blueNonFulfilmentOrderWithExpressDelivery();
        thirdParameters.getBuyer().setUid(clientInfo.getUid());
        expressOrders.add(orderCreateHelper.createOrder(thirdParameters));
        nonExpressOrders.add(orderCreateHelper.createOrder(nonExpressParams));
    }

    @Test
    @DisplayName("Должны вернуться только экспресс заказы по user id с фильтром по дате")
    void shouldFilterExpressUsingToDateFilter() {
        var toTimeStamp = LocalDateTime.now().plusDays(30).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        YandexGoOrders orders = checkouterYandexGoClient.getOrders(
                requestClientInfo.getClientId(),
                requestClientInfo,
                YandexGoOrderParams.builder()
                        .withToTimestamp(toTimeStamp)
                        .withDeliveryFeatures(EnumSet.of(DeliveryFeature.EXPRESS_DELIVERY))
                        .build()
        );

        assertThat(orders.getOrders(), hasSize(1));
        assertThat(orders.getOrders().stream().map(YandexGoOrder::getId).collect(Collectors.toList()),
                contains(expressOrders.get(0).getId()));
        assertDeliveryFeatures(orders);
    }

    @Test
    @DisplayName("Должны вернуться заказы по user id с фильтром по дате")
    void shouldFilterUsingToDateFilter() {
        var toTimeStamp = LocalDateTime.now().plusDays(30).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        YandexGoOrders orders = checkouterYandexGoClient.getOrders(
                requestClientInfo.getClientId(),
                requestClientInfo,
                YandexGoOrderParams.builder()
                        .withToTimestamp(toTimeStamp)
                        .build()
        );

        assertThat(orders.getOrders(), hasSize(2));
        assertThat(orders.getOrders().stream().map(YandexGoOrder::getId).collect(Collectors.toList()),
                containsInAnyOrder(expressOrders.get(0).getId(), nonExpressOrders.get(0).getId()));
        assertDeliveryFeatures(orders);
    }

    @Test
    @DisplayName("Должны вернуться только экспресс заказ по user id")
    void shouldFilterExpressUsingToOrderId() {
        YandexGoOrders orders = checkouterYandexGoClient.getOrders(
                requestClientInfo.getClientId(),
                requestClientInfo,
                YandexGoOrderParams.builder()
                        .withToOrderId(expressOrders.get(2).getId())
                        .withDeliveryFeatures(EnumSet.of(DeliveryFeature.EXPRESS_DELIVERY))
                        .build()
        );

        assertThat(orders.getOrders(), hasSize(2));
        assertThat(orders.getOrders().stream().map(YandexGoOrder::getId).collect(Collectors.toList()),
                containsInAnyOrder(expressOrders.get(1).getId(), expressOrders.get(0).getId()));
        assertDeliveryFeatures(orders);

    }

    @Test
    @DisplayName("Должны вернуться заказы по user id")
    void shouldFilterUsingToOrderId() {
        YandexGoOrders orders = checkouterYandexGoClient.getOrders(
                requestClientInfo.getClientId(),
                requestClientInfo,
                YandexGoOrderParams.builder()
                        .withToOrderId(expressOrders.get(2).getId())
                        .build()
        );

        assertThat(orders.getOrders(), hasSize(4));
        assertThat(orders.getOrders().stream().map(YandexGoOrder::getId).collect(Collectors.toList()),
                containsInAnyOrder(expressOrders.get(1).getId(), expressOrders.get(0).getId(),
                        nonExpressOrders.get(0).getId(), nonExpressOrders.get(1).getId()));
        assertDeliveryFeatures(orders);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("Должен вернуться заказ по id")
    public void testGetOrder(boolean isExpress) {
        Order order = isExpress ? expressOrders.get(0) : nonExpressOrders.get(0);
        YandexGoOrder lightOrder = checkouterYandexGoClient.getOrder(
                order.getId(),
                order.getUserClientInfo()
        );

        assertEquals(order.getId(), lightOrder.getId());
        assertEquals(order.getTotal(), lightOrder.getTotal());
        assertEquals(order.getBuyerTotal(), lightOrder.getBuyerTotal());
        assertEquals(order.getStatus(), lightOrder.getStatus());
        assertEquals(order.getSubstatus(), lightOrder.getSubstatus());
        assertEquals(order.getDelivery().getBuyerAddress(), lightOrder.getDelivery().getBuyerAddress());
        assertEquals(order.getDelivery().getBuyerPrice(), lightOrder.getDelivery().getBuyerPrice());
        assertEquals(order.getDelivery().getFeatures(), lightOrder.getDelivery().getFeatures());
    }

    @Test
    @DisplayName("Должны вернуться заказы по заданному списку id с картинками")
    public void testGetAllOrdersByOrderIds() {
        YandexGoOrders orders =
                checkouterYandexGoClient.getAllOrdersByOrderIds(
                        nonExpressOrders.stream()
                                .map(Order::getId)
                                .collect(Collectors.toList()),
                        nonExpressOrders.get(0).getUserClientInfo()
                );

        assertThat(orders.getOrders(), hasSize(3));
        assertThat(orders.getOrders().stream().map(YandexGoOrder::getId).collect(Collectors.toList()),
                containsInAnyOrder(nonExpressOrders.stream().map(Order::getId).toArray()));
        YandexGoOrder actualOrder = orders.getOrders().get(0);

        assertThat(actualOrder.getItems().get(0).getPictures(), notNullValue());
        assertThat(actualOrder.getItems().get(0).getPictures(), hasSize(1));
        assertThat(actualOrder.getItems().get(0).getPictures().stream().findFirst().orElseThrow()
                .getUrl(), notNullValue());
        assertDeliveryFeatures(orders);
    }

    @ParameterizedTest(name = "Item cargo type {0}: В заказе товар 18+")
    @ValueSource(ints = {20, 910})
    public void testShouldContainRestrictedAge18TrueItemProperty(int restrictedAge18CargoType) {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParametersWithItems(
                OrderItemProvider.orderItemBuilder()
                        .configure(OrderItemProvider::applyDefaults)
                        .cargoTypes(Set.of(restrictedAge18CargoType, 700))
                        .build()
        );

        Order order = orderCreateHelper.createOrder(parameters);
        YandexGoOrders orders = checkouterYandexGoClient
                .getAllOrdersByOrderIds(List.of(order.getId()), order.getUserClientInfo());

        assertThat(orders.getOrders(), hasSize(1));

        YandexGoOrder actualOrder = orders.getOrders().get(0);
        assertThat(actualOrder.getItems().get(0).getRestrictedAge18(), equalTo(true));
    }

    @Test
    @DisplayName("В заказе товар не 18+")
    public void testShouldContainRestrictedAge18FalseItemProperty() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParametersWithItems(
                OrderItemProvider.orderItemBuilder()
                        .configure(OrderItemProvider::applyDefaults)
                        .cargoTypes(Set.of(700))
                        .build()
        );
        Order order = orderCreateHelper.createOrder(parameters);
        YandexGoOrders orders = checkouterYandexGoClient
                .getAllOrdersByOrderIds(List.of(order.getId()), order.getUserClientInfo());

        assertThat(orders.getOrders(), hasSize(1));

        YandexGoOrder actualOrder = orders.getOrders().get(0);
        assertThat(actualOrder.getItems().get(0).getRestrictedAge18(), equalTo(false));
    }

    private void assertDeliveryFeatures(YandexGoOrders orders) {
        var expressIds = expressOrders.stream().map(BasicOrder::getId).collect(Collectors.toSet());
        assertTrue(orders.getOrders().stream()
                .filter(o -> expressIds.contains(o.getId()))
                .allMatch(o -> Objects.equals(o.getDelivery().getFeatures(), Set.of(DeliveryFeature.EXPRESS_DELIVERY)))
        );
        var nonExpressIds = nonExpressOrders.stream().map(BasicOrder::getId).collect(Collectors.toSet());
        assertTrue(orders.getOrders().stream()
                .filter(o -> nonExpressIds.contains(o.getId()))
                .allMatch(o -> isEmpty(o.getDelivery().getFeatures()))
        );
    }
}
