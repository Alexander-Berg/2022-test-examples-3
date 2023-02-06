package ru.yandex.market.checkout.checkouter.delivery;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.platform.commons.util.CollectionUtils;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.antifraud.orders.entity.AntifraudAction;
import ru.yandex.market.antifraud.orders.entity.AntifraudCheckResult;
import ru.yandex.market.antifraud.orders.entity.OrderVerdict;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.feature.type.common.ComplexFeatureType;
import ru.yandex.market.checkout.checkouter.order.ApiSettings;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Context;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderEditOptions;
import ru.yandex.market.checkout.checkouter.order.OrderEditOptionsRequest;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.common.util.SwitchWithWhitelist;
import ru.yandex.market.checkout.helpers.YandexMarketDeliveryHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.helpers.utils.ResultActionsContainer;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.test.providers.ActualDeliveryProvider;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;
import ru.yandex.market.checkout.util.Constants;
import ru.yandex.market.checkout.util.fulfillment.FulfillmentConfigurer;
import ru.yandex.market.checkout.util.report.ShipmentDayAndDateOption;
import ru.yandex.market.common.report.model.ActualDelivery;
import ru.yandex.market.common.report.model.ActualDeliveryOption;
import ru.yandex.market.common.report.model.DeliveryRoute;
import ru.yandex.market.common.report.model.LocalDeliveryOption;
import ru.yandex.market.common.report.model.MarketReportPlace;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.checkouter.order.Color.BLUE;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.PENDING;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.PROCESSING;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.ANOTHER_MOCK_DELIVERY_SERVICE_ID;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_DELIVERY_SERVICE_ID;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_INTAKE_DELIVERY_SERVICE_ID;
import static ru.yandex.market.checkout.test.providers.OrderProvider.SHOP_ID_WITH_SORTING_CENTER;

/**
 * @author mmetlov
 */
public class CRMScenarioTest extends AbstractWebTestBase {

    public static final String ANOTHER_ROUTE = "{\"anotherRoute\":111,\"this\":\"is\"}";
    @Autowired
    private YandexMarketDeliveryHelper yandexMarketDeliveryHelper;
    @Autowired
    private FulfillmentConfigurer fulfillmentConfigurer;

    @Test
    public void cartAcceptsAndReturnsItemId() {
        // делаем синий заказ
        Order order = createOrder(DeliveryType.DELIVERY);
        assertThat(order.getItems(), containsInAnyOrder(
                hasProperty("id", notNullValue())
        ));

        //приходим в /cart по-CRMному и получаем там опцию доставки в другую службу
        order.setDelivery(new Delivery(DeliveryProvider.REGION_ID));
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters(order);
        parameters.setPaymentMethod(PaymentMethod.YANDEX);
        parameters.setContext(Context.SANDBOX);
        parameters.setColor(BLUE);
        parameters.setSandbox(true);
        parameters.setApiSettings(ApiSettings.PRODUCTION);
        ActualDelivery actualDelivery = ActualDeliveryProvider.builder()
                .addDelivery(MOCK_INTAKE_DELIVERY_SERVICE_ID, 3)
                .build();
        parameters.getReportParameters().setActualDelivery(actualDelivery);
        reportMock.resetRequests();
        MultiCart crmCart = orderCreateHelper.cart(parameters);
        assertThat(Iterables.getOnlyElement(crmCart.getCarts()).getItems(), containsInAnyOrder(
                hasProperty("id", notNullValue())
        ));
    }

    private Order createOrder(DeliveryType delivery) {
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(delivery)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .withColor(BLUE)
                .withPartnerInterface(true)
                .buildParameters();

        parameters.getReportParameters().getOrder().getItems()
                .forEach(oi -> {
                    oi.setCargoTypes(Sets.newHashSet(1, 2, 3));
                });

        Order order = orderCreateHelper.createOrder(parameters);
        return orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);
    }

    @Test
    public void testRobocallWithCombinator() {
        // 1 создаем комбинаторный заказ с прозвоном
        checkouterFeatureWriter.writeValue(ComplexFeatureType.COMBINATOR_FLOW, new SwitchWithWhitelist(true,
                singleton(Constants.COMBINATOR_EXPERIMENT)));
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withColor(Color.BLUE)
                .withCombinator(true)
                .buildParameters();
        parameters.setExperiments(Constants.COMBINATOR_EXPERIMENT);
        parameters.setPaymentMethod(PaymentMethod.CASH_ON_DELIVERY);
        parameters.setMinifyOutlets(true);

        ActualDeliveryOption actualDeliveryOption =
                parameters.getReportParameters().getActualDelivery().getResults().get(0).getDelivery().get(0);
        actualDeliveryOption.setDayFrom(actualDeliveryOption.getDayTo());

        DeliveryRoute deliveryRoute = parameters.getReportParameters().getDeliveryRoute();
        ActualDeliveryOption deliveryRouteOption = deliveryRoute.getResults().get(0).getOption();
        deliveryRouteOption.setDayFrom(deliveryRouteOption.getDayTo());

        Set<AntifraudCheckResult> checkResults = new HashSet<>(
                Collections.singletonList(new AntifraudCheckResult(AntifraudAction.ROBOCALL, "", ""))
        );
        parameters.setMultiCartAction(mc ->
                mstatAntifraudConfigurer.mockVerdict(OrderVerdict.builder().checkResults(checkResults).build()));

        Order order = orderCreateHelper.createOrder(parameters);
        assertEquals(PENDING, order.getStatus());
        assertEquals(OrderSubstatus.AWAIT_CONFIRMATION, order.getSubstatus());

        // 2 проверяем что опция актуальна
        OrderEditOptions orderEditOptions = client.getOrderEditOptions(
                order.getId(),
                ClientRole.SYSTEM,
                null,
                Collections.singletonList(BLUE),
                new OrderEditOptionsRequest()
        );
        assertTrue(orderEditOptions.getCurrentDeliveryOptionActual());

        // 3 подкладываем новый маршрут
        deliveryRoute.getResults().get(0).setRoute(ANOTHER_ROUTE);
        reportConfigurer.mockReportPlace(MarketReportPlace.DELIVERY_ROUTE, parameters.getReportParameters());

        // 4 переводим в PROCESSING и ожидаем что новый мрашрут прикопался
        order = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);
        JSONAssert.assertEquals(
                ANOTHER_ROUTE,
                order.getDelivery().getParcels().get(0).getRoute().toString(),
                JSONCompareMode.NON_EXTENSIBLE
        );
    }

    @Test
    public void testRobocallWithCombinatorDeliveryServiceChanged() {
        // 1 создаем комбинаторный заказ с прозвоном
        checkouterFeatureWriter.writeValue(ComplexFeatureType.COMBINATOR_FLOW, new SwitchWithWhitelist(true,
                singleton(Constants.COMBINATOR_EXPERIMENT)));
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withColor(Color.BLUE)
                .withCombinator(true)
                .buildParameters();
        parameters.setExperiments(Constants.COMBINATOR_EXPERIMENT);
        parameters.setPaymentMethod(PaymentMethod.CASH_ON_DELIVERY);
        parameters.setMinifyOutlets(true);

        ActualDeliveryOption actualDeliveryOption =
                parameters.getReportParameters().getActualDelivery().getResults().get(0).getDelivery().get(0);
        actualDeliveryOption.setDayFrom(actualDeliveryOption.getDayTo());

        DeliveryRoute deliveryRoute = parameters.getReportParameters().getDeliveryRoute();
        ActualDeliveryOption deliveryRouteOption = deliveryRoute.getResults().get(0).getOption();
        deliveryRouteOption.setDayFrom(deliveryRouteOption.getDayTo());

        Set<AntifraudCheckResult> checkResults = new HashSet<>(
                Collections.singletonList(new AntifraudCheckResult(AntifraudAction.ROBOCALL, "", ""))
        );
        parameters.setMultiCartAction(mc ->
                mstatAntifraudConfigurer.mockVerdict(OrderVerdict.builder().checkResults(checkResults).build()));

        Order order = orderCreateHelper.createOrder(parameters);
        assertEquals(PENDING, order.getStatus());
        assertEquals(OrderSubstatus.AWAIT_CONFIRMATION, order.getSubstatus());

        // 2 проверяем что опция актуальна
        OrderEditOptions orderEditOptions = client.getOrderEditOptions(
                order.getId(),
                ClientRole.SYSTEM,
                null,
                Collections.singletonList(BLUE),
                new OrderEditOptionsRequest()
        );
        assertTrue(orderEditOptions.getCurrentDeliveryOptionActual());

        // 3 подкладываем новую службу доставки и новый маршрут
        deliveryRoute.getResults().get(0).getOption().setDeliveryServiceId(ANOTHER_MOCK_DELIVERY_SERVICE_ID);
        deliveryRoute.getResults().get(0).setRoute(ANOTHER_ROUTE);
        reportConfigurer.mockReportPlace(MarketReportPlace.DELIVERY_ROUTE, parameters.getReportParameters());

        // 4 переводим в PROCESSING и ожидаем что служба доставки изменилась и новый мрашрут прикопался
        order = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);
        JSONAssert.assertEquals(
                ANOTHER_ROUTE,
                order.getDelivery().getParcels().get(0).getRoute().toString(),
                JSONCompareMode.NON_EXTENSIBLE
        );

        assertEquals(ANOTHER_MOCK_DELIVERY_SERVICE_ID, order.getDelivery().getDeliveryServiceId());
    }

    @ParameterizedTest
    @EnumSource(value = ShipmentDayAndDateOption.class)
    public void testRobocallWithCombinatorPackagingTime(ShipmentDayAndDateOption shipmentDayOrDateOption) {
        checkouterFeatureWriter.writeValue(ComplexFeatureType.COMBINATOR_FLOW, new SwitchWithWhitelist(true,
                singleton(Constants.COMBINATOR_EXPERIMENT)));
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withColor(Color.BLUE)
                .withCombinator(true)
                .buildParameters();
        parameters.setExperiments(Constants.COMBINATOR_EXPERIMENT);
        parameters.setPaymentMethod(PaymentMethod.CASH_ON_DELIVERY);
        parameters.setMinifyOutlets(true);
        LocalDeliveryOption option = parameters.getReportParameters()
                .getDeliveryRoute()
                .getResults()
                .get(0)
                .getOption();

        Set<AntifraudCheckResult> checkResults = new HashSet<>(
                Collections.singletonList(new AntifraudCheckResult(AntifraudAction.ROBOCALL, "", ""))
        );
        parameters.setMultiCartAction(mc ->
                mstatAntifraudConfigurer.mockVerdict(OrderVerdict.builder().checkResults(checkResults).build()));

        Order order = orderCreateHelper.createOrder(parameters);
        assertEquals(PENDING, order.getStatus());
        assertEquals(OrderSubstatus.AWAIT_CONFIRMATION, order.getSubstatus());

        Parcel parcel = order.getDelivery().getParcels().get(0);
        LocalDate oldShipmentDate = parcel.getShipmentDate();
        Instant oldPackagingTime = parcel.getPackagingTime();
        shipmentDayOrDateOption.setupDeliveryOption(option, getClock(), option.getShipmentDay() + 3);
        option.setPackagingTime(option.getPackagingTime().plusDays(3));
        reportConfigurer.mockReportPlace(MarketReportPlace.DELIVERY_ROUTE, parameters.getReportParameters());
        order = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);

        parcel = order.getDelivery().getParcels().get(0);
        LocalDate newShipmentDate = parcel.getShipmentDate();
        Instant newPackagingTime = parcel.getPackagingTime();

        assertNotEquals(oldShipmentDate, newShipmentDate);
        assertNotEquals(oldPackagingTime, newPackagingTime);
    }

    @Test
    public void testRobocallWithCombinatorRouteExpired() {
        // 1 создаем комбинаторный заказ с прозвоном
        checkouterFeatureWriter.writeValue(ComplexFeatureType.COMBINATOR_FLOW, new SwitchWithWhitelist(true,
                singleton(Constants.COMBINATOR_EXPERIMENT)));
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withColor(Color.BLUE)
                .withCombinator(true)
                .buildParameters();
        parameters.setExperiments(Constants.COMBINATOR_EXPERIMENT);
        parameters.setPaymentMethod(PaymentMethod.CASH_ON_DELIVERY);
        parameters.setMinifyOutlets(true);

        Set<AntifraudCheckResult> checkResults = new HashSet<>(
                Collections.singletonList(new AntifraudCheckResult(AntifraudAction.ROBOCALL, "", ""))
        );
        parameters.setMultiCartAction(mc ->
                mstatAntifraudConfigurer.mockVerdict(OrderVerdict.builder().checkResults(checkResults).build()));

        Order order = orderCreateHelper.createOrder(parameters);
        assertEquals(PENDING, order.getStatus());
        assertEquals(OrderSubstatus.AWAIT_CONFIRMATION, order.getSubstatus());

        // 2 подкладываем новый маршрут c ошибкой
        DeliveryRoute deliveryRoute = parameters.getReportParameters().getDeliveryRoute();
        deliveryRoute.setCommonProblems(singletonList("SOME_PROBLEM"));
        reportConfigurer.mockReportPlace(MarketReportPlace.DELIVERY_ROUTE, parameters.getReportParameters());

        // 3 переводим в PROCESSING и ожидаем 400
        orderStatusHelper.updateOrderStatus(order.getId(), ClientInfo.SYSTEM, PROCESSING, null,
                new ResultActionsContainer()
                        .andExpect(status().is(400))
                        .andExpect(content().json("{\"status\":400," +
                                "\"code\":\"CURRENT_DELIVERY_OPTION_EXPIRED\"," +
                                "\"message\":\"Order " + order.getId() + " has expired delivery option.\"}")),
                null
        );
    }

    @Test
    public void testRequestReportWithLmsOutlets() {
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withColor(Color.BLUE)
                .buildParameters();
        parameters.setExperiments(Constants.LMS_OUTLETS_EXPERIMENT);
        parameters.setPaymentMethod(PaymentMethod.CASH_ON_DELIVERY);

        ActualDeliveryOption actualDeliveryOption =
                parameters.getReportParameters().getActualDelivery().getResults().get(0).getDelivery().get(0);
        actualDeliveryOption.setDayFrom(actualDeliveryOption.getDayTo());

        Set<AntifraudCheckResult> checkResults = new HashSet<>(
                Collections.singletonList(new AntifraudCheckResult(AntifraudAction.ROBOCALL, "", ""))
        );
        parameters.setMultiCartAction(mc ->
                mstatAntifraudConfigurer.mockVerdict(OrderVerdict.builder().checkResults(checkResults).build()));

        Order order = orderCreateHelper.createOrder(parameters);
        assertEquals(PENDING, order.getStatus());
        assertEquals(OrderSubstatus.AWAIT_CONFIRMATION, order.getSubstatus());

        reportMock.resetRequests();
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);

        ServeEvent actualDeliveryServeEvent = reportMock.getServeEvents().getRequests().stream()
                .filter(req -> req.getRequest().getQueryParams().get("place").isSingleValued()
                        && req.getRequest().getQueryParams().get("place").firstValue().equals("actual_delivery"))
                .findFirst().get();

        assertEquals(
                "market_use_lms_outlets=1",
                CollectionUtils.getOnlyElement(
                        actualDeliveryServeEvent.getRequest().getQueryParams().get("rearr-factors").values()
                )
        );
    }
}
