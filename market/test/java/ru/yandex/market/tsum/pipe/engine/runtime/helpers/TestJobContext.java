package ru.yandex.market.tsum.pipe.engine.runtime.helpers;

import org.bson.types.ObjectId;
import org.mockito.Mockito;
import ru.yandex.market.request.trace.Module;
import ru.yandex.market.tsum.pipe.engine.definition.resources.Resource;
import ru.yandex.market.tsum.pipe.engine.definition.context.JobProgressContext;
import ru.yandex.market.tsum.pipe.engine.definition.variables.JobVariablesProvider;
import ru.yandex.market.tsum.pipe.engine.definition.job.JobProgress;
import ru.yandex.market.tsum.pipe.engine.definition.context.impl.JobProgressContextImpl;
import ru.yandex.market.tsum.pipe.engine.definition.context.impl.LaunchJobContext;
import ru.yandex.market.tsum.pipe.engine.runtime.notifications.Notificator;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.JobState;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.PipeLaunch;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.TaskState;
import ru.yandex.market.tsum.pipe.engine.source_code.SourceCodeService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static org.mockito.Mockito.mock;
import static ru.yandex.common.util.collections.CollectionUtils.isEmpty;

public class TestJobContext extends LaunchJobContext {

    private static final String DEFAULT_JOB_ID = "1";
    private static final String DEFAULT_FULL_JOB_ID = "1:1:1";
    private static final String DEFAULT_PIPE_LAUNCH_URL = "test-job-context-pipe-launch-url";
    private static final String DEFAULT_JOB_LAUNCH_URL = "test-job-context-job-launch-url";
    private static final String DEFAULT_PIPE_ID = "testPipeId";
    private static final String DEFAULT_USER = "dummy-user";
    private static final ObjectId DEFAULT_PIPE_LAUNCH_ID = new ObjectId("111111111111111111111111");

    private final List<Progress> progressList = new ArrayList<>();
    private String jobId = DEFAULT_JOB_ID;
    private String fullJobId = DEFAULT_FULL_JOB_ID;
    private String pipeLaunchUrl = DEFAULT_PIPE_LAUNCH_URL;
    private String jobLaunchDetailsUrl = DEFAULT_JOB_LAUNCH_URL;
    private JobState jobStateMock = mock(JobState.class);
    private JobVariablesProvider jobVariablesProviderMock = mock(JobVariablesProvider.class);

    private JobProgressContextMock jobProgressContext = new JobProgressContextMock();

    public TestJobContext() {
        this(true);
    }

    public TestJobContext(boolean mockPipeLaunch) {
        super(
            LaunchJobContext.builder()
                .withSourceCodeEntityService(mock(SourceCodeService.class))
                .withNotificator(mock(Notificator.class))
                .withPipeLaunch(mock(PipeLaunch.class))
        );

        // this prevents unnecessary mockito stubbings error.
        if (mockPipeLaunch) {
            Mockito.when(this.pipeLaunch.getPipeId()).thenReturn(DEFAULT_PIPE_ID);
            Mockito.when(this.pipeLaunch.getId()).thenReturn(DEFAULT_PIPE_LAUNCH_ID);
            Mockito.when(this.pipeLaunch.getTriggeredBy()).thenReturn(DEFAULT_USER);
            Mockito.when(this.pipeLaunch.getTriggeredByEmail()).thenCallRealMethod();
        }
    }

    public TestJobContext(String user) {
        this();
        Mockito.when(this.pipeLaunch.getTriggeredBy()).thenReturn(user);
    }

    @Override
    public String getPipeLaunchUrl() {
        return pipeLaunchUrl;
    }

    public void setPipeLaunchUrl(String pipeLaunchUrl) {
        this.pipeLaunchUrl = pipeLaunchUrl;
    }

    @Override
    public String getJobLaunchDetailsUrl() {
        return jobLaunchDetailsUrl;
    }

    public void setJobLaunchDetailsUrl(String jobLaunchDetailsUrl) {
        this.jobLaunchDetailsUrl = jobLaunchDetailsUrl;
    }

    public List<Resource> getProducedResourcesList() {
        return new ArrayList<>(this.resources().getProducedResources());
    }

    public <R extends Resource> R getResource(Class<R> clazz) {
        return this.resources().getProducedResources().stream()
            .filter(clazz::isInstance)
            .map(clazz::cast)
            .findFirst()
            .orElse(null);
    }

    @Override
    public JobState getJobState() {
        return jobStateMock;
    }

    public void setJobStateMock(JobState jobStateMock) {
        this.jobStateMock = jobStateMock;
    }

    @Override
    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    @Override
    public String getFullJobId() {
        return fullJobId;
    }

    public void setFullJobId(String fullJobId) {
        this.fullJobId = fullJobId;
    }

    public List<Progress> getProgressList() {
        return new ArrayList<>(progressList);
    }

    public Progress getLastProgress() {
        JobProgress progress = jobProgressContext.currentProgress;
        return new Progress(
            progress.getText(), progress.getRatio(), new ArrayList<>(progress.getTaskStates().values())
        );
    }

    @Override
    public JobVariablesProvider variables() {
        return jobVariablesProviderMock;
    }

    public static class Progress {


        private final String statusText;
        private final Float progressRatio;
        private final List<TaskState> taskStates;

        public Progress(String statusText, Float progressRatio, List<TaskState> taskStates) {
            this.statusText = statusText;
            this.progressRatio = progressRatio;
            this.taskStates = isEmpty(taskStates) ? emptyList() : unmodifiableList(new ArrayList<>(taskStates));
        }

        public String getStatusText() {
            return statusText;
        }

        public Float getProgressRatio() {
            return progressRatio;
        }

        /**
         * Never returns null.
         */
        public List<TaskState> getTaskStates() {
            return taskStates;
        }
    }

    @Override
    public JobProgressContext progress() {
        return jobProgressContext;
    }

    private static class JobProgressContextMock implements JobProgressContext {
        private JobProgress currentProgress = new JobProgress();

        @Override
        public void update(
            Function<JobProgressContextImpl.ProgressBuilder, JobProgressContextImpl.ProgressBuilder> callback
        ) {
            callback.apply(JobProgressContextImpl.builder(currentProgress));
        }

        @Override
        public TaskState getTaskState(Module module) {
            return getTaskState(0, module);
        }

        @Override
        public TaskState getTaskState(long index, Module module) {
            return currentProgress.getTaskStates().get(module.name() + index);
        }

        @Override
        public Collection<TaskState> getTaskStates() {
            return currentProgress.getTaskStates().values();
        }
    }
}
