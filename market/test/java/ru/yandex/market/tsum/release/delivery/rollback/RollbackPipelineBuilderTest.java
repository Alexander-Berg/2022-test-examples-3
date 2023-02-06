package ru.yandex.market.tsum.release.delivery.rollback;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.tsum.clients.conductor.ConductorBranch;
import ru.yandex.market.tsum.clients.sandbox.SandboxReleaseType;
import ru.yandex.market.tsum.pipe.engine.definition.Pipeline;
import ru.yandex.market.tsum.pipe.engine.definition.job.Job;
import ru.yandex.market.tsum.pipe.engine.definition.stage.Stage;
import ru.yandex.market.tsum.pipe.engine.runtime.di.ResourceService;
import ru.yandex.market.tsum.pipe.engine.runtime.di.model.AbstractResourceContainer;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.JobLaunch;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.JobState;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.StatusChange;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.StatusChangeType;
import ru.yandex.market.tsum.pipelines.common.jobs.conductor_deploy.ConductorDeployJob;
import ru.yandex.market.tsum.pipelines.common.jobs.conductor_deploy.ConductorDeployJobConfig;
import ru.yandex.market.tsum.pipelines.common.jobs.nanny.NannyReleaseJob;
import ru.yandex.market.tsum.pipelines.common.jobs.nanny.NannyReleaseJobConfig;
import ru.yandex.market.tsum.pipelines.idx.IdxUtils;
import ru.yandex.market.tsum.pipelines.idx.jobs.RoleAwareConductorDeployJob;

import static org.mockito.Mockito.when;

/**
 * @author Nikolay Firov <a href="mailto:firov@yandex-team.ru"></a>
 * @date 20.08.18
 */
@RunWith(MockitoJUnitRunner.class)
public class RollbackPipelineBuilderTest {
    private RollbackPipelineBuilder sut;

    @Mock
    private ResourceService resourceService;

    @Mock
    private AbstractResourceContainer resourceContainer;

    @Mock
    private JobState jobState;

    private final Stage stage = new Stage(null, "stable");

    @Before
    public void setup() {
        sut = new RollbackPipelineBuilder(resourceService);
        when(resourceService.loadResources(Mockito.any())).thenReturn(resourceContainer);

        JobLaunch jobLaunch = new JobLaunch(
            0, "", Collections.emptyList(), Collections.singletonList(new StatusChange(StatusChangeType.SUCCESSFUL))
        );

        when(jobState.getJobId()).thenReturn("job-id");
        when(jobState.getLastLaunch()).thenReturn(jobLaunch);
        when(jobState.isSuccessful()).thenReturn(true);

        ConductorDeployJobConfig conductorConfig = ConductorDeployJobConfig.newBuilder(ConductorBranch.STABLE).build();
        when(resourceContainer.getSingleOfType(ConductorDeployJobConfig.class)).thenReturn(conductorConfig);
        when(resourceContainer.getSingleOfTypeIfPresent(ConductorDeployJobConfig.class)).thenReturn(conductorConfig);

        NannyReleaseJobConfig nannyConfig = NannyReleaseJobConfig.builder(SandboxReleaseType.STABLE).build();
        when(resourceContainer.getSingleOfType(NannyReleaseJobConfig.class)).thenReturn(nannyConfig);
        when(resourceContainer.getSingleOfTypeIfPresent(NannyReleaseJobConfig.class)).thenReturn(nannyConfig);
    }

    @Test
    public void createsPipelineForConductorJob() {
        when(jobState.getExecutorClassName()).thenReturn(ConductorDeployJob.class.getName());

        Pipeline pipeline = sut.createPipelineForRollback(Collections.singletonList(jobState), stage);
        List<Job> conductorJobs = pipeline.getJobs().stream()
            .filter(job -> job.getExecutorClass().equals(ConductorDeployJob.class))
            .collect(Collectors.toList());

        Assert.assertEquals(1, conductorJobs.size());
        Assert.assertEquals(true, getConductorRollbackConfig(conductorJobs).isAllowDowngrade());
    }

    @Test
    public void createsPipelineForRoleAwareConductorJob() {
        when(jobState.getExecutorClassName()).thenReturn(RoleAwareConductorDeployJob.class.getName());

        RoleAwareConductorDeployJob.Config customConfig =
            new RoleAwareConductorDeployJob.Config(IdxUtils.ConfigurationDcName.ACTIVE);

        when(resourceContainer.getSingleOfType(RoleAwareConductorDeployJob.Config.class))
            .thenReturn(customConfig);

        Pipeline pipeline = sut.createPipelineForRollback(Collections.singletonList(jobState), stage);
        List<Job> conductorJobs = pipeline.getJobs().stream()
            .filter(job -> job.getExecutorClass().equals(RoleAwareConductorDeployJob.class))
            .collect(Collectors.toList());

        Assert.assertEquals(1, conductorJobs.size());
        Assert.assertEquals(true, getConductorRollbackConfig(conductorJobs).isAllowDowngrade());

        RoleAwareConductorDeployJob.Config customRollbackConfig = (RoleAwareConductorDeployJob.Config)
            conductorJobs.get(0).getStaticResources().stream()
                .filter(x -> x.getClass().equals(RoleAwareConductorDeployJob.Config.class))
                .findFirst().orElseThrow(RuntimeException::new);

        Assert.assertEquals(IdxUtils.ConfigurationDcName.ACTIVE, customRollbackConfig.getConfigurationDcName());
    }

    private ConductorDeployJobConfig getConductorRollbackConfig(List<Job> conductorJobs) {
        return (ConductorDeployJobConfig) conductorJobs.get(0).getStaticResources()
            .stream()
            .filter(x -> x.getClass().equals(ConductorDeployJobConfig.class))
            .findFirst().orElseThrow(RuntimeException::new);
    }

    @Test
    public void createsPipelineForNannyJob() {
        when(jobState.getExecutorClassName()).thenReturn(NannyReleaseJob.class.getName());

        Pipeline pipeline = sut.createPipelineForRollback(Collections.singletonList(jobState), stage);
        List<Job> nannyJobs = pipeline.getJobs().stream()
            .filter(job -> job.getExecutorClass().equals(NannyReleaseJob.class))
            .collect(Collectors.toList());

        Assert.assertEquals(1, nannyJobs.size());

        Assert.assertTrue(
            nannyJobs.get(0).getStaticResources().stream()
                .anyMatch(x -> x.getClass().equals(NannyReleaseJobConfig.class))
        );
    }
}
