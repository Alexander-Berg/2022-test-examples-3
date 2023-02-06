package ru.yandex.market.logistic.gateway.service.util;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistic.gateway.AbstractIntegrationTest;
import ru.yandex.market.logistic.gateway.model.TaskEntityIdType;
import ru.yandex.market.logistic.gateway.model.dto.ClientTaskEntityDto;
import ru.yandex.market.logistic.gateway.model.entity.ClientTask;
import ru.yandex.market.logistic.gateway.repository.ClientTaskRepository;

public class TaskEntityIdsServiceTest extends AbstractIntegrationTest {

    private static final long TASK_ID = 1L;

    @Autowired
    private TaskEntityIdsService taskEntityIdsService;

    @Autowired
    private ClientTaskRepository clientTaskRepository;

    @Test
    @DatabaseSetup("classpath:repository/state/client_task_for_support_single.xml")
    @ExpectedDatabase(
        value = "classpath:repository/expected/task_entities_after_insert.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    public void saveTaskEntityIdsTest() {
        ClientTask task = clientTaskRepository.findTask(TASK_ID);
        List<ClientTaskEntityDto> taskEntityIds = List.of(
            new ClientTaskEntityDto("1", TaskEntityIdType.ORDER),
            new ClientTaskEntityDto("a", TaskEntityIdType.ORDER),
            new ClientTaskEntityDto("b", TaskEntityIdType.ORDER),
            new ClientTaskEntityDto(null, TaskEntityIdType.ORDER)
        );
        taskEntityIdsService.saveTaskEntityIds(task, taskEntityIds);
    }
}
