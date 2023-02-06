package ru.yandex.market.mbo.gwt.client.models.markup_worker;

import com.google.common.collect.ImmutableSet;
import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.mbo.gwt.client.models.markup_worker.tasks.ModelImagesLoadingTaskConfig;
import ru.yandex.market.mbo.gwt.client.models.markup_worker.tasks.ModelImagesQualityTaskConfig;
import ru.yandex.market.mbo.gwt.client.models.markup_worker.tasks.ModelImagesRelevanceTaskConfig;
import ru.yandex.market.mbo.gwt.client.models.markup_worker.tasks.ModelImagesTaskConfig;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author galaev@yandex-team.ru
 * @since 06/12/2017.
 */
public class ModelImagesTaskConfigTest {

    @Test
    public void configNotStarted() throws Exception {
        ModelImagesTaskConfig config = new ModelImagesTaskConfig();
        Set<TaskConfigActionType> actions = config.getAllowedActions().stream()
            .map(TaskConfigAction::getType)
            .collect(Collectors.toSet());
        Set<TaskConfigActionType> expectedActions = ImmutableSet.of(TaskConfigActionType.RUN);

        Assert.assertEquals(expectedActions, actions);
    }

    @Test
    public void firstConfigActive() throws Exception {
        ModelImagesTaskConfig config = new ModelImagesTaskConfig();
        config.setId(1);
        config.setState(TaskConfigState.ACTIVE);
        Set<TaskConfigActionType> actions = config.getAllowedActions().stream()
            .map(TaskConfigAction::getType)
            .collect(Collectors.toSet());
        Set<TaskConfigActionType> expectedActions = ImmutableSet.of(
            TaskConfigActionType.COMPLETE, TaskConfigActionType.BREAK_ALL);

        Assert.assertEquals(expectedActions, actions);
    }

    @Test
    public void configForceFinishing() throws Exception {
        ModelImagesTaskConfig config = new ModelImagesTaskConfig();
        config.setId(1);
        config.setState(TaskConfigState.FORCE_FINISHING);
        Set<TaskConfigActionType> actions = config.getAllowedActions().stream()
            .map(TaskConfigAction::getType)
            .collect(Collectors.toSet());
        Set<TaskConfigActionType> expectedActions = ImmutableSet.of(TaskConfigActionType.BREAK_ALL);

        Assert.assertEquals(expectedActions, actions);
    }

    @Test
    public void secondConfigActive() throws Exception {
        ModelImagesTaskConfig config = new ModelImagesTaskConfig();
        config.setId(1);
        config.setState(TaskConfigState.DISABLED);
        ModelImagesQualityTaskConfig qualityConfig = new ModelImagesQualityTaskConfig();
        qualityConfig.setId(1);
        qualityConfig.setState(TaskConfigState.ACTIVE);
        config.setDependentConfig(qualityConfig);

        Set<TaskConfigActionType> actions = config.getAllowedActions().stream()
            .map(TaskConfigAction::getType)
            .collect(Collectors.toSet());
        Set<TaskConfigActionType> expectedActions = ImmutableSet.of(
            TaskConfigActionType.COMPLETE_DEPENDENT, TaskConfigActionType.BREAK_ALL);

        Assert.assertEquals(expectedActions, actions);
    }

    @Test
    public void thirdConfigActive() throws Exception {
        ModelImagesTaskConfig config = new ModelImagesTaskConfig();
        config.setId(1);
        config.setState(TaskConfigState.DISABLED);
        ModelImagesQualityTaskConfig qualityConfig = new ModelImagesQualityTaskConfig();
        qualityConfig.setId(1);
        qualityConfig.setState(TaskConfigState.DISABLED);
        config.setDependentConfig(qualityConfig);
        ModelImagesRelevanceTaskConfig relevanceConfig = new ModelImagesRelevanceTaskConfig();
        relevanceConfig.setId(1);
        relevanceConfig.setState(TaskConfigState.ACTIVE);
        qualityConfig.setDependentConfig(relevanceConfig);

        Set<TaskConfigActionType> actions = config.getAllowedActions().stream()
            .map(TaskConfigAction::getType)
            .collect(Collectors.toSet());
        Set<TaskConfigActionType> expectedActions = ImmutableSet.of(
            TaskConfigActionType.COMPLETE_ALL, TaskConfigActionType.BREAK_ALL);

        Assert.assertEquals(expectedActions, actions);
    }

    @Test
    public void firstConfigFinished() throws Exception {
        ModelImagesTaskConfig config = new ModelImagesTaskConfig();
        config.getTaskInfos().add(new TaskInfo());
        config.setState(TaskConfigState.DISABLED);
        config.getSingleTaskInfo().setResponseProcessedTasksCount(1);

        Set<TaskConfigActionType> actions = config.getAllowedActions().stream()
            .map(TaskConfigAction::getType)
            .collect(Collectors.toSet());
        Set<TaskConfigActionType> expectedActions = ImmutableSet.of(
            TaskConfigActionType.RUN, TaskConfigActionType.RUN_DEPENDENTS);

        Assert.assertEquals(expectedActions, actions);
    }

    @Test
    public void threeConfigsFinished() throws Exception {
        ModelImagesTaskConfig config = new ModelImagesTaskConfig();
        config.getTaskInfos().add(new TaskInfo());
        config.setState(TaskConfigState.DISABLED);
        config.getSingleTaskInfo().setResponseProcessedTasksCount(1);
        ModelImagesQualityTaskConfig qualityConfig = new ModelImagesQualityTaskConfig();
        qualityConfig.getTaskInfos().add(new TaskInfo());
        qualityConfig.setState(TaskConfigState.DISABLED);
        qualityConfig.getSingleTaskInfo().setResponseProcessedTasksCount(1);
        config.setDependentConfig(qualityConfig);
        ModelImagesRelevanceTaskConfig relevanceConfig = new ModelImagesRelevanceTaskConfig();
        relevanceConfig.getTaskInfos().add(new TaskInfo());
        relevanceConfig.setState(TaskConfigState.DISABLED);
        relevanceConfig.getSingleTaskInfo().setResponseProcessedTasksCount(1);
        qualityConfig.setDependentConfig(relevanceConfig);


        Set<TaskConfigActionType> actions = config.getAllowedActions().stream()
            .map(TaskConfigAction::getType)
            .collect(Collectors.toSet());
        Set<TaskConfigActionType> expectedActions = ImmutableSet.of(
            TaskConfigActionType.RUN, TaskConfigActionType.RUN_DEPENDENTS);

        Assert.assertEquals(expectedActions, actions);
    }

    @Test
    public void allConfigsFinished() throws Exception {
        ModelImagesTaskConfig config = new ModelImagesTaskConfig();
        config.setState(TaskConfigState.DISABLED);
        ModelImagesQualityTaskConfig qualityConfig = new ModelImagesQualityTaskConfig();
        qualityConfig.setState(TaskConfigState.DISABLED);
        config.setDependentConfig(qualityConfig);
        ModelImagesRelevanceTaskConfig relevanceConfig = new ModelImagesRelevanceTaskConfig();
        relevanceConfig.setState(TaskConfigState.DISABLED);
        qualityConfig.setDependentConfig(relevanceConfig);
        ModelImagesLoadingTaskConfig loadingConfig = new ModelImagesLoadingTaskConfig();
        loadingConfig.setState(TaskConfigState.DISABLED);
        relevanceConfig.setDependentConfig(loadingConfig);


        Set<TaskConfigActionType> actions = config.getAllowedActions().stream()
            .map(TaskConfigAction::getType)
            .collect(Collectors.toSet());
        Set<TaskConfigActionType> expectedActions = ImmutableSet.of(
            TaskConfigActionType.RUN);

        Assert.assertEquals(expectedActions, actions);
    }
}
