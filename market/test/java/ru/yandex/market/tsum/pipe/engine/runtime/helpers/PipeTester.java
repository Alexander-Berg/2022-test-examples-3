package ru.yandex.market.tsum.pipe.engine.runtime.helpers;

import com.google.common.base.Preconditions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.stereotype.Component;
import ru.yandex.market.tsum.pipe.engine.definition.Pipeline;
import ru.yandex.market.tsum.pipe.engine.definition.resources.Resource;
import ru.yandex.market.tsum.pipe.engine.definition.stage.Stage;
import ru.yandex.market.tsum.pipe.engine.runtime.JobLauncher;
import ru.yandex.market.tsum.pipe.engine.runtime.PipeProvider;
import ru.yandex.market.tsum.pipe.engine.runtime.di.ResourceDao;
import ru.yandex.market.tsum.pipe.engine.runtime.di.model.ResourceRef;
import ru.yandex.market.tsum.pipe.engine.runtime.di.model.ResourceRefContainer;
import ru.yandex.market.tsum.pipe.engine.runtime.di.model.StoredResource;
import ru.yandex.market.tsum.pipe.engine.runtime.di.model.StoredResourceContainer;
import ru.yandex.market.tsum.pipe.engine.runtime.events.ForceSuccessTriggerEvent;
import ru.yandex.market.tsum.pipe.engine.runtime.events.JobExecutorSucceededEvent;
import ru.yandex.market.tsum.pipe.engine.runtime.events.JobRunningEvent;
import ru.yandex.market.tsum.pipe.engine.runtime.events.JobSucceededEvent;
import ru.yandex.market.tsum.pipe.engine.runtime.events.PipeEvent;
import ru.yandex.market.tsum.pipe.engine.runtime.events.ScheduleChangeEvent;
import ru.yandex.market.tsum.pipe.engine.runtime.events.SubscribersSucceededEvent;
import ru.yandex.market.tsum.pipe.engine.runtime.events.TriggerEvent;
import ru.yandex.market.tsum.pipe.engine.runtime.state.PipeLaunchDao;
import ru.yandex.market.tsum.pipe.engine.runtime.state.PipeStateService;
import ru.yandex.market.tsum.pipe.engine.runtime.state.PrepareLaunchParameters;
import ru.yandex.market.tsum.pipe.engine.runtime.state.StageGroupDao;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.JobLaunch;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.PipeLaunch;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.StageGroupState;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.StatusChangeType;
import ru.yandex.market.tsum.pipe.engine.source_code.SourceCodeService;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 16.03.17
 */
@Component
public class PipeTester {
    @Autowired
    private PipeStateService pipeStateService;

    @Autowired
    private TestJobScheduler testJobScheduler;

    @Autowired
    private TestJobWaitingScheduler testJobWaitingScheduler;

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private PipeLaunchDao pipeLaunchDao;

    @Autowired
    private ResourceDao resourceDao;

    @Autowired
    private MongoConverter mongoConverter;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private GenericApplicationContext applicationContext;

    @Autowired
    private StageGroupDao stageService;

    @Autowired
    private SourceCodeService sourceCodeService;

    @Autowired
    private PipeProvider pipeProvider;

    public String runPipeToCompletion(Pipeline pipeline) {
        return runPipeToCompletion(pipeline, null, Collections.emptyList());
    }

    public String runPipeToCompletion(Pipeline pipeline, String stageGroupId) {
        return runPipeToCompletion(pipeline, stageGroupId, Collections.emptyList());
    }

    public String runPipeToCompletion(Pipeline pipeline, List<? extends Resource> manualResources) {
        return runPipeToCompletion(pipeline, null, manualResources);
    }

    public String runPipeToCompletion(Pipeline pipeline, String stageGroupId, List<? extends Resource> manualResources) {
        String pipeId = BeanRegistrar.registerNamedBean(pipeline, applicationContext);
        return runPipeToCompletion(pipeId, stageGroupId, manualResources);
    }

    public String runPipeToCompletion(String pipeId, List<? extends Resource> manualResources) {
        return runPipeToCompletion(pipeId, null, manualResources);
    }

    public String runPipeToCompletion(String pipeId, String stageGroupId, List<? extends Resource> manualResources) {
        Pipeline pipeline = pipeProvider.get(pipeId);

        if (!pipeline.getStages().isEmpty()) {
            if (stageGroupId == null) {
                stageGroupId = UUID.randomUUID().toString();
            }

            createStageGroupState(stageGroupId, pipeline.getStages());
        }

        String pipeLaunchId = pipeStateService
            .activateLaunch(
                PrepareLaunchParameters.builder()
                    .withPipeId(pipeId)
                    .withTriggeredBy("user42")
                    .withStageGroupId(stageGroupId)
                    .withManualResources(manualResources)
                    .withProjectId("prj")
                    .build()
            ).getId().toString();

        runScheduledJobsToCompletion();
        return pipeLaunchId;
    }

    public String activateLaunch(Pipeline pipeline, Resource... manualResources) {
        return activateLaunch(pipeline, Arrays.asList(manualResources));
    }

    public String activateLaunch(Pipeline pipeline, String stageGroupId, Resource... manualResources) {
        return activateLaunch(pipeline, stageGroupId, Arrays.asList(manualResources));
    }

    public String activateLaunch(Pipeline pipeline, List<? extends Resource> manualResources) {
        return activateLaunch(pipeline, null, manualResources);
    }

    public String activateLaunch(Pipeline pipeline, String stageGroupId, List<? extends Resource> manualResources) {
        if (!pipeline.getStages().isEmpty()) {
            if (stageGroupId == null) {
                stageGroupId = UUID.randomUUID().toString();
            }

            createStageGroupState(stageGroupId, pipeline.getStages());
        }

        String pipeId = BeanRegistrar.registerNamedBean(pipeline, applicationContext);
        PipeLaunch pipeLaunch = pipeStateService.activateLaunch(
            PrepareLaunchParameters.builder()
                .withPipeId(pipeId)
                .withTriggeredBy("user42")
                .withStageGroupId(stageGroupId)
                .withManualResources(manualResources)
                .withProjectId("prj")
                .build()
        );

        return pipeLaunch.getId().toString();
    }

    public void runScheduledJobToCompletion(String jobId) {
        testJobScheduler.getTriggeredJobs().stream()
            .filter(j -> j.getJobLaunchId().getJobId().equals(jobId))
            .forEach(j -> jobLauncher.launchJob(j.getJobLaunchId(), DummyFullJobIdFactory.create()));
    }

    public void runScheduledJobsToCompletion() {
        Queue<TestJobScheduler.TriggeredJob> triggerCommands = testJobScheduler.getTriggeredJobs();
        Queue<TestJobWaitingScheduler.SchedulerTriggeredJob> schedulerTriggeredCommands = testJobWaitingScheduler.getTriggeredJobs();
        while (!triggerCommands.isEmpty() || !schedulerTriggeredCommands.isEmpty()) {
            if (!triggerCommands.isEmpty()) {
                TestJobScheduler.TriggeredJob triggeredJob = triggerCommands.poll();
                jobLauncher.launchJob(triggeredJob.getJobLaunchId(), DummyFullJobIdFactory.create());
            } else {
                TestJobWaitingScheduler.SchedulerTriggeredJob triggeredJob = schedulerTriggeredCommands.poll();
                pipeStateService.recalc(triggeredJob.getJobLaunchId().getPipeLaunchId(),
                    new ScheduleChangeEvent(triggeredJob.getJobLaunchId().getJobId()));
            }
        }
    }

    public Thread runScheduledJobsToCompletionAsync() {
        Thread thread = new Thread(this::runScheduledJobsToCompletion);
        thread.start();
        return thread;
    }

    public void createStageGroupState(String id, List<Stage> stages) {
        if (mongoTemplate.findById(id, StageGroupState.class) == null) {
            mongoTemplate.save(
                new StageGroupState(id)
            );
        }
    }

    public PipeLaunch triggerJob(String pipeLaunchId, String jobId) {
        return pipeStateService.recalc(pipeLaunchId, new TriggerEvent(jobId, "user42", false));
    }
    public PipeLaunch forceTriggerJob(String pipeLaunchId, String jobId) {
        return pipeStateService.recalc(pipeLaunchId, new ForceSuccessTriggerEvent(jobId, "user42"));
    }

    public StoredResourceContainer getProducedResources(String pipeLaunchId, String jobId) {
        JobLaunch lastLaunch = getJobLastLaunch(pipeLaunchId, jobId);
        Preconditions.checkState(lastLaunch.getLastStatusChange().getType().equals(StatusChangeType.SUCCESSFUL));

        ResourceRefContainer producedResourceRefs = lastLaunch.getProducedResources();

        return resourceDao.loadResources(producedResourceRefs);
    }

    @SuppressWarnings("unchecked")
    public <T extends Resource> List<T> getProducedResourcesOfType(String pipeLaunchId, String jobId, Class<T> clazz) {
        JobLaunch lastLaunch = getJobLastLaunch(pipeLaunchId, jobId);
        Preconditions.checkState(lastLaunch.getLastStatusChange().getType().equals(StatusChangeType.SUCCESSFUL));

        ResourceRefContainer producedResourceRefs = lastLaunch.getProducedResources();

        final List<ResourceRef> collect = producedResourceRefs.getResources()
            .stream().filter(resourceRef ->
                clazz.isAssignableFrom(
                    sourceCodeService.getResource(resourceRef.getSourceCodeId()).getClazz())).collect(Collectors.toList());
        final StoredResourceContainer storedResourceContainer =
            resourceDao.loadResources(new ResourceRefContainer(collect));
        return storedResourceContainer.getResources()
            .stream().map(storedResource -> (T)storedResource.instantiate(mongoConverter, sourceCodeService))
            .collect(Collectors.toList());
    }

    public StoredResourceContainer getConsumedResources(String pipeLaunchId, String jobId) {
        JobLaunch lastLaunch = getJobLastLaunch(pipeLaunchId, jobId);

        ResourceRefContainer producedResourceRefs = lastLaunch.getConsumedResources();

        return resourceDao.loadResources(producedResourceRefs);
    }

    public JobLaunch getJobLastLaunch(String pipeLaunchId, String jobId) {
        PipeLaunch pipeLaunch = pipeLaunchDao.getById(pipeLaunchId);

        return pipeLaunch.getJobState(jobId).getLastLaunch();
    }

    public PipeLaunch getPipeLaunch(String pipeLaunchId) {
        return pipeLaunchDao.getById(pipeLaunchId);
    }

    public void savePipeLaunch(PipeLaunch pipeLaunch) {
        pipeLaunchDao.save(pipeLaunch);
    }

    @SuppressWarnings("unchecked")
    public <T extends Resource> T getResourceOfType(StoredResourceContainer resources, Class<T> clazz) {
        return (T) resources.instantiate(clazz.getName(), mongoConverter, sourceCodeService);
    }

    @SuppressWarnings("unchecked")
    public <T extends Resource> T getResource(StoredResource resource) {
        return (T)resource.instantiate(mongoConverter, sourceCodeService);
    }

    public void raiseJobExecuteEventsChain(String launchId, String jobId) {
        pipeStateService.recalc(launchId, new JobRunningEvent(jobId, 1, DummyFullJobIdFactory.create()));
        pipeStateService.recalc(launchId, new JobExecutorSucceededEvent(jobId, 1));
        pipeStateService.recalc(launchId, new SubscribersSucceededEvent(jobId, 1));
        pipeStateService.recalc(launchId, new JobSucceededEvent(jobId, 1));
    }

    public PipeLaunch recalcPipeLaunch(String launchId, PipeEvent pipeEvent) {
        return pipeStateService.recalc(launchId, pipeEvent);
    }
}
