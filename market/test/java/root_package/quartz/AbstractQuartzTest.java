package {root_package}.quartz;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import ru.yandex.market.javaframework.frames.quartz.config.DefaultTmsDataSourceConfig;
import ru.yandex.market.javaframework.frames.quartz.config.TmsConfig;
import {root_package}.config.EmbeddedDbConfig;
import {root_package}.quartz.task.TmsTasks;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {EmbeddedDbConfig.class, DefaultTmsDataSourceConfig.class, TmsConfig.class, TmsTasks.class})
@TestPropertySource({"classpath:test_properties/postgres_test.properties", "classpath:test_properties/quartz_test.properties"})
public abstract class AbstractQuartzTest {

    protected JobExecutionContext mockContext() {
        final JobExecutionContext mockContext = Mockito.mock(JobExecutionContext.class);
        final JobDetail mockJobDetail = Mockito.mock(JobDetail.class);
        Mockito.when(mockContext.getJobDetail()).thenReturn(mockJobDetail);
        Mockito.when(mockJobDetail.getKey()).thenReturn(new JobKey("test"));
        return mockContext;
    }

}

