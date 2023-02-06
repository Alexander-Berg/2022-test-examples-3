package ru.yandex.market.tsum.pipe.engine.runtime;

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
import ru.yandex.market.tsum.pipe.engine.runtime.test_data.common.resources.Res2;
import ru.yandex.market.tsum.pipe.engine.runtime.test_data.diamond.DiamondPipeWithResources;

import java.util.Collections;

import static org.junit.Assert.assertEquals;

/**
 * @author Alexander Kedrik <a href="mailto:alkedr@yandex-team.ru"></a>
 * @date 12.05.2017
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {
    TestConfig.class, PipeServicesConfig.class, MockCuratorConfig.class
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class DiamondPipeWithResourcesTest {
    @Autowired
    private PipeTester pipeTester;

    @Test
    public void test() throws Exception {
        String pipeLaunchId = pipeTester.runPipeToCompletion(
            DiamondPipeWithResources.PIPE_NAME,
            Collections.emptyList()
        );
        StoredResourceContainer producedResources = pipeTester.getProducedResources(
            pipeLaunchId,
            DiamondPipeWithResources.END_JOB
        );
        assertEquals(
            "ProduceRes1 ConvertRes1ToRes2",
            pipeTester.getResourceOfType(producedResources, Res2.class).getS()
        );
    }
}
