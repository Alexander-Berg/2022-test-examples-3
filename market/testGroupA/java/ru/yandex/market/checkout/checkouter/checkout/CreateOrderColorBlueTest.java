package ru.yandex.market.checkout.checkouter.checkout;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import com.github.tomakehurst.wiremock.core.Admin;
import com.github.tomakehurst.wiremock.matching.RequestPattern;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.google.common.collect.Iterables;
import org.hamcrest.Matchers;
import org.hamcrest.core.Is;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.ItemChange;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.client.CartParameters;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Context;
import ru.yandex.market.checkout.checkouter.order.MultiOrder;
import ru.yandex.market.checkout.checkouter.order.OfferItem;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.helpers.DropshipDeliveryHelper;
import ru.yandex.market.checkout.helpers.YandexMarketDeliveryHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.test.providers.ActualDeliveryProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.util.datacamp.DatacampConfigurer;
import ru.yandex.market.checkout.util.report.ItemInfo;
import ru.yandex.market.common.report.model.ActualDeliveryResult;
import ru.yandex.market.common.report.model.MarketReportPlace;
import ru.yandex.market.common.report.model.OfferProblem;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static ru.yandex.market.checkout.providers.BlueParametersProvider.defaultBlueOrderParameters;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.INTAKE_AVAILABLE_DATE;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_DELIVERY_SERVICE_ID;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_SORTING_CENTER_DELIVERY_SERVICE_ID;
import static ru.yandex.market.checkout.test.providers.OrderProvider.SHOP_ID_WITH_SORTING_CENTER;

public class CreateOrderColorBlueTest extends AbstractWebTestBase {

    private static final Logger LOG = LoggerFactory.getLogger(CreateOrderColorBlueTest.class);

    @Autowired
    private Admin reportMock;
    @Autowired
    private YandexMarketDeliveryHelper yandexMarketDeliveryHelper;
    @Autowired
    private DatacampConfigurer datacampConfigurer;

    @BeforeEach
    void start() {
        checkouterProperties.setAsyncActualDeliveryRequest(false);
    }

    @AfterEach
    void end() {
        datacampConfigurer.reset();
    }

    @Test
    public void testCreateOrderWithColor() {
        Parameters parameters = defaultBlueOrderParameters();

        Order order = orderCreateHelper.createOrder(parameters);

        assertEquals(Color.BLUE, order.getRgb());
        Assertions.assertEquals(Boolean.TRUE, order.isFulfilment());

        Order fromGet = orderService.getOrder(order.getId());

        assertEquals(Color.BLUE, fromGet.getRgb());
        Assertions.assertEquals(Boolean.TRUE, order.isFulfilment());
    }

    @Test
    public void shouldAllowToCreateBlueNotFullfilmentOrder() throws Exception {
        Parameters parameters = DropshipDeliveryHelper.getDropshipPrepaidParameters();
        parameters.setContext(Context.SELF_CHECK);
        parameters.setCheckCartErrors(false);
        parameters.cartResultActions().andDo(log()).andExpect(jsonPath("$.validationErrors").doesNotExist());

        orderCreateHelper.cart(parameters);
        Order order = orderCreateHelper.createOrder(parameters);
        order = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);

        assertEquals(OrderStatus.DELIVERED, order.getStatus());
        assertEquals(Color.BLUE, order.getRgb());
        assertEquals(Boolean.FALSE, order.isFulfilment());

        assertThat(reportMock.getServeEvents().getServeEvents()
                .stream()
                .filter(se -> se.getRequest()
                        .queryParameter("place")
                        .containsValue(MarketReportPlace.OFFER_INFO.getId()))
                .peek(x -> LOG.debug("" + x.getRequest()))
                .filter(se -> se.getRequest()
                        .queryParameter("rgb")
                        .containsValue(ru.yandex.market.common.report.model.Color.BLUE.getValue()))
                .collect(Collectors.toList()), hasSize(greaterThanOrEqualTo(1))
        );
    }

    @Test
    public void shouldSetBlueFieldsForBlueNotFullfilmentOrder() {
        Parameters parameters = DropshipDeliveryHelper.getDropshipPrepaidParameters();
        parameters.setCheckCartErrors(false);
        parameters.cartResultActions().andDo(log()).andExpect(jsonPath("$.validationErrors").doesNotExist());

        orderCreateHelper.cart(parameters);
        Order order = orderCreateHelper.createOrder(parameters);
        order = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);

        assertEquals(OrderStatus.DELIVERED, order.getStatus());
        assertEquals(Color.BLUE, order.getRgb());
        assertEquals(Boolean.FALSE, order.isFulfilment());

        order.getItems().forEach(
                item -> {
                    Assertions.assertNotNull(item.getSku());
                    Assertions.assertNotNull(item.getShopSku());
                    Assertions.assertNotNull(item.getMsku());
                    Assertions.assertNotNull(item.getSupplierId());
                }
        );
    }

    @Test
    public void testCheckoutMissingItems() {
        Parameters parameters = defaultBlueOrderParameters();
        parameters.getOrder().getItems().forEach(oi -> {
            oi.setWareMd5(null);
            oi.setShowInfo(null);
        });
        parameters.setColor(Color.BLUE);

        OrderItem item = Iterables.getOnlyElement(parameters.getOrder().getItems());

        parameters.getReportParameters().overrideItemInfo(item.getFeedOfferId()).setHideOffer(true);
        parameters.setCheckCartErrors(false);

        MultiCart cart = orderCreateHelper.cart(parameters);
        assertThat(Iterables.getOnlyElement(cart.getCarts().get(0).getItems()).getChanges(),
                hasItem(ItemChange.MISSING));
    }

    @Test
    public void testCheckoutMissingItems2() throws Exception {
        Parameters parameters = defaultBlueOrderParameters();
        parameters.setColor(Color.BLUE);

        OrderItem item = Iterables.getOnlyElement(parameters.getOrder().getItems());

        parameters.getReportParameters().overrideItemInfo(item.getFeedOfferId()).setHideOffer(true);
        parameters.setCheckOrderCreateErrors(false);

        MultiCart multiCart = parameters.getBuiltMultiCart();

        MultiOrder multiOrder = new MultiOrder();
        multiOrder.setBuyer(parameters.getBuyer());
        multiOrder.setBuyerCurrency(Currency.RUR);
        multiOrder.setBuyerRegionId(parameters.getBuyer().getRegionId());
        multiOrder.setPaymentMethod(parameters.getPaymentMethod());
        multiOrder.setPaymentType(multiCart.getPaymentType());
        multiOrder.setOrders(multiCart.getCarts().subList(0, 1));
        multiOrder.getOrders().get(0)
                .getDelivery()
                .setHash("vujQrQNMAdM1cYADVSGnQEsID4jiUVQLCTQeRlTIMAVNym2GSGBySKLGbMxSwXqb97aYWMt46Q/0xIp8r/e" +
                        "/vlPvYizHLi5m");

        orderCreateHelper.initializeMock(parameters);
        MultiOrder cart = orderCreateHelper.checkout(multiOrder, parameters);
        assertThat(
                Iterables.getOnlyElement(cart.getOrderFailures().get(0).getOrder().getItems()).getChanges(),
                hasItem(ItemChange.MISSING)
        );

        List<ServeEvent> serveEvents = reportMock.getServeEvents().getServeEvents();
        Assertions.assertTrue(serveEvents.stream()
                .noneMatch(se -> se.getRequest().getUrl().contains("place=actual_delivery")), "Нет запросов к " +
                "actual_delivery");
    }

    @Test
    public void testCheckoutMissingAndNotMissing() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();

        OrderItem first = Iterables.getOnlyElement(parameters.getOrder().getItems());
        first.setSku(null);
        first.setShopSku(null);
        first.setSupplierId(null);

        OrderItem second = OrderItemProvider.getOrderItem();

        parameters.getOrder().addItem(second);

        ItemInfo itemInfo = parameters.getReportParameters().overrideItemInfo(first.getFeedOfferId());
        itemInfo.setHideOffer(true);

        ItemInfo secondInfo = parameters.getReportParameters().overrideItemInfo(second.getFeedOfferId());
        secondInfo.setFulfilment(new ItemInfo.Fulfilment(1111L, "1111", "1111"));

        parameters.setCheckCartErrors(false);

        MultiCart cart = orderCreateHelper.cart(parameters);
        assertThat(cart.getCarts().get(0).getItem(first.getFeedOfferId()).getChanges(), hasItem(ItemChange.MISSING));
    }

    @Test
    public void testNoActualDeliveryResults() throws Exception {
        Parameters parameters = defaultBlueOrderParameters();
        parameters.getReportParameters().setActualDelivery(null);
        parameters.setCheckCartErrors(false);
        parameters.cartResultActions()
                .andExpect(jsonPath("$.carts[*].validationErrors").value(hasSize(1)))
                .andExpect(
                        jsonPath("$.carts[*].validationErrors[0].code")
                                .value(containsInAnyOrder("NO_ACTUAL_DELIVERY_OPTIONS"))
                );

        orderCreateHelper.cart(parameters);
    }

    @Test
    public void testActualDeliveryReturnedOfferProblems() throws Exception {
        Parameters parameters = defaultBlueOrderParameters();
        parameters.getReportParameters().setActualDelivery(
                ActualDeliveryProvider.builder()
                        .addOfferProblem(createOfferProblem(parameters))
                        .build()
        );
        parameters.setCheckCartErrors(false);
        parameters.cartResultActions()
                .andExpect(jsonPath("$.carts[*].validationErrors").value(hasSize(1)))
                .andExpect(
                        jsonPath("$.carts[*].validationErrors[0].code")
                                .value(containsInAnyOrder("ACTUAL_DELIVERY_OFFER_PROBLEMS"))
                );

        orderCreateHelper.cart(parameters);
    }

    @Test
    public void testActualDeliveryReturnedCommonProblems() {
        setFixedTime(INTAKE_AVAILABLE_DATE);
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withColor(Color.BLUE)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .withShipmentDay(1)
                .withActualDelivery(
                        ActualDeliveryProvider.builder()
                                .addCommonProblem("NO_POST_OFFICE_FOR_POST_CODE")
                                .build()
                )
                .buildParameters();
        parameters.setCheckCartErrors(false);
        parameters.cartResultActions()
                .andExpect(jsonPath("$.carts[*].validationWarnings").value(hasSize(1)))
                .andExpect(
                        jsonPath("$.carts[*].validationWarnings[0].code")
                                .value(containsInAnyOrder("NO_POST_OFFICE_FOR_POST_CODE"))
                );

        orderCreateHelper.cart(parameters);
    }

    @Test
    public void testActualDeliveryReturnedCommonProblemsWithClientApi() throws IOException {
        setFixedTime(INTAKE_AVAILABLE_DATE);
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withColor(Color.BLUE)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .withShipmentDay(1)
                .withActualDelivery(
                        ActualDeliveryProvider.builder()
                                .addCommonProblem("NO_POST_OFFICE_FOR_POST_CODE")
                                .build()
                )
                .buildParameters();
        orderCreateHelper.initializeMock(parameters);
        CartParameters cartParameters = CartParameters.builder()
                .withUid(parameters.getBuyer().getUid())
                .withRgb(Color.BLUE)
                .build();
        MultiCart cart = client.cart(parameters.getBuiltMultiCart(), cartParameters);
        assertThat(cart.getValidationWarnings(), hasSize(1));
        assertThat(
                Iterables.getOnlyElement(cart.getValidationWarnings()).getCode(),
                is("NO_POST_OFFICE_FOR_POST_CODE")
        );
    }

    @Test
    public void testActualDeliveryReturnedResultsAndCommonProblemsWithClientApi() throws IOException {
        setFixedTime(INTAKE_AVAILABLE_DATE);
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withColor(Color.BLUE)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .withShipmentDay(1)
                .withActualDelivery(
                        ActualDeliveryProvider.builder()
                                .addPickup(100501L, 2, Collections.singletonList(12312303L))
                                .addDelivery(MOCK_SORTING_CENTER_DELIVERY_SERVICE_ID, 1)
                                .addCommonProblem("NO_POST_OFFICE_FOR_POST_CODE")
                                .build()
                )
                .buildParameters();
        orderCreateHelper.initializeMock(parameters);
        CartParameters cartParameters = CartParameters.builder()
                .withUid(parameters.getBuyer().getUid())
                .withRgb(Color.BLUE)
                .build();
        MultiCart cart = client.cart(parameters.getBuiltMultiCart(), cartParameters);
        assertThat(cart.getCarts(), hasSize(1));
        assertThat(Iterables.getOnlyElement(cart.getCarts()).getDeliveryOptions(), Matchers.not(empty()));
        assertThat(
                Iterables.getOnlyElement(cart.getCarts())
                        .getDeliveryOptions()
                        .stream()
                        .map(Delivery::getType)
                        .collect(Collectors.toSet()),
                containsInAnyOrder(DeliveryType.DELIVERY, DeliveryType.PICKUP)
        );
        assertThat(cart.getValidationWarnings(), hasSize(1));
        assertThat(
                Iterables.getOnlyElement(cart.getValidationWarnings()).getCode(),
                is("NO_POST_OFFICE_FOR_POST_CODE")
        );
    }

    @Test
    public void canCreateOrderWhenActualDeliveryReturnedResultsAndCommonProblems() throws IOException {
        setFixedTime(INTAKE_AVAILABLE_DATE);
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_SORTING_CENTER_DELIVERY_SERVICE_ID)
                .withColor(Color.BLUE)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .withShipmentDay(1)
                .withActualDelivery(
                        ActualDeliveryProvider.builder()
                                .addPickup(100501L, 2, Collections.singletonList(12312303L))
                                .addDelivery(MOCK_SORTING_CENTER_DELIVERY_SERVICE_ID, 1)
                                .addCommonProblem("NO_POST_OFFICE_FOR_POST_CODE")
                                .build()
                )
                .buildParameters();
        orderCreateHelper.createOrder(parameters);
    }

    @Test
    public void testNoDeliveryResultsAfterActualization() throws Exception {
        Parameters parameters = defaultBlueOrderParameters();
        ActualDeliveryResult actualDeliveryResult = Iterables.getOnlyElement(
                parameters.getReportParameters().getActualDelivery().getResults()
        );
        actualDeliveryResult.setDelivery(emptyList());
        actualDeliveryResult.setPost(emptyList());
        Iterables.getOnlyElement(actualDeliveryResult.getPickup()).setPaymentMethods(emptySet());

        parameters.setCheckCartErrors(false);
        parameters.cartResultActions()
                .andExpect(jsonPath("$.carts[*].validationErrors").value(hasSize(1)))
                .andExpect(
                        jsonPath("$.carts[*].validationErrors[0].code")
                                .value(containsInAnyOrder("NO_ACTUAL_DELIVERY_OPTIONS"))
                );

        orderCreateHelper.cart(parameters);
    }

    @Nonnull
    private OfferProblem createOfferProblem(Parameters parameters) {
        OfferProblem problem = new OfferProblem();
        problem.setWareId(
                parameters.getBuiltMultiCart()
                        .getCarts()
                        .stream()
                        .flatMap(c -> c.getItems().stream())
                        .map(OfferItem::getWareMd5)
                        .findFirst()
                        .get()
        );
        problem.setProblems(singletonList("NONEXISTENT_OFFER"));
        return problem;
    }

    @Test
    public void testCreateOrderWithProactiveActualDeliveryRequest() {
        Parameters parameters = defaultBlueOrderParameters();

        checkouterProperties.setAsyncActualDeliveryRequest(false);
        orderCreateHelper.cart(parameters);
        //shop_info, offer_info, 2 actual_delivery - основной и уточняющий
        assertThat(reportMock.countRequestsMatching(RequestPattern.everything()).getCount(), Is.is(3));

        checkouterProperties.setAsyncActualDeliveryRequest(true);
        reportMock.resetAll();

        orderCreateHelper.cart(parameters);
        //shop_info, offer_info, 2 actual_delivery - основной и уточняющий
        //проверяем, что не делается дополнительный запрос в actual_delivery
        assertThat(reportMock.countRequestsMatching(RequestPattern.everything()).getCount(), Is.is(3));
    }
}
