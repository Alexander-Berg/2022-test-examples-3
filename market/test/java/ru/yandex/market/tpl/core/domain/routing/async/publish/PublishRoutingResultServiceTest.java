package ru.yandex.market.tpl.core.domain.routing.async.publish;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.tpl.common.util.exception.TplInvalidParameterException;
import ru.yandex.market.tpl.core.domain.routing.RoutingResultPublisher;
import ru.yandex.market.tpl.core.domain.routing.RoutingShift;
import ru.yandex.market.tpl.core.domain.routing.log.RoutingLogDao;
import ru.yandex.market.tpl.core.domain.routing.log.RoutingResultWithShiftDate;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublishRoutingResultServiceTest {

    public static final String REQUEST_ID = UUID.randomUUID().toString();
    public static final String PROCESSING_ID = UUID.randomUUID().toString();
    public static final String ROUTING_REQUEST_ID_OLD_EMPTY_VALUE = null;
    public static final RoutingResultWithShiftDate ROUTING_RESULT_WITH_SHIFT_DATE =
            RoutingResultWithShiftDate.builder().build();
    public static final String ROUTING_REQUEST_ID = UUID.randomUUID().toString();
    @Mock
    private RoutingLogDao routingLogDao;
    @Mock
    private RoutingResultPublisher<? extends RoutingShift> routingResultPublisher;
    @InjectMocks
    private PublishRoutingResultService routingResultService;


    @BeforeEach
    void setUp() {
        reset(routingLogDao, routingResultPublisher);
    }

    @Test
    void process_oldVersion() {
        //given
        var oldPayload = new PublishRoutingResultPayload(
                REQUEST_ID,
                PROCESSING_ID,
                ROUTING_REQUEST_ID_OLD_EMPTY_VALUE
        );


        when(routingLogDao.findResultByProcessingId(eq(PROCESSING_ID))).thenReturn(
                Optional.of(ROUTING_RESULT_WITH_SHIFT_DATE)
        );

        //when
        routingResultService.processPayload(oldPayload);

        //then
        verify(routingResultPublisher).publishRouting(ROUTING_RESULT_WITH_SHIFT_DATE);
        verify(routingLogDao, never()).findResultWithShiftDateByRequestId(any());
    }

    @Test
    void process_newVersion() {
        //given
        var newPayload = new PublishRoutingResultPayload(
                REQUEST_ID,
                PROCESSING_ID,
                ROUTING_REQUEST_ID
        );

        when(routingLogDao.findResultWithShiftDateByRequestId(eq(ROUTING_REQUEST_ID)))
                .thenReturn(Optional.of(ROUTING_RESULT_WITH_SHIFT_DATE));


        //when
        routingResultService.processPayload(newPayload);

        //then
        verify(routingLogDao, never()).findResultByProcessingId(any());
        verify(routingResultPublisher).publishRouting(ROUTING_RESULT_WITH_SHIFT_DATE);
    }

    @Test
    void process_whenNotExists() {
        //given
        var payload = new PublishRoutingResultPayload(
                REQUEST_ID,
                PROCESSING_ID,
                ROUTING_REQUEST_ID_OLD_EMPTY_VALUE
        );


        when(routingLogDao.findResultByProcessingId(eq(PROCESSING_ID))).thenReturn(
                Optional.empty()
        );

        //then
        assertThrows(TplInvalidParameterException.class, () -> routingResultService.processPayload(payload));
    }

    @Test
    void process_whenNotExists_newVersion() {
        //given
        var payload = new PublishRoutingResultPayload(
                REQUEST_ID,
                PROCESSING_ID,
                ROUTING_REQUEST_ID
        );


        when(routingLogDao.findResultByProcessingId(eq(PROCESSING_ID))).thenReturn(
                Optional.empty()
        );
        when(routingLogDao.findResultWithShiftDateByRequestId(eq(ROUTING_REQUEST_ID))).thenReturn(
                Optional.empty()
        );

        //then
        assertThrows(TplInvalidParameterException.class, () -> routingResultService.processPayload(payload));
    }
}
