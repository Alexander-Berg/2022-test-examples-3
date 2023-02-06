package ru.yandex.market.mbi.partnersearch;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.common.test.db.DbUnitTestExecutionListener;
import ru.yandex.market.common.test.mockito.MockitoTestExecutionListener;
import ru.yandex.market.javaframework.main.config.SpringApplicationConfig;
import ru.yandex.market.mbi.partnersearch.config.AppConfig;
import ru.yandex.market.mbi.partnersearch.config.MockElasticConfig;
import ru.yandex.market.mbi.partnersearch.config.YtMockConfig;

import static org.springframework.test.context.TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {
                SpringApplicationConfig.class,
                AppConfig.class,
                MockElasticConfig.class,
                YtMockConfig.class,
        }
)
@TestExecutionListeners(listeners = {
        DbUnitTestExecutionListener.class,
        MockitoTestExecutionListener.class
}, mergeMode = MERGE_WITH_DEFAULTS)
@TestPropertySource({"classpath:test_properties/functional-test.properties"})
public abstract class AbstractFunctionalTest {


}

