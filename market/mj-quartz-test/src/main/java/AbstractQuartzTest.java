package ru.yandex.market.javaframework.quartz.test;

import org.mockito.Mockito;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.quartz.QuartzAutoConfiguration;

import ru.yandex.market.javaframework.postgres.test.AbstractJdbcRecipeTest;
import ru.yandex.market.starter.quartz.config.MjQuartzAutoConfiguration;

@ImportAutoConfiguration({MjQuartzAutoConfiguration.class, QuartzAutoConfiguration.class})
public abstract class AbstractQuartzTest extends AbstractJdbcRecipeTest {

    protected JobExecutionContext mockContext() {
        final JobExecutionContext mockContext = Mockito.mock(JobExecutionContext.class);
        final JobDetail mockJobDetail = Mockito.mock(JobDetail.class);
        Mockito.when(mockContext.getJobDetail()).thenReturn(mockJobDetail);
        Mockito.when(mockJobDetail.getKey()).thenReturn(new JobKey("test"));
        return mockContext;
    }

}
