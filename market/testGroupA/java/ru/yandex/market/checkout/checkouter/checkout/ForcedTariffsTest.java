package ru.yandex.market.checkout.checkouter.checkout;

import java.util.List;
import java.util.stream.Collectors;

import com.github.tomakehurst.wiremock.http.HttpHeader;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.client.CheckoutCommonParams;
import ru.yandex.market.checkout.checkouter.feature.type.common.ComplexFeatureType;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.util.CheckouterPropertiesImpl;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.WhiteParametersProvider;
import ru.yandex.market.common.report.model.MarketReportPlace;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.checkouter.order.MarketReportSearchService.REPORT_EXPERIMENTS_PARAM;
import static ru.yandex.market.checkout.checkouter.report.Experiments.MARKET_DSBS_TARIFFS;
import static ru.yandex.market.checkout.checkouter.report.Experiments.MARKET_DSBS_TARIFFS_VALUE;
import static ru.yandex.market.checkout.checkouter.report.Experiments.MARKET_UNIFIED_TARIFFS;
import static ru.yandex.market.checkout.checkouter.report.Experiments.MARKET_UNIFIED_TARIFFS_VALUE;

public class ForcedTariffsTest extends AbstractWebTestBase {

    @Test
    public void forcedTariffsExperimentToActualDelivery() {
        checkouterFeatureWriter.writeValue(ComplexFeatureType.TARIFFS_AND_LIFT_EXPERIMENT_TOGGLE,
                CheckouterPropertiesImpl.TariffsAndLiftExperimentToggle.FORCE);
        Parameters parameters = WhiteParametersProvider.defaultWhiteParameters();

        Order order = orderCreateHelper.createOrder(parameters);

        List<ServeEvent> actualDeliveryEvents = reportMock.getServeEvents().getServeEvents()
                .stream()
                .filter(se -> se.getRequest()
                        .queryParameter("place")
                        .containsValue(MarketReportPlace.ACTUAL_DELIVERY.getId()))
                .collect(Collectors.toList());

        LoggedRequest actualRequest = actualDeliveryEvents.get(0).getRequest();
        assertTrue(actualRequest.queryParameter(REPORT_EXPERIMENTS_PARAM).values().size() > 0);

        assertTrue(actualRequest.queryParameter(REPORT_EXPERIMENTS_PARAM).values().stream()
                .allMatch(it -> it.contains(MARKET_DSBS_TARIFFS + "=" + MARKET_DSBS_TARIFFS_VALUE)));
        assertTrue(actualRequest.queryParameter(REPORT_EXPERIMENTS_PARAM).values().stream()
                .allMatch(it -> it.contains(MARKET_UNIFIED_TARIFFS + "=" + MARKET_UNIFIED_TARIFFS_VALUE)));
    }

    @Test
    public void forcedTariffsExperimentToLoyalty() throws Exception {
        checkouterFeatureWriter.writeValue(ComplexFeatureType.TARIFFS_AND_LIFT_EXPERIMENT_TOGGLE,
                CheckouterPropertiesImpl.TariffsAndLiftExperimentToggle.FORCE);
        final int requestNumber = 5;

        Parameters parameters = WhiteParametersProvider.defaultWhiteParameters();
        parameters.setMockLoyalty(true);
        MultiCart multiCart = orderCreateHelper.cart(parameters);
        orderCreateHelper.checkout(multiCart, parameters);

        assertThat(loyaltyConfigurer.servedEvents(), Matchers.hasSize(requestNumber));
        List<ServeEvent> events = loyaltyConfigurer.servedEvents();
        List<HttpHeader> experiments =
                events.stream()
                        .map(event -> event.getRequest().getHeaders().getHeader(CheckoutCommonParams.X_EXPERIMENTS))
                        .collect(Collectors.toList());
        assertThat(experiments, Matchers.hasSize(requestNumber));
        assertTrue(experiments.stream().flatMap(it -> it.values().stream())
                .allMatch(it ->
                        it.contains(MARKET_UNIFIED_TARIFFS + "=" + MARKET_UNIFIED_TARIFFS_VALUE)
                                && it.contains(MARKET_DSBS_TARIFFS + "=" + MARKET_DSBS_TARIFFS_VALUE))
        );
    }

}
