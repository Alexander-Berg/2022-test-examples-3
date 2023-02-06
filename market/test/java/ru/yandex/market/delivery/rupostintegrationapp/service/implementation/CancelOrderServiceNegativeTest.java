package ru.yandex.market.delivery.rupostintegrationapp.service.implementation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.delivery.entities.common.ResourceId;
import ru.yandex.market.delivery.entities.request.ds.DsCancelOrderRequest;
import ru.yandex.market.delivery.entities.response.ds.implementation.DsCancelOrderResponseContent;
import ru.yandex.market.delivery.rupostintegrationapp.BaseTest;
import ru.yandex.market.delivery.rupostintegrationapp.service.exception.ServiceInternalException;
import ru.yandex.market.delivery.russianpostapiclient.bean.getinfoaboutordersinbatch.Shipment;
import ru.yandex.market.delivery.russianpostapiclient.client.RussianPostApiClient;
import ru.yandex.market.delivery.russianpostapiclient.processor.ApiMethodProcessingException;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CancelOrderServiceNegativeTest extends BaseTest {
    private static final String ORDER_BARCODE = "SHI_PI_I";

    private static final String ORDER_YANDEX_ID = "ID112233";

    private static final Integer SHIPMENT_ID = 123;

    @Mock
    private RussianPostApiClient client;

    private CancelOrderService service;

    private static Shipment getRussianPostShipment(Integer shipmentId, String barcode) {
        Shipment shipment = new Shipment();
        shipment.setId(shipmentId);
        shipment.setBarcode(barcode);

        return shipment;
    }

    @Test
    void testShipmentNotFoundByBarcodeOrOrderNum() throws ApiMethodProcessingException {
        when(client.searchShipmentsByBarcodeOrOrderNum(any(String.class))).thenReturn(new Shipment[]{});
        DsCancelOrderRequest request = getRequest(null, ORDER_YANDEX_ID);
        service = new CancelOrderService(client);
        DsCancelOrderResponseContent responseContent = service.doJob(request);
        softly.assertThat(responseContent).as("Cancel order response content is null").isNotNull();
    }

    @Test
    void test2ShipmentsHasFound() throws ApiMethodProcessingException {
        when(client.searchShipmentsByBarcodeOrOrderNum(any(String.class)))
            .thenReturn(
                new Shipment[]{
                    getRussianPostShipment(SHIPMENT_ID, ORDER_BARCODE),
                    getRussianPostShipment(SHIPMENT_ID, ORDER_BARCODE)
                }
            );
        DsCancelOrderRequest request = getRequest(ORDER_BARCODE, ORDER_YANDEX_ID);
        service = new CancelOrderService(client);
        softly.assertThatThrownBy(() -> service.doJob(request)).isInstanceOf(ServiceInternalException.class);
    }

    private DsCancelOrderRequest getRequest(String orderDeliveryId, String orderYandexId) {
        DsCancelOrderRequest request = new DsCancelOrderRequest();
        ResourceId orderId = new ResourceId();
        orderId.setDeliveryId(orderDeliveryId);
        orderId.setYandexId(orderYandexId);
        request.getRequestContent().setOrderId(orderId);
        return request;
    }
}
