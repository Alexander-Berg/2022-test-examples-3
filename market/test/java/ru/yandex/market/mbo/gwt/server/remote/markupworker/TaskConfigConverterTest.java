package ru.yandex.market.mbo.gwt.server.remote.markupworker;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import ru.yandex.market.mbo.db.CachedTreeService;
import ru.yandex.market.mbo.gwt.client.models.markup_worker.FullBatchTaskConfig;
import ru.yandex.market.mbo.gwt.client.models.markup_worker.LimitedCountTaskConfig;
import ru.yandex.market.mbo.gwt.client.models.markup_worker.LimitedModelsTaskConfig;
import ru.yandex.market.mbo.gwt.client.models.markup_worker.ScheduledTaskConfig;
import ru.yandex.market.mbo.gwt.client.models.markup_worker.TaskConfigBase;
import ru.yandex.market.mbo.gwt.client.models.markup_worker.TaskConfigFactory;
import ru.yandex.market.mbo.gwt.client.models.markup_worker.TaskConfigWithRetry;
import ru.yandex.market.mbo.gwt.client.models.markup_worker.TaskType;
import ru.yandex.market.mbo.gwt.client.models.markup_worker.tasks.ModelPublicationStatus;
import ru.yandex.market.mbo.gwt.client.models.markup_worker.tasks.ModelType;
import ru.yandex.market.mbo.utils.StatfaceReportCategoryUrlBuilder;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static ru.yandex.market.mbo.gwt.server.remote.markupworker.TaskConfigConverter.convertConfig;
import static ru.yandex.market.mbo.gwt.server.remote.markupworker.TaskConfigConverter.convertTaskType;

/**
 * @author sergtru
 * @since 03.06.2017
 */
@SuppressWarnings("checkstyle:magicNumber")
public class TaskConfigConverterTest {
    @Test
    public void taskTypeTest() throws Exception {
        for (TaskType type: TaskType.values()) {
            Assert.assertEquals(type, convertTaskType(convertTaskType(type)));
        }
    }

    @Test
    public void testModelConversion() throws Exception {
        Random rnd = new Random(1); //fixed seed for reproducible test
        FillTaskConfigContext context = createDummyConfig();
        for (TaskType type: TaskType.values()) {
            TaskConfigBase origin = TaskConfigFactory.createTaskConfig(type);
            fillRandom(origin, rnd);
            TaskConfigBase result = convertConfig(context, convertConfig(origin));
            assertEqualConfigs(origin, result);
        }
    }

    private void fillRandom(TaskConfigBase config, Random rnd) throws IllegalAccessException {
        config.setCategoryId(rnd.nextInt(100000));
        config.setMinBatchSize(rnd.nextInt(1000));
        config.setMaxBatchSize(rnd.nextInt(1000));
        config.setSimultaneousTasksCount(rnd.nextInt(10));

        if (config instanceof LimitedCountTaskConfig) {
            config.setEntitiesCount(rnd.nextInt(1000));
        }
        if (config instanceof LimitedModelsTaskConfig) {
            LimitedModelsTaskConfig typedConfig = (LimitedModelsTaskConfig) config;
            typedConfig.setModelType(anyOf(ModelType.values(), rnd));
            typedConfig.setModelPublicationStatus(anyOf(ModelPublicationStatus.values(), rnd));
        }
        if (config instanceof ScheduledTaskConfig) {
            ScheduledTaskConfig typedConfig = (ScheduledTaskConfig) config;
            typedConfig.setEntitiesCount(rnd.nextInt(1000));
            typedConfig.setIntervalInMinutes(rnd.nextInt(1000));
        }
        if (config instanceof TaskConfigWithRetry) {
            ((TaskConfigWithRetry) config).setRetryInterval(rnd.nextInt(1000));
        }
    }

    private <T> T anyOf(T[] values, Random rnd) {
        return values[rnd.nextInt(values.length)];
    }

    private void assertEqualConfigs(TaskConfigBase expected, TaskConfigBase actual) throws IllegalAccessException {
        assertEquals(expected.getType(), actual.getType());
        assertEquals(expected.getCategoryId(), actual.getCategoryId());
        if (expected instanceof FullBatchTaskConfig) {
            assertEquals(expected.getMaxBatchSize().intValue(), actual.getEntitiesCount());
            assertEquals(expected.getMinBatchSize().intValue(), actual.getEntitiesCount());
        } else {
            assertEquals(expected.getMaxBatchSize(), actual.getMaxBatchSize());
            assertEquals(expected.getMinBatchSize(), actual.getMinBatchSize());
        }
        assertEquals(expected.getSimultaneousTasksCount(), actual.getSimultaneousTasksCount());
        assertEquals(expected.getYangPoolId(), actual.getYangPoolId());

        if (expected instanceof LimitedCountTaskConfig) {
            assertEquals(expected.getEntitiesCount(), actual.getEntitiesCount());
        }
        if (expected instanceof LimitedModelsTaskConfig) {
            LimitedModelsTaskConfig typedExpected = (LimitedModelsTaskConfig) expected;
            LimitedModelsTaskConfig typedActual = (LimitedModelsTaskConfig) actual;
            assertEquals(typedExpected.getModelType(), typedActual.getModelType());
            assertEquals(typedExpected.getModelPublicationStatus(), typedActual.getModelPublicationStatus());
        }
        if (expected instanceof ScheduledTaskConfig) {
            ScheduledTaskConfig typedExpected = (ScheduledTaskConfig) expected;
            ScheduledTaskConfig typedActual = (ScheduledTaskConfig) actual;
            assertEquals(typedExpected.getEntitiesCount(), typedActual.getEntitiesCount());
            assertEquals(typedExpected.getIntervalInMinutes(), typedActual.getIntervalInMinutes());
        }
        if (expected instanceof TaskConfigWithRetry) {
            assertEquals(
                    ((TaskConfigWithRetry) expected).getRetryInterval(),
                    ((TaskConfigWithRetry) actual).getRetryInterval());
        }
    }

    private FillTaskConfigContext createDummyConfig() {
        return new FillTaskConfigContext(null,
            new StatfaceReportCategoryUrlBuilder(Mockito.mock(CachedTreeService.class), false)) {
            @Override
            public long getTotalModelsCount(long categoryId) {
                return 0;
            }

            @Override
            public long getGuruModelsCount(long categoryId) {
                return 0;
            }

            @Override
            public String getModelImagesReportUrl(long categoryId) {
                return null;
            }

            @Override
            public String getParamsMetricsReportUrl(long categoryId) {
                return null;
            }

            @Override
            public String getCategoryParamsMetricsReportUrl(long categoryId) {
                return null;
            }

            @Override
            public String getMatchingAccuracyReportUrl(long categoryId) {
                return null;
            }

            @Override
            public String getDoublesMetricsReportUrl(long categoryId) {
                return null;
            }

            @Override
            public String getFillParametersReportUrl(long categoryId) {
                return null;
            }
        };
    }
}
