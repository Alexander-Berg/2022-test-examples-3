package ru.yandex.market.tsum.pipe.engine.runtime;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.tsum.pipe.engine.definition.context.JobContext;
import ru.yandex.market.tsum.pipe.engine.definition.builder.PipelineBuilder;
import ru.yandex.market.tsum.pipe.engine.definition.job.JobExecutor;
import ru.yandex.market.tsum.pipe.engine.runtime.config.MockCuratorConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.config.PipeServicesConfig;

import ru.yandex.market.tsum.pipe.engine.runtime.config.TestConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.PipeTester;

import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author Alexander Kedrik <a href="mailto:alkedr@yandex-team.ru"></a>
 * @date 21.05.2018
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {
    TestConfig.class, PipeServicesConfig.class, MockCuratorConfig.class,
    JobExecutorConstructorWithParametersTest.SomeConfig.class
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class JobExecutorConstructorWithParametersTest {
    @Autowired
    private PipeTester pipeTester;

    @Autowired
    private Runnable mockRunnable;

    @Test
    public void name() throws InterruptedException {
        PipelineBuilder builder = PipelineBuilder.create();
        builder.withJob(SomeJob.class);
        pipeTester.runPipeToCompletion(builder.build());
        verify(mockRunnable).run();
    }

    private static class SomeJob implements JobExecutor {
        private final Runnable runnable;

        SomeJob(Runnable runnable) {
            this.runnable = runnable;
        }

        @Override
        public UUID getSourceCodeId() {
            return UUID.fromString("226678d2-e413-4285-8005-e09a6b123194");
        }

        @Override
        public void execute(JobContext context) throws Exception {
            runnable.run();
        }
    }

    @Configuration
    public static class SomeConfig {
        @Bean
        public Runnable mockRunnable() {
            return mock(Runnable.class);
        }
    }
}
