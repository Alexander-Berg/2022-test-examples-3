package ru.yandex.market.tsum.multitesting;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.market.tsum.dao.PipelinesDao;
import ru.yandex.market.tsum.dao.ProjectsDao;
import ru.yandex.market.tsum.entity.pipeline.PipelineEntity;
import ru.yandex.market.tsum.entity.pipeline.StoredConfigurationEntity;
import ru.yandex.market.tsum.entity.project.ProjectEntity;
import ru.yandex.market.tsum.multitesting.model.EffectiveMultitestingTimeToLiveSettingsWithSources;
import ru.yandex.market.tsum.multitesting.model.EffectiveMultitestingTimeToLiveSettingsWithSources.Source;
import ru.yandex.market.tsum.multitesting.model.EffectiveMultitestingTimeToLiveSettingsWithSources.ValueWithSource;
import ru.yandex.market.tsum.multitesting.model.MultitestingEnvironment;
import ru.yandex.market.tsum.multitesting.model.MultitestingTimeToLiveSettings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(Parameterized.class)
public class MultitestingTimeToLiveCalculationServiceTest {
    private static final MultitestingTimeToLiveSettings TTL_SETTINGS_FOR_SINGLE_SOURCE = ttlSettings(1, 4, 4);
    private static final String MULTITESTING_ID = "mt-id";
    private static final String PROJECT_ID = "mt-project-id";
    private static final String PIPELINE_ID = "mt-pipeline-id";

    private final MultitestingEnvironment multitestingEnvironment;
    private final MultitestingTimeToLiveCalculationService service;
    private final EffectiveMultitestingTimeToLiveSettingsWithSources expectedResult;

    public MultitestingTimeToLiveCalculationServiceTest(TestCase testCase) {
        this.expectedResult = testCase.expectedResult;

        var sourceCascade = testCase.sourceCascade;

        PipelinesDao pipelinesDao = mock(PipelinesDao.class);
        ProjectsDao projectsDao = mock(ProjectsDao.class);
        service = new MultitestingTimeToLiveCalculationService(projectsDao, pipelinesDao,
            sourceCascade.get(Source.GLOBAL));

        ProjectEntity project = new ProjectEntity();
        project.setMultitestingTimeToLiveSettings(sourceCascade.get(Source.PROJECT));
        when(projectsDao.get(eq(PROJECT_ID))).thenReturn(project);

        PipelineEntity pipeline = new PipelineEntity();
        StoredConfigurationEntity currentPipelineConfiguration = new StoredConfigurationEntity();
        currentPipelineConfiguration.setMultitestingTimeToLiveSettings(sourceCascade.get(Source.PIPELINE));
        pipeline.setCurrentConfiguration(currentPipelineConfiguration);
        when(pipelinesDao.get(eq(PIPELINE_ID))).thenReturn(pipeline);

        multitestingEnvironment = new MultitestingEnvironment(MULTITESTING_ID, PROJECT_ID, PIPELINE_ID,
            sourceCascade.get(Source.MULTITESTING));
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return List.of(
            new Object[]{TestCase.singleSource(Source.GLOBAL)},
            new Object[]{TestCase.singleSource(Source.PROJECT)},
            new Object[]{TestCase.singleSource(Source.PIPELINE)},
            new Object[]{TestCase.singleSource(Source.MULTITESTING)},
            new Object[]{
                new TestCase(
                    ImmutableMap.<Source, MultitestingTimeToLiveSettings>builder()
                        .put(Source.GLOBAL, ttlSettings(null, null, null))
                        .put(Source.PROJECT, ttlSettings(1, null, null))
                        .put(Source.PIPELINE, ttlSettings(null, 2, null))
                        .put(Source.MULTITESTING, ttlSettings(null, null, 3))
                        .build(),
                    EffectiveMultitestingTimeToLiveSettingsWithSources.newBuilder()
                        .withDaysBetweenClosingTicketsAndCleanup(new ValueWithSource<>(1, Source.PROJECT))
                        .withDaysBetweenLastJobExecutionAndComment(new ValueWithSource<>(2, Source.PIPELINE))
                        .withDaysBetweenCommentAndCleanup(new ValueWithSource<>(3, Source.MULTITESTING))
                        .build(),
                    "triangle 1")
            },
            new Object[]{
                new TestCase(
                    ImmutableMap.<Source, MultitestingTimeToLiveSettings>builder()
                        .put(Source.GLOBAL, ttlSettings(null, null, null))
                        .put(Source.PROJECT, ttlSettings(1, 2, 3))
                        .put(Source.PIPELINE, ttlSettings(null, 4, 5))
                        .put(Source.MULTITESTING, ttlSettings(null, null, 6))
                        .build(),
                    EffectiveMultitestingTimeToLiveSettingsWithSources.newBuilder()
                        .withDaysBetweenClosingTicketsAndCleanup(new ValueWithSource<>(1, Source.PROJECT))
                        .withDaysBetweenLastJobExecutionAndComment(new ValueWithSource<>(4, Source.PIPELINE))
                        .withDaysBetweenCommentAndCleanup(new ValueWithSource<>(6, Source.MULTITESTING))
                        .build(),
                    "triangle 2")
            },
            new Object[]{
                new TestCase(
                    ImmutableMap.<Source, MultitestingTimeToLiveSettings>builder()
                        .put(Source.GLOBAL, TTL_SETTINGS_FOR_SINGLE_SOURCE)
                        .put(Source.PROJECT, ttlSettings(null, 2, 2))
                        .put(Source.PIPELINE, ttlSettings(null, null, null))
                        .put(Source.MULTITESTING, ttlSettings(null, null, null))
                        .build(),
                    EffectiveMultitestingTimeToLiveSettingsWithSources.newBuilder()
                        .withDaysBetweenClosingTicketsAndCleanup(new ValueWithSource<>(1, Source.GLOBAL))
                        .withDaysBetweenLastJobExecutionAndComment(new ValueWithSource<>(2, Source.PROJECT))
                        .withDaysBetweenCommentAndCleanup(new ValueWithSource<>(2, Source.PROJECT))
                        .build(),
                    "some settings overridden in project")
            }
        );
    }

    @Test
    public void testGetEffectiveTimeToLiveSettings() {
        assertThat(service.getEffectiveTimeToLiveSettings(multitestingEnvironment))
            .isEqualToComparingFieldByFieldRecursively(expectedResult);
    }

    private static class TestCase {
        final Map<Source, MultitestingTimeToLiveSettings> sourceCascade;
        final EffectiveMultitestingTimeToLiveSettingsWithSources expectedResult;
        final String description;

        TestCase(Map<Source, MultitestingTimeToLiveSettings> sourceCascade,
                 EffectiveMultitestingTimeToLiveSettingsWithSources expectedResult, String description) {
            this.sourceCascade = sourceCascade;
            this.expectedResult = expectedResult;
            this.description = description;
        }

        static TestCase singleSource(Source source) {
            Map<Source, MultitestingTimeToLiveSettings> sourceCascade = new HashMap<>();
            for (Source s : Source.values()) {
                sourceCascade.put(s, s == source ? TTL_SETTINGS_FOR_SINGLE_SOURCE : null);
            }

            EffectiveMultitestingTimeToLiveSettingsWithSources expectedResult =
                EffectiveMultitestingTimeToLiveSettingsWithSources.newBuilder()
                    .withDaysBetweenClosingTicketsAndCleanup(new ValueWithSource<>(
                        TTL_SETTINGS_FOR_SINGLE_SOURCE.getDaysBetweenClosingTicketsAndCleanup(), source))
                    .withDaysBetweenLastJobExecutionAndComment(new ValueWithSource<>(
                        TTL_SETTINGS_FOR_SINGLE_SOURCE.getDaysBetweenLastJobExecutionAndComment(), source))
                    .withDaysBetweenCommentAndCleanup(new ValueWithSource<>(
                        TTL_SETTINGS_FOR_SINGLE_SOURCE.getDaysBetweenCommentAndCleanup(), source))
                    .build();

            return new TestCase(
                Collections.unmodifiableMap(sourceCascade),
                expectedResult,
                String.format("single source: %s", source));
        }

        @Override
        public String toString() {
            return description;
        }
    }

    private static MultitestingTimeToLiveSettings ttlSettings(
        Integer daysBetweenClosingTicketsAndCleanup,
        Integer daysBetweenLastJobExecutionAndComment,
        Integer daysBetweenCommentAndCleanup) {

        return new MultitestingTimeToLiveSettings()
            .withDaysBetweenClosingTicketsAndCleanup(daysBetweenClosingTicketsAndCleanup)
            .withDaysBetweenLastJobExecutionAndComment(daysBetweenLastJobExecutionAndComment)
            .withDaysBetweenCommentAndCleanup(daysBetweenCommentAndCleanup);
    }

}
