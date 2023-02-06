package steps;

import java.util.Date;

import ru.yandex.market.delivery.mdbapp.request.CreateOrder;
import ru.yandex.market.delivery.mdbclient.model.request.SetOrderDeliveryShipmentLabel;

public class RequestsSteps {

    private RequestsSteps() {
    }

    public static CreateOrder getCreateOrderRequest() {
        return getCreateOrderRequest(
            2L,
            "321",
            new Date(),
            "333",
            0L,
            123L,
            0L,
            1L
        );
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    public static CreateOrder getCreateOrderRequest(
        long orderId,
        String externalTrackingNumber,
        Date shipmentDate,
        String externalShipmentId,
        long shopId,
        long deliveryServiceId,
        long inletId,
        long marketShipmentId
    ) {
        CreateOrder request = new CreateOrder();

        request.setOrderId(orderId);
        request.setExternalTrackingNumber(externalTrackingNumber);
        request.setShipmentDate(shipmentDate);
        request.setExternalShipmentId(externalShipmentId);
        request.setShopId(shopId);
        request.setDeliveryServiceId(deliveryServiceId);
        request.setInletId(inletId);
        request.setMarketShipmentId(marketShipmentId);

        return request;
    }

    public static SetOrderDeliveryShipmentLabel getSetOrderDeliveryShipmentLabelRequest() {
        return new SetOrderDeliveryShipmentLabel(1122L, null, "url");
    }
}
