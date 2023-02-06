package ru.yandex.market.checkout.checkouter.events;


import java.util.Collection;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.delivery.Address;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.YandexMarketDeliveryHelper;
import ru.yandex.market.checkout.helpers.utils.EventsTestUtils;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.test.providers.AddressProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.INTAKE_AVAILABLE_DATE;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_DELIVERY_SERVICE_ID;
import static ru.yandex.market.checkout.test.providers.OrderProvider.SHOP_ID_WITH_SORTING_CENTER;

public class EventsControllerDeliveryAddressTest extends AbstractEventsControllerTestBase {

    @Autowired
    private YandexMarketDeliveryHelper yandexMarketDeliveryHelper;
    @Autowired
    private OrderPayHelper payHelper;

    @BeforeEach
    public void before() {
        personalMockConfigurer.mockV1MultiTypesRetrieveAddressAndEmptyGps();
    }

    public static Stream<Arguments> parameterizedTestData() {
        return EventsTestUtils.parameters(Color.BLUE).stream().map(Arguments::of);
    }


    @ParameterizedTest(name = "{0}")
    @MethodSource("parameterizedTestData")
    public void eventsHaveDeliveryShopAddress(String caseName, EventsTestUtils.EventGetter eventGetter)
            throws Exception {
        setFixedTime(INTAKE_AVAILABLE_DATE);
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withColor(Color.BLUE)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .buildParameters();
        parameters.getOrder().getDelivery().setBuyerAddress(AddressProvider.getAddress());

        Order order = orderCreateHelper.createOrder(parameters);
        payHelper.payForOrder(order);

        Collection<OrderHistoryEvent> events = eventGetter.getEvents(order.getId(), mockMvc, serializationService);
        assertThat(events)
                .extracting(OrderHistoryEvent::getOrderAfter)
                .extracting(Order::getDelivery)
                .extracting(Delivery::getShopAddress)
                .extracting(Address::getPostcode)
                .containsOnly(AddressProvider.POSTCODE);

        assertThat(events)
                .extracting(OrderHistoryEvent::getOrderAfter)
                .extracting(Order::getDelivery)
                .extracting(Delivery::getShopAddress)
                .extracting(Address::getGps)
                .doesNotContainNull();
    }
}
