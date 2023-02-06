package ru.yandex.market.tpl.carrier.tms.executor.table_cleaner;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.carrier.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.carrier.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.carrier.tms.TmsIntTest;

import static ru.yandex.market.tpl.carrier.tms.service.table_cleaner.TableCleanerService.CleanerMode.JAVA_DATE_TIME;
import static ru.yandex.market.tpl.carrier.tms.service.table_cleaner.TableCleanerService.CleanerMode.NATIVE_DATE_TIME;
import static ru.yandex.market.tpl.carrier.tms.service.table_cleaner.TableCleanerService.CleanerMode.NATIVE_LIMIT;

@TmsIntTest
public class QueueLogCleanExecutorTest {

    @Autowired
    private QueueLogCleanExecutor queueLogCleanExecutor;

    @Autowired
    private ConfigurationServiceAdapter configurationServiceAdapter;

    @SneakyThrows
    @Test
    void smokeTest() {
        configurationServiceAdapter.mergeValue(ConfigurationProperties.QUEUE_LOG_CLEANER_MODE, JAVA_DATE_TIME);
        queueLogCleanExecutor.doRealJob(Mockito.mock(JobExecutionContext.class));
    }

    @SneakyThrows
    @Test
    void smokeTestNativeDateTime() {
        configurationServiceAdapter.mergeValue(ConfigurationProperties.QUEUE_LOG_CLEANER_MODE, NATIVE_DATE_TIME);
        queueLogCleanExecutor.doRealJob(Mockito.mock(JobExecutionContext.class));
    }

    @SneakyThrows
    @Test
    void smokeTestNativeLimit() {
        configurationServiceAdapter.mergeValue(ConfigurationProperties.QUEUE_LOG_CLEANER_MODE, NATIVE_LIMIT);
        queueLogCleanExecutor.doRealJob(Mockito.mock(JobExecutionContext.class));
    }
}
