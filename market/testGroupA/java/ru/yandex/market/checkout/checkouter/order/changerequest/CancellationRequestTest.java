package ru.yandex.market.checkout.checkouter.order.changerequest;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderEditRequest;
import ru.yandex.market.checkout.checkouter.order.changerequest.parcel.ParcelCancelChangeRequestPayload;
import ru.yandex.market.checkout.helpers.YandexMarketDeliveryHelper;
import ru.yandex.market.checkout.test.providers.BuyerProvider;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static ru.yandex.market.checkout.checkouter.order.Color.BLUE;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.PROCESSING;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.USER_CHANGED_MIND;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.INTAKE_AVAILABLE_DATE;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_DELIVERY_SERVICE_ID;
import static ru.yandex.market.checkout.test.providers.OrderProvider.SHOP_ID_WITH_SORTING_CENTER;

public class CancellationRequestTest extends AbstractWebTestBase {

    private static final String NOTES = "notes";

    @Autowired
    private YandexMarketDeliveryHelper yandexMarketDeliveryHelper;

    @Test
    public void shouldCreateCancellationRequest() {
        setFixedTime(INTAKE_AVAILABLE_DATE);
        Order order = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .withColor(BLUE)
                .build();
        order = orderStatusHelper.proceedOrderToStatus(order, PROCESSING);

        OrderEditRequest orderEditRequest = new OrderEditRequest();
        orderEditRequest.setCancellationRequest(new CancellationRequest(USER_CHANGED_MIND, NOTES));

        List<ChangeRequest> changeRequests = client.editOrder(order.getId(), ClientRole.USER, BuyerProvider.UID,
                singletonList(BLUE), orderEditRequest);

        Order orderFromDB = client.getOrder(order.getId(), ClientRole.SYSTEM, 1L);

        assertEquals(USER_CHANGED_MIND, orderFromDB.getCancellationRequest().getSubstatus());
        assertEquals(NOTES, orderFromDB.getCancellationRequest().getNotes());

        assertNotNull(changeRequests);

        assertThat(changeRequests, hasSize(2));

        ChangeRequest cancellationRequest = changeRequests.stream()
                .filter(changeRequest -> changeRequest.getType().equals(ChangeRequestType.CANCELLATION))
                .findFirst().orElseThrow();

        assertEquals(ChangeRequestStatus.NEW, cancellationRequest.getStatus());
        assertEquals(ChangeRequestType.CANCELLATION, cancellationRequest.getType());

        CancellationRequestPayload cancellationRequestPayload =
                (CancellationRequestPayload) cancellationRequest.getPayload();

        assertEquals(USER_CHANGED_MIND, cancellationRequestPayload.getSubstatus());
        assertEquals(NOTES, cancellationRequestPayload.getNotes());

        ChangeRequest cancellationParcelRequest = changeRequests.stream()
                .filter(changeRequest -> changeRequest.getType().equals(ChangeRequestType.PARCEL_CANCELLATION))
                .findFirst().orElseThrow();

        assertEquals(ChangeRequestStatus.NEW, cancellationParcelRequest.getStatus());
        assertEquals(ChangeRequestType.PARCEL_CANCELLATION, cancellationParcelRequest.getType());

        ParcelCancelChangeRequestPayload cancellationParcelRequestPayload =
                (ParcelCancelChangeRequestPayload) cancellationParcelRequest.getPayload();

        assertEquals(
                order.getDelivery().getParcels().iterator().next().getId(),
                cancellationParcelRequestPayload.getParcelId()
        );
    }
}
