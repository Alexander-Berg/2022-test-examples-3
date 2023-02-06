package ru.yandex.market.tsum.pipe.engine.runtime;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.tsum.pipe.engine.definition.resources.Resource;
import ru.yandex.market.tsum.pipe.engine.runtime.config.MockCuratorConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.config.PipeServicesConfig;

import ru.yandex.market.tsum.pipe.engine.runtime.config.TestConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.di.model.StoredResourceContainer;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.PipeTester;
import ru.yandex.market.tsum.pipe.engine.runtime.test_data.increment.IncrementPipe;
import ru.yandex.market.tsum.pipe.engine.runtime.test_data.increment.resources.IntegerResource;

import java.util.Collections;
import java.util.List;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 16.03.17
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {
    TestConfig.class, PipeServicesConfig.class, MockCuratorConfig.class
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class PipeManualResourcesTest {
    @Autowired
    private PipeTester pipeTester;

    @Test
    public void test() throws Exception {
        List<? extends Resource> manualResources = Collections.singletonList(new IntegerResource(6));

        String pipeLaunchId = pipeTester.runPipeToCompletion(IncrementPipe.PIPE_ID, manualResources);
        StoredResourceContainer producedResources = pipeTester.getProducedResources(pipeLaunchId, IncrementPipe.JOB_ID);
        IntegerResource result = pipeTester.getResourceOfType(producedResources, IntegerResource.class);

        Assert.assertEquals(7, (int) result.getValue());
    }

    @Test(expected = IllegalArgumentException.class)
    public void noManualResourcesProvided() throws Exception {
        List<? extends Resource> manualResources = Collections.emptyList();
        pipeTester.runPipeToCompletion(IncrementPipe.PIPE_ID, manualResources);
    }
}
