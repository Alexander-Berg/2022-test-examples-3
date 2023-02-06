package ru.yandex.market.tsup.service.calendaring;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import one.util.streamex.StreamEx;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentMatcher;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.common.util.functional.Function;
import ru.yandex.market.logistics.calendaring.client.CalendaringServiceClientApi;
import ru.yandex.market.logistics.calendaring.client.dto.BookSlotRequest;
import ru.yandex.market.logistics.calendaring.client.dto.BookSlotResponse;
import ru.yandex.market.tsup.config.external.calendaring_service.CalendaringServiceConfig;
import ru.yandex.market.tsup.service.data_provider.entity.calendaring.BookedTimeSlotDto;
import ru.yandex.market.tsup.service.data_provider.entity.calendaring.PointBookingInfoDto;
import ru.yandex.market.tsup.service.data_provider.entity.calendaring.TimeSlotDto;
import ru.yandex.market.tsup.service.tsup_property.TsupPropertyService;

import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class CalendaringSlotBookingProcessorTest {
    public static final ZoneId TIME_ZONE = ZoneId.of("Europe/Moscow");
    private TestableClock clock;

    private TsupPropertyService propertyService;

    @BeforeEach
    void setUp() {
        propertyService = mock(TsupPropertyService.class);

        clock = new TestableClock();
        clock.setFixed(Instant.parse("2021-01-01T12:00:00.00Z"), TIME_ZONE);
    }

    @DisplayName("Проверка брони слотов на складах")
    @ParameterizedTest
    @MethodSource("slotExpirationSupported")
    void bookSlots(boolean slotExpirationSupported) {
        when(propertyService.bookedSlotReservationTimeMinutes())
            .thenReturn(Optional.of(30).filter(t -> slotExpirationSupported));

        CalendaringServiceClientApi csClient = mock(CalendaringServiceClientApi.class);
        CalendaringSlotBookingProcessor bookingProcessor =
            new CalendaringSlotBookingProcessor(csClient, propertyService, clock);

        BookSlotRequestListArgumentMatcher bookSlotRequestListArgumentMatcher = new BookSlotRequestListArgumentMatcher(
            List.of(
                Pair.of(TestData.BOOKING_INFO_1_2, TestData.SLOT_1_2),
                Pair.of(TestData.BOOKING_INFO_3, TestData.SLOT_3),
                Pair.of(TestData.BOOKING_INFO_4, TestData.SLOT_4)
            ),
            4,
            slotExpirationSupported
        );

        when(csClient.bookSlots(argThat(bookSlotRequestListArgumentMatcher)))
            .thenReturn(List.of(
                new BookSlotResponse(
                    1L, 10L, zonedDateTime(TestData.DATE, 10, 0), zonedDateTime(TestData.DATE, 11, 0)
                ),
                new BookSlotResponse(
                    1L, 10L, zonedDateTime(TestData.DATE, 10, 0), zonedDateTime(TestData.DATE, 11, 0)
                ),
                new BookSlotResponse(
                    3L, 5L, zonedDateTime(TestData.DATE, 11, 0), zonedDateTime(TestData.DATE.plusDays(1), 11, 30)
                ),
                new BookSlotResponse(
                    4L, 1L, zonedDateTime(TestData.DATE, 15, 30), zonedDateTime(TestData.DATE.plusDays(1), 16, 0)
                )
            ));

        Map<Integer, BookedTimeSlotDto> actual = bookingProcessor.bookSlots(
            List.of(
                TestData.BOOKING_INFO_1_2,
                TestData.BOOKING_INFO_3,
                TestData.BOOKING_INFO_4
            ),
            Map.of(
                0, TestData.SLOT_1_2,
                1, TestData.SLOT_1_2,
                2, TestData.SLOT_3,
                3, TestData.SLOT_4
            )
        );

        Map<Integer, BookedTimeSlotDto> expected = Map.of(
            0,
            new BookedTimeSlotDto(1L, 10L, zonedDateTime(TestData.DATE, 10, 0), zonedDateTime(TestData.DATE, 11, 0)),
            1,
            new BookedTimeSlotDto(1L, 10L, zonedDateTime(TestData.DATE, 10, 0), zonedDateTime(TestData.DATE, 11, 0)),
            2,
            new BookedTimeSlotDto(
                3L,
                5L,
                zonedDateTime(TestData.DATE, 11, 0),
                zonedDateTime(TestData.DATE.plusDays(1), 11, 30)
            ),
            3,
            new BookedTimeSlotDto(
                4L,
                1L,
                zonedDateTime(TestData.DATE, 15, 30),
                zonedDateTime(TestData.DATE.plusDays(1), 16, 0)
            )
        );

        Assertions.assertEquals(expected, actual);

        verify(csClient).bookSlots(argThat(bookSlotRequestListArgumentMatcher));
        verifyNoMoreInteractions(csClient);
    }

    @Nonnull
    private ZonedDateTime zonedDateTime(LocalDate date, int hour, int minute) {
        return date.atTime(hour, minute).atOffset(TestData.ZONE_OFFSET).toZonedDateTime();
    }

    @RequiredArgsConstructor
    private static class BookSlotRequestListArgumentMatcher implements ArgumentMatcher<List<BookSlotRequest>> {
        private final List<Pair<PointBookingInfoDto, TimeSlotDto>> initialData;
        private final int count;
        private final boolean slotExpirationSupported;
        private final ObjectMapper om = new ObjectMapper();

        @Override
        public boolean matches(List<BookSlotRequest> slotRequest) {
            return slotRequest.size() == count &&
                StreamEx.of(initialData)
                    .mapToEntry(Pair::getLeft, Function.identity())
                    .mapKeys(PointBookingInfoDto::getIndexes)
                    .flatMapKeys(Collection::stream)
                    .allMatch(entry -> this.matchesSingle(entry.getValue().getLeft(), entry.getValue().getRight(),
                            slotRequest.get(entry.getKey())));
                    //.allMatch((index, d) -> this.matchesSingle(d.getLeft(), d.getRight(), slotRequest.get(index)));
        }

        @SneakyThrows
        private boolean matchesSingle(
            PointBookingInfoDto bookingInfo,
            TimeSlotDto slot,
            BookSlotRequest slotRequest
        ) {
            // Don't compare this fields. Set to expected value.
            String externalId = slotRequest.getExternalId();
            String tripExternalId = slotRequest.getTripExternalId();

            String actual = om.writeValueAsString(slotRequest);
            String expected = om.writeValueAsString(new BookSlotRequest(
                bookingInfo.getSupplierType(),
                null,
                bookingInfo.getPartnerId(),
                null,
                bookingInfo.getBookingType(),
                slot.getFromDateTime(),
                slot.getToDateTime(),
                externalId,
                CalendaringServiceConfig.SOURCE,
                tripExternalId,
                bookingInfo.getTakenItems(),
                bookingInfo.getTakenPallets(),
                Map.of(),
                null,
                false,
                slotExpirationSupported
                    ? ZonedDateTime.of(2021, 1, 1, 15, 30, 0, 0, TIME_ZONE)
                    : null
            ));
            return expected.equals(actual);
        }
    }

    static Stream<Arguments> slotExpirationSupported() {
        return Stream.of(
            Arguments.of(true),
            Arguments.of(false)
        );
    }
}
