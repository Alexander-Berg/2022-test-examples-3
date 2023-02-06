package ru.yandex.market.communication.proxy;

import org.mockito.Mockito;
import org.quartz.JobExecutionContext;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

import ru.yandex.market.common.test.db.DbUnitTestExecutionListener;
import ru.yandex.market.common.test.mockito.MockitoTestExecutionListener;
import ru.yandex.market.mboc.common.utils.PGaaSZonkyInitializer;

import static org.springframework.test.context.TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {
                ru.yandex.market.javaframework.main.config.SpringApplicationConfig.class
        }
)
@TestExecutionListeners(
        listeners = {
                DependencyInjectionTestExecutionListener.class,
                DbUnitTestExecutionListener.class,
                MockitoTestExecutionListener.class
        },
        mergeMode = MERGE_WITH_DEFAULTS
)
@AutoConfigureMockMvc
@ActiveProfiles(profiles = "functionalTest")
@TestPropertySource(locations = "/99_functional_application.properties")
public abstract class AbstractCommunicationProxyTest {

    protected JobExecutionContext mockContext() {
        return Mockito.mock(JobExecutionContext.class);
    }
}
