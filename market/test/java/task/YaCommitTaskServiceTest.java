package ru.yandex.sandbox.client.task;

import java.util.Collections;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.sandbox.client.SandboxClient;
import ru.yandex.sandbox.client.SandboxClientFactory;

/**
 * @author albazh
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

        YaCommitTask task = (YaCommitTask) new YaCommitTask()
                .setDescription("Autocommit after successful precommit check")
                .setTags(Collections.singletonList("ci-check-id-" + "test-albazh"))
                .setContext(new YaCommitTaskContext(
                        "zipatch:https://proxy.sandbox.yandex-team.ru/474273110",
                        "trunk", "3370368",
                        "This is commit message of future commit",
                        "albazh"
                ).toMap());

        String taskId = service.create(task, true);

        service.stopTask(Long.parseLong(taskId));
    }

    @Test
    @Ignore
    public void stopTask() {
        YaCommitTaskService service = new YaCommitTaskService(sandboxClient);
        service.stopTask(209188075);
    }

}
