package ru.yandex.market.checkout.checkouter.service.assessor;

import java.nio.charset.StandardCharsets;

import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.KeeperException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import ru.yandex.market.checkout.application.AbstractServicesTestBase;

public class AssessorServiceImplTest extends AbstractServicesTestBase {

    @Autowired
    private CuratorFramework mbiCurator;
    @Autowired
    private AssessorService assessorService;
    @Value("/checkout/assessor/uids")
    private String path;

    @Test
    public void testAssessorService() throws Exception {
        createOrSet("[1,2,3]");

        // waiting for watch
        Thread.sleep(100);

        Assertions.assertTrue(assessorService.checkAssessor(1L));
        Assertions.assertTrue(assessorService.checkAssessor(2L));

        Assertions.assertFalse(assessorService.checkAssessor(4L));
    }

    @Test
    public void shouldReReadNode() throws Exception {
        createOrSet("[1,2,3]");

        // waiting for watch
        Thread.sleep(100);

        createOrSet("[5,6,7]");


        Thread.sleep(100);

        Assertions.assertTrue(assessorService.checkAssessor(5L));
        Assertions.assertFalse(assessorService.checkAssessor(4L));
    }

    @AfterEach
    public void cleanUp() throws Exception {
        mbiCurator.delete().forPath(path);
    }

    private void createOrSet(String value) throws Exception {
        try {
            mbiCurator.create()
                    .creatingParentsIfNeeded()
                    .forPath(path, value.getBytes(StandardCharsets.UTF_8));
        } catch (KeeperException.NodeExistsException ex) {
            mbiCurator.setData()
                    .forPath(path, value.getBytes(StandardCharsets.UTF_8));
        }
    }

}
