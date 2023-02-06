package ru.yandex.market.delivery.transport_manager.service.xdoc;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.dto.QuotaLimits;
import ru.yandex.market.delivery.transport_manager.domain.entity.TimeSlot;
import ru.yandex.market.delivery.transport_manager.domain.entity.TmPropertyKey;
import ru.yandex.market.delivery.transport_manager.domain.entity.Transportation;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnit;
import ru.yandex.market.delivery.transport_manager.service.PropertyService;
import ru.yandex.market.delivery.transport_manager.service.xdoc.data.SupplyGroupsForOutbound;
import ru.yandex.market.delivery.transport_manager.util.matcher.GetAvailableLimitRequestArgumentMatcher;
import ru.yandex.market.delivery.transport_manager.util.matcher.UpdateQuotaRequestArgumentMatcher;
import ru.yandex.market.logistics.calendaring.client.CalendaringServiceClientApi;
import ru.yandex.market.logistics.calendaring.client.dto.AvailableLimitResponse;
import ru.yandex.market.logistics.calendaring.client.dto.RequestSizeResponse;
import ru.yandex.market.logistics.calendaring.client.dto.enums.BookingType;

class XDocTransportQuotaUpdatingServiceTest extends AbstractContextualTest {
    private static final LocalDate DATE = LocalDate.of(2021, 10, 1);
    private static final LocalDate COLLIDING_DATE = LocalDate.of(2021, 10, 2);

    @Autowired
    private XDocTransportQuotaUpdatingService quotaUpdatingService;

    @Autowired
    private PropertyService<TmPropertyKey> propertyService;

    @Autowired
    private CalendaringServiceClientApi csClient;

    @BeforeEach
    void setUp() {
        Mockito.when(propertyService.getBoolean(TmPropertyKey.ENABLE_XDOC_TRANSPORT_CALENDARING)).thenReturn(true);
    }

    @AfterEach
    void tearDown() {
        Mockito.verifyNoMoreInteractions(csClient);
    }

    @Test
    void withQuotaOptimisticBooking() {
        Mockito.when(csClient.getAvailableLimit(Mockito.argThat(
            new GetAvailableLimitRequestArgumentMatcher(1L, BookingType.MOVEMENT_WITHDRAW, DATE)
        ))).thenReturn(new AvailableLimitResponse(List.of(
            new RequestSizeResponse(DATE, 0L, 10L)
        )));
        Mockito.when(csClient.getAvailableLimit(Mockito.argThat(
            new GetAvailableLimitRequestArgumentMatcher(2L, BookingType.XDOCK_TRANSPORT_SUPPLY, DATE)
        ))).thenReturn(new AvailableLimitResponse(List.of(
            new RequestSizeResponse(DATE, 100L, 10L)
        )));

        Transportation transportation = new Transportation()
            .setOutboundUnit(
                new TransportationUnit()
                    .setPartnerId(1L)
                    .setBookedTimeSlot(
                        new TimeSlot()
                            .setCalendaringServiceId(3L)
                            .setZoneId(ZoneId.systemDefault().getId())
                            .setFromDate(DATE.atTime(10, 0).atZone(ZoneId.systemDefault()).toLocalDateTime())
                    )
            )
            .setInboundUnit(
                new TransportationUnit()
                    .setPartnerId(2L)
                    .setBookedTimeSlot(
                        new TimeSlot()
                            .setCalendaringServiceId(4L)
                            .setZoneId(ZoneId.systemDefault().getId())
                            .setFromDate(DATE.atTime(20, 0).atZone(ZoneId.systemDefault()).toLocalDateTime())
                    )
            );

        SupplyGroupsForOutbound groupsForOutbound =
            new SupplyGroupsForOutbound(Collections.emptyList(), new QuotaLimits(5L, 50L));
        Optional<SupplyGroupsForOutbound> result = quotaUpdatingService.withQuotaOptimisticBooking(
            transportation,
            (t, l, s) -> Optional.of(groupsForOutbound),
            null
        );

        Mockito.verify(csClient).getAvailableLimit(Mockito.argThat(
            new GetAvailableLimitRequestArgumentMatcher(1L, BookingType.MOVEMENT_WITHDRAW, DATE)
        ));
        Mockito.verify(csClient).getAvailableLimit(Mockito.argThat(
            new GetAvailableLimitRequestArgumentMatcher(2L, BookingType.XDOCK_TRANSPORT_SUPPLY, DATE)
        ));
        Mockito.verify(csClient).updateQuota(Mockito.argThat(
            new UpdateQuotaRequestArgumentMatcher(3L, 5, 0, false)
        ));
        Mockito.verify(csClient).updateQuota(Mockito.argThat(
            new UpdateQuotaRequestArgumentMatcher(4L, 5, 50, false)
        ));

        Assertions.assertEquals(Optional.of(groupsForOutbound), result);
    }

    @Test
    void withQuotaOptimisticBookingFail() {
        Mockito.when(csClient.getAvailableLimit(Mockito.argThat(
            new GetAvailableLimitRequestArgumentMatcher(1L, BookingType.MOVEMENT_WITHDRAW, DATE)
        ))).thenReturn(new AvailableLimitResponse(List.of(
            new RequestSizeResponse(DATE, 0L, 10L)
        )));
        Mockito.when(csClient.getAvailableLimit(Mockito.argThat(
            new GetAvailableLimitRequestArgumentMatcher(2L, BookingType.XDOCK_TRANSPORT_SUPPLY, DATE)
        ))).thenReturn(new AvailableLimitResponse(List.of(
            new RequestSizeResponse(DATE, 100L, 10L)
        )));
        Mockito.when(csClient.updateQuota(Mockito.any()))
            .thenThrow(new IllegalStateException());

        Transportation transportation = new Transportation()
            .setOutboundUnit(
                new TransportationUnit()
                    .setPartnerId(1L)
                    .setBookedTimeSlot(
                        new TimeSlot()
                            .setCalendaringServiceId(3L)
                            .setZoneId(ZoneId.systemDefault().getId())
                            .setFromDate(DATE.atTime(10, 0).atZone(ZoneId.systemDefault()).toLocalDateTime())
                    )
            )
            .setInboundUnit(
                new TransportationUnit()
                    .setPartnerId(2L)
                    .setBookedTimeSlot(
                        new TimeSlot()
                            .setCalendaringServiceId(4L)
                            .setZoneId(ZoneId.systemDefault().getId())
                            .setFromDate(DATE.atTime(20, 0).atZone(ZoneId.systemDefault()).toLocalDateTime())
                    )
            );

        Assertions.assertThrows(
            IllegalStateException.class,
            () -> quotaUpdatingService.withQuotaOptimisticBooking(
                transportation,
                (t, l, s) -> Optional.of(
                    new SupplyGroupsForOutbound(Collections.emptyList(), new QuotaLimits(5L, 50L))
                ),
                null
            )
        );

        Mockito.verify(csClient).getAvailableLimit(Mockito.argThat(
            new GetAvailableLimitRequestArgumentMatcher(1L, BookingType.MOVEMENT_WITHDRAW, DATE)
        ));
        Mockito.verify(csClient).getAvailableLimit(Mockito.argThat(
            new GetAvailableLimitRequestArgumentMatcher(2L, BookingType.XDOCK_TRANSPORT_SUPPLY, DATE)
        ));
        Mockito.verify(csClient).updateQuota(Mockito.argThat(
            new UpdateQuotaRequestArgumentMatcher(3L, 5, 0, false)
        ));
    }

    @Test
    void withQuotaOptimisticBookingNull() {
        Mockito.when(csClient.getAvailableLimit(Mockito.argThat(
            new GetAvailableLimitRequestArgumentMatcher(1L, BookingType.MOVEMENT_WITHDRAW, DATE)
        ))).thenReturn(new AvailableLimitResponse(List.of(
            new RequestSizeResponse(DATE, 0L, 10L)
        )));
        Mockito.when(csClient.getAvailableLimit(Mockito.argThat(
            new GetAvailableLimitRequestArgumentMatcher(2L, BookingType.XDOCK_TRANSPORT_SUPPLY, DATE)
        ))).thenReturn(new AvailableLimitResponse(List.of(
            new RequestSizeResponse(DATE, 100L, 10L)
        )));

        Transportation transportation = new Transportation()
            .setOutboundUnit(
                new TransportationUnit()
                    .setPartnerId(1L)
                    .setBookedTimeSlot(
                        new TimeSlot()
                            .setId(3L)
                            .setZoneId(ZoneId.systemDefault().getId())
                            .setFromDate(DATE.atTime(10, 0).atZone(ZoneId.systemDefault()).toLocalDateTime())
                    )
            )
            .setInboundUnit(
                new TransportationUnit()
                    .setPartnerId(2L)
                    .setBookedTimeSlot(
                        new TimeSlot()
                            .setId(4L)
                            .setZoneId(ZoneId.systemDefault().getId())
                            .setFromDate(DATE.atTime(20, 0).atZone(ZoneId.systemDefault()).toLocalDateTime())
                    )
            );

        Optional<SupplyGroupsForOutbound> result = quotaUpdatingService.withQuotaOptimisticBooking(
            transportation,
            (t, l, s) -> Optional.empty(),
            null
        );

        Mockito.verify(csClient).getAvailableLimit(Mockito.argThat(
            new GetAvailableLimitRequestArgumentMatcher(1L, BookingType.MOVEMENT_WITHDRAW, DATE)
        ));
        Mockito.verify(csClient).getAvailableLimit(Mockito.argThat(
            new GetAvailableLimitRequestArgumentMatcher(2L, BookingType.XDOCK_TRANSPORT_SUPPLY, DATE)
        ));

        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    public void withQuotaOptimisticBookingCollidingDate() {
        Mockito.when(csClient.getAvailableLimit(Mockito.argThat(
            new GetAvailableLimitRequestArgumentMatcher(1L, BookingType.MOVEMENT_WITHDRAW, COLLIDING_DATE)
        ))).thenReturn(new AvailableLimitResponse(List.of(
            new RequestSizeResponse(DATE, 0L, 10L)
        )));
        Mockito.when(csClient.getAvailableLimit(Mockito.argThat(
            new GetAvailableLimitRequestArgumentMatcher(2L, BookingType.XDOCK_TRANSPORT_SUPPLY, COLLIDING_DATE)
        ))).thenReturn(new AvailableLimitResponse(List.of(
            new RequestSizeResponse(DATE, 100L, 10L)
        )));

        Transportation transportation = new Transportation()
            .setOutboundUnit(
                new TransportationUnit()
                    .setPartnerId(1L)
                    .setBookedTimeSlot(
                        new TimeSlot()
                            .setCalendaringServiceId(3L)
                            .setZoneId("+4")
                            .setFromDate(COLLIDING_DATE.atTime(0, 0))
                    )
            )
            .setInboundUnit(
                new TransportationUnit()
                    .setPartnerId(2L)
                    .setBookedTimeSlot(
                        new TimeSlot()
                            .setCalendaringServiceId(4L)
                            .setZoneId("+4")
                            .setFromDate(COLLIDING_DATE.atTime(0, 0))
                    )
            );

        SupplyGroupsForOutbound groupsForOutbound =
            new SupplyGroupsForOutbound(Collections.emptyList(), new QuotaLimits(5L, 50L));
        Optional<SupplyGroupsForOutbound> result = quotaUpdatingService.withQuotaOptimisticBooking(
            transportation,
            (t, l, s) -> Optional.of(groupsForOutbound),
            null
        );

        Mockito.verify(csClient).getAvailableLimit(Mockito.argThat(
            new GetAvailableLimitRequestArgumentMatcher(1L, BookingType.MOVEMENT_WITHDRAW, COLLIDING_DATE)
        ));
        Mockito.verify(csClient).getAvailableLimit(Mockito.argThat(
            new GetAvailableLimitRequestArgumentMatcher(2L, BookingType.XDOCK_TRANSPORT_SUPPLY, COLLIDING_DATE)
        ));
        Mockito.verify(csClient).updateQuota(Mockito.argThat(
            new UpdateQuotaRequestArgumentMatcher(3L, 5, 0, false)
        ));
        Mockito.verify(csClient).updateQuota(Mockito.argThat(
            new UpdateQuotaRequestArgumentMatcher(4L, 5, 50, false)
        ));

        Assertions.assertEquals(Optional.of(groupsForOutbound), result);
    }
}
