package ru.yandex.market.ff.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import ru.yandex.market.ff.client.enums.RequestStatus;
import ru.yandex.market.ff.client.enums.RequestType;
import ru.yandex.market.ff.dbqueue.producer.SendMbiNotificationQueueProducer;
import ru.yandex.market.ff.enums.ExternalOperationType;
import ru.yandex.market.ff.model.dbqueue.SendMbiNotificationPayload;
import ru.yandex.market.ff.model.entity.FulfillmentInfo;
import ru.yandex.market.ff.model.entity.ShopRequest;
import ru.yandex.market.ff.model.entity.Supplier;
import ru.yandex.market.ff.service.exception.MbiNotificationException;
import ru.yandex.market.ff.service.implementation.MbiNotificationServiceImpl;
import ru.yandex.market.mbi.api.client.MbiApiClient;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit тесты для {@link MbiNotificationService}.
 *
 * @author avetokhin 27/03/18.
 */
@ExtendWith(MockitoExtension.class)
class MbiNotificationServiceTest {

    private static final LocalDateTime REQUESTED_DATE = LocalDateTime.of(2020, 8, 25, 12, 30);

    private static final int NOTIFICATION_TYPE = 10;
    private static final long REQUEST_ID = 66;
    private static final long REQUEST_ID_2 = REQUEST_ID + 1;
    private static final long SERVICE_ID = 145;
    private static final long SERVICE_ID_2 = 147;
    private static final String SERVICE_REQUEST_ID = "1567349";
    private static final String SERVICE_REQUEST_ID_2 = "1567350";
    private static final long SUPPLIER_ID = 55;
    private static final String PARAMS_XML = ""
            + "<request-info>"
            + "<id>66</id>"
            + "<service-request-id>1567349</service-request-id>"
            + "<destination-warehouse-id>145</destination-warehouse-id>"
            + "<destination-warehouse-name>Rostov</destination-warehouse-name>"
            + "<merchandise-receipt-date>25 августа</merchandise-receipt-date>"
            + "<merchandise-receipt-time>12:30</merchandise-receipt-time>"
            + "</request-info>";
    private static final String EXTERNAL_REQUEST_ID = "abc345";

    private MbiNotificationService service;

    @Mock
    private MbiApiClient client;

    @Mock
    private FulfillmentInfoService fulfillmentInfoService;

    @Mock
    private ShopRequestFetchingService shopRequestFetchingService;

    @Mock
    private SendMbiNotificationQueueProducer sendMbiNotificationQueueProducer;

    @Mock
    private ConcreteEnvironmentParamService environmentParamService;

    private ArgumentCaptor<SendMbiNotificationPayload> argumentCaptor;

    @BeforeEach
    void init() {
        service = new MbiNotificationServiceImpl(shopRequestFetchingService, fulfillmentInfoService,
                environmentParamService, sendMbiNotificationQueueProducer);
        when(fulfillmentInfoService.getFulfillmentInfoOrThrow(eq(SERVICE_ID)))
                .thenAnswer(invocation -> {
                    FulfillmentInfo fulfillmentInfo = new FulfillmentInfo();
                    fulfillmentInfo.setId(SERVICE_ID);
                    fulfillmentInfo.setName("Rostov");
                    return fulfillmentInfo;
                });
        argumentCaptor = ArgumentCaptor.forClass(SendMbiNotificationPayload.class);
    }

    @Test
    void sendNotification() {
        final ShopRequest request = createRequest();
        service.sendNotification(NOTIFICATION_TYPE, request);

        verify(sendMbiNotificationQueueProducer).produceSingle(argumentCaptor.capture());
        Assertions.assertEquals(SUPPLIER_ID, argumentCaptor.getValue().getSupplierId());
        Assertions.assertEquals(NOTIFICATION_TYPE, argumentCaptor.getValue().getNotificationType());
        Assertions.assertEquals(PARAMS_XML, argumentCaptor.getValue().getData());
    }

    @Test
    void sendMoveNotification() {
        final Supplier supplier = new Supplier();
        supplier.setId(SUPPLIER_ID);
        final ShopRequest request1 = new ShopRequest();
        request1.setId(REQUEST_ID);
        request1.setType(RequestType.SUPPLY);
        request1.setServiceId(SERVICE_ID);
        request1.setServiceRequestId(SERVICE_REQUEST_ID);
        request1.setExternalOperationType(ExternalOperationType.MOVE);
        request1.setSupplier(supplier);
        request1.setExternalRequestId(EXTERNAL_REQUEST_ID);
        final ShopRequest request2 = new ShopRequest();
        request2.setId(REQUEST_ID_2);
        request2.setType(RequestType.WITHDRAW);
        request2.setServiceId(SERVICE_ID_2);
        request2.setServiceRequestId(SERVICE_REQUEST_ID);
        request2.setExternalOperationType(ExternalOperationType.MOVE);
        request2.setSupplier(supplier);
        request2.setExternalRequestId(EXTERNAL_REQUEST_ID);
        when(fulfillmentInfoService.getFulfillmentInfoOrThrow(eq(SERVICE_ID_2)))
                .thenAnswer(invocation -> {
                    FulfillmentInfo fulfillmentInfo = new FulfillmentInfo();
                    fulfillmentInfo.setId(SERVICE_ID_2);
                    fulfillmentInfo.setName("Tomilino");
                    return fulfillmentInfo;
                });
        when(shopRequestFetchingService.getRequestByExternalRequestId(
                eq(SUPPLIER_ID),
                eq(EXTERNAL_REQUEST_ID),
                eq(RequestType.WITHDRAW)
        )).thenReturn(Optional.of(request2));
        service.sendNotification(NOTIFICATION_TYPE, request1);

        String paramsXml = ""
                + "<request-info>"
                + "<id>66</id>"
                + "<service-request-id>1567349</service-request-id>"
                + "<source-warehouse-id>147</source-warehouse-id>"
                + "<source-warehouse-name>Tomilino</source-warehouse-name>"
                + "<destination-warehouse-id>145</destination-warehouse-id>"
                + "<destination-warehouse-name>Rostov</destination-warehouse-name>"
                + "</request-info>";
        verify(fulfillmentInfoService).getFulfillmentInfoOrThrow(SERVICE_ID);
        verify(shopRequestFetchingService)
                .getRequestByExternalRequestId(SUPPLIER_ID, EXTERNAL_REQUEST_ID, RequestType.WITHDRAW);
        verify(fulfillmentInfoService).getFulfillmentInfoOrThrow(SERVICE_ID_2);
        verify(sendMbiNotificationQueueProducer).produceSingle(argumentCaptor.capture());
        Assertions.assertEquals(SUPPLIER_ID, argumentCaptor.getValue().getSupplierId());
        Assertions.assertEquals(NOTIFICATION_TYPE, argumentCaptor.getValue().getNotificationType());
        Assertions.assertEquals(paramsXml, argumentCaptor.getValue().getData());
    }

    @Test
    void sendNotificationWithReadyWithdrawals() {
        final ShopRequest request = createRequest();

        final Supplier supplier = new Supplier();
        supplier.setId(SUPPLIER_ID);

        final ShopRequest request2 = new ShopRequest();
        request2.setId(REQUEST_ID_2);
        request2.setType(RequestType.WITHDRAW);
        request2.setServiceId(SERVICE_ID);
        request2.setServiceRequestId(SERVICE_REQUEST_ID_2);
        request2.setSupplier(supplier);
        request2.setExternalRequestId(EXTERNAL_REQUEST_ID);

        when(shopRequestFetchingService.findAllBySupplierIdAndServiceIdAndTypeAndStatus(
                eq(SUPPLIER_ID),
                eq(SERVICE_ID),
                eq(RequestType.WITHDRAW),
                eq(RequestStatus.READY_TO_WITHDRAW)
        )).thenReturn(List.of(request2));

        when(environmentParamService.sendReadyWithdrawalsInNotifications()).thenReturn(true);


        service.sendNotification(NOTIFICATION_TYPE, request);

        String paramsXml = ""
                + "<request-info>"
                + "<id>66</id>"
                + "<service-request-id>1567349</service-request-id>"
                + "<destination-warehouse-id>145</destination-warehouse-id>"
                + "<destination-warehouse-name>Rostov</destination-warehouse-name>"
                + "<merchandise-receipt-date>25 августа</merchandise-receipt-date>"
                + "<merchandise-receipt-time>12:30</merchandise-receipt-time>"
                + "<ready-withdrawals-map>"
                + "<67>1567350</67>"
                + "</ready-withdrawals-map>"
                + "</request-info>";

        verify(sendMbiNotificationQueueProducer).produceSingle(argumentCaptor.capture());
        Assertions.assertEquals(SUPPLIER_ID, argumentCaptor.getValue().getSupplierId());
        Assertions.assertEquals(NOTIFICATION_TYPE, argumentCaptor.getValue().getNotificationType());
        Assertions.assertEquals(paramsXml, argumentCaptor.getValue().getData());
    }


    @Test
    void sendNotificationWithException() {
        Assertions.assertThrows(MbiNotificationException.class, () -> {
            final ShopRequest request = createRequest();
            when(fulfillmentInfoService.getFulfillmentInfoOrThrow(anyLong())).thenThrow(new RuntimeException());
            service.sendNotification(NOTIFICATION_TYPE, request);
        });
    }

    @Test
    void sendNotificationQuietly() {
        final ShopRequest request = createRequest();
        service.sendNotificationQuietly(NOTIFICATION_TYPE, request);

        verify(sendMbiNotificationQueueProducer).produceSingle(argumentCaptor.capture());
        Assertions.assertEquals(SUPPLIER_ID, argumentCaptor.getValue().getSupplierId());
        Assertions.assertEquals(NOTIFICATION_TYPE, argumentCaptor.getValue().getNotificationType());
        Assertions.assertEquals(PARAMS_XML, argumentCaptor.getValue().getData());
    }

    @Test
    void sendNotificationQuietlyWithException() {
        final ShopRequest request = createRequest();
        when(client.sendMessageToSupplier(anyLong(), anyInt(), anyString())).thenThrow(new RuntimeException());
        service.sendNotificationQuietly(NOTIFICATION_TYPE, request);

        verify(sendMbiNotificationQueueProducer).produceSingle(argumentCaptor.capture());
        Assertions.assertEquals(SUPPLIER_ID, argumentCaptor.getValue().getSupplierId());
        Assertions.assertEquals(NOTIFICATION_TYPE, argumentCaptor.getValue().getNotificationType());
        Assertions.assertEquals(PARAMS_XML, argumentCaptor.getValue().getData());
    }

    @Test
    void sendNotificationWithoutDataQuietly() {
        service.sendNotificationQuietly(NOTIFICATION_TYPE, SUPPLIER_ID);

        verify(sendMbiNotificationQueueProducer).produceSingle(argumentCaptor.capture());
        Assertions.assertEquals(SUPPLIER_ID, argumentCaptor.getValue().getSupplierId());
        Assertions.assertEquals(NOTIFICATION_TYPE, argumentCaptor.getValue().getNotificationType());
        Assertions.assertNull(argumentCaptor.getValue().getData());
    }

    private ShopRequest createRequest() {
        final ShopRequest request = new ShopRequest();
        final Supplier supplier = new Supplier();
        supplier.setId(SUPPLIER_ID);
        request.setId(REQUEST_ID);
        request.setType(RequestType.SUPPLY);
        request.setServiceId(SERVICE_ID);
        request.setServiceRequestId(SERVICE_REQUEST_ID);
        request.setExternalOperationType(ExternalOperationType.INBOUND);
        request.setSupplier(supplier);
        request.setRequestedDate(REQUESTED_DATE);
        return request;
    }

}
