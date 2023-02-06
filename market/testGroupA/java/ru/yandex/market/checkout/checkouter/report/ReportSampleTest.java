package ru.yandex.market.checkout.checkouter.report;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.admin.model.GetServeEventsResult;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryFeature;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType;
import ru.yandex.market.checkout.checkouter.feature.type.common.ComplexFeatureType;
import ru.yandex.market.checkout.checkouter.order.ActualDeliveryRequestBuilder;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.DeliveryRouteRequestBuilder;
import ru.yandex.market.checkout.checkouter.order.MarketReportSearchService;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.ReportSearchParameters;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.service.personal.PersonalDataService;
import ru.yandex.market.checkout.common.util.SwitchWithWhitelist;
import ru.yandex.market.checkout.helpers.YandexMarketDeliveryHelper;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;
import ru.yandex.market.checkout.util.Constants;
import ru.yandex.market.common.report.model.MarketReportPlace;
import ru.yandex.market.common.report.model.ReportException;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static java.util.Collections.singleton;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_DELIVERY_SERVICE_ID;

public class ReportSampleTest extends AbstractWebTestBase {

    @Autowired
    private WireMockServer reportMock;
    @Autowired
    private MarketReportSearchService marketReportSearchService;
    @Autowired
    private PersonalDataService personalDataService;
    @Autowired
    private YandexMarketDeliveryHelper yandexMarketDeliveryHelper;

    @Test
    public void shouldFailWithReportException() {
        reportMock.stubFor(
                get(urlPathEqualTo("/yandsearch"))
                        .withQueryParam("place", equalTo("mainreport"))
                        .willReturn(new ResponseDefinitionBuilder().withStatus(503))
        );

        OrderItem orderItem = new OrderItem();
        orderItem.setFeedId(383182L);
        orderItem.setOfferId("1");

        Order order = new Order();
        order.setRgb(Color.BLUE);
        order.addItem(orderItem);

        RuntimeException runtimeException = Assertions.assertThrows(RuntimeException.class, () -> {
            marketReportSearchService.searchItems(ReportSearchParameters.builder(order).build(), order.getItemKeys());
        });
        assertTrue(runtimeException.getCause() instanceof ReportException);
    }

    @Test
    public void testYandexPlusPerkIsSentToActualDelivery() {
        checkouterProperties.setSendYandexPlusPerkToReport(true);
        reportMock.stubFor(
                get(urlPathEqualTo("/yandsearch"))
                        .withQueryParam("place", equalTo("actual_delivery"))
                        .willReturn(new ResponseDefinitionBuilder().withStatus(500))
        );
        reportMock.stubFor(
                get(urlPathEqualTo("/yandsearch"))
                        .withQueryParam("place", equalTo("actual_delivery"))
                        .withQueryParam("perks", equalTo("yandex_plus"))
                        .willReturn(ResponseDefinitionBuilder.okForEmptyJson())
        );
        var order = new Order();
        order.setRgb(Color.BLUE);
        order.setDelivery(DeliveryProvider.getShopDelivery());
        var actualDelivery =
                marketReportSearchService.searchActualDelivery(new ActualDeliveryRequestBuilder()
                        .withOrder(order)
                        .withYandexPlus(true));
        // в ответе не 500, значит перк отправился
        MatcherAssert.assertThat(actualDelivery, is(notNullValue()));
    }

    @Test
    public void testYandexPlusPerkIsSentToDeliveryRouteWithNonStreamingParser() {
        checkouterProperties.setSendYandexPlusPerkToReport(true);
        //удалить тест вместе с рубильником
        checkouterProperties.setEnableDeliveryRouteStreamingJsonParser(false);
        reportMock.stubFor(
                get(urlPathEqualTo("/yandsearch"))
                        .withQueryParam("place", equalTo("delivery_route"))
                        .willReturn(new ResponseDefinitionBuilder().withStatus(500))
        );
        reportMock.stubFor(
                get(urlPathEqualTo("/yandsearch"))
                        .withQueryParam("place", equalTo("delivery_route"))
                        .withQueryParam("perks", equalTo("yandex_plus"))
                        .willReturn(ResponseDefinitionBuilder.okForEmptyJson())
        );
        var order = new Order();
        order.setRgb(Color.BLUE);
        order.setDelivery(DeliveryProvider.getShopDelivery());
        var deliveryRoute =
                marketReportSearchService.searchDeliveryRoute(new DeliveryRouteRequestBuilder()
                        .withColor(order.getRgb())
                        .withOrderDelivery(order.getDelivery(), personalDataService)
                        .withPaymentMethod(PaymentMethod.YANDEX)
                        .withYandexPlus(true));
        // в ответе не 500, значит перк отправился
        MatcherAssert.assertThat(deliveryRoute, is(notNullValue()));
    }

    @Test
    public void testYandexPlusPerkIsSentToDeliveryRoute() {
        checkouterProperties.setSendYandexPlusPerkToReport(true);
        checkouterProperties.setEnableDeliveryRouteStreamingJsonParser(true);
        reportMock.stubFor(
                get(urlPathEqualTo("/yandsearch"))
                        .withQueryParam("place", equalTo("delivery_route"))
                        .willReturn(new ResponseDefinitionBuilder().withStatus(500))
        );
        reportMock.stubFor(
                get(urlPathEqualTo("/yandsearch"))
                        .withQueryParam("place", equalTo("delivery_route"))
                        .withQueryParam("perks", equalTo("yandex_plus"))
                        .willReturn(ResponseDefinitionBuilder.okForEmptyJson())
        );
        var order = new Order();
        order.setRgb(Color.BLUE);
        order.setDelivery(DeliveryProvider.getShopDelivery());
        var deliveryRoute =
                marketReportSearchService.searchDeliveryRoute(new DeliveryRouteRequestBuilder()
                        .withColor(order.getRgb())
                        .withOrderDelivery(order.getDelivery(), personalDataService)
                        .withPaymentMethod(PaymentMethod.YANDEX)
                        .withYandexPlus(true));
        // в ответе не 500, значит перк отправился
        MatcherAssert.assertThat(deliveryRoute, is(notNullValue()));
    }

    @Test
    public void testDeferredCourierIsSentToDeliveryRoute() {
        checkouterProperties.setSendYandexPlusPerkToReport(true);
        checkouterProperties.setEnableDeliveryRouteStreamingJsonParser(true);
        reportMock.stubFor(
                get(urlPathEqualTo("/yandsearch"))
                        .withQueryParam("place", equalTo("delivery_route"))
                        .willReturn(new ResponseDefinitionBuilder().withStatus(500))
        );
        reportMock.stubFor(
                get(urlPathEqualTo("/yandsearch"))
                        .withQueryParam("place", equalTo("delivery_route"))
                        .withQueryParam("delivery-subtype", equalTo("deferred-courier"))
                        .willReturn(ResponseDefinitionBuilder.okForEmptyJson())
        );
        var order = new Order();
        order.setRgb(Color.BLUE);
        order.setDelivery(DeliveryProvider.getShopDelivery());
        order.getDelivery().setFeatures(Set.of(DeliveryFeature.DEFERRED_COURIER));
        var deliveryRoute =
                marketReportSearchService.searchDeliveryRoute(new DeliveryRouteRequestBuilder()
                        .withColor(order.getRgb())
                        .withOrderDelivery(order.getDelivery(), personalDataService)
                        .withPaymentMethod(PaymentMethod.YANDEX));
        // в ответе не 500, значит deferred-courier отправился
        MatcherAssert.assertThat(deliveryRoute, is(notNullValue()));
    }

    @Test
    public void shouldSendPerks() {
        checkouterFeatureWriter.writeValue(ComplexFeatureType.COMBINATOR_FLOW, new SwitchWithWhitelist(true,
                singleton(Constants.COMBINATOR_EXPERIMENT)));
        checkouterFeatureWriter.writeValue(BooleanFeatureType.ENABLE_SEND_ALL_PERKS_TO_REPORT, true);
        var parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.PICKUP)
                .withColor(Color.BLUE)
                .withCombinator(true)
                .buildParameters();
        parameters.setExperiments(Constants.COMBINATOR_EXPERIMENT);
        parameters.setMinifyOutlets(true);
        parameters.setForceDeliveryId(111L);
        var perks = "payment_system_extra_cashback," +
                "yandex_extra_fashion_cashback,referral_program,yandex_employee_extra_cashback," +
                "yandex_extra_pharma_cashback,yandex_extra_cashback,yandex_employee,yandex_plus,yandex_cashback," +
                "last_payment_not_mir,not_delivery_onboarding_users";
        parameters.configuration().cart().request().setPerks(perks);
        orderCreateHelper.createOrder(parameters);
        var reportEvents = reportMock.getServeEvents();
        var deliveryRouteRequests = getReportRequestsWithPlace(reportEvents, MarketReportPlace.DELIVERY_ROUTE);
        var actualDeliveryRequests = getReportRequestsWithPlace(reportEvents, MarketReportPlace.ACTUAL_DELIVERY);
        var offerInfoRequests = getReportRequestsWithPlace(reportEvents, MarketReportPlace.OFFER_INFO);
        assertThat(deliveryRouteRequests, hasSize(greaterThanOrEqualTo(1)));
        assertThat(actualDeliveryRequests, hasSize(greaterThanOrEqualTo(1)));
        assertThat(offerInfoRequests, hasSize(greaterThanOrEqualTo(1)));
        checkPerks(perks, actualDeliveryRequests);
        checkPerks(perks, deliveryRouteRequests);
        checkPerks(perks, offerInfoRequests);
    }

    private void checkPerks(String perks, List<LoggedRequest> actualDeliveryRequests) {
        for (var actualDeliveryRequest : actualDeliveryRequests) {
            assertTrue(actualDeliveryRequest.queryParameter("perks").containsValue(perks));
        }
    }

    private List<LoggedRequest> getReportRequestsWithPlace(GetServeEventsResult reportEvents,
                                                           MarketReportPlace deliveryRoute) {
        return reportEvents.getServeEvents().stream()
                .map(ServeEvent::getRequest)
                .filter(request -> request.queryParameter("place").containsValue(deliveryRoute.getId()))
                .collect(Collectors.toList());
    }
}
