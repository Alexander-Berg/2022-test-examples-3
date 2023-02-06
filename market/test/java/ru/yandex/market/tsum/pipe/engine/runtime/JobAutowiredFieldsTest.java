package ru.yandex.market.tsum.pipe.engine.runtime;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.tsum.pipe.engine.runtime.config.MockCuratorConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.config.PipeServicesConfig;

import ru.yandex.market.tsum.pipe.engine.runtime.config.TestConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.di.model.StoredResourceContainer;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.PipeTester;
import ru.yandex.market.tsum.pipe.engine.runtime.test_data.autowired_job.AutowiredPipe;
import ru.yandex.market.tsum.pipe.engine.runtime.test_data.resource_injection.resources.Resource451;

import java.util.Collections;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 13.04.17
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {
    TestConfig.class, PipeServicesConfig.class, MockCuratorConfig.class
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class JobAutowiredFieldsTest {
    @Autowired
    private PipeTester pipeTester;

    @Test
    public void simpleInjection() throws Exception {
        String pipeLaunchId = pipeTester.runPipeToCompletion(
            AutowiredPipe.PIPE_ID,
            Collections.emptyList()
        );

        StoredResourceContainer producedResources = pipeTester.getProducedResources(
            pipeLaunchId, AutowiredPipe.JOB_ID
        );

        Resource451 result = pipeTester.getResourceOfType(producedResources, Resource451.class);
        Assert.assertEquals(451, (int) result.getValue());
    }
}
