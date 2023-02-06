package ru.yandex.market.tms.quartz2.executors;

import org.quartz.JobExecutionContext;

import ru.yandex.market.tms.quartz2.pure.QuartzJob;
import ru.yandex.market.tms.quartz2.spring.CronTrigger;

/**
 * @author komarovns
 * @date 27.09.18
 */


@CronTrigger(cronExpression = "0 0 1 1 * ? 2020", description = "Test executor")
public class TestClassExecutor extends QuartzJob {
    @Override
    public void doJob(JobExecutionContext context) {
    }
}
