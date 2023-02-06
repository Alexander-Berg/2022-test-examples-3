package ru.yandex.market.tsup.service.data_provider.primitive.external.calendaring_service.search;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.calendaring.client.CalendaringServiceClientApi;
import ru.yandex.market.logistics.calendaring.client.dto.FreeSlotsForDayResponse;
import ru.yandex.market.logistics.calendaring.client.dto.FreeSlotsResponse;
import ru.yandex.market.logistics.calendaring.client.dto.GetFreeSlotsRequest;
import ru.yandex.market.logistics.calendaring.client.dto.TimeSlotResponse;
import ru.yandex.market.logistics.calendaring.client.dto.WarehouseFreeSlotsResponse;
import ru.yandex.market.logistics.calendaring.client.dto.enums.BookingType;
import ru.yandex.market.logistics.calendaring.client.dto.enums.SupplierType;
import ru.yandex.market.tsup.AbstractContextualTest;
import ru.yandex.market.tsup.service.data_provider.entity.calendaring.TimeSlotDto;

class SlotSearchDataProviderTest extends AbstractContextualTest {
    public static final LocalDate DATE = LocalDate.of(2022, 1, 25);

    @Autowired
    private SlotSearchDataProvider slotSearchDataProvider;

    @Autowired
    private CalendaringServiceClientApi calendaringServiceClientApi;

    @Test
    void provide() {
        SlotSearchProviderFilter filter = SlotSearchProviderFilter.builder()
            .partnerId(172)
            .slotDurationMinutes(60)
            .supplierType(SupplierType.FIRST_PARTY)
            .bookingType(BookingType.XDOCK_TRANSPORT_WITHDRAW)
            .from(DATE.atTime(10, 0))
            .to(DATE.plusDays(1).atTime(11, 0))
            .takenPallets(1)
            .takenItems(2)
            .build();

        Mockito.when(calendaringServiceClientApi.getFreeSlots(
            Mockito.argThat(new GetFreeSlotsRequestArgumentMatcher(filter))
        )).thenReturn(new FreeSlotsResponse(
            List.of(
                new WarehouseFreeSlotsResponse(172, List.of(
                    new FreeSlotsForDayResponse(DATE, ZoneOffset.UTC, List.of(
                        new TimeSlotResponse(LocalTime.of(10, 0), LocalTime.of(11, 0)),
                        new TimeSlotResponse(LocalTime.of(13, 0), LocalTime.of(14, 0)),
                        new TimeSlotResponse(LocalTime.of(14, 0), LocalTime.of(15, 0))
                    )),
                    new FreeSlotsForDayResponse(DATE.plusDays(1), ZoneOffset.UTC, List.of(
                        new TimeSlotResponse(LocalTime.of(10, 0), LocalTime.of(11, 0))
                    ))
                ))
            )
        ));

        List<TimeSlotDto> actual = slotSearchDataProvider.provide(
            filter,
            null
        );

        softly.assertThat(actual).containsExactly(
            new TimeSlotDto(DATE, LocalTime.of(10, 0), LocalTime.of(11, 0), ZoneOffset.UTC),
            new TimeSlotDto(DATE, LocalTime.of(13, 0), LocalTime.of(14, 0), ZoneOffset.UTC),
            new TimeSlotDto(DATE, LocalTime.of(14, 0), LocalTime.of(15, 0), ZoneOffset.UTC),
            new TimeSlotDto(DATE.plusDays(1), LocalTime.of(10, 0), LocalTime.of(11, 0), ZoneOffset.UTC)
        );

        Mockito
            .verify(calendaringServiceClientApi)
            .getFreeSlots(Mockito.argThat(new GetFreeSlotsRequestArgumentMatcher(filter)));
        Mockito.verifyNoMoreInteractions(calendaringServiceClientApi);
    }

    @RequiredArgsConstructor
    private static class GetFreeSlotsRequestArgumentMatcher implements ArgumentMatcher<GetFreeSlotsRequest> {
        private final SlotSearchProviderFilter slotSearchProviderFilter;

        @Override
        public boolean matches(GetFreeSlotsRequest argument) {
            return argument.getBookingType().equals(slotSearchProviderFilter.getBookingType()) &&
                argument.getSupplierType().equals(slotSearchProviderFilter.getSupplierType()) &&
                argument.getFrom().equals(slotSearchProviderFilter.getFrom()) &&
                argument.getTo().equals(slotSearchProviderFilter.getTo()) &&
                argument.getWarehouseIds().equals(Set.of(slotSearchProviderFilter.getPartnerId())) &&
                argument.getTakenPallets() == slotSearchProviderFilter.getTakenPallets() &&
                argument.getTakenItems() == slotSearchProviderFilter.getTakenItems() &&
                argument.getSlotDurationMinutes() == slotSearchProviderFilter.getSlotDurationMinutes();
        }
    }
}
