package ru.yandex.market.wms.common.spring.service;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jms.core.JmsTemplate;

import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.common.spring.dto.TaskActionLogDto;
import ru.yandex.market.wms.shared.libs.async.jms.QueueNameConstants;
import ru.yandex.market.wms.shared.libs.authorization.SecurityDataProvider;
import ru.yandex.market.wms.shared.libs.utils.time.WarehouseTimeZoneConverter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

class ClickHouseTaskActionLogConsumerTest extends IntegrationTest {

    @Autowired
    private SecurityDataProvider securityDataProvider;

    @MockBean
    @Autowired
    private JmsTemplate defaultJmsTemplate;

    @Autowired
    ClickHouseTaskActionLogConsumer taskActionLogConsumer;

    @MockBean
    @Autowired
    WarehouseTimeZoneConverter warehouseTimeZoneConverter;

    @BeforeEach
    public void init() {
        Mockito.reset(defaultJmsTemplate);
    }

    @Test
    public void sendWait() {

        when(warehouseTimeZoneConverter.convertFromUTC(any(LocalDateTime.class))).thenReturn(
                LocalDateTime.parse("2021-10-12T00:00:00")
        );

        doNothing().when(defaultJmsTemplate).convertAndSend(any(String.class), any(TaskActionLogDto.class));

        String location = "test";

        TaskActionLogDto expected = TaskActionLogDto.builder()
                .user(securityDataProvider.getUser())
                .operationType(TaskActionLogDto.OperationType.PACKING)
                .eventType(TaskActionLogDto.TaskActionEventType.WAIT)
                .eventDate(LocalDateTime.parse("2021-10-12T00:00:00"))
                .location(location)
                .build();

        taskActionLogConsumer.sendWait(TaskActionLogDto.OperationType.PACKING, location);

        Mockito.verify(defaultJmsTemplate, Mockito.times(1))
                .convertAndSend(QueueNameConstants.QUEUE_TASK_ACTION_LOG, expected);
    }

    @Test
    public void sendWaitWhenJmsTemplateThrowExceptionThanNoException() {

        when(warehouseTimeZoneConverter.convertFromUTC(any(LocalDateTime.class))).thenReturn(
                LocalDateTime.parse("2021-10-12T00:00:00")
        );

        Mockito.doThrow(RuntimeException.class).when(defaultJmsTemplate).convertAndSend(any(String.class),
                any(TaskActionLogDto.class));

        taskActionLogConsumer.sendWait(TaskActionLogDto.OperationType.PACKING, "test");
    }

    @Test
    public void sendReceive() {

        when(warehouseTimeZoneConverter.convertFromUTC(any(LocalDateTime.class))).thenReturn(
                LocalDateTime.parse("2021-10-12T00:00:00")
        );

        doNothing().when(defaultJmsTemplate).convertAndSend(any(String.class), any(TaskActionLogDto.class));

        String location = "test";

        TaskActionLogDto expected = TaskActionLogDto.builder()
                .user(securityDataProvider.getUser())
                .operationType(TaskActionLogDto.OperationType.PACKING)
                .eventType(TaskActionLogDto.TaskActionEventType.RECEIVE)
                .eventDate(LocalDateTime.parse("2021-10-12T00:00:00"))
                .location(location)
                .build();

        taskActionLogConsumer.sendReceive(TaskActionLogDto.OperationType.PACKING, location);

        Mockito.verify(defaultJmsTemplate, Mockito.times(1))
                .convertAndSend(QueueNameConstants.QUEUE_TASK_ACTION_LOG, expected);
    }

    @Test
    public void sendReceiveWhenJmsTemplateThrowExceptionThanNoException() {

        when(warehouseTimeZoneConverter.convertFromUTC(any(LocalDateTime.class))).thenReturn(
                LocalDateTime.parse("2021-10-12T00:00:00")
        );

        Mockito.doThrow(RuntimeException.class).when(defaultJmsTemplate).convertAndSend(any(String.class),
                any(TaskActionLogDto.class));

        taskActionLogConsumer.sendReceive(TaskActionLogDto.OperationType.PACKING, "test");
    }

    @Test
    public void sendFinish() {

        when(warehouseTimeZoneConverter.convertFromUTC(any(LocalDateTime.class))).thenReturn(
                LocalDateTime.parse("2021-10-12T00:00:00")
        );

        doNothing().when(defaultJmsTemplate).convertAndSend(any(String.class), any(TaskActionLogDto.class));

        String location = "test";

        TaskActionLogDto expected = TaskActionLogDto.builder()
                .user(securityDataProvider.getUser())
                .operationType(TaskActionLogDto.OperationType.PACKING)
                .eventType(TaskActionLogDto.TaskActionEventType.FINISH)
                .eventDate(LocalDateTime.parse("2021-10-12T00:00:00"))
                .location(location)
                .build();

        taskActionLogConsumer.sendFinish(TaskActionLogDto.OperationType.PACKING, location);

        Mockito.verify(defaultJmsTemplate, Mockito.times(1))
                .convertAndSend(QueueNameConstants.QUEUE_TASK_ACTION_LOG, expected);
    }

    @Test
    public void sendFinishWhenJmsTemplateThrowExceptionThanNoException() {

        when(warehouseTimeZoneConverter.convertFromUTC(any(LocalDateTime.class))).thenReturn(
                LocalDateTime.parse("2021-10-12T00:00:00")
        );

        Mockito.doThrow(RuntimeException.class).when(defaultJmsTemplate).convertAndSend(any(String.class),
                any(TaskActionLogDto.class));

        String location = "test";

        taskActionLogConsumer.sendFinish(TaskActionLogDto.OperationType.PACKING, location);
    }
}
