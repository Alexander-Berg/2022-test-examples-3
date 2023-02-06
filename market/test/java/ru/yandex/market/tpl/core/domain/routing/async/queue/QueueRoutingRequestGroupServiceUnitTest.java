package ru.yandex.market.tpl.core.domain.routing.async.queue;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.routing.service.CreateShiftRoutingRequestCommandData;
import ru.yandex.market.tpl.common.util.exception.TplRoutingCouriersNotFoundException;
import ru.yandex.market.tpl.common.util.exception.TplRoutingOrdersNotFoundException;
import ru.yandex.market.tpl.core.domain.routing.RoutingShift;
import ru.yandex.market.tpl.core.domain.routing.RoutingShiftProvider;
import ru.yandex.market.tpl.core.domain.routing.log.RoutingLogDao;
import ru.yandex.market.tpl.core.domain.routing.log.RoutingLogRecord;
import ru.yandex.market.tpl.core.domain.shift.AbstractCreateShiftRoutingRequestCommandFactory;
import ru.yandex.market.tpl.core.external.routing.api.RoutingMockType;
import ru.yandex.market.tpl.core.external.routing.api.RoutingProfileType;
import ru.yandex.market.tpl.core.external.routing.api.RoutingResultStatus;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class QueueRoutingRequestGroupServiceUnitTest {
    @Mock
    private RoutingLogDao routingLogDao;
    @Mock
    private AbstractCreateShiftRoutingRequestCommandFactory<CreateShiftRoutingRequestCommandData> createShiftRoutingRequestCommandFactory;
    @Mock
    private RoutingShiftProvider<RoutingShift> shiftRepository;

    @InjectMocks
    private QueueRoutingRequestGroupService<CreateShiftRoutingRequestCommandData> queueRoutingRequestGroupService;

    @Test
    void checkNotThrows_when_CouriersNotFound() {
        //given
        doThrow(new TplRoutingCouriersNotFoundException("Курьеры не найдены"))
                .when(createShiftRoutingRequestCommandFactory).createCommandData(any(), any(), any(), any());
        when(routingLogDao.findRoutingRequestRecordsForGroup(any())).thenReturn(List.of(createRoutingLog()));
        var shift = mock(RoutingShift.class);
        when(shiftRepository.findByShiftDateAndSortingCenterId(any(), anyLong())).thenReturn(Optional.of(shift));

        //then
        assertDoesNotThrow(() -> queueRoutingRequestGroupService.processPayload(createPayload()));
    }

    @Test
    void checkNotThrows_when_OrdersNotFound() {
        //given
        doThrow(new TplRoutingOrdersNotFoundException("Заказы не найдены"))
                .when(createShiftRoutingRequestCommandFactory).createCommandData(any(), any(), any(), any());
        when(routingLogDao.findRoutingRequestRecordsForGroup(any())).thenReturn(List.of(createRoutingLog()));
        var shift = mock(RoutingShift.class);
        when(shiftRepository.findByShiftDateAndSortingCenterId(any(), anyLong())).thenReturn(Optional.of(shift));

        //then
        assertDoesNotThrow(() -> queueRoutingRequestGroupService.processPayload(createPayload()));
    }

    private QueueRoutingRequestGroupPayload createPayload() {
        return new QueueRoutingRequestGroupPayload("requestId", 1L);
    }

    private RoutingLogRecord createRoutingLog() {
        return new RoutingLogRecord(
                1L, "requestId", LocalDate.now(), 1L, RoutingMockType.REAL,
                RoutingProfileType.GROUP,
                Instant.now(), Instant.now(), RoutingResultStatus.QUEUE_ROUTING_REQUEST_GROUP, null);
    }
}
