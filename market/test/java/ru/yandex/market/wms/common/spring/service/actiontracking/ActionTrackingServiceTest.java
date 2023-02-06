package ru.yandex.market.wms.common.spring.service.actiontracking;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jms.core.JmsTemplate;

import ru.yandex.market.wms.common.model.enums.ProcessType;
import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.common.spring.config.BaseTestConfig;
import ru.yandex.market.wms.common.spring.config.IntegrationTestConfig;
import ru.yandex.market.wms.shared.libs.async.jms.QueueNameConstants;
import ru.yandex.market.wms.shared.libs.async.jms.RetryableMessagePostProcessor;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;

@SpringBootTest(classes = {
        BaseTestConfig.class,
        IntegrationTestConfig.class,
        ActionTrackingServiceTest.TestConfig.class
})
class ActionTrackingServiceTest extends IntegrationTest {

    @Autowired
    ActionTrackingService actionTrackingService;

    @MockBean
    @Autowired
    JmsTemplate jmsTemplate;

    @Test
    @DatabaseSetup("/db/service/actiontracking/indirect-activity-exist/db.xml")
    @ExpectedDatabase(value =
            "/db/service/actiontracking/indirect-activity-exist/after-indirect-activity-exist.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void updateUserActionStatusWhenIndirectActivityExist() throws InterruptedException {

        Mockito.doNothing().when(jmsTemplate)
                .convertAndSend(
                        ArgumentMatchers.eq(QueueNameConstants.TTS_INDIRECT_ACTIVITY),
                        ArgumentMatchers.any(ru.yandex.market.wms.common.spring.dto.IndirectActivityDto.class),
                        ArgumentMatchers.any(RetryableMessagePostProcessor.class));

        actionTrackingService.updateUserActionStatus(ProcessType.CONSOLIDATION, "test", "test", "area");

        Mockito.verify(jmsTemplate, Mockito.times(1)).convertAndSend(
                ArgumentMatchers.eq(QueueNameConstants.TTS_INDIRECT_ACTIVITY),
                ArgumentMatchers.any(ru.yandex.market.wms.common.spring.dto.IndirectActivityDto.class),
                ArgumentMatchers.any()
        );
    }

    @Test
    @DatabaseSetup("/db/service/actiontracking/no-complete-on-event/db.xml")
    @ExpectedDatabase(value =
            "/db/service/actiontracking/no-complete-on-event/after-indirect-activity-exist.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void updateUserActionStatusWhenIndirectNoCompleteOnEventActivityExist() {

        Mockito.doNothing().when(jmsTemplate)
                .convertAndSend(
                        ArgumentMatchers.eq(QueueNameConstants.TTS_INDIRECT_ACTIVITY),
                        ArgumentMatchers.any(ru.yandex.market.wms.common.spring.dto.IndirectActivityDto.class),
                        ArgumentMatchers.any(RetryableMessagePostProcessor.class));

        actionTrackingService.updateUserActionStatus(ProcessType.CONSOLIDATION, "test", "test", "area");

        Mockito.verify(jmsTemplate, Mockito.times(0)).convertAndSend(
                ArgumentMatchers.eq(QueueNameConstants.TTS_INDIRECT_ACTIVITY),
                ArgumentMatchers.any(ru.yandex.market.wms.common.spring.dto.IndirectActivityDto.class),
                ArgumentMatchers.any(RetryableMessagePostProcessor.class)
        );
    }

    @Configuration
    static class TestConfig {
        @Bean
        TaskExecutor taskExecutor() {
            return new SyncTaskExecutor();
        }
    }
}
