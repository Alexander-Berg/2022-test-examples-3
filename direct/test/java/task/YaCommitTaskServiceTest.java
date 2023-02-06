package ru.yandex.sandbox.client.task;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.sandbox.client.SandboxClient;
import ru.yandex.sandbox.client.SandboxClientFactory;
import ru.yandex.sandbox.client.api.SandboxTask;

/**
 * @author albazh
 * <p>
 * You can find started tasks by tag, e.g.
 * https://sandbox.yandex-team.ru/tasks/?tags=CI-CHECK-ID-TEST-ALBAZH&page=1&pageCapacity=20&forPage=tasks
 */
public class YaCommitTaskServiceTest {

    private static final Logger logger = LoggerFactory.getLogger(YaCommitTaskServiceTest.class);

    private SandboxClient sandboxClient;

    @Before
    public void setUp() {
        this.sandboxClient = SandboxClientFactory.builder(
                10,
                // REPLACE with arcanum-robot oauth-token
                "XXXX-YYYY12351"
        ).build().create(SandboxClient.class);
    }

    @Test
    @Ignore
    public void startTask() {
        YaCommitTaskService service = new YaCommitTaskService(sandboxClient);

        SandboxTask task = new YaCommitTaskBuilder()
                .setDescription("Autocommit after successful precommit check")
                .setSvnPath("trunk")
                .setSvnRevision("3370368")
                .setCommitAuthor("albazh")
                .setCommitMessage("This is commit message of future commit")
                .setArcadiaPatch("zipatch:https://proxy.sandbox.yandex-team.ru/474273110")
                .setDryRun(true)
                .setCiCheckId("test-ci-check-id")
                .build();

        String taskId = service.create(task, true);

        service.stopTask(Long.parseLong(taskId));
    }

    @Test
    @Ignore
    public void stopTask() {
        YaCommitTaskService service = new YaCommitTaskService(sandboxClient);
        service.stopTask(209188075);
    }

    @Test
    @Ignore
    public void startAlreadyStartedTask() {
        YaCommitTaskService service = new YaCommitTaskService(sandboxClient);
        service.startTask(211287341);
    }

    @Test
    @Ignore
    public void startSuccessfullyFinishedTask() {
        YaCommitTaskService service = new YaCommitTaskService(sandboxClient);
        service.startTask(209147605);
    }

    @Test
    @Ignore
    public void startFailedTask() {
        YaCommitTaskService service = new YaCommitTaskService(sandboxClient);
        service.startTask(209186703);
    }

}
