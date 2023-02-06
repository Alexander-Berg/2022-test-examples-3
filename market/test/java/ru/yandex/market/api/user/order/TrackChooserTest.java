package ru.yandex.market.api.user.order;

import org.junit.Test;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryServiceType;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.delivery.tracking.Track;
import ru.yandex.market.checkout.checkouter.order.Order;

import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.market.api.ApiMatchers.*;
import static ru.yandex.market.api.user.order.TrackChooser.choose;

public class TrackChooserTest extends UnitTestBase {
    @Test
    public void emptyIfOrderWithoutDelivery() {
        Order order = new Order();
        assertThat(
            choose(order),
            emptyOptional()
        );
    }

    @Test
    public void emptyIfOrderWithoutShipments() {
        Order order = new Order();
        order.setDelivery(new Delivery());
        assertThat(
            choose(order),
            emptyOptional()
        );
    }

    @Test
    public void emptyIfOrderWithoutTrack() {
        Order order = new Order();

        Delivery delivery = new Delivery();
        delivery.setParcels(Collections.singletonList(new Parcel()));

        order.setDelivery(delivery);

        assertThat(
            choose(order),
            emptyOptional()
        );
    }

    @Test
    public void chooseTrackForDeliveryService() {
        //TODO пока выбирается первый, но надо выбирать тот, что будет проставлен флагом СД:
        // https://st.yandex-team.ru/MARKETCHECKOUT-5292
        Order order = new Order();
        Delivery delivery = new Delivery();
        Parcel orderShipment = new Parcel();

        Track t1 = new Track();
        t1.setId(100L);
        t1.setDeliveryServiceType(DeliveryServiceType.SORTING_CENTER);

        Track t2 = new Track();
        t2.setDeliveryServiceType(DeliveryServiceType.CARRIER);
        t2.setId(200L);

        orderShipment.setTracks(Arrays.asList(t1, t2));

        delivery.setParcels(Collections.singletonList(orderShipment));
        order.setDelivery(delivery);

        assertThat(
            choose(order),
            optionalHasValue(
                map(Track::getId, "'id'", is(200L))
            )
        );
    }
}
