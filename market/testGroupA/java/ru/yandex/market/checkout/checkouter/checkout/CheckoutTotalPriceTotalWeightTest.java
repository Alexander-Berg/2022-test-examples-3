package ru.yandex.market.checkout.checkouter.checkout;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.actual.ActualItem;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.feature.type.common.ComplexFeatureType;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.MultiOrder;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderFailure;
import ru.yandex.market.checkout.common.util.SwitchWithWhitelist;
import ru.yandex.market.checkout.helpers.ActualizeHelper;
import ru.yandex.market.checkout.helpers.YandexMarketDeliveryHelper;
import ru.yandex.market.checkout.helpers.utils.ActualizeParameters;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.ActualItemProvider;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;
import ru.yandex.market.checkout.util.Constants;
import ru.yandex.market.checkout.util.report.ItemInfo;

import static java.util.Collections.singleton;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_DELIVERY_SERVICE_ID;

public class CheckoutTotalPriceTotalWeightTest extends AbstractWebTestBase {

    @Autowired
    private WireMockServer reportMock;
    @Autowired
    private ActualizeHelper actualizeHelper;
    @Autowired
    private YandexMarketDeliveryHelper yandexMarketDeliveryHelper;

    @Test
    public void shouldPassTotalWeightAndTotalPriceToReportActualDelivery() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.getItems()
                .forEach(oi -> oi.setCount(20));

        Order order = orderCreateHelper.createOrder(parameters);

        assertThat(order.getRgb(), CoreMatchers.is(Color.BLUE));

        List<ServeEvent> events = reportMock.getAllServeEvents();
        List<ServeEvent> actualDeliveryCalls = events.stream()
                .filter(se -> se.getRequest().getQueryParams().get("place").containsValue("actual_delivery"))
                .collect(Collectors.toList());

        assertThat(actualDeliveryCalls, hasSize(3));

        actualDeliveryCalls.forEach(se -> {
            assertThat(se.getRequest().getQueryParams().get("total-price").values(),
                    CoreMatchers.hasItem("5000"));
            assertThat(se.getRequest().getQueryParams().get("total-weight-kg").values(),
                    CoreMatchers.hasItem("20"));
        });
    }

    @Test
    public void shouldPassTotalWeightAndTotalPriceToReportDeliveryRoute() throws Exception {
        checkouterFeatureWriter.writeValue(ComplexFeatureType.COMBINATOR_FLOW, new SwitchWithWhitelist<>(true,
                singleton(Constants.COMBINATOR_EXPERIMENT)));
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withColor(Color.BLUE)
                .withCombinator(true)
                .buildParameters();
        parameters.setExperiments(Constants.COMBINATOR_EXPERIMENT);
        parameters.setMinifyOutlets(true);
        parameters.getItems()
                .forEach(oi -> oi.setCount(20));

        reportMock.resetAll();
        MultiCart multiCart = orderCreateHelper.cart(parameters);
        orderCreateHelper.checkout(multiCart, parameters);

        List<ServeEvent> events = reportMock.getAllServeEvents();
        List<ServeEvent> deliveryRouteCalls = events.stream()
                .filter(se -> se.getRequest().getQueryParams().get("place").containsValue("delivery_route"))
                .collect(Collectors.toList());

        deliveryRouteCalls.forEach(se -> {
            assertThat(se.getRequest().getQueryParams().get("total-price").values(),
                    CoreMatchers.hasItem("5000"));
            assertThat(se.getRequest().getQueryParams().get("total-weight-kg").values(),
                    CoreMatchers.hasItem("20"));
        });
    }

    @Test
    public void shouldCalculateTotalWeightAndTotalPriceForClickAndCollect() {
        Parameters parameters = BlueParametersProvider.clickAndCollectOrderParameters();
        parameters.setDeliveryType(DeliveryType.PICKUP);
        parameters.getOrder().setDelivery(DeliveryProvider.getEmptyDelivery());
        parameters.getItems()
                .forEach(oi -> {
                    oi.setWeight(null);
                    ItemInfo itemInfo = parameters.getReportParameters().overrideItemInfo(oi.getFeedOfferId());
                    itemInfo.setHideWeight(true);
                    itemInfo.setHideDimensions(true);
                    oi.setCount(20);
                });

        Order order = orderCreateHelper.createOrder(parameters);

        assertThat(order.getRgb(), CoreMatchers.is(Color.BLUE));
        assertThat(order.getDelivery().getDeliveryPartnerType(),
                CoreMatchers.is(DeliveryPartnerType.SHOP));

        List<ServeEvent> events = reportMock.getAllServeEvents();
        List<ServeEvent> actualDeliveryCalls = events.stream()
                .filter(se -> se.getRequest().getQueryParams().get("place").containsValue("actual_delivery"))
                .collect(Collectors.toList());

        assertThat(actualDeliveryCalls, hasSize(2));

        BigDecimal expectedTotalPrice = order.getItems()
                .stream()
                .map(oi -> oi.getBuyerPrice().multiply(BigDecimal.valueOf(oi.getCount())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal expectedTotalWeight = BigDecimal.ZERO;

        actualDeliveryCalls.forEach(se -> {
            LoggedRequest request = se.getRequest();
            assertThat(request.getQueryParams().get("total-price").values(),
                    CoreMatchers.hasItem(expectedTotalPrice.toString()));
            assertThat(request.getQueryParams().get("total-weight-kg").values(),
                    CoreMatchers.hasItem(expectedTotalWeight.toString()));
        });
    }

    @Test
    public void shouldActualizeCart() throws Exception {
        ActualItem request = ActualItemProvider.buildActualItem();

        ActualizeParameters actualizeParameters = new ActualizeParameters(request);
        actualizeParameters.getReportParameters().overrideItemInfo(request.getFeedOfferId()).setWeight(BigDecimal.TEN);

        ActualItem response = actualizeHelper.actualizeItem(actualizeParameters);
    }

    @Test
    public void shouldFailWithShopErrorWhenActualizationSuccessfulButNotAccept() throws Exception {
        Parameters parameters = new Parameters();
        parameters.setAcceptOrder(false);
        pushApiConfigurer.mockCart(parameters.getOrder(), parameters.getPushApiDeliveryResponses(), false);
        pushApiConfigurer.mockAccept(parameters.getOrder(), false);

        parameters.turnOffErrorChecks();

        MultiOrder multiOrder = orderCreateHelper.createMultiOrder(parameters);

        assertThat(multiOrder.getOrderFailures(), Matchers.hasSize(1));
        assertThat(multiOrder.getOrderFailures().get(0).getErrorCode(), equalTo(OrderFailure.Code.SHOP_ERROR));
    }

    @Test
    public void shouldFailWithShopErrorWhenActualizationSuccessfulButNotAcceptParallelReservation() throws Exception {
        Parameters parameters = new Parameters();
        parameters.setAcceptOrder(false);
        pushApiConfigurer.mockCart(parameters.getOrder(), parameters.getPushApiDeliveryResponses(), false);
        pushApiConfigurer.mockAccept(parameters.getOrder(), false);

        parameters.turnOffErrorChecks();

        MultiOrder multiOrder = orderCreateHelper.createMultiOrder(parameters);

        assertThat(multiOrder.getOrderFailures(), Matchers.hasSize(1));
        assertThat(multiOrder.getOrderFailures().get(0).getErrorCode(), equalTo(OrderFailure.Code.SHOP_ERROR));
    }
}
