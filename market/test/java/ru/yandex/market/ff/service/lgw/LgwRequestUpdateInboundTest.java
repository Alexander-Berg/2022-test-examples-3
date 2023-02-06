package ru.yandex.market.ff.service.lgw;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.market.core.supplier.model.SupplierType;
import ru.yandex.market.ff.base.CommonTest;
import ru.yandex.market.ff.client.enums.RequestStatus;
import ru.yandex.market.ff.client.enums.RequestType;
import ru.yandex.market.ff.client.enums.StockType;
import ru.yandex.market.ff.client.enums.TimeSlotStatus;
import ru.yandex.market.ff.model.TypeSubtype;
import ru.yandex.market.ff.model.converter.LgwClientInboundConverter;
import ru.yandex.market.ff.model.converter.LgwClientStatusConverter;
import ru.yandex.market.ff.model.converter.LgwToFfwfConverter;
import ru.yandex.market.ff.model.entity.BookedTimeSlot;
import ru.yandex.market.ff.model.entity.RequestSubTypeEntity;
import ru.yandex.market.ff.model.entity.ShopRequest;
import ru.yandex.market.ff.model.entity.Supplier;
import ru.yandex.market.ff.repository.RequestItemRepository;
import ru.yandex.market.ff.repository.RequestRealSupplierInfoRepository;
import ru.yandex.market.ff.repository.ShopRequestRepository;
import ru.yandex.market.ff.repository.SupplierRepository;
import ru.yandex.market.ff.service.CalendaringServiceClientWrapperService;
import ru.yandex.market.ff.service.ConcreteEnvironmentParamService;
import ru.yandex.market.ff.service.EnvironmentParamService;
import ru.yandex.market.ff.service.FulfillmentInfoService;
import ru.yandex.market.ff.service.RequestSubTypeService;
import ru.yandex.market.ff.service.ShopRequestFetchingService;
import ru.yandex.market.ff.service.ShopRequestModificationService;
import ru.yandex.market.ff.service.TimeSlotsService;
import ru.yandex.market.ff.service.exception.InconsistentRequestChangeException;
import ru.yandex.market.ff.service.implementation.lgw.LgwRequestSupplyService;
import ru.yandex.market.ff.service.implementation.lgw.RequestApiTypeValidationService;
import ru.yandex.market.ff.util.WarehouseDateTimeUtils;
import ru.yandex.market.logistic.gateway.client.FulfillmentClient;
import ru.yandex.market.logistic.gateway.common.model.common.DateTimeInterval;
import ru.yandex.market.logistic.gateway.common.model.common.Partner;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Inbound;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.InboundType;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.ResourceId;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class LgwRequestUpdateInboundTest extends CommonTest {

    public static final LocalDateTime REQUESTED_DATE =
            LocalDateTime.of(2018, 1, 1, 10, 10, 10);
    public static final LocalDateTime ACTIVE_DATE_2 =
            LocalDateTime.of(2018, 11, 11, 10, 10, 10);
    private static final long REQUEST_ID_1 = 11L;
    private static final long REQUEST_ID_2 = 2L;
    private static final long INFOR_WAREHOUSE_ID = 147L;
    private static final String SERVICE_REQUEST_ID = "serviceRequestId";
    private LgwRequestSupplyService lgwRequestSupplyService;
    private FulfillmentClient fulfillmentClient;
    private TimeSlotsService timeSlotsService;
    private ShopRequestModificationService shopRequestModificationService;
    private FulfillmentInfoService fulfillmentInfoService;

    SoftAssertions softly = new SoftAssertions();

    @BeforeEach
    void init() {
        fulfillmentClient = Mockito.mock(FulfillmentClient.class);
        RequestItemRepository requestItemRepository = Mockito.mock(RequestItemRepository.class);
        SupplierRepository supplierRepository = Mockito.mock(SupplierRepository.class);
        ShopRequestFetchingService shopRequestFetchingService = Mockito.mock(ShopRequestFetchingService.class);
        shopRequestModificationService = Mockito.mock(ShopRequestModificationService.class);
        EnvironmentParamService environmentParamService = Mockito.mock(EnvironmentParamService
                .class);
        ConcreteEnvironmentParamService concreteEnvironmentParamService =
                Mockito.mock(ConcreteEnvironmentParamService.class);
        RequestRealSupplierInfoRepository requestRealSupplierInfoRepository =
                mock(RequestRealSupplierInfoRepository.class);
        RequestSubTypeService requestSubTypeService =  mock(RequestSubTypeService.class);
        fulfillmentInfoService = Mockito.mock(FulfillmentInfoService.class);
        LgwClientInboundConverter inboundConverter = new LgwClientInboundConverter(concreteEnvironmentParamService,
                new LgwClientStatusConverter(), shopRequestFetchingService, requestRealSupplierInfoRepository,
                requestSubTypeService, mock(ShopRequestRepository.class), fulfillmentInfoService);
        LgwToFfwfConverter lgwToFfwfConverter = new LgwToFfwfConverter();
        CalendaringServiceClientWrapperService csClientWrapperService =
                mock(CalendaringServiceClientWrapperService.class);

        timeSlotsService = mock(TimeSlotsService.class);
        lgwRequestSupplyService = new LgwRequestSupplyService(
                fulfillmentClient,
                environmentParamService,
                requestItemRepository,
                inboundConverter,
                new LgwClientStatusConverter(),
                supplierRepository,
                shopRequestFetchingService,
                shopRequestModificationService,
                lgwToFfwfConverter,
                null,
                concreteEnvironmentParamService,
                timeSlotsService,
                csClientWrapperService,
                mock(RequestApiTypeValidationService.class),
                requestSubTypeService
        );
        when(timeSlotsService.findBookedSlotForRequest(REQUEST_ID_1, TimeSlotStatus.UPDATING))
                .thenAnswer(i -> {
                    BookedTimeSlot bookedTimeSlot = new BookedTimeSlot();
                    bookedTimeSlot.setFromTime(REQUESTED_DATE);
                    bookedTimeSlot.setToTime(REQUESTED_DATE.plusMinutes(30));
                    return Optional.of(bookedTimeSlot);
                });
        when(timeSlotsService.findBookedSlotForRequest(REQUEST_ID_2, TimeSlotStatus.ACTIVE))
                .thenAnswer(i -> {
                    BookedTimeSlot bookedTimeSlot = new BookedTimeSlot();
                    bookedTimeSlot.setFromTime(ACTIVE_DATE_2);
                    bookedTimeSlot.setToTime(ACTIVE_DATE_2.plusMinutes(30));
                    return Optional.of(bookedTimeSlot);
                });
        when(timeSlotsService.findBookedSlotForRequest(REQUEST_ID_2, TimeSlotStatus.UPDATING))
                .thenAnswer(i -> {
                    BookedTimeSlot bookedTimeSlot = new BookedTimeSlot();
                    bookedTimeSlot.setFromTime(REQUESTED_DATE);
                    bookedTimeSlot.setToTime(REQUESTED_DATE.plusMinutes(30));
                    return Optional.of(bookedTimeSlot);
                });
        RequestSubTypeEntity requestSubTypeEntity = new RequestSubTypeEntity();
        requestSubTypeEntity.setLgwTypeForSendToService("DEFAULT");
        when(requestSubTypeService.getEntityByRequestTypeAndSubtype(new TypeSubtype(RequestType.SUPPLY, any())))
                .thenReturn(requestSubTypeEntity);
    }

    @AfterEach
    void tearDown() {
        softly.assertAll();
    }

    @Test
    void shouldChangeActiveSlotToUpdated() throws Exception {
        ShopRequest shopRequest = createShopRequest(REQUEST_ID_2, RequestStatus.ACCEPTED_BY_SERVICE);
        Inbound expectedInbound = createExpectedInbound(REQUEST_ID_2);
        lgwRequestSupplyService.updateRequest(shopRequest);

        verify(shopRequestModificationService).updateStatus(shopRequest, RequestStatus.SENT_TO_SERVICE);
        verify(fulfillmentClient).updateInbound(expectedInbound, new Partner(INFOR_WAREHOUSE_ID));
    }

    @Test
    void passUpdateRequest() throws Exception {
        ShopRequest shopRequest = createShopRequest(REQUEST_ID_1, RequestStatus.ACCEPTED_BY_SERVICE);
        Inbound expectedInbound = createExpectedInbound(REQUEST_ID_1);
        lgwRequestSupplyService.updateRequest(shopRequest);

        verify(shopRequestModificationService).updateStatus(shopRequest, RequestStatus.SENT_TO_SERVICE);
        verify(fulfillmentClient).updateInbound(expectedInbound, new Partner(INFOR_WAREHOUSE_ID));
    }

    @Test
    void passUpdateRequestWithInvalidStatus() {
        for (RequestStatus status : RequestStatus.values()) {
            if (status == RequestStatus.ACCEPTED_BY_SERVICE) {
                continue;
            }
            ShopRequest shopRequest = createShopRequest(REQUEST_ID_1, status);

            InconsistentRequestChangeException exception =
                    assertThrows(InconsistentRequestChangeException.class,
                            () -> lgwRequestSupplyService.updateRequest(shopRequest));
            softly.assertThat(exception.getMessage())
                    .isEqualTo("Request must be in ACCEPTED_BY_SERVICE status to be pushed to LGW");

            verifyZeroInteractions(fulfillmentClient);
        }
    }


    @Test
    void failToUpdateSlotWithoutSlotInUpdatingStatus() {
        ShopRequest shopRequest = createShopRequest(REQUEST_ID_1, RequestStatus.ACCEPTED_BY_SERVICE);

        when(timeSlotsService.findBookedSlotForRequest(REQUEST_ID_1, TimeSlotStatus.UPDATING))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> lgwRequestSupplyService.updateRequest(shopRequest));

        softly.assertThat(exception.getMessage()).isEqualTo(
                "Could not find time slot in UPDATING status for request with id=" + REQUEST_ID_1);

        verify(timeSlotsService).findBookedSlotForRequest(REQUEST_ID_1, TimeSlotStatus.UPDATING);
    }

    private ShopRequest createShopRequest(long requestId, RequestStatus status) {
        ShopRequest shopRequest = new ShopRequest();
        shopRequest.setId(requestId);
        shopRequest.setServiceId(INFOR_WAREHOUSE_ID);
        shopRequest.setType(RequestType.SUPPLY);
        shopRequest.setStockType(StockType.EXPIRED);
        shopRequest.setStockTypeTo(StockType.DEFECT);
        shopRequest.setRequestedDate(REQUESTED_DATE);
        shopRequest.setStatus(status);
        shopRequest.setServiceRequestId(SERVICE_REQUEST_ID);
        shopRequest.setSupplier(createFirstPartySupplier());
        return shopRequest;
    }

    private Supplier createFirstPartySupplier() {
        Supplier supplier = new Supplier();
        supplier.setSupplierType(SupplierType.FIRST_PARTY);
        return supplier;
    }

    private Inbound createExpectedInbound(long requestId) {
        return new Inbound.InboundBuilder(createResourceId(String.valueOf(requestId), SERVICE_REQUEST_ID),
                InboundType.DEFAULT,
                Collections.emptyList(),
                new DateTimeInterval(WarehouseDateTimeUtils.toOffsetDateTime(REQUESTED_DATE, INFOR_WAREHOUSE_ID),
                        WarehouseDateTimeUtils.toOffsetDateTime(REQUESTED_DATE.plusMinutes(30), INFOR_WAREHOUSE_ID)))
                .build();
    }

    private ResourceId createResourceId(String yandexId, String partnerId) {
        return ResourceId.builder()
                .setYandexId(yandexId)
                .setPartnerId(partnerId)
                .setFulfillmentId(partnerId)
                .build();
    }
}
