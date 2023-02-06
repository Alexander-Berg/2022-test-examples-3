package ru.yandex.market.checkout.checkouter.checkout;

import java.util.List;
import java.util.stream.Collectors;

import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.MarketReportSearchService;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;

import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

public class CheckoutShowMultiServiceIntervalsTest extends AbstractWebTestBase {

    @Test
    void shouldPassParameterToReportPlaceActualDelivery() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.configuration().cart().request().setShowMultiServiceIntervals(Boolean.TRUE);

        Order order = orderCreateHelper.createOrder(parameters);

        MatcherAssert.assertThat(order.getRgb(), CoreMatchers.is(Color.BLUE));

        List<ServeEvent> events = reportMock.getAllServeEvents();
        List<ServeEvent> actualDeliveryCalls = events.stream()
                .filter(se -> se.getRequest().getQueryParams().get("place")
                        .containsValue("actual_delivery"))
                .collect(Collectors.toList());

        MatcherAssert.assertThat(actualDeliveryCalls, hasSize(3));

        actualDeliveryCalls.forEach(se -> {
            MatcherAssert.assertThat(se.getRequest().getQueryParams().get(
                    MarketReportSearchService.SHOW_MULTI_SERVICE_INTERVALS_PARAM).isPresent(),
                    CoreMatchers.is(true));
            MatcherAssert.assertThat(se.getRequest().getQueryParams().get(
                    MarketReportSearchService.SHOW_MULTI_SERVICE_INTERVALS_PARAM).values(),
                    CoreMatchers.hasItem("1"));
        });
    }
}
