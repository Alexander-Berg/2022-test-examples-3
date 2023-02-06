package ru.yandex.market.sc.tms.domain.order;

import java.time.Clock;
import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.sc.core.configuration.ConfigurationProperties;
import ru.yandex.market.sc.core.domain.delivery_service.model.DeliveryServiceType;
import ru.yandex.market.sc.core.domain.order.model.OrderLike;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.tms.test.EmbeddedDbTmsTest;
import ru.yandex.market.tpl.common.db.configuration.ConfigurationService;
import ru.yandex.market.tpl.common.startrek.StartrekService;
import ru.yandex.market.tpl.common.startrek.ticket.StartrekTicket;
import ru.yandex.market.tpl.common.startrek.ticket.TicketQueueType;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.sc.core.test.TestFactory.order;

/**
 * @author valter
 */
@EmbeddedDbTmsTest
class MultiplaceOrderReturnTaskTest {

    private static final String ZERO_DURATION = "PT0H";
    private static final String LONG_DURATION = "PT24H";

    @Autowired
    TestFactory testFactory;
    @Autowired
    MultiplaceOrderReturnTask multiplaceOrderReturnTask;
    @Autowired
    ConfigurationService configurationService;
    @Autowired
    Clock clock;
    @MockBean
    StartrekService startrekService;

    SortingCenter sortingCenter;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
        doReturn(ticketMock()).when(startrekService).createTicket(any(), any(), any(), any());
    }

    private StartrekTicket ticketMock() {
        var ticket = mock(StartrekTicket.class);
        doReturn("TICKET-123").when(ticket).getKey();
        return ticket;
    }

    @Test
    void noTicketForMiddleMileOrders() {
        initConfiguration(ZERO_DURATION);
        testFactory.createForToday(
                order(sortingCenter).places("1", "2").dsType(DeliveryServiceType.TRANSIT).build()
        ).acceptPlaces("1").get();
        multiplaceOrderReturnTask.returnNotFullyArrived(clock.instant().plus(Duration.ofMillis(1)));
        verifyNoTicketCreated();
    }

    @Test
    void returnNotFullyArrivedCreatesTicketZeroDuration() {
        initConfiguration(ZERO_DURATION);
        var order = testFactory.createForToday(
                order(sortingCenter).places("1", "2").build()
        ).acceptPlaces("1").get();
        multiplaceOrderReturnTask.returnNotFullyArrived(clock.instant().plus(Duration.ofMillis(1)));
        verifySingleTicketCreated(order);
    }

    @Test
    void returnNotFullyArrivedCreatesTicketLongDuration() {
        initConfiguration(LONG_DURATION);
        var order = testFactory.createForToday(
                order(sortingCenter).places("1", "2").build()
        ).acceptPlaces("1").get();
        multiplaceOrderReturnTask.returnNotFullyArrived(clock.instant().plus(Duration.ofHours(25)));
        verifySingleTicketCreated(order);
    }

    @Test
    void returnNotFullyArrivedNoTicketForNewPartialMultiplaceOrder() {
        initConfiguration(LONG_DURATION);
        testFactory.createForToday(
                order(sortingCenter).places("1", "2").build()
        ).acceptPlaces("1").get();
        multiplaceOrderReturnTask.returnNotFullyArrived(clock.instant().plus(Duration.ofHours(23)));
        verifyNoTicketCreated();
    }

    @Test
    void returnNotFullyArrivedNoTicketForFullyArrivedOrder() {
        initConfiguration(ZERO_DURATION);
        testFactory.createForToday(
                order(sortingCenter).places("1", "2").build()
        ).acceptPlaces("1", "2").get();
        multiplaceOrderReturnTask.returnNotFullyArrived(clock.instant().plus(Duration.ofMillis(1)));
        verifyNoTicketCreated();
    }

    @Test
    void returnNotFullyArrivedNoTicketForNonMultiplaceOrder() {
        initConfiguration(ZERO_DURATION);
        testFactory.createForToday(
                order(sortingCenter).build()
        ).accept().get();
        multiplaceOrderReturnTask.returnNotFullyArrived(clock.instant().plus(Duration.ofMillis(1)));
        verifyNoTicketCreated();
    }

    @Test
    void returnNotFullyArrivedNoTicketDuplicates() {
        initConfiguration(ZERO_DURATION);
        var order = testFactory.createForToday(
                order(sortingCenter).places("1", "2").build()
        ).acceptPlaces("1").get();
        multiplaceOrderReturnTask.returnNotFullyArrived(clock.instant().plus(Duration.ofMillis(1)));
        multiplaceOrderReturnTask.returnNotFullyArrived(clock.instant().plus(Duration.ofMillis(1)));
        verifySingleTicketCreated(order);
    }

    private void initConfiguration(String returnThreshold) {
        configurationService.mergeValue(ConfigurationProperties.DURATION_TO_CANCEL_MULTIPLACE_ORDER, returnThreshold);
    }

    private void verifySingleTicketCreated(OrderLike order) {
        verify(startrekService, times(1)).createTicket(
                eq(TicketQueueType.CALL_CUSTOMER),
                any(),
                contains(order.getExternalId()),
                any()
        );
    }

    private void verifyNoTicketCreated() {
        verify(startrekService, never()).createTicket(any(), anyString(), any(), any());
    }

}
