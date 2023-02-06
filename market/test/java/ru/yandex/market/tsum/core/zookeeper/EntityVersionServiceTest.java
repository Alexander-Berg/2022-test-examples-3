package ru.yandex.market.tsum.core.zookeeper;

import org.apache.curator.framework.CuratorFramework;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.tsum.core.config.TestZkConfig;

import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 23.01.18
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestZkConfig.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class EntityVersionServiceTest {
    private static final long TIMEOUT_MILLIS = 60_000L;

    private EntityVersionService sut;

    private String entityId;

    @Autowired
    private CuratorFramework curatorFramework;

    @Before
    public void setUp() throws Exception {
        entityId = new ObjectId().toString();
        sut = new EntityVersionService(curatorFramework, "/some/entity/", 200, 100);
    }

    @Test(timeout = TIMEOUT_MILLIS)
    public void setOne() {
        String launchId = new ObjectId().toString();
        int value = 666;
        sut.setIfLessThan(launchId, value);

        Assert.assertEquals(
            Long.valueOf(value),
            sut.observe(launchId).blockingFirst()
        );
    }

    @Test(timeout = TIMEOUT_MILLIS)
    public void setInDescOrder() {
        int maxValue = 50;

        for (int i = maxValue; i > 0; --i) {
            sut.setIfLessThan(entityId, i);
        }

        Assert.assertEquals(
            Long.valueOf(maxValue),
            sut.observe(entityId).blockingFirst()
        );
    }

    @Test(timeout = TIMEOUT_MILLIS)
    public void setParallel() throws InterruptedException {
        int maxValue = 200;

        Thread writingThread = new Thread(() -> {
            IntStream.range(0, maxValue + 1)
                .parallel()
                .forEach(val -> sut.setIfLessThan(entityId, val));
        });

        writingThread.start();
        writingThread.join();

        Assert.assertEquals(
            Long.valueOf(maxValue),
            sut.observe(entityId)
                .doOnEach(System.out::println)
                .takeUntil(r -> r == maxValue)
                .blockingLast()
        );
    }

}