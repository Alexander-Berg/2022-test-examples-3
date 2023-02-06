package ru.yandex.market.wms.servicebus.async.service.consumer;

import java.time.LocalDateTime;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jms.core.JmsTemplate;

import ru.yandex.market.wms.common.spring.dto.TaskActionLogDto;
import ru.yandex.market.wms.servicebus.IntegrationTest;
import ru.yandex.market.wms.servicebus.repository.ClickHouseTaskActionLogRepository;
import ru.yandex.market.wms.shared.libs.async.jms.QueueNameConstants;

public class ClickHouseTaskActionLogAsyncServiceTest extends IntegrationTest {

    @Autowired
    private JmsTemplate defaultJmsTemplate;

    @MockBean
    @Autowired
    private ClickHouseTaskActionLogRepository dao;

    @AfterEach
    public void resetMocks() {
        Mockito.reset(dao);
    }

    @Test
    public void consumeTaskActionLog() {

        final TaskActionLogDto dto = TaskActionLogDto.builder()
                .user("test")
                .operationType(TaskActionLogDto.OperationType.PACKING)
                .eventType(TaskActionLogDto.TaskActionEventType.WAIT)
                .eventDate(LocalDateTime.parse("2021-10-01T12:00:00"))
                .build();

        defaultJmsTemplate.convertAndSend(QueueNameConstants.QUEUE_TASK_ACTION_LOG, dto);

        Mockito.verify(dao, Mockito.timeout(1000).times(1)).insert(dto);
    }

}
