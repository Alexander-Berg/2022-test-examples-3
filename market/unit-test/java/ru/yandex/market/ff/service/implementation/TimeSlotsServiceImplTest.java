package ru.yandex.market.ff.service.implementation;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.core.supplier.model.SupplierType;
import ru.yandex.market.ff.base.SoftAssertionSupport;
import ru.yandex.market.ff.client.dto.BookedTimeSlotDto;
import ru.yandex.market.ff.client.enums.CalendaringMode;
import ru.yandex.market.ff.client.enums.RequestItemAttribute;
import ru.yandex.market.ff.client.enums.RequestType;
import ru.yandex.market.ff.client.enums.TimeSlotStatus;
import ru.yandex.market.ff.model.bo.AvailableRequestSize;
import ru.yandex.market.ff.model.bo.AvailableRequestSizeDefault;
import ru.yandex.market.ff.model.bo.RequestTypeAndSupplierId;
import ru.yandex.market.ff.model.converter.BookedTimeSlotConverter;
import ru.yandex.market.ff.model.entity.RequestSubTypeEntity;
import ru.yandex.market.ff.model.entity.ShopRequest;
import ru.yandex.market.ff.model.entity.SlotSize;
import ru.yandex.market.ff.model.entity.SlotSizeProperty;
import ru.yandex.market.ff.model.entity.Supplier;
import ru.yandex.market.ff.repository.BookedTimeSlotsRepository;
import ru.yandex.market.ff.repository.ShopRequestRepository;
import ru.yandex.market.ff.repository.ShopRequestValidatedDatesRepository;
import ru.yandex.market.ff.repository.implementation.ShopRequestJdbcRepository;
import ru.yandex.market.ff.service.ConcreteEnvironmentParamService;
import ru.yandex.market.ff.service.DateTimeService;
import ru.yandex.market.ff.service.LimitService;
import ru.yandex.market.ff.service.RealSupplierService;
import ru.yandex.market.ff.service.RequestItemAttributeService;
import ru.yandex.market.ff.service.RequestSizeCalculationService;
import ru.yandex.market.ff.service.RequestSubTypeService;
import ru.yandex.market.ff.service.SlotDurationService;
import ru.yandex.market.ff.service.TakenLimitsByRequestService;
import ru.yandex.market.ff.service.timeslot.CalendarBookingService;
import ru.yandex.market.ff.service.timeslot.TimeSlotsServiceImpl;
import ru.yandex.market.ff.util.WarehouseDateTimeUtils;
import ru.yandex.market.logistics.calendaring.client.dto.FreeSlotsResponse;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class TimeSlotsServiceImplTest extends SoftAssertionSupport {

    private static final long MOCK_WH_ID = 1337;
    private static final long REQUEST_ID = 12345;
    private static final long LONG_REQUEST_ID = 123456;
    private static final long THIRD_PARTY_REQUEST_ID = 1234567;
    private static final long SHADOW_SUPPLY_REQUEST_ID = 1234568;
    private static final long REQUEST_WITH_BOOKED_SLOT_ID = 200;
    private static final long INFOR_EKB_ID = 300;
    private static final LocalDate DEFAULT_DATE = LocalDate.of(2019, 10, 10);
    private static final LocalDateTime DATE_TIME_NOW = LocalDateTime.of(DEFAULT_DATE, LocalTime.of(0, 1));
    private static final LocalDateTime START_DATE_TIME = LocalDateTime.of(DEFAULT_DATE, LocalTime.of(0, 0));
    private static final LocalDateTime FINISH_DATE_TIME = LocalDateTime.of(DEFAULT_DATE, LocalTime.of(4, 0));
    private static final int SUPPLIER_RATING = 50;

    private final BookedTimeSlotsRepository repository = mock(BookedTimeSlotsRepository.class);
    private final CalendaringServiceClientWrapperServiceImpl csClientWrapperService =
            mock(CalendaringServiceClientWrapperServiceImpl.class);
    private final ConcreteEnvironmentParamService environmentParamService = mock(ConcreteEnvironmentParamService.class);
    private final RequestSizeCalculationService requestSizeCalculationService =
            mock(RequestSizeCalculationService.class);
    private final LimitService limitService = mock(LimitService.class);
    private final ShopRequestRepository shopRequestRepository = mock(ShopRequestRepository.class);
    private final TakenLimitsByRequestService takenLimitsByRequestService = mock(TakenLimitsByRequestService.class);
    private final DateTimeService dateTimeService = mock(DateTimeService.class);

    private final ShopRequestValidatedDatesRepository shopRequestValidatedDatesRepository =
            mock(ShopRequestValidatedDatesRepository.class);

    private final ShopRequestJdbcRepository shopRequestJdbcRepository = mock(ShopRequestJdbcRepository.class);

    private final CalendarBookingService calendarBookingService = mock(CalendarBookingService.class);

    private final PublishToLogbrokerCalendarShopRequestChangeService
            publishToLogbrokerCalendarShopRequestChangeService =
            mock(PublishToLogbrokerCalendarShopRequestChangeService.class);

    private final RequestItemAttributeService requestItemAttributeService = mock(RequestItemAttributeService.class);

    private final RealSupplierService realSupplierService = mock(RealSupplierService.class);
    private final BookedTimeSlotConverter bookedTimeSlotConverter = mock(BookedTimeSlotConverter.class);
    private final RequestSubTypeService requestSubTypeService = mock(RequestSubTypeService.class);
    private final SlotDurationService slotDurationService = new SlotDurationServiceImpl(requestSubTypeService,
            requestItemAttributeService);

    private final TimeSlotsServiceImpl service = new TimeSlotsServiceImpl(
            repository, csClientWrapperService, dateTimeService,
            requestSizeCalculationService,
            shopRequestRepository,
            takenLimitsByRequestService, shopRequestValidatedDatesRepository,
            publishToLogbrokerCalendarShopRequestChangeService,
            realSupplierService,
            calendarBookingService,
            bookedTimeSlotConverter,
            slotDurationService);

    private final ShopRequest shadowSupplyRequest = new ShopRequest();

    @BeforeEach
    public void prepare() {
        when(environmentParamService.getCalendaringStep()).thenReturn(30);
        when(environmentParamService.shouldFilterCalendaringIntervalsBySupplierRating()).thenReturn(false);
        AvailableRequestSize availableRequestSize = new AvailableRequestSizeDefault();
        when(limitService
                .getAvailableRequestSizeForDate(any(), anyLong(), anyCollection(), any(RequestType.class)))
                .thenReturn(ImmutableMap.of(DEFAULT_DATE, availableRequestSize));
        Supplier firstPartySupplier = new Supplier();
        firstPartySupplier.setSupplierType(SupplierType.FIRST_PARTY);
        Supplier thirdPartySupplier = new Supplier();
        thirdPartySupplier.setSupplierType(SupplierType.THIRD_PARTY);
        ShopRequest request = new ShopRequest();
        request.setId(REQUEST_ID);
        request.setServiceId(MOCK_WH_ID);
        request.setSupplier(firstPartySupplier);
        request.setCalendaringMode(CalendaringMode.REQUIRED);
        request.setRequestedDate(START_DATE_TIME);
        request.setType(RequestType.SHADOW_SUPPLY);

        ShopRequest longRequest = new ShopRequest();
        longRequest.setId(LONG_REQUEST_ID);
        longRequest.setServiceId(MOCK_WH_ID);
        longRequest.setSupplier(firstPartySupplier);
        longRequest.setCalendaringMode(CalendaringMode.AUTO);
        longRequest.setRequestedDate(START_DATE_TIME);
        longRequest.setSupplierRating(SUPPLIER_RATING);
        longRequest.setType(RequestType.SHADOW_SUPPLY);

        ShopRequest thirdPartyRequest = new ShopRequest();
        thirdPartyRequest.setId(THIRD_PARTY_REQUEST_ID);
        thirdPartyRequest.setServiceId(MOCK_WH_ID);
        thirdPartyRequest.setSupplier(thirdPartySupplier);
        thirdPartyRequest.setRequestedDate(START_DATE_TIME);
        thirdPartyRequest.setSupplierRating(SUPPLIER_RATING);
        thirdPartyRequest.setType(RequestType.SHADOW_SUPPLY);


        shadowSupplyRequest.setId(SHADOW_SUPPLY_REQUEST_ID);
        shadowSupplyRequest.setSupplier(thirdPartySupplier);
        shadowSupplyRequest.setType(RequestType.SHADOW_SUPPLY);

        when(shopRequestJdbcRepository.getRequestsTypeWithSuppliers(Set.of(REQUEST_WITH_BOOKED_SLOT_ID)))
                .thenReturn(Map.of(REQUEST_WITH_BOOKED_SLOT_ID,
                        RequestTypeAndSupplierId.builder().requestType(RequestType.SHADOW_SUPPLY).supplierId(10L)
                                .build()));

        when(requestSizeCalculationService.countTakenPallets(REQUEST_ID)).thenReturn(1L);
        when(requestSizeCalculationService.countTakenPallets(LONG_REQUEST_ID)).thenReturn(22L);
        when(requestSizeCalculationService.countTakenPallets(THIRD_PARTY_REQUEST_ID)).thenReturn(55L);
        when(requestSizeCalculationService.countTakenPallets(SHADOW_SUPPLY_REQUEST_ID)).thenReturn(55L);

        when(dateTimeService.localDateTimeNow()).thenReturn(DATE_TIME_NOW);

        RequestSubTypeEntity requestSubTypeEntity = new RequestSubTypeEntity();
        requestSubTypeEntity.setSlotSizeProperty(new SlotSizeProperty(false,
                List.of(new SlotSize(0, 30), new SlotSize(20, 60))));
        when(requestSubTypeService.getEntityByRequestTypeAndSubtype(any())).thenReturn(requestSubTypeEntity);
        when(requestItemAttributeService.checkAttribute(anyLong(), eq(RequestItemAttribute.CTM))).thenReturn(false);
    }

    @AfterEach
    public void validateMocks() {
        verifyNoMoreInteractions(csClientWrapperService);
    }

    @Test
    public void happyPathForWarehouseInAnotherTimeZone() {
        long warehouseId = INFOR_EKB_ID;
        long bookingId = 123L;
        long gateId = 1L;
        LocalDateTime slotCreatedAtExpected = WarehouseDateTimeUtils.fromMoscowToWarehouseLocalDate(
                DATE_TIME_NOW, warehouseId
        );

        BookedTimeSlotDto slot = new BookedTimeSlotDto();
        slot.setBookingId(bookingId);
        slot.setServiceId(warehouseId);
        slot.setGateId(gateId);
        slot.setStatus(TimeSlotStatus.UPDATING);
        slot.setFrom(START_DATE_TIME);
        slot.setTo(FINISH_DATE_TIME);

        service.upsertTimeSlotWithoutCheck(slot);

        verify(repository).upsertTimeSlotWithoutCheck(
                bookingId,
                warehouseId,
                gateId,
                slotCreatedAtExpected,
                START_DATE_TIME,
                FINISH_DATE_TIME,
                TimeSlotStatus.UPDATING.getId()
        );
    }

    @Test
    public void deleteSlotsForRequestWorksCorrect() {
        doNothing().when(repository).deactivateAllByRequestIdIn(anyCollection());

        service.deactivateSlotsForRequests(Set.of(10L));
        verify(repository, times(1)).deactivateAllByRequestIdIn(Set.of(10L));
        verify(csClientWrapperService, times(1)).putCancelledBookedSlot(10L);
    }

    @Test
    public void testUseCalendaringWithEmptyServiceId() {
        when(requestSizeCalculationService.countTakenItems(anyLong())).thenReturn(11L);
        when(requestSizeCalculationService.countTakenPallets(anyLong())).thenReturn(0L);
        when(csClientWrapperService.getFreeSlots(any())).thenReturn(new FreeSlotsResponse(new ArrayList<>()));
        ShopRequest shopRequest = getShopRequest(2L, null, CalendaringMode.REQUIRED);
        Supplier firstPartySupplier = new Supplier();
        firstPartySupplier.setSupplierType(SupplierType.FIRST_PARTY);
        shopRequest.setSupplier(firstPartySupplier);

        service.getFreeSlotsDates(shopRequest, 1L, 60, ofHours(10), ofHours(11));
        verify(csClientWrapperService).getFreeSlots(any());
    }

    private static ShopRequest getShopRequest(long id, Long serviceId, CalendaringMode calendaringMode) {
        return getShopRequest(id, serviceId, calendaringMode, LocalDateTime.now());
    }

    private static ShopRequest getShopRequest(long id, Long serviceId, CalendaringMode calendaringMode,
                                              LocalDateTime requestedDate) {
        ShopRequest request = new ShopRequest();
        request.setId(id);
        request.setServiceId(serviceId);
        request.setRequestedDate(requestedDate);
        request.setCalendaringMode(calendaringMode);
        request.setType(RequestType.SHADOW_SUPPLY);
        Supplier supplier = new Supplier();
        supplier.setSupplierType(SupplierType.THIRD_PARTY);
        request.setSupplier(supplier);
        return request;
    }

    private static LocalDateTime ofHours(int hours) {
        return START_DATE_TIME.plusHours(hours);
    }
}
