package ru.yandex.market.crm.operatorwindow;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.crm.operatorwindow.jmf.TicketFirstLine;
import ru.yandex.market.crm.operatorwindow.jmf.entity.BeruOutgoingCallTicket;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.entity.EntityService;
import ru.yandex.market.jmf.module.ticket.Ticket;
import ru.yandex.market.jmf.module.ticket.test.impl.TicketTestUtils;
import ru.yandex.market.jmf.timings.ServiceTime;
import ru.yandex.market.jmf.timings.geo.Geobase;
import ru.yandex.market.jmf.timings.test.impl.ServiceTimeTestUtils;
import ru.yandex.market.ocrm.module.order.domain.Order;
import ru.yandex.market.ocrm.module.order.test.OrderTestUtils;

import static ru.yandex.market.crm.operatorwindow.Constants.Service.BERU_PREORDER_CONFIRMATION;


@Transactional
public class BeruOutgoingCallTicketTest extends AbstractModuleOwTest {

    private static final long TEST_REGION_ID = 200;
    private static final String TEST_ZONE_ID = "America/Los_Angeles";
    private static final long DEFAULT_REGION_ID = 213;
    private static final String DEFAULT_ZONE_ID = "Europe/Moscow";

    @Inject
    private ServiceTimeTestUtils serviceTimeTestUtils;
    @Inject
    private Geobase geobaseService;
    @Inject
    private EntityService entityService;
    @Inject
    private BcpService bcpService;
    @Inject
    private TicketTestUtils ticketTestUtils;
    @Inject
    private OrderTestUtils orderTestUtils;

    @BeforeEach
    public void setUp() {
        Mockito.when(geobaseService.getTimeZoneByRegionId(Mockito.eq(DEFAULT_REGION_ID))).thenReturn(DEFAULT_ZONE_ID);
        Mockito.when(geobaseService.getTimeZoneByRegionId(Mockito.eq(TEST_REGION_ID))).thenReturn(TEST_ZONE_ID);
    }

    @Test
    public void testDeferTicketAtUnavailableCustomerCallTime() {
        OffsetDateTime now = OffsetDateTime.now();
        LocalDate day = now.toLocalDate().plusDays(2);

        ServiceTime supportTime = serviceTimeTestUtils.createServiceTime();
        String dayOfWeek = day.getDayOfWeek().name().toLowerCase();
        entityService.setAttribute(supportTime, "periods", Set.of(
                serviceTimeTestUtils.createPeriod(supportTime, dayOfWeek, "12:00", "23:59")
        ));
        ticketTestUtils.setServiceTime(BERU_PREORDER_CONFIRMATION, supportTime);

        OffsetDateTime expectedDeferDate = LocalDateTime.of(day, LocalTime.of(12, 0))
                .atZone(ZoneId.of(TEST_ZONE_ID))
                .toOffsetDateTime();

        BeruOutgoingCallTicket ticket = createTicket();

        Assertions.assertEquals(Ticket.STATUS_DEFERRED, ticket.getStatus());
        Assertions.assertEquals(0L, (long) ticket.getDeferCount());
        Assertions.assertNotNull(ticket.getDeferBackTimer());
        OffsetDateTime deadline = ticket.getDeferBackTimer().getDeadline();

        long difference = Duration.between(expectedDeferDate, deadline).abs().getSeconds();
        Assertions.assertTrue(difference < 10, "Время откладывание " + expectedDeferDate + "!=" + deadline);
    }

    @Test
    public void testSetWaitingDeferTimer() {
        OffsetDateTime now = OffsetDateTime.now();
        LocalDate day = now.toLocalDate();

        ServiceTime supportTime = serviceTimeTestUtils.createServiceTime();
        String prevDayOfWeek = day.minusDays(1).getDayOfWeek().name().toLowerCase();
        String dayOfWeek = day.getDayOfWeek().name().toLowerCase();
        String nextDayOfWeek = day.plusDays(1).getDayOfWeek().name().toLowerCase();
        entityService.setAttribute(supportTime, "periods", Set.of(
                serviceTimeTestUtils.createPeriod(supportTime, prevDayOfWeek, "00:00", "23:59"),
                serviceTimeTestUtils.createPeriod(supportTime, dayOfWeek, "00:00", "23:59"),
                serviceTimeTestUtils.createPeriod(supportTime, nextDayOfWeek, "00:00", "12:34")
        ));
        ticketTestUtils.setServiceTime(BERU_PREORDER_CONFIRMATION, supportTime);

        OffsetDateTime expectedWaitingDeferDate = LocalDateTime.of(day.plusDays(1), LocalTime.of(12, 34))
                .atZone(ZoneId.of(TEST_ZONE_ID))
                .toOffsetDateTime();

        BeruOutgoingCallTicket ticket = createTicket();

        Assertions.assertEquals(Ticket.STATUS_REGISTERED, ticket.getStatus());
        Assertions.assertNotNull(ticket.getWaitingDeferTimer());
        OffsetDateTime deadline = ticket.getWaitingDeferTimer().getDeadline();
        Assertions.assertTrue(Duration.between(expectedWaitingDeferDate, deadline).abs().getSeconds() < 10);
    }

    @Test
    public void testResetResolutionOnTransitionFromDeferred() {
        ticketTestUtils.setServiceTime24x7(BERU_PREORDER_CONFIRMATION);
        BeruOutgoingCallTicket ticket = createTicket();
        bcpService.edit(ticket, Map.of(
                Ticket.STATUS, Ticket.STATUS_DEFERRED,
                Ticket.DEFER_TIME, Duration.ofHours(1),
                Ticket.RESOLUTION, Constants.Resolution.UNREACHABLE_TO_CALL
        ));
        bcpService.edit(ticket, Map.of(
                Ticket.STATUS, Ticket.STATUS_REOPENED
        ));
        Assertions.assertNull(ticket.getResolution());
    }

    private BeruOutgoingCallTicket createTicket() {
        Order order = orderTestUtils.createOrder(Map.of(Order.DELIVERY_REGION_ID, TEST_REGION_ID));
        return ticketTestUtils.createTicket(
                BeruOutgoingCallTicket.FQN,
                Map.of(
                        Ticket.SERVICE, BERU_PREORDER_CONFIRMATION,
                        TicketFirstLine.ORDER, order
                ));
    }
}
