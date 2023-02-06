package ru.yandex.market.tpl.tms.service.external.sqs;

import java.time.Clock;

import org.apache.logging.log4j.core.util.ReflectionUtil;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jms.core.JmsTemplate;

import ru.yandex.market.logistics.les.base.Event;
import ru.yandex.market.tpl.api.model.order.partner.MovementHistoryEventType;
import ru.yandex.market.tpl.core.domain.movement.DsMovementQueryService;
import ru.yandex.market.tpl.core.domain.movement.Movement;
import ru.yandex.market.tpl.core.domain.movement.MovementRepository;
import ru.yandex.market.tpl.core.domain.movement.event.history.MovementHistoryEvent;
import ru.yandex.market.tpl.core.domain.usershift.additional_data.UserShiftAdditionalDataRepository;
import ru.yandex.market.tpl.core.external.sqs.SqsQueueProperties;
import ru.yandex.market.tpl.tms.service.external.sqs.mapper.MovementStatusHistoryMapper;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MovementStatusHistorySenderTest {

    public static final String MOVEMENT_OUT_QUEUE = "courier_out";

    @Mock
    private JmsTemplate jmsTemplate;

    @Mock
    private MovementRepository movementRepository;

    @Mock
    private DsMovementQueryService dsMovementQueryService;

    @Mock
    private UserShiftAdditionalDataRepository userShiftAdditionalDataRepository;

    private Clock clock;

    private MovementStatusHistorySender sender;
    private MovementStatusHistoryMapper mapper;

    @BeforeEach
    void init() {
        clock = Clock.systemDefaultZone();

        SqsQueueProperties sqsQueueProperties = new SqsQueueProperties();
        sqsQueueProperties.setSource("courier");
        sqsQueueProperties.setOutQueue(MOVEMENT_OUT_QUEUE);

        var mockedMovement = new Movement();
        mockedMovement.setId(12345098L);
        when(movementRepository.findByIdOrThrow(12345098L)).thenReturn(mockedMovement);

        mapper = new MovementStatusHistoryMapper(
                movementRepository,
                dsMovementQueryService,
                userShiftAdditionalDataRepository
        );

        sender = new MovementStatusHistorySender(
                mapper,
                sqsQueueProperties,
                jmsTemplate,
                clock
        );
    }

    @Test
    void send() throws NoSuchFieldException {
        MovementHistoryEvent event = new MovementHistoryEvent();
        setProtectedFields(event);

        sender.send(event);

        ArgumentCaptor<Event> argumentCaptor = ArgumentCaptor.forClass(Event.class);
        Mockito.verify(jmsTemplate).convertAndSend(Mockito.eq(MOVEMENT_OUT_QUEUE), argumentCaptor.capture());

        Event eventDto = argumentCaptor.getValue();
        Assertions.assertThat(eventDto.getEventType()).isEqualTo(MovementStatusHistorySender.EVENT_TYPE);
        Assertions.assertThat(eventDto.getEventId()).isEqualTo(event.getId().toString());
    }

    private void setProtectedFields(MovementHistoryEvent event) throws NoSuchFieldException {
        ReflectionUtil.setFieldValue(event.getClass().getSuperclass().getDeclaredField("id"), event, 1L);
        ReflectionUtil.setFieldValue(
                event.getClass().getDeclaredField("type"),
                event,
                MovementHistoryEventType.MOVEMENT_CONFIRMED
        );
        ReflectionUtil.setFieldValue(event.getClass().getDeclaredField("movementId"), event, 12345098L);
    }
}
