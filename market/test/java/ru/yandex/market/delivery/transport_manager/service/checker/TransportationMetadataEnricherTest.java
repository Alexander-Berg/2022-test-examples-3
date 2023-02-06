package ru.yandex.market.delivery.transport_manager.service.checker;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.delivery.transport_manager.converter.TimeSlotConverter;
import ru.yandex.market.delivery.transport_manager.domain.entity.TimeSlot;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnit;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnitType;
import ru.yandex.market.delivery.transport_manager.service.cache.CacheService;
import ru.yandex.market.delivery.transport_manager.service.external.cs.CalendaringServiceReceiver;
import ru.yandex.market.delivery.transport_manager.service.external.lms.LogisticsPointReceiver;
import ru.yandex.market.delivery.transport_manager.service.external.lms.PartnerReceiver;
import ru.yandex.market.delivery.transport_manager.service.external.lms.PartnerSettingsReceiver;
import ru.yandex.market.delivery.transport_manager.service.external.marketd.LegalInfoReceiver;
import ru.yandex.market.logistics.calendaring.client.dto.BookingResponseV2;
import ru.yandex.market.logistics.calendaring.client.dto.enums.BookingStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class TransportationMetadataEnricherTest {

    public static final ZonedDateTime FROM = ZonedDateTime.of(2022, 1, 1, 12, 0, 0, 0, ZoneOffset.UTC);
    public static final ZonedDateTime TO = ZonedDateTime.of(2022, 1, 1, 13, 0, 0, 0, ZoneOffset.UTC);
    private TransportationMetadataEnricher enricher;
    private CalendaringServiceReceiver
        calendaringServiceReceiver;

    @BeforeEach
    void setUp() {

        calendaringServiceReceiver = mock(CalendaringServiceReceiver.class);
        enricher = new TransportationMetadataEnricher(
            mock(LogisticsPointReceiver.class),
            mock(PartnerSettingsReceiver.class),
            mock(PartnerReceiver.class),
            mock(LegalInfoReceiver.class),
            calendaringServiceReceiver,
            mock(PartnerMethodsCheckService.class),
            new TimeSlotConverter(),
            mock(CacheService.class)
        );
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(calendaringServiceReceiver);
    }

    @Test
    void enrichCalendaringSlotSuccess() {
        when(calendaringServiceReceiver.getByCalendaringServiceId(eq(1000L)))
            .thenReturn(Optional.of(new BookingResponseV2(
                1000L,
                "TSUP",
                "abcd",
                "efgh",
                1L,
                FROM,
                TO,
                BookingStatus.ACTIVE,
                LocalDateTime.of(2022, 1, 1, 10, 0, 0),
                172
            )));

        TransportationUnit unit = new TransportationUnit();
        unit.setId(1L);
        unit.setType(TransportationUnitType.OUTBOUND);
        unit.setSelectedCalendaringServiceId(1000L);

        enricher.enrichCalendaringSlot(unit, unit::setBookedTimeSlot);

        assertThat(unit.getBookedTimeSlot())
            .isEqualTo(new TimeSlot()
                .setCalendaringServiceId(1000L)
                .setFromDate(FROM.toLocalDateTime())
                .setToDate(TO.toLocalDateTime())
                .setGateId(1L)
                .setZoneId("Z")
            );

        verify(calendaringServiceReceiver).getByCalendaringServiceId(eq(1000L));
    }

    @Test
    void enrichCalendaringSlotMissing() {
        TransportationUnit unit = new TransportationUnit();
        unit.setId(1L);
        unit.setType(TransportationUnitType.OUTBOUND);

        enricher.enrichCalendaringSlot(unit, unit::setBookedTimeSlot);

        assertThat(unit.getBookedTimeSlot()).isNull();
    }

    @Test
    void enrichCalendaringSlotCancelled() {
        when(calendaringServiceReceiver.getByCalendaringServiceId(eq(1000L)))
            .thenReturn(Optional.of(new BookingResponseV2(
                1000L,
                "TSUP",
                "abcd",
                "efgh",
                1L,
                FROM,
                TO,
                BookingStatus.CANCELLED,
                LocalDateTime.of(2022, 1, 1, 10, 0, 0),
                172
            )));

        TransportationUnit unit = new TransportationUnit();
        unit.setId(1L);
        unit.setType(TransportationUnitType.OUTBOUND);
        unit.setSelectedCalendaringServiceId(1000L);

        assertThatThrownBy(() -> enricher.enrichCalendaringSlot(unit, s -> {
            Assertions.fail("Should not call!");
        }))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Can't use not ACTIVE slot 1000 for OUTBOUND 1. Status is CANCELLED.");
        verify(calendaringServiceReceiver).getByCalendaringServiceId(eq(1000L));
    }
}
