package ru.yandex.market.checkout.providers;

import java.time.Instant;

import ru.yandex.market.checkout.checkouter.delivery.shipment.ParcelStatus;
import ru.yandex.market.checkout.checkouter.request.ParcelPatchRequest;

public class ParcelPatchRequestProvider {

    private ParcelPatchRequestProvider() {
        throw new UnsupportedOperationException();
    }

    public static ParcelPatchRequest getDeliveredAtUpdateRequest(Instant deliveredAt) {
        ParcelPatchRequest request = new ParcelPatchRequest();
        request.setDeliveredAt(deliveredAt);
        return request;
    }

    public static ParcelPatchRequest getLabelUrlUpdateRequest(String labelUrl) {
        ParcelPatchRequest request = new ParcelPatchRequest();
        request.setLabelUrl(labelUrl);
        return request;
    }

    public static ParcelPatchRequest getStatusUpdateRequest(ParcelStatus status) {
        ParcelPatchRequest request = new ParcelPatchRequest();
        request.setParcelStatus(status);
        return request;
    }
}
