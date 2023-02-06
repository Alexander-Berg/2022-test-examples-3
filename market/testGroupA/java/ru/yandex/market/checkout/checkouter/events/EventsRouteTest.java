package ru.yandex.market.checkout.checkouter.events;

import java.util.Collection;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.feature.type.common.ComplexFeatureType;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.common.util.SwitchWithWhitelist;
import ru.yandex.market.checkout.helpers.YandexMarketDeliveryHelper;
import ru.yandex.market.checkout.helpers.utils.EventsTestUtils;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.test.providers.DeliveryResultProvider;

import static java.util.Collections.singleton;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_DELIVERY_SERVICE_ID;
import static ru.yandex.market.checkout.util.Constants.COMBINATOR_EXPERIMENT;

public class EventsRouteTest extends AbstractEventsControllerTestBase {

    @Autowired
    private YandexMarketDeliveryHelper yandexMarketDeliveryHelper;

    public static Stream<Arguments> parameterizedTestData() {
        return EventsTestUtils.parameters(Color.BLUE).stream().map(Arguments::of);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("parameterizedTestData")
    public void test(String caseName, EventsTestUtils.EventGetter eventGetter) throws Exception {
        checkouterFeatureWriter.writeValue(ComplexFeatureType.COMBINATOR_FLOW, new SwitchWithWhitelist(true,
                singleton(COMBINATOR_EXPERIMENT)));
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withColor(Color.BLUE)
                .withCombinator(true)
                .buildParameters();
        parameters.setExperiments(COMBINATOR_EXPERIMENT);
        parameters.setMinifyOutlets(true);

        Order order = orderCreateHelper.createOrder(parameters);
        Collection<OrderHistoryEvent> events = eventGetter.getEvents(order.getId(), mockMvc, serializationService);
        events.forEach(e -> JSONAssert.assertEquals(
                DeliveryResultProvider.ROUTE,
                e.getOrderAfter().getDelivery().getParcels().get(0).getRoute().toString(),
                JSONCompareMode.NON_EXTENSIBLE
        ));
    }
}
