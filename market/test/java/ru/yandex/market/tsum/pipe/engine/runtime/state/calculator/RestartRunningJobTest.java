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
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.JobLaunch;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.PipeLaunch;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.StatusChangeType;
import ru.yandex.market.tsum.pipe.engine.runtime.test_data.common.WaitingForInterruptOnceJob;

import java.util.concurrent.Semaphore;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Alexander Kedrik <a href="mailto:alkedr@yandex-team.ru"></a>
 * @date 14.06.2018
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {
    TestConfig.class, PipeServicesConfig.class, PipeZkConfiguration.class,
    TestZkConfig.class, RestartRunningJobTest.Config.class
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class RestartRunningJobTest {
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
    public void test() throws Exception {
        String triggeredBy = "some user";

        // Создаём пайплайн с одной джобой
        PipelineBuilder builder = PipelineBuilder.create();
        JobBuilder job1 = builder.withJob(WaitingForInterruptOnceJob.class).withId("job1");

        // Запускаем пайплайн в отдельном потоке чтобы иметь возможность что-то делать пока он работает.
        String pipeLaunchId = pipeTester.activateLaunch(builder.build());
        Thread thread = pipeTester.runScheduledJobsToCompletionAsync();

        // Ждём запуска job1. Первый запуск job1 вызывает semaphore.release() и долго висит после этого.
        semaphore.acquire();

        // Пока job1 работает, триггерим её с shouldRestartIfAlreadyRunning=true.
        // Это отменит первый запуск job1 и поставит в очередь второй запуск job1.
        pipeStateService.recalc(pipeLaunchId, new TriggerEvent(job1.getId(), triggeredBy, true));

        // Ждём завершения runPipeToCompletionAsync.
        thread.join();

        PipeLaunch pipeLaunch = pipeLaunchDao.getById(pipeLaunchId);
        assertThat(pipeLaunch.getJobState(job1.getId()).getLaunches())
            .extracting(JobLaunch::getLastStatusChangeType)
            .containsExactly(StatusChangeType.INTERRUPTED, StatusChangeType.SUCCESSFUL);
    }

    @Configuration
    public static class Config {
        @Bean
        public Semaphore semaphore() {
            return new Semaphore(0, true);
        }
    }
}
