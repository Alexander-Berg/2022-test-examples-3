package ru.yandex.market.ff.service.implementation;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.ff.client.enums.CalendaringMode;
import ru.yandex.market.ff.client.enums.RequestStatus;
import ru.yandex.market.ff.client.enums.RequestType;
import ru.yandex.market.ff.config.HistoryAgencyContextBeanConfig;
import ru.yandex.market.ff.dbqueue.producer.CreateDocumentTicketProducer;
import ru.yandex.market.ff.dbqueue.producer.service.SendingToCorrectValidationProducerService;
import ru.yandex.market.ff.model.entity.ShopRequest;
import ru.yandex.market.ff.repository.CalendarBookingRepository;
import ru.yandex.market.ff.repository.InitialAcceptanceNotFinishedRepository;
import ru.yandex.market.ff.repository.RequestItemMarketBarcodeRepository;
import ru.yandex.market.ff.repository.RequestItemMarketVendorCodeRepository;
import ru.yandex.market.ff.repository.RequestStatusHistoryRepository;
import ru.yandex.market.ff.repository.ShopRequestRepository;
import ru.yandex.market.ff.service.ConcreteEnvironmentParamService;
import ru.yandex.market.ff.service.DateTimeService;
import ru.yandex.market.ff.service.FeatureToggleService;
import ru.yandex.market.ff.service.MonitoringEventService;
import ru.yandex.market.ff.service.RequestDocumentService;
import ru.yandex.market.ff.service.RequestItemService;
import ru.yandex.market.ff.service.RequestRelationService;
import ru.yandex.market.ff.service.RequestResourcesReleasingService;
import ru.yandex.market.ff.service.RequestSizeCalculationService;
import ru.yandex.market.ff.service.RequestStatusHistoryService;
import ru.yandex.market.ff.service.RequestSubTypeService;
import ru.yandex.market.ff.service.ShopRequestFetchingService;
import ru.yandex.market.ff.service.ShopRequestModificationService;
import ru.yandex.market.ff.service.ShopRequestStatusService;
import ru.yandex.market.ff.service.TakenLimitsByRequestService;
import ru.yandex.market.ff.service.TimeSlotsService;
import ru.yandex.market.ff.service.exception.InconsistentRequestChangeException;
import ru.yandex.market.logistics.calendaring.client.CalendaringServiceClientApi;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit тесты для {@link ShopRequestFetchingService}.
 *
 * @author avetokhin 12/01/18.
 */
@ExtendWith(MockitoExtension.class)
public class ShopRequestModificationServiceUnitTest {

    private static final long ID = 1;
    private static long requestId = 123L;

    @Mock
    private ShopRequestRepository shopRequestRepository;

    @Mock
    private RequestItemService requestItemService;

    @Mock
    private RequestStatusHistoryRepository requestStatusHistoryRepository;

    @Mock
    private DateTimeService dateTimeService;

    private RequestResourcesReleasingService requestResourcesReleasingService;
    @Mock
    private TimeSlotsService timeSlotsService;

    @Mock
    private SendingToCorrectValidationProducerService sendingToCorrectValidationProducerService;

    @Mock
    private TakenLimitsByRequestService takenLimitsByRequestService;

    @Mock
    private RequestSizeCalculationService requestSizeCalculationService;

    @Mock
    private RequestItemMarketVendorCodeRepository marketVendorCodeRepository;

    @Mock
    private RequestItemMarketBarcodeRepository marketBarcodeRepository;

    @Mock
    private MonitoringEventService monitoringEventService;

    @Mock
    private PublishToLogbrokerCalendarShopRequestChangeService publishToLogbrokerCalendarShopRequestChangeService;

    @Mock
    private RequestDocumentService requestDocumentService;

    @Mock
    private FeatureToggleService featureToggleService;

    @Mock
    private ShopRequestFetchingService shopRequestFetchingService;

    @Mock
    private CalendaringServiceClientApi csClient;

    @Mock
    private ConcreteEnvironmentParamService concreteEnvironmentParamService;

    @Mock
    private CalendarBookingRepository calendarBookingRepository;

    @Mock
    private SendRequestService sendRequestService;

    private RequestStatusHistoryService requestStatusHistoryService;

    private Map<RequestType, Map<RequestStatus, List<RequestStatus>>> allowedStatusMoves =
            ImmutableMap.<RequestType, Map<RequestStatus, List<RequestStatus>>>builder()
                    // Доступные для теста обычной поставки переходы.
                    .put(RequestType.SUPPLY, ImmutableMap.<RequestStatus, List<RequestStatus>>builder()
                            .put(RequestStatus.CREATED, List.of(
                                    RequestStatus.VALIDATED, RequestStatus.INVALID,
                                    RequestStatus.WAITING_FOR_CONFIRMATION, RequestStatus.CANCELLED
                            ))
                            .put(RequestStatus.SENT_TO_SERVICE,
                                    List.of(RequestStatus.REJECTED_BY_SERVICE, RequestStatus.CANCELLED))
                            .put(RequestStatus.ACCEPTED_BY_SERVICE,
                                    List.of(RequestStatus.SENT_TO_SERVICE))
                            .build())
                    // Доступные для теста перемещения переходы.
                    .put(RequestType.TRANSFER, ImmutableMap.<RequestStatus, List<RequestStatus>>builder()
                            .put(RequestStatus.CREATED, List.of(
                                    RequestStatus.ACCEPTED_BY_SERVICE, RequestStatus.REJECTED_BY_SERVICE
                            ))
                            .build())
                    .build();

    private ShopRequestModificationService service;

    public static Collection<Object[]> params() {
        Collection<Object[]> parameters = new ArrayList<>();

        for (CalendaringMode calendaringMode : CalendaringMode.values()) {
            for (RequestStatus currentStatus : RequestStatus.values()) {
                for (RequestStatus newStatus : RequestStatus.values()) {
                    if (currentStatus == RequestStatus.INITIAL_ACCEPTANCE_COMPLETED) {
                        continue;
                    }
                    boolean shouldBeRemovedQuota = newStatus == RequestStatus.REJECTED_BY_SERVICE ||
                            newStatus == RequestStatus.CANCELLED;
                    boolean expectingGood = currentStatus == newStatus
                            || currentStatus == RequestStatus.CREATED && newStatus == RequestStatus.VALIDATED
                            || currentStatus == RequestStatus.CREATED && newStatus == RequestStatus.INVALID
                            || currentStatus == RequestStatus.CREATED &&
                            newStatus == RequestStatus.WAITING_FOR_CONFIRMATION
                            || currentStatus == RequestStatus.ACCEPTED_BY_SERVICE &&
                            newStatus == RequestStatus.SENT_TO_SERVICE;
                    parameters.add(new Object[] {currentStatus, newStatus, calendaringMode,
                            shouldBeRemovedQuota, !expectingGood});
                }
            }
        }
        return parameters;
    }

    @BeforeEach
    void init() {
        MockitoAnnotations.initMocks(this);
        when(shopRequestRepository.save(any(ShopRequest.class))).then(invocation -> invocation.getArgument(0));
        when(takenLimitsByRequestService.shouldRemoveTakenLimitsOnStatusChange(any(), any())).thenAnswer(invocation -> {
            RequestStatus status = invocation.getArgument(1);
            return shouldBeRemovedQuota(status);
        });
        when(dateTimeService.localDateTimeNow()).thenReturn(LocalDateTime.MAX);
        requestResourcesReleasingService =
                new RequestResourcesReleasingServiceImpl(takenLimitsByRequestService, timeSlotsService,
                        csClient, calendarBookingRepository);
        requestStatusHistoryService =
                new RequestStatusHistoryServiceImpl(dateTimeService, requestStatusHistoryRepository);

        CreateDocumentTicketProducer createDocumentTicketProducer = mock(CreateDocumentTicketProducer.class);

        InitialAcceptanceNotFinishedRepository initialAcceptanceNotFinishedRepository =
                mock(InitialAcceptanceNotFinishedRepository.class);

        RequestSubTypeService requestSubTypeService = mock(RequestSubTypeService.class);

        ShopRequestStatusService shopRequestStatusService =
                new ShopRequestStatusServiceImpl(requestStatusHistoryRepository,
                        shopRequestRepository,
                        dateTimeService,
                        Set.of(),
                        requestResourcesReleasingService,
                        allowedStatusMoves,
                        EnumSet.of(RequestType.UTILIZATION_WITHDRAW),
                        Map.of(),
                        monitoringEventService, publishToLogbrokerCalendarShopRequestChangeService,
                        createDocumentTicketProducer, concreteEnvironmentParamService, requestStatusHistoryService,
                        initialAcceptanceNotFinishedRepository, requestSubTypeService,
                        () -> new HistoryAgencyContextBeanConfig().getHistoryContext());

        service = new ShopRequestModificationServiceImpl(
                shopRequestRepository,
                shopRequestStatusService,
                requestSizeCalculationService,
                requestItemService,
                dateTimeService,
                sendingToCorrectValidationProducerService,
                marketBarcodeRepository,
                marketVendorCodeRepository,
                publishToLogbrokerCalendarShopRequestChangeService,
                shopRequestFetchingService,
                requestDocumentService,
                featureToggleService,
                concreteEnvironmentParamService,
                requestSubTypeService,
                mock(RequestRelationService.class),
                sendRequestService
        );
    }

    @ParameterizedTest
    @MethodSource("params")
    void testStatusChange(RequestStatus currentStatus, RequestStatus newStatus, CalendaringMode calendaringMode,
                          boolean shouldBeRemovedQuota, boolean expectingFail) {
        assertRequestUpdate(currentStatus, newStatus, calendaringMode, shouldBeRemovedQuota, expectingFail);
    }

    private void assertRequestUpdate(final RequestStatus currentStatus,
                                     final RequestStatus newStatus,
                                     final CalendaringMode calendaringMode,
                                     final boolean shouldBeRemovedQuota,
                                     final boolean expectingFail) {
        LocalDateTime requestedDate = LocalDateTime.now();
        final ShopRequest request = request(ID, currentStatus, calendaringMode, requestedDate);
        final ShopRequest expectedRequest = request(ID, newStatus, calendaringMode, requestedDate);

        try {
            service.updateStatus(request, newStatus);
            verify(shopRequestRepository).save(eq(Collections.singletonList(expectedRequest)));
            if (shouldBeRemovedQuota) {
                ArgumentCaptor<Collection<Long>> captor = ArgumentCaptor.forClass(Collection.class);
                verify(takenLimitsByRequestService).deleteByRequestIdIn(captor.capture());
                Collection<Long> collection = captor.getValue();
                assertEquals(1, collection.size());
                assertEquals(ID, (long) collection.iterator().next());
            } else {
                verify(takenLimitsByRequestService, never()).deleteByRequestIdIn(anyCollection());
            }
        } catch (InconsistentRequestChangeException e) {
            if (!expectingFail) {
                fail(String.format("Request status change from %s to %s must fail", currentStatus, newStatus));
            }
        }
    }

    private static boolean shouldBeRemovedQuota(RequestStatus newStatus) {
        return newStatus == RequestStatus.REJECTED_BY_SERVICE || newStatus == RequestStatus.CANCELLED;
    }

    private static ShopRequest request(long id, RequestStatus status, CalendaringMode calendaringMode,
                                       LocalDateTime requestedDate) {
        final ShopRequest request = new ShopRequest() {

            @Override
            public int hashCode() {
                return super.hashCode();
            }

            @Override
            public boolean equals(final Object o) {
                if (this == o) {
                    return true;
                }
                if (!(o instanceof ShopRequest)) {
                    return false;
                }
                final ShopRequest request = (ShopRequest) o;
                return Objects.equals(id, request.getId()) && Objects.equals(request.getStatus(), request.getStatus());
            }
        };
        request.setId(id);
        request.setType(RequestType.SUPPLY);
        request.setStatus(status);
        request.setCalendaringMode(calendaringMode);
        request.setRequestedDate(requestedDate);
        return request;
    }

    @Test
    public void testUseCalendaringToCancelSlot() {
        Set<Long> requestIds = Set.of(requestId);
        Set<Long> bookingIds = Set.of(456L);

        when(calendarBookingRepository.findBookingIdsByRequestIds(requestIds)).thenReturn(bookingIds);
        ShopRequest shopRequest = new ShopRequest();
        shopRequest.setCalendaringMode(CalendaringMode.REQUIRED);
        shopRequest.setId(requestId);
        requestResourcesReleasingService.releaseResourcesIfRequired(List.of(shopRequest),
                RequestStatus.CANCELLED);
        verify(csClient).cancelSlots(bookingIds);
        verify(timeSlotsService).deactivateSlotsForRequests(any());
    }
    @Test
    public void testUpdateRequestedDateWithoutXDocCheck() {
        LocalDateTime time = LocalDateTime.parse("2007-12-03T10:15:30");

        ShopRequest shopRequest = new ShopRequest();
        shopRequest.setCalendaringMode(CalendaringMode.REQUIRED);
        shopRequest.setId(requestId);
        service
                .updateRequestedDateWithoutXDocCheck(shopRequest, time);
        verify(sendRequestService).sendIfNeeded(shopRequest);
    }
}
