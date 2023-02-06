package ru.yandex.market.ff.dbqueue.service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.model.dbqueue.ModificationConsolidatedShippingPayload;
import ru.yandex.market.ff.service.DateTimeService;
import ru.yandex.market.logistics.calendaring.client.CalendaringServiceClientApi;
import ru.yandex.market.logistics.calendaring.client.dto.BookingListResponseV2;
import ru.yandex.market.logistics.calendaring.client.dto.BookingResponseV2;
import ru.yandex.market.logistics.calendaring.client.dto.DecreaseConsolidatedSlotRequest;
import ru.yandex.market.logistics.calendaring.client.dto.enums.BookingStatus;

import static org.mockito.ArgumentMatchers.any;

public class ModificationConsolidatedShippingProcessingServiceTest extends IntegrationTest {
    @Autowired
    private ModificationConsolidatedShippingProcessingService service;

    @Autowired
    private CalendaringServiceClientApi calendaringServiceClientApi;

    @Autowired
    private DateTimeService dateTimeService;

    @Test
    @DatabaseSetup("classpath:db-queue/service/modification-consolidated-shipping/1/before.xml")
    @ExpectedDatabase(value = "classpath:db-queue/service/modification-consolidated-shipping/1/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void processPayloadOneShopRequest() {
        service.processPayload(new ModificationConsolidatedShippingPayload(1));
    }


    @Test
    @DatabaseSetup("classpath:db-queue/service/modification-consolidated-shipping/2/before.xml")
    @ExpectedDatabase(value = "classpath:db-queue/service/modification-consolidated-shipping/2/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void processPayloadDecreaseConsolidatedShippingSlot() {
        ZonedDateTime now = dateTimeService.localDateTimeNow().atZone(ZoneId.of("Europe/Moscow"));
        Mockito.doReturn(
                new BookingListResponseV2(
                        List.of(new BookingResponseV2(
                                2,
                                "FFWF",
                                "2",
                                null,
                                1,
                                now,
                                now.plusMinutes(60),
                                BookingStatus.ACTIVE,
                                null,
                                1
                        ))
                )
        ).when(calendaringServiceClientApi).getBookingsByIdsV2(any(), any());
        service.processPayload(new ModificationConsolidatedShippingPayload(1));


        ArgumentCaptor<DecreaseConsolidatedSlotRequest> captor =
                ArgumentCaptor.forClass(DecreaseConsolidatedSlotRequest.class);

        Mockito.verify(calendaringServiceClientApi).decreaseConsolidatedSlot(
                captor.capture()
        );
        Assertions.assertEquals(captor.getValue().getBookings().size(), 1);
        Assertions.assertEquals(captor.getValue().getBookings().get(0).getId(), 200);
        Assertions.assertEquals(captor.getValue().getTo(), now.toLocalDateTime().plusMinutes(30));
    }


    @Test
    @DatabaseSetup("classpath:db-queue/service/modification-consolidated-shipping/3/before.xml")
    @ExpectedDatabase(value = "classpath:db-queue/service/modification-consolidated-shipping/3/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void processPayloadNoDecreaseConsolidatedShippingSlot() {
        ZonedDateTime now = dateTimeService.localDateTimeNow().atZone(ZoneId.of("Europe/Moscow"));
        Mockito.doReturn(
                new BookingListResponseV2(
                        List.of(new BookingResponseV2(
                                2,
                                "FFWF",
                                "2",
                                null,
                                1,
                                now,
                                now.plusMinutes(30),
                                BookingStatus.ACTIVE,
                                null,
                                1
                        ))
                )
        ).when(calendaringServiceClientApi).getBookingsByIdsV2(any(), any());
        service.processPayload(new ModificationConsolidatedShippingPayload(1));
        Mockito.verify(calendaringServiceClientApi, Mockito.never()).decreaseConsolidatedSlot(any());
    }

}
