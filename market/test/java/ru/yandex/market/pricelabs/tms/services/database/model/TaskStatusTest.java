package ru.yandex.market.pricelabs.tms.services.database.model;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import ru.yandex.market.pricelabs.services.database.model.TaskStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.pricelabs.services.database.model.TaskStatus.FAILURE;
import static ru.yandex.market.pricelabs.services.database.model.TaskStatus.INVALID;
import static ru.yandex.market.pricelabs.services.database.model.TaskStatus.SUCCESS;
import static ru.yandex.market.pricelabs.services.database.model.TaskStatus.UNRECOVERABLE;

class TaskStatusTest {

    @Test
    void testCompleteStatues() {
        // Список используется в классе TasksServiceImpl#getNonExportedTasks
        assertEquals(Set.of(SUCCESS, FAILURE, INVALID, UNRECOVERABLE),
                Stream.of(TaskStatus.values())
                        .filter(TaskStatus::isComplete)
                        .collect(Collectors.toSet()));
    }
}
