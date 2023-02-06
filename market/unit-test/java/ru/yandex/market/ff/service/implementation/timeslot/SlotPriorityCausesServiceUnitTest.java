package ru.yandex.market.ff.service.implementation.timeslot;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import ru.yandex.market.core.supplier.model.SupplierType;
import ru.yandex.market.ff.client.dto.BookedTimeSlotDto;
import ru.yandex.market.ff.client.enums.RequestType;
import ru.yandex.market.ff.model.bo.RequestTypeAndSupplierId;
import ru.yandex.market.ff.model.bo.SlotPriorityCause;
import ru.yandex.market.ff.model.dto.FreeTimeSlotDTO;
import ru.yandex.market.ff.model.entity.ShopRequest;
import ru.yandex.market.ff.model.entity.Supplier;
import ru.yandex.market.ff.repository.implementation.ShopRequestJdbcRepository;
import ru.yandex.market.ff.service.timeslot.CalendarBookingService;
import ru.yandex.market.ff.service.timeslot.SlotPriorityTypeService;
import ru.yandex.market.ff.service.timeslot.ThirdPartySlotPriorityCausesService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SlotPriorityCausesServiceUnitTest {

    private static final long GATE_ID = 1;
    private static final LocalDateTime DATE = LocalDateTime.of(2018, 1, 1, 9, 0, 0);
    private static final long REQUEST_ID = 2;
    private static final RequestType REQUEST_TYPE = RequestType.WITHDRAW;
    private static final ShopRequest REQUEST = getShopRequest();
    private static final BookedTimeSlotDto BOOKED_TIME_SLOT = getBookedTimeSlot(2, 3);
    private static final long SUPPLIER_ID = 10;

    private final ShopRequestJdbcRepository shopRequestJdbcRepository = mock(ShopRequestJdbcRepository.class);

    private final SlotPriorityTypeService slotPriorityTypeService = mock(SlotPriorityTypeService.class);
    private final CalendarBookingService calendarBookingService = mock(CalendarBookingService.class);
    private final ThirdPartySlotPriorityCausesService service =
            new ThirdPartySlotPriorityCausesService(slotPriorityTypeService,
                    shopRequestJdbcRepository);

    @BeforeEach
    void init() {
        when(shopRequestJdbcRepository.getRequestsTypeWithSuppliers(any()))
                .thenReturn(Map.of(REQUEST_ID,
                        RequestTypeAndSupplierId.builder().requestType(RequestType.WITHDRAW).supplierId(SUPPLIER_ID)
                                .build()));

        when(calendarBookingService.getRequest(any(), any())).thenReturn(REQUEST);

        when(calendarBookingService.getBookingIdToRequest(any())).thenReturn(Map.of(REQUEST_ID, REQUEST));
    }

    @Test
    void getPriorityCausesForSlotWhenFreeSlotBeforeBookedSlotTest() {

        FreeTimeSlotDTO freeTimeSlotDTO = new FreeTimeSlotDTO(GATE_ID, DATE.plusHours(1), DATE.plusHours(2));
        List<FreeTimeSlotDTO> freeSlots = List.of(freeTimeSlotDTO);

        List<BookedTimeSlotDto> bookedTimeSlots = List.of(BOOKED_TIME_SLOT);
        service.fillPriorityCauses(getShopRequest(RequestType.WITHDRAW), freeSlots, bookedTimeSlots);
        List<SlotPriorityCause> priorityCauses = freeSlots.get(0).getPriorityCauses();

        assertEquals(1, priorityCauses.size());
        assertEquals(REQUEST_ID, priorityCauses.get(0).getRequestId());
        assertEquals(REQUEST_TYPE, priorityCauses.get(0).getRequestType());
        assertEquals(BOOKED_TIME_SLOT.getFrom(), priorityCauses.get(0).getFrom());
        assertEquals(BOOKED_TIME_SLOT.getTo(), priorityCauses.get(0).getTo());
    }

    @Test
    void getPriorityCausesForSlotWhenFreeSlotAfterBookedSlotTest() {

        FreeTimeSlotDTO freeTimeSlotDTO = new FreeTimeSlotDTO(GATE_ID, DATE.plusHours(3), DATE.plusHours(4));
        List<FreeTimeSlotDTO> freeSlots = List.of(freeTimeSlotDTO);

        List<BookedTimeSlotDto> bookedTimeSlots = List.of(BOOKED_TIME_SLOT);
        service.fillPriorityCauses(getShopRequest(RequestType.WITHDRAW), freeSlots, bookedTimeSlots);
        List<SlotPriorityCause> priorityCauses = freeSlots.get(0).getPriorityCauses();

        assertEquals(1, priorityCauses.size());
        assertEquals(REQUEST_ID, priorityCauses.get(0).getRequestId());
        assertEquals(REQUEST_TYPE, priorityCauses.get(0).getRequestType());
        assertEquals(BOOKED_TIME_SLOT.getFrom(), priorityCauses.get(0).getFrom());
        assertEquals(BOOKED_TIME_SLOT.getTo(), priorityCauses.get(0).getTo());
    }

    @Test
    void getPriorityCausesForSlotWhenFreeSlotAtSameTimeWithBookedSlotTest() {

        FreeTimeSlotDTO freeTimeSlotDTO = new FreeTimeSlotDTO(GATE_ID, DATE.plusHours(2), DATE.plusHours(3));
        List<FreeTimeSlotDTO> freeSlots = List.of(freeTimeSlotDTO);

        List<BookedTimeSlotDto> bookedTimeSlots = List.of(BOOKED_TIME_SLOT);
        service.fillPriorityCauses(getShopRequest(RequestType.WITHDRAW), freeSlots, bookedTimeSlots);
        List<SlotPriorityCause> priorityCauses = freeSlots.get(0).getPriorityCauses();

        assertEquals(1, priorityCauses.size());
        assertEquals(REQUEST_ID, priorityCauses.get(0).getRequestId());
        assertEquals(REQUEST_TYPE, priorityCauses.get(0).getRequestType());
        assertEquals(BOOKED_TIME_SLOT.getFrom(), priorityCauses.get(0).getFrom());
        assertEquals(BOOKED_TIME_SLOT.getTo(), priorityCauses.get(0).getTo());
    }

    @Test
    void getPriorityCausesOrderByFromAndToTest() {

        FreeTimeSlotDTO freeTimeSlotDTO = new FreeTimeSlotDTO(GATE_ID, DATE.plusHours(2), DATE.plusHours(3));
        List<FreeTimeSlotDTO> freeSlots = List.of(freeTimeSlotDTO);

        BookedTimeSlotDto bookedTimeSlotSameTime = getBookedTimeSlot(2, 3);
        BookedTimeSlotDto bookedTimeSlotAfter = getBookedTimeSlot(3, 4);
        BookedTimeSlotDto bookedTimeSlotBefore = getBookedTimeSlot(1, 2);

        List<BookedTimeSlotDto> bookedTimeSlots =
                List.of(bookedTimeSlotSameTime, bookedTimeSlotAfter, bookedTimeSlotBefore);
        service.fillPriorityCauses(getShopRequest(RequestType.WITHDRAW), freeSlots, bookedTimeSlots);
        List<SlotPriorityCause> priorityCauses = freeSlots.get(0).getPriorityCauses();

        assertEquals(3, priorityCauses.size());
        assertEquals(REQUEST_ID, priorityCauses.get(0).getRequestId());
        assertEquals(REQUEST_ID, priorityCauses.get(1).getRequestId());
        assertEquals(REQUEST_ID, priorityCauses.get(2).getRequestId());
        assertEquals(REQUEST_TYPE, priorityCauses.get(0).getRequestType());
        assertEquals(REQUEST_TYPE, priorityCauses.get(1).getRequestType());
        assertEquals(REQUEST_TYPE, priorityCauses.get(2).getRequestType());
        assertEquals(bookedTimeSlotBefore.getFrom(), priorityCauses.get(0).getFrom());
        assertEquals(bookedTimeSlotBefore.getTo(), priorityCauses.get(0).getTo());
        assertEquals(bookedTimeSlotSameTime.getFrom(), priorityCauses.get(1).getFrom());
        assertEquals(bookedTimeSlotSameTime.getTo(), priorityCauses.get(1).getTo());
        assertEquals(bookedTimeSlotAfter.getFrom(), priorityCauses.get(2).getFrom());
        assertEquals(bookedTimeSlotAfter.getTo(), priorityCauses.get(2).getTo());
    }

    private static ShopRequest getShopRequest() {
        return getShopRequest(REQUEST_TYPE);
    }

    private static ShopRequest getShopRequest(RequestType requestType) {
        ShopRequest request = new ShopRequest();
        request.setId(REQUEST_ID);
        request.setType(requestType);
        Supplier supplier = new Supplier();
        supplier.setSupplierType(SupplierType.THIRD_PARTY);
        supplier.setId(SUPPLIER_ID);
        request.setSupplier(supplier);
        return request;
    }

    private static BookedTimeSlotDto getBookedTimeSlot(int i, int i2) {
        BookedTimeSlotDto bookedTimeSlot = new BookedTimeSlotDto();
        bookedTimeSlot.setFrom(DATE.plusHours(i));
        bookedTimeSlot.setTo(DATE.plusHours(i2));
        bookedTimeSlot.setGateId(GATE_ID);
        bookedTimeSlot.setRequestId(REQUEST.getId());
        return bookedTimeSlot;
    }

}
