package ru.yandex.market.checkout.checkouter.order.getOrder;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.Iterables;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.delivery.tracking.Track;
import ru.yandex.market.checkout.checkouter.delivery.tracking.TrackId;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.helpers.OrderDeliveryHelper;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.test.providers.TrackProvider;

import static org.hamcrest.MatcherAssert.assertThat;

public class GetOrdersTrackSortOrderTest extends AbstractWebTestBase {

    private static final String ANOTHER_TRACK_CODE = "defdef";

    @Autowired
    private OrderPayHelper orderPayHelper;
    @Autowired
    private OrderDeliveryHelper orderDeliveryHelper;

    @Test
    public void shouldSortTrackByCreationDateAndId() throws Exception {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.setPaymentMethod(PaymentMethod.YANDEX);

        Order order = orderCreateHelper.createOrder(parameters);
        orderPayHelper.payForOrder(order);

        Track track = TrackProvider.createTrack();
        Track anotherTrack = TrackProvider.createTrack(ANOTHER_TRACK_CODE, TrackProvider.DELIVERY_SERVICE_ID);

        Parcel orderShipment = new Parcel();
        orderShipment.setTracks(Arrays.asList(track, anotherTrack));

        Delivery delivery = new Delivery();
        delivery.setParcels(Collections.singletonList(orderShipment));

        Order updated = orderDeliveryHelper.updateOrderDelivery(order.getId(), ClientInfo.SYSTEM, delivery);

        orderUpdateService.updateTrackSetTrackerId(order.getId(), new TrackId(TrackProvider.TRACK_CODE,
                TrackProvider.DELIVERY_SERVICE_ID), 123L);

        Order orderFromGet = orderService.getOrder(order.getId());

        List<String> trackCodes = Iterables.getOnlyElement(orderFromGet.getDelivery().getParcels()).getTracks().stream()
                .map(Track::getTrackCode)
                .collect(Collectors.toList());

        assertThat(trackCodes, Matchers.contains(TrackProvider.TRACK_CODE, ANOTHER_TRACK_CODE));
    }
}
