package ru.yandex.market.tsup.service.calendaring;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.market.tsup.service.data_provider.entity.calendaring.TimeSlotDto;
import ru.yandex.market.tsup.service.data_provider.primitive.external.calendaring_service.search.SlotSearchDataProvider;
import ru.yandex.market.tsup.service.data_provider.primitive.external.calendaring_service.search.SlotSearchProviderFilter;

class CalendaringSlotSelectorTest {

    @Test
    void findSlots() {
        LocalDateTime firstSlotStart = TestData.DATE.atTime(TestData.POINT_PARAMS_1.getArrivalStartTime());
        LocalDateTime firstSlotEnd = TestData.DATE.atTime(TestData.POINT_PARAMS_1.getArrivalEndTime());

        SlotSearchDataProvider slotSearchDataProvider = Mockito.mock(SlotSearchDataProvider.class);
        CalendaringSlotSelector slotSelector = new CalendaringSlotSelector(slotSearchDataProvider);

        SlotSearchProviderFilter searchProviderFilter1_2 = new SlotSearchProviderFilter(
            TestData.BOOKING_INFO_1_2.getPartnerId(),
            TestData.BOOKING_INFO_1_2.getBookingType(),
            TestData.BOOKING_INFO_1_2.getTimeSlotDurationMinutes(),
            firstSlotStart,
            firstSlotEnd,
            TestData.BOOKING_INFO_1_2.getSupplierType(),
            TestData.BOOKING_INFO_1_2.getTakenPallets(),
            TestData.BOOKING_INFO_1_2.getTakenPallets()
        );

        SlotSearchProviderFilter searchProviderFilter3 = new SlotSearchProviderFilter(
            TestData.BOOKING_INFO_3.getPartnerId(),
            TestData.BOOKING_INFO_3.getBookingType(),
            TestData.BOOKING_INFO_3.getTimeSlotDurationMinutes(),
            // Последний слот погрузки + 24 часа трансфер + 1 час на погрузку
            searchProviderFilter1_2.getFrom().plusHours(25),
            searchProviderFilter1_2.getFrom().plusHours(25).plusHours(6),
            TestData.BOOKING_INFO_3.getSupplierType(),
            TestData.BOOKING_INFO_3.getTakenPallets(),
            TestData.BOOKING_INFO_3.getTakenPallets()
        );

        SlotSearchProviderFilter searchProviderFilter4 = new SlotSearchProviderFilter(
            TestData.BOOKING_INFO_4.getPartnerId(),
            TestData.BOOKING_INFO_4.getBookingType(),
            TestData.BOOKING_INFO_4.getTimeSlotDurationMinutes(),
            // Слот выгрузки + 4 часа трансфер + 1 час выгрузка + 30 минут - ожидание слота
            searchProviderFilter3.getFrom().plusHours(5).plusMinutes(30),
            searchProviderFilter3.getFrom().plusHours(5).plusMinutes(30).plusHours(6),
            TestData.BOOKING_INFO_4.getSupplierType(),
            TestData.BOOKING_INFO_4.getTakenPallets(),
            TestData.BOOKING_INFO_4.getTakenPallets()
        );

        Mockito.when(slotSearchDataProvider.provide(
                Mockito.eq(searchProviderFilter1_2),
                Mockito.isNull()
            ))
            .thenReturn(List.of(
                TestData.SLOT_1_2,
                plus(TestData.SLOT_1_2, 0, 30),
                plus(TestData.SLOT_1_2, 0, 30),
                plus(TestData.SLOT_1_2, 3, 0)
            ));

        Mockito.when(slotSearchDataProvider.provide(
                Mockito.eq(searchProviderFilter3),
                Mockito.isNull()
            ))
            .thenReturn(List.of(
                TestData.SLOT_3,
                plus(TestData.SLOT_3, 3, 0)
            ));

        Mockito.when(slotSearchDataProvider.provide(
                Mockito.eq(searchProviderFilter4),
                Mockito.isNull()
            ))
            .thenReturn(List.of(
                TestData.SLOT_4,
                plus(TestData.SLOT_4, 3, 0)
            ));

        Map<Integer, TimeSlotDto> actual = slotSelector.findSlots(
            List.of(
                TestData.BOOKING_INFO_1_2,
                TestData.BOOKING_INFO_3,
                TestData.BOOKING_INFO_4
            ),
            Pair.of(
                firstSlotStart,
                firstSlotEnd
            )
        );

        Map<Integer, TimeSlotDto> expected = Map.of(
            0, TestData.SLOT_1_2,
            1, TestData.SLOT_1_2,
            2, TestData.SLOT_3,
            3, TestData.SLOT_4
        );

        Assertions.assertEquals(
            expected,
            actual
        );

        Mockito.verify(slotSearchDataProvider).provide(Mockito.eq(searchProviderFilter1_2), Mockito.isNull());
        Mockito.verify(slotSearchDataProvider).provide(Mockito.eq(searchProviderFilter3), Mockito.isNull());
        Mockito.verify(slotSearchDataProvider).provide(Mockito.eq(searchProviderFilter4), Mockito.isNull());
        Mockito.verifyNoMoreInteractions(slotSearchDataProvider);
    }

    private TimeSlotDto plus(TimeSlotDto slot, int h, int m) {
        return new TimeSlotDto(
            slot.getDate(),
            slot.getFrom().plusHours(h).plusMinutes(m),
            slot.getTo().plusHours(h).plusMinutes(m),
            slot.getZoneOffset()
        );
    }
}
