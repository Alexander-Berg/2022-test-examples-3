package ru.yandex.market.fintech.fintechutils;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;

import ru.yandex.market.common.test.db.DbUnitTestExecutionListener;
import ru.yandex.market.common.test.mockito.MockitoTestExecutionListener;
import ru.yandex.market.javaframework.main.config.SpringApplicationConfig;

@ExtendWith(SpringExtension.class)
@TestExecutionListeners(listeners = {
        DependencyInjectionTestExecutionListener.class,
        MockitoTestExecutionListener.class,
        DirtiesContextTestExecutionListener.class,
        DbUnitTestExecutionListener.class
}, mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = {
        SpringApplicationConfig.class
    }
)
@TestPropertySource("classpath:functionalTest.properties")
public abstract class AbstractFunctionalTest  {
}

