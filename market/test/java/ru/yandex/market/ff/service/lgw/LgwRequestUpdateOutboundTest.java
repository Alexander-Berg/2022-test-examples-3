package ru.yandex.market.ff.service.lgw;

import java.time.LocalDateTime;
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
import ru.yandex.market.ff.client.enums.TimeSlotStatus;
import ru.yandex.market.ff.enums.FulfillmentServiceType;
import ru.yandex.market.ff.model.converter.LgwClientOutboundConverter;
import ru.yandex.market.ff.model.converter.LgwClientStatusConverter;
import ru.yandex.market.ff.model.converter.LgwToFfwfConverter;
import ru.yandex.market.ff.model.entity.BookedTimeSlot;
import ru.yandex.market.ff.model.entity.FulfillmentInfo;
import ru.yandex.market.ff.model.entity.ShopRequest;
import ru.yandex.market.ff.model.entity.Supplier;
import ru.yandex.market.ff.repository.LogisticsPointRepository;
import ru.yandex.market.ff.repository.RequestItemRepository;
import ru.yandex.market.ff.repository.SupplierRepository;
import ru.yandex.market.ff.service.CalendaringServiceClientWrapperService;
import ru.yandex.market.ff.service.ConcreteEnvironmentParamService;
import ru.yandex.market.ff.service.FulfillmentInfoService;
import ru.yandex.market.ff.service.RequestSubTypeService;
import ru.yandex.market.ff.service.ShopRequestFetchingService;
import ru.yandex.market.ff.service.ShopRequestModificationService;
import ru.yandex.market.ff.service.TimeSlotsService;
import ru.yandex.market.ff.service.exception.InconsistentRequestChangeException;
import ru.yandex.market.ff.service.implementation.lgw.FulfilmentRequestClient;
import ru.yandex.market.ff.service.implementation.lgw.LgwRequestCommonClient;
import ru.yandex.market.ff.service.implementation.lgw.LgwRequestWithdrawService;
import ru.yandex.market.ff.service.implementation.lgw.RequestApiTypeValidationService;
import ru.yandex.market.ff.util.WarehouseDateTimeUtils;
import ru.yandex.market.logistic.gateway.client.FulfillmentClient;
import ru.yandex.market.logistic.gateway.common.model.common.DateTimeInterval;
import ru.yandex.market.logistic.gateway.common.model.common.Outbound;
import ru.yandex.market.logistic.gateway.common.model.common.OutboundType;
import ru.yandex.market.logistic.gateway.common.model.common.Partner;
import ru.yandex.market.logistic.gateway.common.model.common.ResourceId;
import ru.yandex.market.logistic.gateway.common.model.common.request.restricted.PutOutboundRestrictedData;
import ru.yandex.market.mbi.api.client.MbiApiClient;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class LgwRequestUpdateOutboundTest extends CommonTest {

    public static final LocalDateTime REQUESTED_DATE =
            LocalDateTime.of(2018, 1, 1, 10, 10, 10);
    private static final long REQUEST_ID_1 = 11L;
    private static final long INFOR_WAREHOUSE_ID = 147L;
    private static final String SERVICE_REQUEST_ID = "serviceRequestId";
    private static final String TRANSPORTATION_ID = "TMT111";

    private LgwRequestWithdrawService lgwRequestWithdrawService;
    private FulfillmentClient fulfillmentClient;
    private TimeSlotsService timeSlotsService;
    private ShopRequestModificationService shopRequestModificationService;
    private CalendaringServiceClientWrapperService csClientWrapperService;
    private RequestSubTypeService requestSubTypeService;

    SoftAssertions softly = new SoftAssertions();

    @BeforeEach
    void init() {
        fulfillmentClient = Mockito.mock(FulfillmentClient.class);
        ConcreteEnvironmentParamService concreteEnvironmentParamService =
                Mockito.mock(ConcreteEnvironmentParamService.class);

        FulfillmentInfoService fulfillmentInfoService = mock(FulfillmentInfoService.class);

        timeSlotsService = mock(TimeSlotsService.class);
        ShopRequestFetchingService shopRequestFetchingService = Mockito.mock(ShopRequestFetchingService.class);
        shopRequestModificationService = Mockito.mock(ShopRequestModificationService.class);
        csClientWrapperService = mock(CalendaringServiceClientWrapperService.class);
        requestSubTypeService = mock(RequestSubTypeService.class);
        RequestSubTypeService subTypeService = mock(RequestSubTypeService.class);
        LogisticsPointRepository logisticsPointRepository = mock(LogisticsPointRepository.class);
        lgwRequestWithdrawService = new LgwRequestWithdrawService(
                fulfillmentClient,
                Mockito.mock(RequestItemRepository.class),
                new LgwClientOutboundConverter(concreteEnvironmentParamService, new LgwClientStatusConverter(),
                        subTypeService, shopRequestFetchingService),
                new LgwClientStatusConverter(),
                Mockito.mock(SupplierRepository.class),
                shopRequestFetchingService,
                shopRequestModificationService,
                Mockito.mock(MbiApiClient.class),
                new LgwToFfwfConverter(),
                mock(LgwRequestCommonClient.class),
                new FulfilmentRequestClient(fulfillmentClient),
                timeSlotsService,
                concreteEnvironmentParamService,
                csClientWrapperService,
                mock(RequestApiTypeValidationService.class),
                requestSubTypeService,
                logisticsPointRepository
        );
        when(fulfillmentInfoService.getFulfillmentInfoOrThrow(anyLong()))
                .thenAnswer(invocation -> {
                    FulfillmentInfo fulfillmentInfo = new FulfillmentInfo();
                    fulfillmentInfo.setType(FulfillmentServiceType.FULFILLMENT);
                    return fulfillmentInfo;
                });
        when(timeSlotsService.findBookedSlotForRequest(REQUEST_ID_1, TimeSlotStatus.UPDATING))
                .thenAnswer(i -> {
                    BookedTimeSlot bookedTimeSlot = new BookedTimeSlot();
                    bookedTimeSlot.setFromTime(REQUESTED_DATE);
                    bookedTimeSlot.setToTime(REQUESTED_DATE.plusMinutes(30));
                    return Optional.of(bookedTimeSlot);
                });
    }

    @AfterEach
    void tearDown() {
        softly.assertAll();
    }

    @Test
    void passUpdateRequest() throws Exception {
        ShopRequest shopRequest = createShopRequest(REQUEST_ID_1, RequestStatus.ACCEPTED_BY_SERVICE);
        Outbound expectedOutbound = createExpectedOutbound();
        lgwRequestWithdrawService.updateRequest(shopRequest);

        verify(shopRequestModificationService).updateStatus(shopRequest, RequestStatus.SENT_TO_SERVICE);
        verify(fulfillmentClient).putOutbound(
            expectedOutbound,
            new Partner(INFOR_WAREHOUSE_ID),
            PutOutboundRestrictedData.builder().setTransportationId(TRANSPORTATION_ID).build(),
            null
        );
    }

    @Test
    void passUpdateRequestWithInvalidStatus() {
        for (RequestStatus status : RequestStatus.values()) {
            switch (status) {
                case ACCEPTED_BY_SERVICE:
                case PLAN_REGISTRY_ACCEPTED:
                case IN_PROGRESS:
                case READY_TO_WITHDRAW:
                    continue;
                default:
            }
            ShopRequest shopRequest = createShopRequest(REQUEST_ID_1, status);

            InconsistentRequestChangeException exception =
                    assertThrows(InconsistentRequestChangeException.class,
                            () -> lgwRequestWithdrawService.updateRequest(shopRequest));
            softly.assertThat(exception.getMessage())
                    .isEqualTo("Request must be in ACCEPTED_BY_SERVICE, IN_PROGRESS, READY_TO_WITHDRAW status to" +
                            " be pushed to LGW");

            verifyZeroInteractions(fulfillmentClient);
        }
    }

    @Test
    void failToUpdateSlotWithoutSlotInUpdatingStatus() {
        ShopRequest shopRequest = createShopRequest(REQUEST_ID_1, RequestStatus.ACCEPTED_BY_SERVICE);

        when(timeSlotsService.findBookedSlotForRequest(REQUEST_ID_1, TimeSlotStatus.UPDATING))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> lgwRequestWithdrawService.updateRequest(shopRequest));

        softly.assertThat(exception.getMessage()).isEqualTo(
                "Could not find time slot in UPDATING status for request with id=" + REQUEST_ID_1);

        verify(timeSlotsService).findBookedSlotForRequest(REQUEST_ID_1, TimeSlotStatus.UPDATING);
    }

    private ShopRequest createShopRequest(long requestId, RequestStatus status) {
        ShopRequest shopRequest = new ShopRequest();
        shopRequest.setId(requestId);
        shopRequest.setServiceId(INFOR_WAREHOUSE_ID);
        shopRequest.setType(RequestType.SUPPLY);
        shopRequest.setRequestedDate(REQUESTED_DATE);
        shopRequest.setStatus(status);
        shopRequest.setServiceRequestId(SERVICE_REQUEST_ID);
        shopRequest.setSupplier(createFirstPartySupplier());
        shopRequest.setTransportationId(TRANSPORTATION_ID);
        return shopRequest;
    }

    private Supplier createFirstPartySupplier() {
        Supplier supplier = new Supplier();
        supplier.setSupplierType(SupplierType.FIRST_PARTY);
        return supplier;
    }

    private Outbound createExpectedOutbound() {
        return Outbound.builder(createResourceId(String.valueOf(REQUEST_ID_1), SERVICE_REQUEST_ID),
                new DateTimeInterval(WarehouseDateTimeUtils.toOffsetDateTime(REQUESTED_DATE, INFOR_WAREHOUSE_ID),
                        WarehouseDateTimeUtils.toOffsetDateTime(REQUESTED_DATE.plusMinutes(30), INFOR_WAREHOUSE_ID)))
                .setOutboundType(OutboundType.UNKNOWN)
                .build();
    }

    private ResourceId createResourceId(String yandexId, String partnerId) {
        return ResourceId.builder()
                .setYandexId(yandexId)
                .setPartnerId(partnerId)
                .build();
    }
}
