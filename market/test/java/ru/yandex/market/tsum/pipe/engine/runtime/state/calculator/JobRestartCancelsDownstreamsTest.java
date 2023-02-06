package ru.yandex.market.tsum.pipe.engine.runtime.state.calculator;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.tsum.core.config.TestZkConfig;
import ru.yandex.market.tsum.pipe.engine.definition.builder.JobBuilder;
import ru.yandex.market.tsum.pipe.engine.definition.builder.PipelineBuilder;
import ru.yandex.market.tsum.pipe.engine.runtime.config.PipeServicesConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.config.PipeZkConfiguration;

import ru.yandex.market.tsum.pipe.engine.runtime.config.TestConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.events.TriggerEvent;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.PipeTester;
import ru.yandex.market.tsum.pipe.engine.runtime.state.PipeLaunchDao;
import ru.yandex.market.tsum.pipe.engine.runtime.state.PipeStateService;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.JobState;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.PipeLaunch;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.StatusChangeType;
import ru.yandex.market.tsum.pipe.engine.definition.DummyJob;
import ru.yandex.market.tsum.pipe.engine.runtime.test_data.common.WaitingForInterruptOnceJob;

import java.util.concurrent.Semaphore;

import static org.junit.Assert.assertEquals;

/**
 * @author Alexander Kedrik <a href="mailto:alkedr@yandex-team.ru"></a>
 * @date 14.06.2018
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {
    TestConfig.class, PipeServicesConfig.class, PipeZkConfiguration.class,
    TestZkConfig.class, JobRestartCancelsDownstreamsTest.Config.class
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class JobRestartCancelsDownstreamsTest {
    @Autowired
    private PipeTester pipeTester;

    @Autowired
    private PipeLaunchDao pipeLaunchDao;

    @Autowired
    private PipeStateService pipeStateService;

    @Autowired
    private Semaphore semaphore;

    /**
     * Если этот тест упал по таймауту, то скорее всего не отменилась джоба, которая должна была отмениться.
     */
    @Test(timeout = 120000)
    public void shouldInterruptDownstreams() throws Exception {
        String triggeredBy = "some user";

        // Создаём пайплайн job1 -> job2
        PipelineBuilder builder = PipelineBuilder.create();
        JobBuilder job1 = builder.withJob(DummyJob.class).withId("job1");
        JobBuilder job2 = builder.withJob(WaitingForInterruptOnceJob.class).withId("job2").withUpstreams(job1);

        // Запускаем пайплайн в отдельном потоке чтобы иметь возможность что-то делать пока он работает.
        String pipeLaunchId = pipeTester.activateLaunch(builder.build());
        Thread thread = pipeTester.runScheduledJobsToCompletionAsync();

        // Ждём запуска job2. Первый запуск job2 вызывает semaphore.release() и долго висит после этого.
        semaphore.acquire();

        // Пока job2 работает, рестартим job1. Это отменит первый запуск job2 и поставит в очередь второй запуск job1.
        pipeStateService.recalc(pipeLaunchId, new TriggerEvent(job1.getId(), triggeredBy, false));

        // Ждём завершения runPipeToCompletionAsync.
        thread.join();

        // После всех этих махинаций:
        // - у job1 два запуска, оба успешные
        // - у job2 два запуска, первый отменён пользователем triggeredBy, второй успешный
        PipeLaunch pipeLaunch = pipeLaunchDao.getById(pipeLaunchId);
        JobState job1State = pipeLaunch.getJobState(job1.getId());
        assertEquals(2, job1State.getLaunches().size());
        assertEquals(StatusChangeType.SUCCESSFUL, job1State.getLaunches().get(0).getLastStatusChangeType());
        assertEquals(StatusChangeType.SUCCESSFUL, job1State.getLaunches().get(1).getLastStatusChangeType());

        JobState job2State = pipeLaunch.getJobState(job2.getId());
        assertEquals(2, job2State.getLaunches().size());
        assertEquals(StatusChangeType.INTERRUPTED, job2State.getLaunches().get(0).getLastStatusChangeType());
        assertEquals(triggeredBy, job2State.getLaunches().get(0).getInterruptedBy());
        assertEquals(StatusChangeType.SUCCESSFUL, job2State.getLaunches().get(1).getLastStatusChangeType());
    }

    @Configuration
    public static class Config {
        @Bean
        public Semaphore semaphore() {
            return new Semaphore(0, true);
        }
    }
}
