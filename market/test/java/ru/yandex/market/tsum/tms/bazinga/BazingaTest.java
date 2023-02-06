package ru.yandex.market.tsum.tms.bazinga;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.commune.bazinga.BazingaTaskManager;
import ru.yandex.commune.bazinga.impl.FullJobId;
import ru.yandex.commune.bazinga.impl.OnetimeJob;
import ru.yandex.market.tsum.core.TestMongo;
import ru.yandex.market.tsum.core.TestZooKeeper;
import ru.yandex.market.tsum.tms.config.BazingaConfig;

/**
 * @author Dmitry Andreev <a href="mailto:AndreevDm@yandex-team.ru"></a>
 * @date 01/11/16
 */
@RunWith(SpringJUnit4ClassRunner.class)
@TestPropertySource({"classpath:bazinga-test.properties", "classpath:test.properties"})
@ContextConfiguration(classes = {TestZooKeeper.class, TestMongo.class, TestOneTimeTask.class, BazingaConfig.class})
public class BazingaTest {

    @Autowired
    private BazingaTaskManager bazingaTaskManager;

    @Test
    public void testTask() throws Exception {
        TestOneTimeTask task = new TestOneTimeTask();
        FullJobId jobId = bazingaTaskManager.schedule(task);
        for (int i = 0; i < 1000; i++) {
            Thread.sleep(10);
            if (isCompleted(jobId)) {
                return;
            }
        }
        Assert.fail("Task did not executed");
    }

    public boolean isCompleted(FullJobId jobId) {
        if (!TestOneTimeTask.EXECUTED) {
            return false;
        }
        OnetimeJob job = bazingaTaskManager.getOnetimeJob(jobId).get();
        return job.getValue().getStatus().isCompleted();
    }
}
