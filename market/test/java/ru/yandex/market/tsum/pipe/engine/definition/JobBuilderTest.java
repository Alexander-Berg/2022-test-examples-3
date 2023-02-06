package ru.yandex.market.tsum.pipe.engine.definition;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.tsum.pipe.engine.definition.builder.JobBuilder;
import ru.yandex.market.tsum.pipe.engine.definition.builder.PipelineBuilder;
import ru.yandex.market.tsum.pipe.engine.definition.context.JobContext;
import ru.yandex.market.tsum.pipe.engine.definition.job.ExecutorInfo;
import ru.yandex.market.tsum.pipe.engine.definition.job.JobExecutor;
import ru.yandex.market.tsum.pipe.engine.definition.job.ExecutorDefaults;

import java.util.UUID;

/**
 * @author Nikolay Firov <a href="mailto:firov@yandex-team.ru"></a>
 * @date 03.05.18
 */
public class JobBuilderTest {
    @ExecutorInfo(
        defaults = @ExecutorDefaults(retries = 2)
    )
    private static class TestJob implements JobExecutor {
        @Override
        public UUID getSourceCodeId() {
            return UUID.fromString("07096dff-25b1-4b2a-a5eb-f90dbc3f1994");
        }

        @Override
        public void execute(JobContext context) throws Exception {
        }
    }

    @Test
    public void usesDefaults() {
        JobBuilder builder = PipelineBuilder.create().withJob(TestJob.class);
        Assert.assertEquals(2, builder.getRetries());
        Assert.assertFalse(builder.isAdapter());
    }
}
