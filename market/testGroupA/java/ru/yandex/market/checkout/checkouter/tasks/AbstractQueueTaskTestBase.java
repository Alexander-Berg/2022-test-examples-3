package ru.yandex.market.checkout.checkouter.tasks;

import java.util.concurrent.TimeUnit;

import org.apache.curator.framework.CuratorFramework;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.common.tasks.ZooTask;

import static org.awaitility.Awaitility.await;

public class AbstractQueueTaskTestBase extends AbstractWebTestBase {

    @Autowired
    private CuratorFramework curator;

    protected void runTask(ZooTask task, String queuePath) {
        task.runOnce();
        await().atMost(1000, TimeUnit.SECONDS).until(
                () -> curator.checkExists().forPath(queuePath).getNumChildren() == 0 && !task.isLocked()
        );
    }
}
