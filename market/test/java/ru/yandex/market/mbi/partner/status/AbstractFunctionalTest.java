package ru.yandex.market.mbi.partner.status;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.common.test.db.DbUnitTestExecutionListener;
import ru.yandex.market.common.test.mockito.MockitoTestExecutionListener;
import ru.yandex.market.javaframework.main.config.SpringApplicationConfig;

import static org.springframework.test.context.TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS;

@TestExecutionListeners(listeners = {
        DbUnitTestExecutionListener.class,
        MockitoTestExecutionListener.class
}, mergeMode = MERGE_WITH_DEFAULTS)
@ExtendWith(SpringExtension.class)
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
    classes = {
        SpringApplicationConfig.class
    }
)
public abstract class AbstractFunctionalTest {
    protected JobExecutionContext mockContext() {
        final JobExecutionContext mockContext = Mockito.mock(JobExecutionContext.class);
        final JobDetail mockJobDetail = Mockito.mock(JobDetail.class);
        Mockito.when(mockContext.getJobDetail()).thenReturn(mockJobDetail);
        Mockito.when(mockJobDetail.getKey()).thenReturn(new JobKey("test"));
        return mockContext;
    }
}

