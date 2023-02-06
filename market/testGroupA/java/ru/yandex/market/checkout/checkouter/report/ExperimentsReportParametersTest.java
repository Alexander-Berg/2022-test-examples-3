package ru.yandex.market.checkout.checkouter.report;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.client.CartParameters;
import ru.yandex.market.checkout.checkouter.client.CheckoutParameters;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.ItemService;
import ru.yandex.market.checkout.checkouter.order.MultiOrder;
import ru.yandex.market.checkout.checkouter.order.OrderPropertyType;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.common.report.model.MarketReportPlace;

import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static java.util.stream.Collectors.toSet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;

public class ExperimentsReportParametersTest extends AbstractWebTestBase {

    private static final EnumSet<MarketReportPlace> PLACES_WITH_EXPERIMENTS = EnumSet.of(
            MarketReportPlace.ACTUAL_DELIVERY,
            MarketReportPlace.OFFER_INFO,
            MarketReportPlace.OUTLETS
    );

    @Autowired
    private WireMockServer reportMock;

    @Test
    public void testChangeColorWithExperimentsOnCart() {
        Parameters parameters = BlueParametersProvider.defaultBlueNonFulfilmentOrderParameters();
        parameters.setExperiments(Experiments.BERU_USE_WHITE_REPORT + "=1");
        parameters.getReportParameters().setExperiments(Experiments.BERU_USE_WHITE_REPORT + "=1");
        orderCreateHelper.cart(reportConfigurerWhite, parameters);

        verifyReportColorCalls(reportMockWhite, ru.yandex.market.common.report.model.Color.BLUE, 4);
        verifyReportColorCalls(reportMockWhite, null, 1); // shop_info has no color in params

    }

    @Test
    public void shouldPassExperimentsToReportOnCart() {
        Parameters parameters = BlueParametersProvider.defaultBlueNonFulfilmentOrderParameters();
        parameters.setExperiments("showcase_universal=1;market_model_wizard_analogs_count=10");
        parameters.getReportParameters().setExperiments("showcase_universal=1;market_model_wizard_analogs_count=10");
        orderCreateHelper.cart(parameters);

        assertReportCalledWithRearrFlags();


        verifyReportColorCalls(reportMock, null, 1); // shop_info has no color in params
        verifyReportColorCalls(reportMock, ru.yandex.market.common.report.model.Color.BLUE, 4);
    }

    @Test
    public void shouldNotPassExperimentsToReportIfNoneSpecifiedOnCart() {
        Parameters parameters = BlueParametersProvider.defaultBlueNonFulfilmentOrderParameters();
        orderCreateHelper.cart(parameters);

        assertReportCalledWithoutRearrFlags();
    }

    @Test
    public void shouldPassExperimentsToReportOnClientCart() throws IOException {
        Parameters parameters = BlueParametersProvider.defaultBlueNonFulfilmentOrderParameters();
        parameters.getReportParameters().setExperiments("showcase_universal=1;market_model_wizard_analogs_count=10");
        orderCreateHelper.initializeMock(parameters);

        CartParameters cartParameters = CartParameters.builder()
                .withUid(parameters.getBuyer().getUid())
                .withRgb(Color.BLUE)
                .withExperiments("showcase_universal=1;market_model_wizard_analogs_count=10")
                .build();
        client.cart(parameters.getBuiltMultiCart(), cartParameters);

        assertReportCalledWithRearrFlags();
    }

    @Test
    public void shouldNotPassExperimentsToReportIfNoneSpecifiedOnClientCart() throws IOException {
        Parameters parameters = BlueParametersProvider.defaultBlueNonFulfilmentOrderParameters();
        orderCreateHelper.initializeMock(parameters);
        client.cart(parameters.getBuiltMultiCart(), parameters.getBuyer().getUid());

        assertReportCalledWithoutRearrFlags();
    }

    @Test
    public void testChangeColorWithExperimentsOnCheckout() throws Exception {
        Parameters parameters = BlueParametersProvider.defaultBlueNonFulfilmentOrderParameters();
        parameters.setExperiments(Experiments.BERU_USE_WHITE_REPORT + "=1");
        parameters.getReportParameters().setExperiments(Experiments.BERU_USE_WHITE_REPORT + "=1");

        MultiCart cart = orderCreateHelper.cart(reportConfigurerWhite, parameters);
        MultiOrder order = orderCreateHelper.mapCartToOrder(
                cart,
                parameters);
        reportMockWhite.resetRequests();
        orderCreateHelper.checkout(order, parameters);

        verifyReportColorCalls(reportMockWhite, ru.yandex.market.common.report.model.Color.BLUE, 4);
        verifyReportColorCalls(reportMockWhite, null, 1); // shop_info has no color in params

    }

    @Test
    public void shouldPassExperimentsToReportOnCheckout() throws Exception {
        Parameters parameters = BlueParametersProvider.defaultBlueNonFulfilmentOrderParameters();
        parameters.setExperiments("showcase_universal=1;market_model_wizard_analogs_count=10");
        parameters.getReportParameters().setExperiments("showcase_universal=1;market_model_wizard_analogs_count=10");

        MultiOrder order = orderCreateHelper.mapCartToOrder(orderCreateHelper.cart(parameters), parameters);
        reportMock.resetRequests();
        orderCreateHelper.checkout(order, parameters);

        assertReportCalledWithRearrFlags();

        verifyReportColorCalls(reportMock, null, 1); // shop_info has no color in params
        verifyReportColorCalls(reportMock, ru.yandex.market.common.report.model.Color.BLUE, 4);
    }

    @Test
    public void shouldSaveExperimentsInOrderPropertiesOnCheckout() throws Exception {
        final String experiments = "rearr-factors=fair-common-dimensions-algo=1";

        Parameters parameters = BlueParametersProvider.defaultBlueNonFulfilmentOrderParameters();
        parameters.setExperiments(experiments);
        parameters.getReportParameters().setExperiments(experiments);

        MultiOrder order = orderCreateHelper.mapCartToOrder(orderCreateHelper.cart(parameters), parameters);
        order = orderCreateHelper.checkout(order, parameters);

        Assertions.assertEquals(experiments,
                order.getCarts().iterator().next().getProperty(OrderPropertyType.EXPERIMENTS));
    }

    @Test
    public void shouldNotPassExperimentsToReportIfNoneSpecifiedOnCheckout() throws Exception {
        Parameters parameters = BlueParametersProvider.defaultBlueNonFulfilmentOrderParameters();

        MultiOrder order = orderCreateHelper.mapCartToOrder(orderCreateHelper.cart(parameters), parameters);
        reportMock.resetRequests();
        orderCreateHelper.checkout(order, parameters);

        assertReportCalledWithoutRearrFlags();
    }

    @Test
    public void shouldPassExperimentsToReportOnClientCheckout() throws IOException {
        Parameters parameters = BlueParametersProvider.defaultBlueNonFulfilmentOrderParameters();
        parameters.setExperiments("showcase_universal=1;market_model_wizard_analogs_count=10");
        parameters.getReportParameters().setExperiments("showcase_universal=1;market_model_wizard_analogs_count=10");
        MultiOrder order = orderCreateHelper.mapCartToOrder(orderCreateHelper.cart(parameters), parameters);
        orderCreateHelper.initializeMock(parameters);
        pushApiConfigurer.mockAccept(order.getCarts().get(0), true);
        reportMock.resetRequests();

        CheckoutParameters checkoutParameters = CheckoutParameters.builder()
                .withUid(parameters.getBuyer().getUid())
                .withRgb(Color.BLUE)
                .withExperiments("showcase_universal=1;market_model_wizard_analogs_count=10")
                .build();
        client.checkout(order, checkoutParameters);

        assertReportCalledWithRearrFlags();
    }

    @Test
    public void shouldNotPassExperimentsToReportIfNoneSpecifiedOnClientCheckout() throws IOException {
        Parameters parameters = BlueParametersProvider.defaultBlueNonFulfilmentOrderParameters();
        MultiOrder order = orderCreateHelper.mapCartToOrder(orderCreateHelper.cart(parameters), parameters);
        orderCreateHelper.initializeMock(parameters);
        reportMock.resetRequests();
        client.checkout(order, parameters.getBuyer().getUid());

        assertReportCalledWithoutRearrFlags();
    }

    @Test
    public void testAvailableServicesInCart() {
        Parameters parameters = BlueParametersProvider.defaultBlueNonFulfilmentOrderParameters();
        parameters.setExperiments(Experiments.BERU_USE_WHITE_REPORT + "=1");
        parameters.getReportParameters().setExperiments(Experiments.BERU_USE_WHITE_REPORT + "=1");

        parameters.getOrder().getItems().iterator().next().setServices(Collections.singleton(new ItemService() {{
            setServiceId(111L);
            setPrice(BigDecimal.ONE);
            setDate(new Date());
            setToTime(LocalTime.now());
            setTitle("Установка ОС");
        }}));

        MultiCart cart = orderCreateHelper.cart(reportConfigurerWhite, parameters);

        Assertions.assertFalse(cart.getCarts().get(0).getItems().iterator().next().getAvailableServices().isEmpty());
    }

    private void assertReportCalledWithoutRearrFlags() {
        List<LoggedRequest> requestList = getRequestsWithRearFlags();
        assertThat(requestList, empty());
    }

    private void assertReportCalledWithRearrFlags() {
        List<LoggedRequest> requestList = getRequestsWithRearFlags();
        assertThat(requestList, not(empty()));
        assertThat(
                PLACES_WITH_EXPERIMENTS.stream().map(MarketReportPlace::getId).collect(toSet()),
                containsInAnyOrder(
                        requestList.stream()
                                .map(r -> r.getQueryParams().get("place").firstValue())
                                .distinct()
                                .toArray(String[]::new)
                )
        );
    }

    private List<LoggedRequest> getRequestsWithRearFlags() {
        return reportMock.findAll(
                getRequestedFor(anyUrl())
                        .withQueryParam(
                                "rearr-factors",
                                equalTo("showcase_universal=1;market_model_wizard_analogs_count=10")
                        )
        );
    }
}
