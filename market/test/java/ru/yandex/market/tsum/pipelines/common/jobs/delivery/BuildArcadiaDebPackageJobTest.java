package ru.yandex.market.tsum.pipelines.common.jobs.delivery;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.validation.constraints.NotNull;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.tsum.clients.sandbox.SandboxClient;
import ru.yandex.market.tsum.clients.sandbox.SandboxTask;
import ru.yandex.market.tsum.clients.sandbox.TaskInputDto;
import ru.yandex.market.tsum.clients.sandbox.TaskResource;
import ru.yandex.market.tsum.clients.startrek.StartrekCommentNotification;
import ru.yandex.market.tsum.context.TestTsumJobContext;
import ru.yandex.market.tsum.pipe.engine.definition.context.JobContext;
import ru.yandex.market.tsum.pipe.engine.definition.context.JobProgressContext;
import ru.yandex.market.tsum.pipe.engine.definition.context.ResourcesJobContext;
import ru.yandex.market.tsum.pipe.engine.definition.resources.Resource;
import ru.yandex.market.tsum.pipe.engine.runtime.config.JobTesterConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.JobTester;
import ru.yandex.market.tsum.pipe.engine.runtime.notifications.Notificator;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.JobState;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.PipeLaunch;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.PipeLaunchRefImpl;
import ru.yandex.market.tsum.pipelines.common.resources.DeliveryPipelineParams;
import ru.yandex.market.tsum.pipelines.common.resources.FixVersion;
import ru.yandex.market.tsum.pipelines.common.resources.ReleaseInfo;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.tsum.test_utils.SandboxTestUtils.getParameterValue;
import static ru.yandex.market.tsum.test_utils.SandboxTestUtils.successfulTaskDto;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 19.03.18
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {JobTesterConfig.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class BuildArcadiaDebPackageJobTest {
    private static final Long TASK_ID = 1L;
    private static final String DIST_REPOSITORY = "market-common";
    private static final String MODULE_PATH = "arcadia/market/svn-data";
    private static final String PACKAGE_NAME = "yandex-market-svn-data";
    private static final String FIX_VERSION_NAME = "2018.1.1";
    private static final String REVISION = "2";

    @Autowired
    private JobTester jobTester;
    private SandboxClient sandboxClient;
    private ArgumentCaptor<TaskInputDto> taskInputCaptor;

    @Before
    public void setUp() {
        sandboxClient = mock(SandboxClient.class);

        when(sandboxClient.newSandboxTaskRunner()).thenCallRealMethod();

        SandboxTask taskDto = successfulTaskDto(TASK_ID);

        taskInputCaptor = ArgumentCaptor.forClass(TaskInputDto.class);
        when(sandboxClient.createTask(taskInputCaptor.capture())).thenReturn(taskDto);
        when(sandboxClient.getTask(TASK_ID)).thenReturn(taskDto);
    }

    @Test
    public void test() throws Exception {
        BuildArcadiaDebPackageJob sut = jobTester.jobInstanceBuilder(BuildArcadiaDebPackageJob.class)
            .withResources(
                BuildArcadiaDebPackageJob.configBuilder()
                    .withDistRepository(DIST_REPOSITORY)
                    .withModulePath(MODULE_PATH)
                    .withPackageName(PACKAGE_NAME)
                    .withRunTests(true)
                    .withDebUser("robot-mrkt-idx-sb")
                    .withDebMail("robot-mrkt-idx-sb@yandex-team.ru")
                    .withSshKeyName("robot-mrkt-idx-sb")
                    .withGpgPrivateKeyName("robot-mrkt-idx-sb-gpg-private")
                    .withGpgPublicKeyName("robot-mrkt-idx-sb-gpg-public")
                    .withYtTokenKeyName("robot-mrkt-idx-sb-yt-token")
                    .withGenerateUnstripBinary(true)
                    .build()
                    .toArray(new Resource[0])
            )
            .withResource(new DeliveryPipelineParams(REVISION, "1", "1"))
            .withResource(new ReleaseInfo(new FixVersion(1, FIX_VERSION_NAME), "MARKETINFRA-1"))
            .withBean(sandboxClient)
            .withBean(mock(Notificator.class))
            .create();

        Notificator notificator = mock(Notificator.class);

        TestTsumJobContext context = mock(TestTsumJobContext.class);
        JobProgressContext jobProgressContext = mock(JobProgressContext.class);
        ResourcesJobContext resourcesJobContext = mock(ResourcesJobContext.class);
        when(context.resources()).thenReturn(resourcesJobContext);
        when(context.progress()).thenReturn(jobProgressContext);
        when(context.notifications()).thenReturn(notificator);
        when(context.getPipeLaunch())
            .thenReturn(
                PipeLaunch.builder()
                    .withTriggeredBy("me")
                    .withLaunchRef(PipeLaunchRefImpl.create("pipe-id"))
                    .withStages(Collections.emptyList())
                    .withProjectId("prj")
                    .build()
            );

        sut.execute(context);

        TaskInputDto taskInputDto = taskInputCaptor.getValue();
        assertEquals(DIST_REPOSITORY, getParameterValue(taskInputDto, "dist_repos"));
        assertEquals(MODULE_PATH, getParameterValue(taskInputDto, "module_path"));
        assertEquals(
            FIX_VERSION_NAME + ".0",
            getParameterValue(taskInputDto, "package_version")
        );
        assertEquals(REVISION, getParameterValue(taskInputDto, "revision"));
    }

    @Test
    public void startrekNotificationWorks() {
        JobState jobState = mock(JobState.class);
        when(jobState.getTitle()).thenReturn("foo");

        JobContext context = mock(JobContext.class);
        when(context.getPipeLaunchUrl()).thenReturn("http://example.yandex.net/pipe");
        when(context.getJobLaunchDetailsUrl()).thenReturn("http://example.yandex.net/pipe/job");
        when(context.getJobState()).thenReturn(jobState);

        List<TaskResource> resources = new ArrayList<>();
        resources.add(
            createTaskResource("http://example.yandex.net/bar", "BAR", "bar desc")
        );
        resources.add(
            createTaskResource("http://example.yandex.net/baz", "BAZ", "baz desc")
        );

        StartrekCommentNotification notification = BuildArcadiaDebPackageNotifications.createStartrekNotification(
            context,
            "yandex-market-foo",
            "1.0",
            resources
        );

        String expectedComment = "" +
            "**foo:** Собран пакет yandex-market-foo версии 1.0.\n" +
            "\n" +
            "Джоба Sandbox создала ресурсы:\n" +
            "- ((http://example.yandex.net/bar BAR)): bar desc\n" +
            "- ((http://example.yandex.net/baz BAZ)): baz desc\n" +
            "\n" +
            "((http://example.yandex.net/pipe Перейти к пайплайну))\n" +
            "((http://example.yandex.net/pipe/job Перейти к пайплайн задаче))";

        assertEquals(expectedComment, notification.getStartrekComment());
    }

    private static @NotNull
    TaskResource createTaskResource(String httpLink, String type, String description) {
        TaskResource resource = new TaskResource(1);
        resource.setHttpLink(httpLink);
        resource.setType(type);
        resource.setDescription(description);
        return resource;
    }
}
