package ru.yandex.market.ff.service.implementation;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import ru.yandex.market.core.supplier.model.SupplierType;
import ru.yandex.market.ff.base.SoftAssertionSupport;
import ru.yandex.market.ff.client.enums.CalendaringMode;
import ru.yandex.market.ff.client.enums.RequestType;
import ru.yandex.market.ff.dbqueue.producer.UpdateCalendaringExternalIdQueueProducer;
import ru.yandex.market.ff.model.dbqueue.UpdateCalendaringExternalIdPayload;
import ru.yandex.market.ff.model.entity.CalendarBookingEntity;
import ru.yandex.market.ff.model.entity.ShopRequest;
import ru.yandex.market.ff.model.entity.Supplier;
import ru.yandex.market.ff.repository.CalendarBookingRepository;
import ru.yandex.market.ff.service.timeslot.ChangeActiveTimeSlotService;
import ru.yandex.market.ff.service.timeslot.ChangeActiveTimeSlotServiceImpl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ChangeActiveTimeSlotServiceImplTest extends SoftAssertionSupport {

    private static final long BOOKING_ID = 300;
    private static final long INFOR_EKB_ID = 300;

    private final CalendarBookingRepository calendarBookingRepository = mock(CalendarBookingRepository.class);
    private final UpdateCalendaringExternalIdQueueProducer updateCalendaringExternalIdQueueProducer = mock(
            UpdateCalendaringExternalIdQueueProducer.class
    );

    private final ChangeActiveTimeSlotService service = new ChangeActiveTimeSlotServiceImpl(
            calendarBookingRepository,
            updateCalendaringExternalIdQueueProducer
    );

    @Test
    public void createBasedOnShadowHappyPathForWarehouseInAnotherTimeZoneWithCsBooking() {
        long warehouseId = INFOR_EKB_ID;
        long requestIdActive = 2L;
        long requestIdNew = 3L;

        ShopRequest currentActiveSlotHolder = getShopRequest(
                requestIdActive, warehouseId, CalendaringMode.REQUIRED
        );
        ShopRequest newSlotHolder = getShopRequest(
                requestIdNew, warehouseId, CalendaringMode.REQUIRED
        );

        when(calendarBookingRepository.findByRequestIdForUpdate(requestIdActive))
                .thenReturn(Optional.of(
                        new CalendarBookingEntity(BOOKING_ID, currentActiveSlotHolder, LocalDateTime.now(), false)));

        service.changeActiveSlotHolder(currentActiveSlotHolder, newSlotHolder);

        verify(updateCalendaringExternalIdQueueProducer).produceSingle(new UpdateCalendaringExternalIdPayload(
                BOOKING_ID,
                currentActiveSlotHolder.getId(),
                newSlotHolder.getId()
        ));

        verify(calendarBookingRepository, never()).save(any(CalendarBookingEntity.class));
    }

    private static ShopRequest getShopRequest(long id, long serviceId, CalendaringMode calendaringMode) {
        return getShopRequest(id, serviceId, calendaringMode, LocalDateTime.now());
    }

    private static ShopRequest getShopRequest(long id, long serviceId, CalendaringMode calendaringMode,
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
}
