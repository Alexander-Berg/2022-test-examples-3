package ru.yandex.market.tsum.pipe.engine.runtime.test_data.common;

import ru.yandex.market.tsum.pipe.engine.definition.context.JobContext;
import ru.yandex.market.tsum.pipe.engine.definition.job.JobExecutor;

import java.util.UUID;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 11.12.17
 */
public class FailingJob implements JobExecutor {
    @Override
    public UUID getSourceCodeId() {
        return UUID.fromString("c8d782e3-616d-4ec9-90de-22666200ebb7");
    }

    @Override
    public void execute(JobContext context) throws Exception {
        throw new RuntimeException("Ouch!");
    }
}
