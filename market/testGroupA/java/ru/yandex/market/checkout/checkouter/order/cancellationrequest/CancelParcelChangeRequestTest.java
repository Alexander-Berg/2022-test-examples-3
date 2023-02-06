package ru.yandex.market.checkout.checkouter.order.cancellationrequest;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.order.CancellationRequest;
import ru.yandex.market.checkout.checkouter.order.OptionalOrderPart;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequest;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestPatchRequest;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestStatus;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestType;
import ru.yandex.market.checkout.checkouter.order.changerequest.parcel.ParcelCancelChangeRequestPayload;
import ru.yandex.market.checkout.helpers.CancellationRequestHelper;
import ru.yandex.market.checkout.helpers.YandexMarketDeliveryHelper;
import ru.yandex.market.checkout.test.providers.BuyerProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.checkouter.order.Color.BLUE;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.PROCESSING;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.USER_CHANGED_MIND;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_DELIVERY_SERVICE_ID;
import static ru.yandex.market.checkout.test.providers.OrderProvider.SHOP_ID_WITH_SORTING_CENTER;

public class CancelParcelChangeRequestTest extends AbstractWebTestBase {

    public static final EnumSet<OptionalOrderPart> CHANGE_REQUEST_PART = EnumSet.of(OptionalOrderPart.CHANGE_REQUEST);
    private static final String NOTES = "notes";
    @Autowired
    private YandexMarketDeliveryHelper yandexMarketDeliveryHelper;
    @Autowired
    private CancellationRequestHelper cancellationRequestHelper;

    /**
     * Запрос на отмену заказа с успшеным ответом.
     */
    @Test
    public void cancelOrderSuccessTest() throws Exception {
        Order order = createOrder();

        Set<ChangeRequest> requests = cancelOrder(order);
        checkCancellation(ChangeRequestStatus.APPLIED, requests, order.getId());
    }

    /**
     * Запрос на отмену заказа с неудачным ответом.
     */
    @Test
    public void cancelOrderRejectedTest() throws Exception {
        Order order = createOrder();

        Set<ChangeRequest> requests = cancelOrder(order);

        processChangeRequestsStatus(ChangeRequestStatus.PROCESSING, requests, order.getId());
        checkCancellation(ChangeRequestStatus.REJECTED, requests, order.getId());
    }

    @Nonnull
    private Set<ChangeRequest> cancelOrder(Order order) throws Exception {
        CancellationRequest cancellationRequest = new CancellationRequest(USER_CHANGED_MIND, NOTES);
        ClientInfo clientInfo = new ClientInfo(ClientRole.USER, BuyerProvider.UID);
        cancellationRequestHelper.createCancellationRequestByEditApi(order.getId(), cancellationRequest, clientInfo);

        Set<Long> parcelIds = order.getDelivery().getParcels().stream()
                .map(Parcel::getId)
                .collect(Collectors.toSet());

        Order orderAfter = client.getOrder(order.getId(), ClientRole.SYSTEM, 0L, CHANGE_REQUEST_PART);
        return checkParcelChangeRequests(order.getId(), parcelIds).stream()
                .map(id -> getParcelCancelChangeRequestId(id, orderAfter))
                .collect(Collectors.toSet());
    }


    private void checkCancellation(ChangeRequestStatus cancellationStatus, Set<ChangeRequest> requests, Long orderId) {

        processChangeRequestsStatus(cancellationStatus, requests, orderId);

        Order afterCancel = client.getOrder(orderId, ClientRole.SYSTEM, 0L, CHANGE_REQUEST_PART);
        Set<Long> requestsIds = requests.stream()
                .map(ChangeRequest::getId)
                .collect(Collectors.toSet());
        Objects.requireNonNull(afterCancel.getChangeRequests()).stream()
                .filter(request -> requestsIds.contains(request.getId()))
                .map(ChangeRequest::getStatus)
                .forEach(status -> assertEquals(cancellationStatus, status));

        OrderStatus expectedStatus =
                cancellationStatus == ChangeRequestStatus.APPLIED ? OrderStatus.CANCELLED : PROCESSING;
        assertEquals(expectedStatus, afterCancel.getStatus());
    }

    private void processChangeRequestsStatus(
            ChangeRequestStatus cancellationStatus,
            Set<ChangeRequest> requests,
            Long orderId
    ) {
        requests.forEach(request -> client.updateChangeRequestStatus(
                orderId,
                request.getId(),
                ClientRole.SYSTEM,
                0L,
                createRequest(cancellationStatus, request)
        ));
    }

    @Nonnull
    private ChangeRequestPatchRequest createRequest(ChangeRequestStatus cancellationStatus, ChangeRequest request) {
        ParcelCancelChangeRequestPayload payload = (ParcelCancelChangeRequestPayload) request.getPayload();
        return new ChangeRequestPatchRequest(
                cancellationStatus,
                "",
                new ParcelCancelChangeRequestPayload(
                        payload.getParcelId(),
                        payload.getSubstatus(),
                        payload.getNotes(),
                        null
                )
        );
    }

    private Order createOrder() {
        Order order = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .withColor(BLUE)
                .build();
        order = orderStatusHelper.proceedOrderToStatus(order, PROCESSING);
        return order;
    }

    @Nonnull
    private Set<Long> checkParcelChangeRequests(Long orderId, Set<Long> parcelIdsToCheck) {
        Order orderAfter = client.getOrder(
                orderId,
                ClientRole.SYSTEM,
                0L,
                Set.of(OptionalOrderPart.CHANGE_REQUEST)
        );

        Set<Long> cancelChangeRequestParcelIds = Objects.requireNonNull(orderAfter.getChangeRequests()).stream()
                .filter(changeRequest -> changeRequest.getType().equals(ChangeRequestType.PARCEL_CANCELLATION))
                .filter(changeRequest -> changeRequest.getStatus().equals(ChangeRequestStatus.NEW))
                .map(ChangeRequest::getPayload)
                .map(payload -> ((ParcelCancelChangeRequestPayload) payload).getParcelId())
                .collect(Collectors.toSet());

        assertTrue(cancelChangeRequestParcelIds.containsAll(parcelIdsToCheck));

        return cancelChangeRequestParcelIds;
    }

    @Nonnull
    public ChangeRequest getParcelCancelChangeRequestId(Long parcelId, Order order) {
        return Objects.requireNonNull(order.getChangeRequests()).stream()
                .filter(changeRequest -> changeRequest.getType().equals(ChangeRequestType.PARCEL_CANCELLATION))
                .filter(changeRequest ->
                        ((ParcelCancelChangeRequestPayload) changeRequest.getPayload()).getParcelId().equals(parcelId))
                .findFirst()
                .orElseThrow();
    }
}
