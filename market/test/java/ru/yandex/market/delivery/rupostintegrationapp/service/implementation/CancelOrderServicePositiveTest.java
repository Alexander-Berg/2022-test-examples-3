package ru.yandex.market.delivery.rupostintegrationapp.service.implementation;

import java.util.ArrayList;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.delivery.entities.common.ResourceId;
import ru.yandex.market.delivery.entities.request.ds.DsCancelOrderRequest;
import ru.yandex.market.delivery.entities.response.ds.implementation.DsCancelOrderResponseContent;
import ru.yandex.market.delivery.rupostintegrationapp.BaseTest;
import ru.yandex.market.delivery.russianpostapiclient.bean.deleteordersfrombatch.DeleteOrdersFromBatchResponse;
import ru.yandex.market.delivery.russianpostapiclient.bean.getinfoaboutordersinbatch.Shipment;
import ru.yandex.market.delivery.russianpostapiclient.client.RussianPostApiClient;
import ru.yandex.market.delivery.russianpostapiclient.processor.ApiMethodProcessingException;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CancelOrderServicePositiveTest extends BaseTest {
    private static final String ORDER_BARCODE = "SHI_PI_I";

    private static final String ORDER_YANDEX_ID = "ID112233";

    private static final Integer SHIPMENT_ID = 123;
    @Mock
    private RussianPostApiClient client;

    static Stream<Arguments> data() {
        return Stream.of(
            Arguments.of(
                getRequest(ORDER_BARCODE, null),
                getRussianPostShipment(SHIPMENT_ID, ORDER_BARCODE),
                getDeleteOrderResponse(SHIPMENT_ID)
            ),
            Arguments.of(
                getRequest(null, ORDER_YANDEX_ID),
                getRussianPostShipment(SHIPMENT_ID, ORDER_BARCODE),
                getDeleteOrderResponse(SHIPMENT_ID)
            ),
            Arguments.of(
                getRequest(ORDER_BARCODE, ORDER_YANDEX_ID),
                getRussianPostShipment(SHIPMENT_ID, ORDER_BARCODE),
                getDeleteOrderResponse(SHIPMENT_ID)
            ),
            Arguments.of(
                getRequest(ORDER_BARCODE, ORDER_YANDEX_ID),
                null,
                getDeleteOrderResponse(SHIPMENT_ID)
            )
        );
    }

    private static DsCancelOrderRequest getRequest(String orderDeliveryId, String orderYandexId) {
        DsCancelOrderRequest request = new DsCancelOrderRequest();

        ResourceId orderId = new ResourceId();
        orderId.setDeliveryId(orderDeliveryId);
        orderId.setYandexId(orderYandexId);

        request.getRequestContent().setOrderId(orderId);

        return request;
    }

    private static DeleteOrdersFromBatchResponse getDeleteOrderResponse(Integer resultShipmentId) {
        DeleteOrdersFromBatchResponse response = new DeleteOrdersFromBatchResponse();

        ArrayList<Integer> resultIds = new ArrayList<>();
        resultIds.add(resultShipmentId);

        response.setResultIds(resultIds);

        return response;
    }

    private static Shipment getRussianPostShipment(Integer shipmentId, String barcode) {
        Shipment shipment = new Shipment();
        shipment.setId(shipmentId);
        shipment.setBarcode(barcode);

        return shipment;
    }

    @ParameterizedTest
    @MethodSource("data")
    void doJobCorrectDataTest(
        DsCancelOrderRequest request,
        Shipment shipment,
        DeleteOrdersFromBatchResponse deleteOrdersFromBatchResponse
    ) throws ApiMethodProcessingException {
        when(client.deleteOrderFromBatch(any(Integer.class))).thenReturn(deleteOrdersFromBatchResponse);
        when(client.getShipmentByBarcode(any(String.class))).thenReturn(shipment);
        when(client.searchShipmentsByBarcodeOrOrderNum(any(String.class))).thenReturn(new Shipment[]{shipment});
        when(client.searchShipmentById(any(String.class)))
            .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

        CancelOrderService service = new CancelOrderService(client);
        DsCancelOrderResponseContent responseContent = service.doJob(request);
        softly.assertThat(responseContent).as("Cancel order response content is null").isNotNull();
    }
}
