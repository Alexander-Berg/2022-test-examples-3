package ru.yandex.market.wms.servicebus.repository;

import java.time.LocalDateTime;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.common.spring.dto.TaskActionLogDto;
import ru.yandex.market.wms.servicebus.IntegrationTest;

public class ClickHouseTaskActionLogRepositoryTest extends IntegrationTest {

    @Autowired
    private ClickHouseTaskActionLogRepository daoTemplate;

    @Test
    @DatabaseSetup(
            value = "/repository/task-action-log/empty.xml",
            connection = "clickHouseConnection")
    @ExpectedDatabase(
            value = "/repository/task-action-log/after.xml",
            connection = "clickHouseConnection",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void insertList() {
        final List<TaskActionLogDto> dtos = List.of(
                TaskActionLogDto.builder()
                        .user("test")
                        .operationType(TaskActionLogDto.OperationType.PACKING)
                        .eventType(TaskActionLogDto.TaskActionEventType.WAIT)
                        .eventDate(LocalDateTime.parse("2021-10-01T12:00:00"))
                        .location("test")
                        .build(),
                TaskActionLogDto.builder()
                        .user("test2")
                        .operationType(TaskActionLogDto.OperationType.PACKING)
                        .eventType(TaskActionLogDto.TaskActionEventType.WAIT)
                        .eventDate(LocalDateTime.parse("2021-10-01T12:00:00"))
                        .location("test")
                        .build()
        );

        daoTemplate.insert(dtos);
    }

    @Test
    @DatabaseSetup(
            value = "/repository/task-action-log/empty.xml",
            connection = "clickHouseConnection")
    @ExpectedDatabase(
            value = "/repository/task-action-log/after-single-row.xml",
            connection = "clickHouseConnection",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void insertSingle() {
        final TaskActionLogDto dto =
                TaskActionLogDto.builder()
                        .user("test")
                        .operationType(TaskActionLogDto.OperationType.PACKING)
                        .eventType(TaskActionLogDto.TaskActionEventType.WAIT)
                        .eventDate(LocalDateTime.parse("2021-10-01T12:00:00"))
                        .location("test")
                        .build();
        daoTemplate.insert(dto);
    }
}
