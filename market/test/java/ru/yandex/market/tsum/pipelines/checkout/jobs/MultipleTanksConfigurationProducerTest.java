package ru.yandex.market.tsum.pipelines.checkout.jobs;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.hamcrest.CustomTypeSafeMatcher;
import org.hamcrest.collection.IsCollectionWithSize;
import org.hamcrest.collection.IsIterableWithSize;
import org.hamcrest.core.AllOf;
import org.hamcrest.core.Every;
import org.hamcrest.core.Is;
import org.hamcrest.core.StringContains;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.market.tsum.pipe.engine.definition.ActualBooleanResource;
import ru.yandex.market.tsum.pipe.engine.definition.Pipeline;
import ru.yandex.market.tsum.pipe.engine.definition.builder.PipelineBuilder;
import ru.yandex.market.tsum.pipe.engine.runtime.config.MockCuratorConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.config.PipeServicesConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.config.TestConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.di.model.ResourceRef;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.PipeTester;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.JobLaunch;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.JobState;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.PipeLaunch;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {TestConfig.class, PipeServicesConfig.class, MockCuratorConfig.class})
public class MultipleTanksConfigurationProducerTest {
    @Autowired
    private PipeTester pipeTester;

    @Test
    public void all_jobs_should_fail_if_no_tank_config_defined() {
        final MultipleTanksShootingConfiguration configuration = new MultipleTanksShootingConfiguration();
        final String pipeLaunchId = pipeTester.runPipeToCompletion(pipeline(),
            Collections.singletonList(configuration));
        final PipeLaunch pipeLaunch = pipeTester.getPipeLaunch(pipeLaunchId);
        final Map<String, JobState> jobs = pipeLaunch.getJobs();
        allJobsFailed(jobs.values());
        for (JobState value : jobs.values()) {
            final JobLaunch lastLaunch = value.getLastLaunch();
            final String executionExceptionStacktrace = lastLaunch.getExecutionExceptionStacktrace();
            Assert.assertNotNull(executionExceptionStacktrace);
            Assert.assertThat(executionExceptionStacktrace, new StringContains("Не заданы танки, для стрельбы"));
        }
    }

    @Test
    public void all_jobs_should_fail_if_tank_used_more_than_once() {
        final MultipleTanksShootingConfiguration configuration = new MultipleTanksShootingConfiguration();
        configuration.setPerTankShootingOptions(Arrays.asList(
            getPerTankShootingOptions("tank1"),
            getPerTankShootingOptions("tank2"),
            getPerTankShootingOptions("tank3")
        ));
        final String pipeLaunchId = pipeTester.runPipeToCompletion(pipeline(),
            Collections.singletonList(configuration));
        final PipeLaunch pipeLaunch = pipeTester.getPipeLaunch(pipeLaunchId);
        final Map<String, JobState> jobs = pipeLaunch.getJobs();
        allJobsFailed(jobs.values());
        for (JobState value : jobs.values()) {
            final JobLaunch lastLaunch = value.getLastLaunch();
            final String executionExceptionStacktrace = lastLaunch.getExecutionExceptionStacktrace();
            Assert.assertNotNull(executionExceptionStacktrace);
            Assert.assertThat(executionExceptionStacktrace, new StringContains("Текущая конфигурация может запустить " +
                "параллельно только 2 танков"));
        }
    }

    @Test
    public void all_jobs_should_fail_if_too_many_tank_configs_defined() {
        final MultipleTanksShootingConfiguration configuration = new MultipleTanksShootingConfiguration();
        configuration.setPerTankShootingOptions(List.of(
            getPerTankShootingOptions("tank1"),
            getPerTankShootingOptions("tank1")
        ));
        final String pipeLaunchId = pipeTester.runPipeToCompletion(pipeline(),
            Collections.singletonList(configuration));
        final PipeLaunch pipeLaunch = pipeTester.getPipeLaunch(pipeLaunchId);
        final Map<String, JobState> jobs = pipeLaunch.getJobs();
        Assert.assertThat(jobs.values(), IsCollectionWithSize.hasSize(2));
        for (JobState value : jobs.values()) {
            final JobLaunch lastLaunch = value.getLastLaunch();
            final String executionExceptionStacktrace = lastLaunch.getExecutionExceptionStacktrace();
            Assert.assertNotNull(executionExceptionStacktrace);
            Assert.assertThat(executionExceptionStacktrace, new StringContains(" танк используется в " +
                "стрельбах несколько раз. Список повторяющихся танков: tank1"));
        }
        allJobsFailed(jobs.values());
    }

    @Test
    public void single_boolean_true_produced_if_single_per_tank_config_has_passed() {
        final MultipleTanksShootingConfiguration configuration = new MultipleTanksShootingConfiguration();
        configuration.setPerTankShootingOptions(List.of(
            getPerTankShootingOptions("tank1")
        ));
        final MultipleTanksShootingConfiguration.DefaultShootingOptions defaultShootingOptions =
            getDefaultShootingOptions();
        configuration.setDefaultShootingOptions(defaultShootingOptions);
        final Pipeline pipeline = pipeline();
        final String pipeLaunchId = pipeTester.runPipeToCompletion(pipeline,
            Collections.singletonList(configuration));
        final PipeLaunch pipeLaunch = pipeTester.getPipeLaunch(pipeLaunchId);
        final Map<String, JobState> jobs = pipeLaunch.getJobs();
        Assert.assertThat(jobs.values(), IsCollectionWithSize.hasSize(2));
        assertProducesBooleanResource(pipeLaunchId, "tank1", true);
        assertProducesBooleanResource(pipeLaunchId, "tank2", false);
        allJobsSucceed(jobs.values());
    }

    @Test
    public void single_tank_config_produced() {
        final MultipleTanksShootingConfiguration configuration = new MultipleTanksShootingConfiguration();
        configuration.setPerTankShootingOptions(List.of(
            getPerTankShootingOptions("tank1")
        ));
        final MultipleTanksShootingConfiguration.DefaultShootingOptions defaultShootingOptions =
            getDefaultShootingOptions();
        configuration.setDefaultShootingOptions(defaultShootingOptions);
        int cartRepeats = 666;
        defaultShootingOptions.
            setCheckouterConfig(PandoraCheckouterConfigImpl.builder()
                .setCartRepeats(cartRepeats)
                .build());
        final Pipeline pipeline = pipeline();
        final String pipeLaunchId = pipeTester.runPipeToCompletion(pipeline,
            Collections.singletonList(configuration));
        final PipeLaunch pipeLaunch = pipeTester.getPipeLaunch(pipeLaunchId);
        final Map<String, JobState> jobs = pipeLaunch.getJobs();
        Assert.assertThat(jobs.values(), IsCollectionWithSize.hasSize(2));
        assertProducesBooleanResource(pipeLaunchId, "tank1", true);
        assertProducesBooleanResource(pipeLaunchId, "tank2", false);
        allJobsSucceed(jobs.values());
        final List<MultipleTanksShootingBundle> resources =
            pipeTester.getProducedResourcesOfType(pipeLaunchId, "tank1", MultipleTanksShootingBundle.class);
        Assert.assertThat(() -> resources.stream()
                .map(MultipleTanksShootingBundle::getCheckouterConfig)
                .map(PandoraCheckouterConfigImpl::getCartRepeats)
                .iterator(),
            AllOf.allOf(
                IsIterableWithSize.iterableWithSize(1),
                Every.everyItem(Is.is(cartRepeats))
            ));
    }

    public void single_tank_config_produced_with_per_tank_checkouter_config() {
        final MultipleTanksShootingConfiguration configuration = new MultipleTanksShootingConfiguration();
        final MultipleTanksShootingConfiguration.PerTankShootingOptions perTankShootingOptions =
            getPerTankShootingOptions("tank1");
        final int cartRepeats = 666;
        perTankShootingOptions.setCheckouterConfig(PandoraCheckouterConfigImpl.builder()
            .setCartRepeats(cartRepeats)
            .build());
        configuration.setPerTankShootingOptions(Collections.singletonList(perTankShootingOptions));
        final MultipleTanksShootingConfiguration.DefaultShootingOptions defaultShootingOptions =
            getDefaultShootingOptions();
        defaultShootingOptions.setCheckouterConfig(
            PandoraCheckouterConfigImpl.builder()
                .setCartRepeats(cartRepeats + 1)
                .build()
        );
        configuration.setDefaultShootingOptions(defaultShootingOptions);
        final Pipeline pipeline = pipeline();
        final String pipeLaunchId = pipeTester.runPipeToCompletion(pipeline,
            Collections.singletonList(configuration));
        final PipeLaunch pipeLaunch = pipeTester.getPipeLaunch(pipeLaunchId);
        final Map<String, JobState> jobs = pipeLaunch.getJobs();
        Assert.assertThat(jobs.values(), IsCollectionWithSize.hasSize(2));
        assertProducesBooleanResource(pipeLaunchId, "tank1", true);
        assertProducesBooleanResource(pipeLaunchId, "tank2", false);
        allJobsSucceed(jobs.values());
        final List<PandoraCheckouterConfigImpl> producedCheckouterResources =
            pipeTester.getProducedResourcesOfType(pipeLaunchId, "tank1", PandoraCheckouterConfigImpl.class);
        Assert.assertThat(() -> producedCheckouterResources
                .stream().map(PandoraCheckouterConfigImpl::getCartRepeats)
                .iterator(),
            AllOf.allOf(
                IsIterableWithSize.iterableWithSize(1),
                Every.everyItem(Is.is(cartRepeats))
            ));
    }

    public void single_tank_config_produced_with_customized_checkouter_config() {
        final MultipleTanksShootingConfiguration configuration = new MultipleTanksShootingConfiguration();
        final MultipleTanksShootingConfiguration.PerTankShootingOptions perTankShootingOptions =
            getPerTankShootingOptions("tank1");
        final int cartRepeats = 666;
        perTankShootingOptions.setCheckouterConfig(
            PandoraCheckouterConfigImpl.builder()
                .setCartRepeats(cartRepeats)
                .build()
        );
        configuration.setPerTankShootingOptions(Collections.singletonList(perTankShootingOptions));
        final MultipleTanksShootingConfiguration.DefaultShootingOptions defaultShootingOptions =
            getDefaultShootingOptions();
        defaultShootingOptions.setCheckouterConfig(null);
        configuration.setDefaultShootingOptions(defaultShootingOptions);
        final Pipeline pipeline = pipeline();
        final String pipeLaunchId = pipeTester.runPipeToCompletion(pipeline,
            Collections.singletonList(configuration));
        final PipeLaunch pipeLaunch = pipeTester.getPipeLaunch(pipeLaunchId);
        final Map<String, JobState> jobs = pipeLaunch.getJobs();
        Assert.assertThat(jobs.values(), IsCollectionWithSize.hasSize(2));
        assertProducesBooleanResource(pipeLaunchId, "tank1", true);
        assertProducesBooleanResource(pipeLaunchId, "tank2", false);
        allJobsSucceed(jobs.values());
        final List<PandoraCheckouterConfigImpl> producedCheckouterResources =
            pipeTester.getProducedResourcesOfType(pipeLaunchId, "tank1", PandoraCheckouterConfigImpl.class);
        Assert.assertThat(() -> producedCheckouterResources
                .stream().map(PandoraCheckouterConfigImpl::getCartRepeats)
                .iterator(),
            AllOf.allOf(
                IsIterableWithSize.iterableWithSize(1),
                Every.everyItem(Is.is(cartRepeats))
            ));
    }

    @NotNull
    private MultipleTanksShootingConfiguration.DefaultShootingOptions getDefaultShootingOptions() {
        final MultipleTanksShootingConfiguration.DefaultShootingOptions defaultShootingOptions =
            new MultipleTanksShootingConfiguration.DefaultShootingOptions();
        defaultShootingOptions.setCheckouterConfig(new PandoraCheckouterConfigImpl.Builder().build());
        defaultShootingOptions.setLoyaltyShootingOptions(LoyaltyShootingOptions.builder().build());
        defaultShootingOptions.setRegionSpecificConfig(PandoraRegionSpecificConfigImpl.builder().build());
        defaultShootingOptions.setMutableConfig(PandoraMutableConfigImpl.builder().build());
        return defaultShootingOptions;
    }

    private void assertProducesBooleanResource(String pipeLaunchId, String jobId, Boolean expected) {
        final List<ResourceRef> producedResources = pipeTester.getJobLastLaunch(pipeLaunchId,
            jobId).getProducedResources().getResources();
        Assert.assertThat(producedResources, IsCollectionWithSize.hasSize(2));
        final Collection<ActualBooleanResource> resources =
            pipeTester.getProducedResourcesOfType(pipeLaunchId, jobId, ActualBooleanResource.class);
        Assert.assertThat(
            resources,
            AllOf.allOf(IsIterableWithSize.iterableWithSize(1),
                Every.everyItem(new CustomTypeSafeMatcher<>(String.format("%s produces %b", jobId, expected)) {
                    @Override
                    protected boolean matchesSafely(ActualBooleanResource item) {
                        return item.getValue().equals(expected);
                    }
                })));
    }

    private void allJobsFailed(Collection<JobState> jobs) {
        Assert.assertThat(jobs,
            Every.everyItem(new CustomTypeSafeMatcher<>("Все джобы завершились с ошибкой") {
                @Override
                protected boolean matchesSafely(JobState item) {
                    return item.isLastStatusChangeTypeFailed();
                }
            }));
    }

    private void allJobsSucceed(Collection<JobState> jobs) {
        Assert.assertThat(jobs,
            Every.everyItem(new CustomTypeSafeMatcher<>("Все джобы завершились успешно") {
                @Override
                protected boolean matchesSafely(JobState item) {
                    return !item.isLastStatusChangeTypeFailed();
                }
            }));
    }

    private MultipleTanksShootingConfiguration.PerTankShootingOptions getPerTankShootingOptions(String tankBaseUrl) {
        final MultipleTanksShootingConfiguration.PerTankShootingOptions options =
            new MultipleTanksShootingConfiguration.PerTankShootingOptions();
        final PandoraTankConfigImpl tankConfig = new PandoraTankConfigImpl();
        tankConfig.setTankBaseUrl(tankBaseUrl);
        options.setTankConfig(tankConfig);
        return options;
    }

    static Pipeline pipeline() {
        PipelineBuilder builder = PipelineBuilder.create();
        builder.withJob(MultipleTanksConfigurationProducer.class, "tank1");
        builder.withJob(MultipleTanksConfigurationProducer.class, "tank2");
        builder.withManualResource(MultipleTanksShootingConfiguration.class);
        return builder.build();
    }
}
