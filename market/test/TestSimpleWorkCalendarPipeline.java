package ru.yandex.market.tsum.pipelines.test;

import java.util.concurrent.TimeUnit;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.market.tsum.pipe.engine.definition.Pipeline;
import ru.yandex.market.tsum.pipe.engine.definition.builder.AdapterJobBuilder;
import ru.yandex.market.tsum.pipe.engine.definition.builder.JobBuilder;
import ru.yandex.market.tsum.pipe.engine.definition.builder.PipelineBuilder;
import ru.yandex.market.tsum.pipe.engine.definition.common.CanRunWhen;
import ru.yandex.market.tsum.pipe.engine.definition.common.TypeOfSchedulerConstraint;
import ru.yandex.market.tsum.pipelines.common.jobs.dummy.SleepDummyJob;
import ru.yandex.market.tsum.pipelines.common.jobs.finish_release.FinishReleaseJobConfig;
import ru.yandex.market.tsum.release.ReleaseJobTags;

/**
 * @author Mishunin Andrei <a href="mailto:mishunin@yandex-team.ru"></a>
 * @date 25.03.2019
 */
@Configuration
public class TestSimpleWorkCalendarPipeline {
    @Bean(name = "test-simple-work-calendar")
    public Pipeline pipeline() {

        PipelineBuilder builder = PipelineBuilder.create();
        JobBuilder startJob = builder.withJob(SleepDummyJob.class, "start")
            .withTitle("Start");

        AdapterJobBuilder orJob = builder.withAdapterJob("or")
            .withTitle("OR");

        JobBuilder workJob = builder.withJob(SleepDummyJob.class, TypeOfSchedulerConstraint.WORK.name())
            .withUpstreams(startJob)
            .withScheduler()
            .workDaysHours(0, (int) TimeUnit.DAYS.toHours(1))
            .build();

        JobBuilder preHolidayJob = builder.withJob(SleepDummyJob.class, TypeOfSchedulerConstraint.PRE_HOLIDAY.name())
            .withUpstreams(startJob)
            .withScheduler()
            .preHolidayHours(0, (int) TimeUnit.DAYS.toHours(1))
            .build();

        JobBuilder holidayJob = builder.withJob(SleepDummyJob.class, TypeOfSchedulerConstraint.HOLIDAY.name())
            .withUpstreams(startJob)
            .withScheduler()
            .holidayHours(0, (int) TimeUnit.DAYS.toHours(1))
            .build();

        orJob.withUpstreams(CanRunWhen.ANY_COMPLETED, workJob, preHolidayJob, holidayJob);

        builder.withJob(SleepDummyJob.class, "End")
            .withUpstreams(orJob)
            .withManualTrigger()
            .withResources(FinishReleaseJobConfig.DO_NOTHING_CONFIG)
            .withTags(ReleaseJobTags.FINAL_JOB);

        return builder.build();
    }
}
