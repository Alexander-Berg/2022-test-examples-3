package ru.yandex.market.delivery.mdbapp.integration.transformer;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryServiceType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.delivery.tracking.Track;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.delivery.mdbapp.components.queue.parcel.OrderParcelDto;
import ru.yandex.market.delivery.mdbapp.integration.payload.CancelLgwDsParcel;
import ru.yandex.market.logistic.gateway.common.model.common.Partner;
import ru.yandex.market.logistic.gateway.common.model.delivery.ResourceId;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static ru.yandex.market.checkout.checkouter.order.Color.BLUE;
import static ru.yandex.market.checkout.checkouter.order.Color.RED;

/**
 * Test for {@link LgwDsCancelParcelTransformer}.
 */
@RunWith(Parameterized.class)
public class LgwDsCancelParcelTransformerTest {

    private static final Long ORDER_ID = 1L;
    private static final Long PARCEL_ID = 100L;
    private static final Long PARTNER_ID = 123L;
    private static final String TRACK_CODE = "TRACK";

    private final LgwDsCancelParcelTransformer lgwDsCancelParcelTransformer = new LgwDsCancelParcelTransformer();

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Parameter
    public OrderParcelDto orderParcelDto;

    @Parameter(1)
    public CancelLgwDsParcel cancelLgwDsParcel;

    @Parameters()
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{

            {buildOrderParcelDto(true, false, BLUE), buildCancelDto(true)},
            {buildOrderParcelDto(true, false, RED), buildCancelDto(true)},

            {buildOrderParcelDto(false, false, BLUE), buildCancelDto(true)},
            {buildOrderParcelDto(false, false, RED), buildCancelDto(false)},

            {buildOrderParcelDto(true, true, BLUE), buildCancelDto(true)},
            {buildOrderParcelDto(false, true, BLUE), buildCancelDto(true)},
        });
    }

    @Test
    public void testTransform() {
        assertThat(lgwDsCancelParcelTransformer.transform(orderParcelDto))
            .as("Incorrect transformed")
            .isEqualTo(cancelLgwDsParcel);
    }

    private static OrderParcelDto buildOrderParcelDto(boolean isFulfillment, boolean isPost, Color color) {
        Order order = new Order();
        order.setId(ORDER_ID);
        order.setFulfilment(isFulfillment);
        order.setRgb(color);

        OrderItem item = new OrderItem();
        item.setWarehouseId(Math.toIntExact(PARTNER_ID));
        item.setFulfilmentWarehouseId(PARTNER_ID);
        order.setItems(Collections.singleton(item));

        Track track = new Track();
        track.setDeliveryServiceId(PARTNER_ID);
        track.setTrackCode(TRACK_CODE);

        Parcel parcel = new Parcel();
        parcel.setId(PARCEL_ID);
        parcel.setTracks(Collections.singletonList(track));

        Delivery delivery = new Delivery();

        if (isPost) {
            delivery.setType(DeliveryType.POST);
            track.setDeliveryServiceType(DeliveryServiceType.FULFILLMENT);
        } else {
            delivery.setType(DeliveryType.DELIVERY);
            track.setDeliveryServiceType(DeliveryServiceType.CARRIER);
        }

        delivery.setDeliveryServiceId(PARTNER_ID);
        delivery.setParcels(Collections.singletonList(parcel));

        order.setDelivery(delivery);

        return new OrderParcelDto(order, parcel);
    }

    private static CancelLgwDsParcel buildCancelDto(boolean cancelByTrack) {
        return new CancelLgwDsParcel(
            ResourceId.builder()
                .setYandexId(String.valueOf(ORDER_ID))
                .setPartnerId(TRACK_CODE)
                .build(),
            ResourceId.builder()
                .setYandexId(String.valueOf(PARCEL_ID))
                .setPartnerId(TRACK_CODE)
                .build(),
            new Partner(PARTNER_ID),
            cancelByTrack
        );
    }
}
