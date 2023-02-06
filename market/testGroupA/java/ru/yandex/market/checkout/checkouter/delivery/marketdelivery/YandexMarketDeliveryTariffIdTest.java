package ru.yandex.market.checkout.checkouter.delivery.marketdelivery;

import com.google.common.collect.Iterables;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.event.PagedEvents;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.helpers.EventsGetHelper;
import ru.yandex.market.checkout.helpers.YandexMarketDeliveryHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.test.providers.ActualDeliveryProvider;
import ru.yandex.market.common.report.model.ActualDelivery;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static ru.yandex.market.checkout.checkouter.order.Color.BLUE;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_DELIVERY_SERVICE_ID;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.RUSPOST_DELIVERY_SERVICE_ID;

public class YandexMarketDeliveryTariffIdTest extends AbstractWebTestBase {

    private static final long TARIFF_ID = 2234562L;

    @Autowired
    private YandexMarketDeliveryHelper yandexMarketDeliveryHelper;
    @Autowired
    private EventsGetHelper eventsGetHelper;

    @ParameterizedTest
    @EnumSource(value = DeliveryType.class, names = {"DELIVERY", "PICKUP", "POST"})
    public void shouldSaveTariffIdIfPresent(DeliveryType deliveryType) throws Exception {
        ActualDeliveryProvider.ActualDeliveryBuilder builder = ActualDeliveryProvider.builder();
        long deliveryServiceId;
        switch (deliveryType) {
            case DELIVERY:
                deliveryServiceId = MOCK_DELIVERY_SERVICE_ID;
                builder.addDelivery(deliveryServiceId, 1);
                break;
            case POST:
                deliveryServiceId = RUSPOST_DELIVERY_SERVICE_ID;
                builder.addPost(1, deliveryServiceId);
                break;
            case PICKUP:
                deliveryServiceId = MOCK_DELIVERY_SERVICE_ID;
                builder.addPickup(deliveryServiceId, 1);
                break;
            default:
                throw new IllegalStateException("Unsupported deliveryType: " + deliveryType);
        }

        ActualDelivery actualDelivery = builder
                .withTariffId(TARIFF_ID)
                .build();

        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryType(deliveryType)
                .withColor(BLUE)
                .withDeliveryServiceId(deliveryServiceId)
                .withShipmentDay(1)
                .withActualDelivery(actualDelivery)
                .buildParameters();

        long tariffId = TARIFF_ID;

        Order order = orderCreateHelper.createOrder(parameters);

        assertThat(order.getDelivery().getTariffId(), is(tariffId));

        PagedEvents events = eventsGetHelper.getOrderHistoryEvents(order.getId());
        OrderHistoryEvent event = Iterables.getFirst(events.getItems(), null);

        assertThat(event, notNullValue());
        assertThat(event.getOrderAfter().getDelivery().getTariffId(), is(tariffId));
        assertThat(event.getOrderBefore().getDelivery().getTariffId(), is(tariffId));
    }
}
