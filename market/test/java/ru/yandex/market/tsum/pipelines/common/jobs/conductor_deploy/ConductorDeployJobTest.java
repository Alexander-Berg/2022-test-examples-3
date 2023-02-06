package ru.yandex.market.tsum.pipelines.common.jobs.conductor_deploy;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.util.concurrent.Futures;
import org.apache.commons.collections.CollectionUtils;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.market.tsum.clients.conductor.ConductorBranch;
import ru.yandex.market.tsum.clients.conductor.ConductorClient;
import ru.yandex.market.tsum.clients.conductor.ConductorClient.AutoInstallMode;
import ru.yandex.market.tsum.clients.conductor.ConductorTask;
import ru.yandex.market.tsum.clients.conductor.ConductorTicket;
import ru.yandex.market.tsum.clients.conductor.ConductorTicket.TicketStatus;
import ru.yandex.market.tsum.clients.conductor.filter.FilterType;
import ru.yandex.market.tsum.clients.pollers.Poller;
import ru.yandex.market.tsum.context.impl.ReleaseJobContextImpl;
import ru.yandex.market.tsum.core.notify.common.NotificationCenter;
import ru.yandex.market.tsum.pipe.engine.definition.context.JobContext;
import ru.yandex.market.tsum.pipe.engine.runtime.exceptions.JobManualFailException;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.TestJobContext;
import ru.yandex.market.tsum.pipe.engine.runtime.notifications.Notificator;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.PipeLaunch;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.PipeLaunchRefImpl;
import ru.yandex.market.tsum.pipelines.common.resources.ChangelogEntry;
import ru.yandex.market.tsum.pipelines.common.resources.ConductorPackage;
import ru.yandex.market.tsum.pipelines.common.resources.FixVersion;
import ru.yandex.market.tsum.pipelines.common.resources.ReleaseInfo;

import static org.mockito.Mockito.times;
import static org.springframework.test.annotation.DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD;

/**
 * @author Dmitry Andreev <a href="mailto:AndreevDm@yandex-team.ru"></a>
 * @date 07/06/2017
 */
@DirtiesContext(classMode = BEFORE_EACH_TEST_METHOD)
@ContextConfiguration(classes = {
    ConductorDeployJobTest.ConductorDeployJobContextConfiguration.class,
})
@RunWith(SpringRunner.class)
@SuppressWarnings("checkstyle:magicNumber")
public class ConductorDeployJobTest {
    private static final JobContext CONTEXT = new ReleaseJobContextImpl(ReleaseJobContextImpl.builder()) {
        @Override
        public String getPipeLaunchUrl() {
            return "https://ya.ru/";
        }

        @Override
        public String getJobLaunchDetailsUrl() {
            return "https://ya.ru/42";
        }

        @Override
        public String getFullJobId() {
            return "42:21";
        }

        @Override
        public PipeLaunch getPipeLaunch() {
            return PipeLaunch.builder()
                .withLaunchRef(PipeLaunchRefImpl.create("10"))
                .withTriggeredBy("user42")
                .withCreatedDate(new Date())
                .withStages(Collections.emptyList())
                .withProjectId("prj")
                .build();
        }
    };

    @Rule
    public ExpectedException expected = ExpectedException.none();

    @Autowired
    private ConductorDeployJob conductorDeployJob;

    @Autowired
    private ConductorClient conductorClient;

    @Test
    public void testWithOutRelease() throws Exception {

        List<ConductorPackage> packages = Arrays.asList(
            new ConductorPackage("", "", "a \nb\nc\nd"),
            new ConductorPackage("", "", "b\n c \ne"),
            new ConductorPackage("", "", null),
            new ConductorPackage("", "", null, Arrays.asList(
                new ChangelogEntry("", "f")
            ))
        );

        Assert.assertEquals(
            "Тикет создан автоматически с помощью релизного пайплайна.\n" +
                "Ссылка на пайплайн: https://ya.ru/\n" +
                "Ссылка на задачу: https://ya.ru/42\n" +
                "\n" +
                "Изменения:\n" +
                "a\n" +
                "b\n" +
                "c\n" +
                "d\n" +
                "e\n" +
                "f\n" +
                "\n" +
                "\n" +
                "Pipe job id: 42:21\n" +
                "Pipeline id: 10",
            conductorDeployJob.createComment(CONTEXT, null, packages)
        );
    }


    @Test
    public void testWithRelease() throws Exception {

        List<ConductorPackage> packages = Arrays.asList(
            new ConductorPackage("", "", "b\nc\ne"),
            new ConductorPackage("", "", "a\nb\nc\nd"),
            new ConductorPackage("", "", null)
        );

        ReleaseInfo releaseInfo = new ReleaseInfo(new FixVersion(42, "2017.42 Что-то с чем-то"), "MI-42");

        Assert.assertEquals(
            "Тикет создан автоматически с помощью релизного пайплайна.\n" +
                "Ссылка на пайплайн: https://ya.ru/\n" +
                "Ссылка на задачу: https://ya.ru/42\n" +
                "\n" +
                "Релиз: 2017.42 Что-то с чем-то\n" +
                "Тикет: https://st.yandex-team.ru/MI-42\n" +
                "\n" +
                "Изменения:\n" +
                "b\n" +
                "c\n" +
                "e\n" +
                "a\n" +
                "d\n" +
                "\n" +
                "\n" +
                "Pipe job id: 42:21\n" +
                "Pipeline id: 10",
            conductorDeployJob.createComment(CONTEXT, releaseInfo, packages)
        );
    }

    @Test
    public void testEmpty() throws Exception {

        Assert.assertEquals(
            "Тикет создан автоматически с помощью релизного пайплайна.\n" +
                "Ссылка на пайплайн: https://ya.ru/\n" +
                "Ссылка на задачу: https://ya.ru/42\n" +
                "\n" +
                "\n" +
                "Pipe job id: 42:21\n" +
                "Pipeline id: 10",
            conductorDeployJob.createComment(CONTEXT, null, Collections.emptyList())
        );
    }

    @Test
    public void executionFinishedOnTicketDone() throws Throwable {
        JobContext jobContext = new TestJobContext();

        configureAddTicketStub();

        Mockito.when(conductorClient.getTicketWithTasks(Mockito.anyInt()))
            .thenReturn(Futures.immediateFuture(
                new ConductorTicket(ConductorTicket.builder()
                    .setStatus(TicketStatus.DONE))));

        conductorDeployJob.execute(jobContext);
    }

    @Test
    public void executionNeverPollsTicketWhenNotRequired() throws Throwable {
        JobContext jobContext = new TestJobContext();
        configureAddTicketStub();
        conductorDeployJob.setConfig(
            ConductorDeployJobConfig.newBuilder(ConductorBranch.TESTING)
                .setWaitTicketDone(false)
                .build()
        );
        conductorDeployJob.execute(jobContext);
        Mockito.verify(conductorClient, Mockito.never()).getTicketWithTasks(Mockito.anyInt());
    }

    @Test
    public void autoInstallModeIsPassedToConductorClient() throws Throwable {
        JobContext jobContext = new TestJobContext();
        ArgumentCaptor<AutoInstallMode> captor = ArgumentCaptor.forClass(AutoInstallMode.class);
        configureAddTicketStub(captor::capture);
        conductorDeployJob.setConfig(
            ConductorDeployJobConfig.newBuilder(ConductorBranch.TESTING)
                .setWaitTicketDone(false)
                .setAutoInstallMode(AutoInstallMode.DO_NOT_AUTOINSTALL)
                .build()
        );
        conductorDeployJob.execute(jobContext);
        Assert.assertEquals(AutoInstallMode.DO_NOT_AUTOINSTALL, captor.getValue());
    }

    @Test
    public void executionFailsWhenStatusFail() throws Throwable {
        expected.expect(JobManualFailException.class);
        expected.expectMessage("Expected ticket's http://localhost status to be `done`, but it's `failed`");

        JobContext jobContext = new TestJobContext();

        configureAddTicketStub();

        Mockito.when(conductorClient.getTicketWithTasks(Mockito.anyInt()))
            .thenReturn(Futures.immediateFuture(
                new ConductorTicket(ConductorTicket.builder()
                    .setStatus(TicketStatus.FAILED)
                    .setUrl("http://localhost"))));

        conductorDeployJob.execute(jobContext);
    }

    @Test
    public void executionFailsWhenStatusObsolete() throws Throwable {
        expected.expect(JobManualFailException.class);
        expected.expectMessage("Expected ticket's http://localhost status to be `done`, but it's `obsolete`");

        JobContext jobContext = new TestJobContext();

        configureAddTicketStub();

        Mockito.when(conductorClient.getTicketWithTasks(Mockito.anyInt()))
            .thenReturn(Futures.immediateFuture(
                new ConductorTicket(ConductorTicket.builder()
                    .setStatus(TicketStatus.OBSOLETE)
                    .setUrl("http://localhost"))));

        conductorDeployJob.execute(jobContext);
    }

    @Test
    public void executionFailsWhenStatusRejected() throws Throwable {
        expected.expect(JobManualFailException.class);
        expected.expectMessage("Expected ticket's http://localhost status to be `done`, but it's `rejected`");

        JobContext jobContext = new TestJobContext();

        configureAddTicketStub();

        Mockito.when(conductorClient.getTicketWithTasks(Mockito.anyInt()))
            .thenReturn(Futures.immediateFuture(
                new ConductorTicket(ConductorTicket.builder()
                    .setStatus(TicketStatus.REJECTED)
                    .setUrl("http://localhost"))));

        conductorDeployJob.execute(jobContext);
    }

    @Test
    public void executionFailsWithTaskStatusIfItFailed() throws Throwable {
        expected.expect(JobManualFailException.class);
        expected.expectMessage("Expected ticket's http://localhost tasks to be `done`," +
            " but task CS-FROZEN-TASK is failed");

        conductorDeployJob.setConfig(
            ConductorDeployJobConfig.newBuilder(ConductorBranch.TESTING)
                .waitForProjects("infra")
                .build());

        JobContext jobContext = new TestJobContext();

        configureAddTicketStub();

        Mockito.when(conductorClient.getTicketWithTasks(Mockito.anyInt()))
            .thenReturn(Futures.immediateFuture(
                new ConductorTicket(ConductorTicket.builder()
                    .setTasks(Collections.singletonList(
                        new ConductorTask() {{
                            setName("CS-FROZEN-TASK");
                            setStatus("failed");
                            setProject("infra");
                        }}))
                    .setStatus(TicketStatus.IN_WORK)
                    .setUrl("http://localhost"))));

        conductorDeployJob.execute(jobContext);
    }

    @Test
    public void inWorkTicketExecutionContinues() throws Throwable {
        JobContext jobContext = new TestJobContext();

        configureAddTicketStub();

        Mockito.when(conductorClient.getTicketWithTasks(Mockito.anyInt()))
            .thenReturn(
                Futures.immediateFuture(
                    new ConductorTicket(ConductorTicket.builder()
                        .setTasks(Collections.singletonList(new ConductorTask() {{
                            setStatus("in_work");
                        }}))
                        .setStatus(TicketStatus.IN_WORK))),
                Futures.immediateFuture(
                    new ConductorTicket(ConductorTicket.builder()
                        .setTasks(Collections.singletonList(new ConductorTask() {{
                            setStatus("done");
                        }}))
                        .setStatus(TicketStatus.DONE))));

        conductorDeployJob.execute(jobContext);

        Mockito.verify(conductorClient, times(2))
            .getTicketWithTasks(Mockito.anyInt());
    }

    @Test
    public void frozenTicketExecutionContinues() throws Throwable {
        JobContext jobContext = new TestJobContext();

        configureAddTicketStub();

        Mockito.when(conductorClient.getTicketWithTasks(Mockito.anyInt()))
            .thenReturn(
                Futures.immediateFuture(
                    new ConductorTicket(ConductorTicket.builder()
                        .setTasks(Arrays.asList(
                            new ConductorTask() {{
                                setStatus("frozen");
                            }},
                            new ConductorTask() {{
                                setStatus("done");
                            }}
                        ))
                        .setStatus(TicketStatus.FAILED))),
                Futures.immediateFuture(
                    new ConductorTicket(ConductorTicket.builder()
                        .setTasks(Arrays.asList(
                            new ConductorTask() {{
                                setStatus("done");
                            }},
                            new ConductorTask() {{
                                setStatus("done");
                            }}
                        ))
                        .setStatus(TicketStatus.DONE))));

        conductorDeployJob.execute(jobContext);

        Mockito.verify(conductorClient, times(2))
            .getTicketWithTasks(Mockito.anyInt());
    }

    @Test
    public void maybeMissingTicketWaitProjectExecutionContinues() throws Throwable {
        JobContext jobContext = new TestJobContext();

        conductorDeployJob.setConfig(
            ConductorDeployJobConfig.newBuilder(ConductorBranch.TESTING)
                .waitForProjects("infra")
                .build());

        configureAddTicketStub();

        Mockito.when(conductorClient.getTicketWithTasks(Mockito.anyInt()))
            .thenReturn(
                Futures.immediateFuture(
                    new ConductorTicket(ConductorTicket.builder()
                        .setStatus(TicketStatus.MAYBE_MISSING))),
                Futures.immediateFuture(
                    new ConductorTicket(ConductorTicket.builder()
                        .setTasks(Arrays.asList(
                            new ConductorTask() {{
                                setStatus("new");
                                setProject("infra");
                            }},
                            new ConductorTask() {{
                                setStatus("done");
                            }}
                        ))
                        .setStatus(TicketStatus.IN_WORK))),
                Futures.immediateFuture(
                    new ConductorTicket(ConductorTicket.builder()
                        .setTasks(Arrays.asList(
                            new ConductorTask() {{
                                setStatus("done");
                                setProject("infra");
                            }},
                            new ConductorTask() {{
                                setStatus("done");
                            }}
                        )).setStatus(TicketStatus.DONE))));

        conductorDeployJob.execute(jobContext);

        Mockito.verify(conductorClient, times(3))
            .getTicketWithTasks(Mockito.anyInt());
    }

    @Test
    public void unknownStatusTicketExecutionContinuesWithEmptyTasks() throws Throwable {
        JobContext jobContext = new TestJobContext();

        configureAddTicketStub();

        Mockito.when(conductorClient.getTicketWithTasks(Mockito.anyInt()))
            .thenReturn(
                Futures.immediateFuture(
                    new ConductorTicket(ConductorTicket.builder()
                        .setStatus(TicketStatus.IN_WORK))),
                Futures.immediateFuture(
                    new ConductorTicket(ConductorTicket.builder()
                        .setStatus(TicketStatus.DONE))));

        conductorDeployJob.execute(jobContext);

        Mockito.verify(conductorClient, times(2))
            .getTicketWithTasks(Mockito.anyInt());
    }

    @Test
    public void executionFailsWithAnyFailedTaskAndWatchedProject() throws Throwable {
        expected.expect(JobManualFailException.class);
        expected.expectMessage("Expected ticket's http://localhost tasks to be `done`," +
            " but task CS-FAILED-TASK is failed");

        conductorDeployJob.setConfig(
            ConductorDeployJobConfig.newBuilder(ConductorBranch.TESTING)
                .waitForProjects("infra")
                .build());

        JobContext jobContext = new TestJobContext();

        configureAddTicketStub();

        Mockito.when(conductorClient.getTicketWithTasks(Mockito.anyInt()))
            .thenReturn(Futures.immediateFuture(
                new ConductorTicket(ConductorTicket.builder()
                    .setTasks(Arrays.asList(
                        new ConductorTask() {{
                            setName("CS-FAILED-TASK");
                            setStatus("failed");
                            setProject("infra");
                        }},
                        new ConductorTask() {{
                            setStatus("done");
                            setProject("infra");
                        }}))
                    .setStatus(TicketStatus.IN_WORK)
                    .setUrl("http://localhost"))));

        conductorDeployJob.execute(jobContext);
    }

    @Test
    public void inworkExecutionEndsWithAllDoneTaskAndWatchedProject() throws Throwable {
        conductorDeployJob.setConfig(
            ConductorDeployJobConfig.newBuilder(ConductorBranch.TESTING)
                .waitForProjects("infra")
                .build());

        JobContext jobContext = new TestJobContext();

        configureAddTicketStub();

        Mockito.when(conductorClient.getTicketWithTasks(Mockito.anyInt()))
            .thenReturn(Futures.immediateFuture(
                new ConductorTicket(ConductorTicket.builder()
                    .setTasks(Arrays.asList(
                        new ConductorTask() {{
                            setStatus("done");
                            setProject("infra");
                        }},
                        new ConductorTask() {{
                            setStatus("done");
                            setProject("infra");
                        }}))
                    .setStatus(TicketStatus.IN_WORK))));

        conductorDeployJob.execute(jobContext);
    }

    @Test
    public void failsByTicketStatusPriorToTask() throws Throwable {
        expected.expect(JobManualFailException.class);
        expected.expectMessage("Expected ticket's http://localhost status to be `done`, but it's `failed`");

        conductorDeployJob.setConfig(
            ConductorDeployJobConfig.newBuilder(ConductorBranch.TESTING)
                .waitForProjects("infra")
                .build());

        JobContext jobContext = new TestJobContext();

        configureAddTicketStub();

        Mockito.when(conductorClient.getTicketWithTasks(Mockito.anyInt()))
            .thenReturn(Futures.immediateFuture(
                new ConductorTicket(ConductorTicket.builder()
                    .setTasks(Arrays.asList(
                        new ConductorTask() {{
                            setStatus("failed");
                            setProject("some-other");
                        }},
                        new ConductorTask() {{
                            setStatus("done");
                            setProject("infra");
                        }}))
                    .setStatus(TicketStatus.FAILED)
                    .setUrl("http://localhost"))));

        conductorDeployJob.execute(jobContext);
    }

    @Test
    public void ignoreFailedTaskInUnwatchedProject() throws Throwable {
        conductorDeployJob.setConfig(
            ConductorDeployJobConfig.newBuilder(ConductorBranch.TESTING)
                .waitForProjects("infra")
                .build());

        JobContext jobContext = new TestJobContext();

        configureAddTicketStub();

        Mockito.when(conductorClient.getTicketWithTasks(Mockito.anyInt()))
            .thenReturn(Futures.immediateFuture(
                new ConductorTicket(ConductorTicket.builder()
                    .setTasks(Arrays.asList(
                        new ConductorTask() {{
                            setStatus("failed");
                            setProject("some-other");
                        }},
                        new ConductorTask() {{
                            setStatus("done");
                            setProject("infra");
                        }}))
                    .setStatus(TicketStatus.DONE))));

        conductorDeployJob.execute(jobContext);
    }

    @Test
    public void getChangeLog() throws ExecutionException, InterruptedException {
        String package1 = "package1";
        String package2 = "package2";
        String package3 = "package3";
        String package4 = "package4";
        String package5 = "package5";

        List<ConductorPackage> packages = Arrays.asList(
            new ConductorPackage(package1, "2017.1.3", null,
                Arrays.asList(
                    new ChangelogEntry("2017.1.1", "* QUEUE-1 change 1", "user42"),
                    new ChangelogEntry("2017.1.2", "* QUEUE-1 change 2", "user42"),
                    new ChangelogEntry("2017.1.3", "* QUEUE-1 change 3", "user42")
                )
            ),
            new ConductorPackage(package2, "2017.1.4", null,
                Arrays.asList(
                    new ChangelogEntry("2017.1.1", "* QUEUE-2 change 1", "user42"),
                    new ChangelogEntry("2017.1.2", "* QUEUE-2 change 2", "user42"),
                    new ChangelogEntry("2017.1.3", "* QUEUE-2 change 3", "user42")
                )
            ),
            new ConductorPackage(package3, "2017.1.5", null,
                Arrays.asList(
                    new ChangelogEntry("2017.1.3", "* QUEUE-3 change 3", "user42"),
                    new ChangelogEntry("2017.1.4", "* QUEUE-3 change 4", "user42"),
                    new ChangelogEntry("2017.1.5", "* QUEUE-3 change 5", "user42")
                )
            ),
            new ConductorPackage(package4, "2017.1.6", null,
                Arrays.asList(
                    new ChangelogEntry("2017.1.3", "* QUEUE-4 change 1", "user42"),
                    new ChangelogEntry("2017.1.4", "* QUEUE-4 change 2", "user42"),
                    new ChangelogEntry("2017.1.5", "* QUEUE-4 change 3", "user42")
                )
            ),
            new ConductorPackage(package5, "2017.1.7", null,
                Arrays.asList(
                    new ChangelogEntry("1.1.1.1.1", "* QUEUE-5 change 1", "user42"),
                    new ChangelogEntry(null, "* QUEUE-5 change 2", "user42"),
                    new ChangelogEntry("2017.1.7", "* QUEUE-5 change 3", "user42")
                )
            )
        );

        Mockito.when(conductorClient.getLastDeployedVersion(
            package1, Collections.singletonList(ConductorBranch.TESTING), Collections.emptyList()
        )).thenReturn("2017.1.1");

        Mockito.when(conductorClient.getLastDeployedVersion(
            package2, Collections.singletonList(ConductorBranch.TESTING), Collections.emptyList()
        )).thenReturn(null);

        Mockito.when(conductorClient.getLastDeployedVersion(
            package3, Collections.singletonList(ConductorBranch.TESTING), Collections.emptyList()
        )).thenReturn("2017.1.5");

        Mockito.when(conductorClient.getLastDeployedVersion(
            package4, Collections.singletonList(ConductorBranch.TESTING), Collections.emptyList()
        )).thenReturn("1.1.1.1.1.1.1");

        Mockito.when(conductorClient.getLastDeployedVersion(
            package5, Collections.singletonList(ConductorBranch.TESTING), Collections.emptyList()
        )).thenReturn("2017.1.7");

        Assert.assertEquals(
            "* QUEUE-1 change 2\n* QUEUE-1 change 3\n* QUEUE-2 change 1\n* QUEUE-2 change 2\n" +
                "* QUEUE-2 change 3\n* QUEUE-4 change 1\n* QUEUE-4 change 2\n* QUEUE-4 change 3\n" +
                "* QUEUE-5 change 1\n* QUEUE-5 change 2",
            conductorDeployJob.getChangeLog(packages)
        );

        ConductorDeployJob conductorDeployJobWithoutFilter = new ConductorDeployJob();
        conductorDeployJobWithoutFilter.setConfig(
            ConductorDeployJobConfig.newBuilder(ConductorBranch.TESTING).build()
        );

        Assert.assertEquals("* QUEUE-1 change 1\n* QUEUE-1 change 2\n* QUEUE-1 change 3\n* QUEUE-2 change 1\n* " +
                "QUEUE-2 change 2\n" +
                "* QUEUE-2 change 3\n* QUEUE-3 change 3\n* QUEUE-3 change 4\n* QUEUE-3 change 5\n" +
                "* QUEUE-4 change 1\n* QUEUE-4 change 2\n* QUEUE-4 change 3\n" +
                "* QUEUE-5 change 1\n* QUEUE-5 change 2\n* QUEUE-5 change 3",
            conductorDeployJobWithoutFilter.getChangeLog(packages)
        );
    }

    @Test
    public void getOnlyAnotherVersionPackages() throws ExecutionException, InterruptedException {
        conductorDeployJob.setConfig(
            ConductorDeployJobConfig.newBuilder(ConductorBranch.HOTFIX)
                .setDeployOnlyAnotherVersions(true)
                .addFilters(
                    new ConductorFilterConfig(FilterType.WORKFLOWS, "projectByWorkflow", "someWorkflow"),
                    new ConductorFilterConfig(FilterType.PACKAGE_VERSIONS, "somePackage", "someVersion"),
                    new ConductorFilterConfig(FilterType.DEPLOY_GROUPS, "projectByDeployGroups", "someWorkflow",
                        "someDeployGroup"),
                    new ConductorFilterConfig(FilterType.PROJECTS, "project1,project2,project3")
                )
                .build()
        );
        List<String> projects = Arrays.asList("projectByWorkflow", "projectByDeployGroups",
            "project1", "project2", "project3");

        Mockito.when(conductorClient.getLastDeployedVersion(
            "yandex-clickphite",
            Arrays.asList(ConductorBranch.STABLE, ConductorBranch.HOTFIX, ConductorBranch.FALLBACK),
            projects
        )).thenReturn("1.0.0").thenReturn("3.0.0").thenReturn("3.0.0");

        Mockito.when(conductorClient.getLastDeployedVersion(
            "yandex-market-checkouter",
            Arrays.asList(ConductorBranch.STABLE, ConductorBranch.HOTFIX, ConductorBranch.FALLBACK),
            projects
        )).thenReturn("1.0.0").thenReturn("5.0.0").thenReturn("2.0.0");

        // Каждый вызов conductorDeployJob.getOnlyAnotherVersionPackages() возвращает разные состояния (см. моки выше)
        Assert.assertTrue(
            CollectionUtils.isEqualCollection(
                Stream.of(
                        ConductorDeployJobContextConfiguration.checkouterPackage,
                        ConductorDeployJobContextConfiguration.clickphitePackage
                    )
                    .map(ConductorPackage::getPackageName)
                    .collect(Collectors.toSet()),
                conductorDeployJob.getOnlyAnotherVersionPackages().stream()
                    .map(ConductorPackage::getPackageName)
                    .collect(Collectors.toSet()))
        );

        List<ConductorPackage> next = conductorDeployJob.getOnlyAnotherVersionPackages();
        Assert.assertEquals(1, next.size());
        Assert.assertEquals(
            ConductorDeployJobContextConfiguration.checkouterPackage.getPackageName(), next.get(0).getPackageName()
        );

        Assert.assertTrue(conductorDeployJob.getOnlyAnotherVersionPackages().isEmpty());
    }

    private void configureAddTicketStub() {
        configureAddTicketStub(Mockito::any);
    }

    private void configureAddTicketStub(Supplier<AutoInstallMode> autoInstallModeSupplier) {
        Mockito.when(
                conductorClient.addTicket(
                    Mockito.any(),
                    Mockito.any(),
                    Mockito.any(),
                    Mockito.any(),
                    Mockito.any(),
                    autoInstallModeSupplier.get(),
                    Mockito.anyBoolean(),
                    Mockito.anyBoolean()))
            .thenReturn(Futures.immediateFuture(
                new ConductorTicket(ConductorTicket.builder()
                    .setUrl("http://localhost")
                    .setStatus(TicketStatus.DONE))));
    }

    static class TestConductorDeployJob extends ConductorDeployJob {
        @Override
        protected Poller.PollerBuilder<ConductorTicket> createPoller() {
            return super.createPoller().allowIntervalLessThenOneSecond(true).interval(0, TimeUnit.MILLISECONDS);
        }

        @Override
        public UUID getSourceCodeId() {
            return UUID.fromString("f21a20dd-297a-4d63-b7ff-9d576b5a2c7f");
        }
    }

    @Configuration
    static class ConductorDeployJobContextConfiguration {
        static ConductorPackage checkouterPackage = new ConductorPackage(
            "yandex-market-checkouter", "2.0.0", "Эра водолея");
        static ConductorPackage clickphitePackage = new ConductorPackage(
            "yandex-clickphite", "3.0.0", "Другая эра");

        static FixVersion release = new FixVersion(1L, "Release");
        static ReleaseInfo releaseInfo = new ReleaseInfo(release, "MARKETCHECKOUT-135135");
        static List<ConductorPackage> packages = Arrays.asList(
            checkouterPackage,
            clickphitePackage
        );
        static ConductorBranch branch = ConductorBranch.TESTING;

        @Bean
        ConductorDeployJob conductorDeployJob() {
            ConductorDeployJob conductorDeployJob = new TestConductorDeployJob();
            conductorDeployJob.setConfig(
                ConductorDeployJobConfig.newBuilder(branch)
                    .setWaitFrozenTasks(true).setFilterDeployedChangelogs(true).build()
            );

            conductorDeployJob.setReleaseInfo(releaseInfo);
            conductorDeployJob.setPackages(packages);

            return conductorDeployJob;
        }

        @Bean
        ConductorClient conductorClient() {
            return Mockito.mock(ConductorClient.class);
        }

        @Bean
        NotificationCenter notificationCenter() {
            return Mockito.mock(NotificationCenter.class);
        }

        @Bean
        Notificator notificator() {
            return Mockito.mock(Notificator.class);
        }
    }
}
