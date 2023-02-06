package ru.yandex.market.tpl.core.task.flow;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Component;

import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.task.TestTaskFlowUtils;
import ru.yandex.market.tpl.core.task.defaults.TaskDefaults;
import ru.yandex.market.tpl.core.task.projection.TaskActionType;
import ru.yandex.market.tpl.core.task.projection.TaskFlowType;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author sekulebyakin
 */
@RequiredArgsConstructor
public class TaskFlowServiceTest extends TplAbstractTest {

    private final TaskFlowService taskFlowService;

    @Component
    @RequiredArgsConstructor
    public static class TestTaskFlowConfigurator implements TaskFlowConfigurator {

        private final TaskDefaults taskDefaults;

        @Override
        public TaskFlowConfiguration configure() {
            return new TaskFlowConfiguration(TaskFlowType.TEST_FLOW)
                    // Первое действие - с условием и с пост обработчиком
                    .action(TaskActionType.EMPTY_ACTION)
                    .precondition(taskDefaults.configurationFlag(ConfigurationProperties.CORE_TASK_V2_ENABLED, true))
                    .afterActionHandler(TestTaskFlowUtils.emptyHandler())
                    // Второе действие - с двумя условиями и без пост обработчиком
                    .action(TaskActionType.EMPTY_ACTION)
                    .preconditions(List.of(
                            taskDefaults.configurationFlag(ConfigurationProperties.CORE_TASK_V2_ENABLED, true),
                            taskDefaults.configurationFlag(ConfigurationProperties.CORE_TASK_V2_ENABLED, true)
                    ))
                    // Третье действие - без условия и с двумя пост обработчиком
                    .action(TaskActionType.EMPTY_ACTION)
                    .afterActionHandlers(List.of(
                            TestTaskFlowUtils.emptyHandler(),
                            TestTaskFlowUtils.emptyHandler()
                    ))
                    // Червертое действие - без условия и без пост обработчиком
                    .action(TaskActionType.EMPTY_ACTION)
                    .build();
        }
    }

    @Test
    void buildFlowsOnStartupTest() {
        var testFlowConfig = taskFlowService.getFlowConfig(TaskFlowType.TEST_FLOW);
        assertThat(testFlowConfig).isNotNull();
        assertThat(testFlowConfig.getType()).isEqualTo(TaskFlowType.TEST_FLOW);

        var cubes = testFlowConfig.getCubeConfigs();
        assertThat(cubes).hasSize(4);
        assertCubeValid(cubes.get(0), TaskActionType.EMPTY_ACTION, 1, 1);
        assertCubeValid(cubes.get(1), TaskActionType.EMPTY_ACTION, 2, 0);
        assertCubeValid(cubes.get(2), TaskActionType.EMPTY_ACTION, 0, 2);
        assertCubeValid(cubes.get(3), TaskActionType.EMPTY_ACTION, 0, 0);
    }

    private void assertCubeValid(CubeConfiguration<?,?> cube, TaskActionType<?,?> type, int preconditionsCount, int handlersCount) {
        assertThat(cube.getType()).isEqualTo(type);
        assertThat(cube.getPreconditions()).hasSize(preconditionsCount);
        assertThat(cube.getAfterActionHandlers()).hasSize(handlersCount);
    }

}
