package ru.yandex.market.delivery.mdbapp;

import com.github.springtestdbunit.DbUnitTestExecutionListener;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.dataset.ReplacementDataSetLoader;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockitoTestExecutionListener;
import org.springframework.boot.test.mock.mockito.ResetMocksTestExecutionListener;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.delivery.mdbapp.configuration.IntegrationTestConfiguration;
import ru.yandex.market.logistics.test.integration.db.listener.CleanDatabase;
import ru.yandex.market.logistics.test.integration.db.listener.ResetDatabaseTestExecutionListener;
import ru.yandex.market.logistics.test.integration.jpa.HibernateQueriesExecutionListener;

import static org.junit.jupiter.params.ParameterizedTest.INDEX_PLACEHOLDER;

@ExtendWith({
    SpringExtension.class,
    SoftAssertionsExtension.class,
})
@SpringBootTest(
    classes = IntegrationTestConfiguration.class,
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = "spring.config.name=integration-test"
)
@AutoConfigureMockMvc(secure = false)
@TestExecutionListeners({
    DependencyInjectionTestExecutionListener.class,
    ResetDatabaseTestExecutionListener.class,
    TransactionalTestExecutionListener.class,
    DbUnitTestExecutionListener.class,
    MockitoTestExecutionListener.class,
    ResetMocksTestExecutionListener.class,
    HibernateQueriesExecutionListener.class,
})
@CleanDatabase
@DbUnitConfiguration(dataSetLoader = ReplacementDataSetLoader.class)
@ComponentScan({
    "ru.yandex.market.delivery.mdbapp.components.builder",
    "ru.yandex.market.delivery.mdbapp.components.failover",
    "ru.yandex.market.delivery.mdbapp.components.geo",
    "ru.yandex.market.delivery.mdbapp.components.logging",
    "ru.yandex.market.delivery.mdbapp.components.service",
    "ru.yandex.market.delivery.mdbapp.components.storage.domain",
    "ru.yandex.market.delivery.mdbapp.components.storage.repository",
    "ru.yandex.market.delivery.mdbapp.integration.converter",
    "ru.yandex.market.delivery.mdbapp.integration.gateway",
    "ru.yandex.market.delivery.mdbapp.integration.enricher",
    "ru.yandex.market.delivery.mdbapp.integration.service",
    "ru.yandex.market.delivery.mdbapp.components.specification",
    "ru.yandex.market.delivery.mdbapp.configuration.queue",
    "ru.yandex.market.delivery.mdbapp.components.queue.producer",
    "ru.yandex.market.delivery.mdbapp.components.queue",
})
@TestPropertySource("classpath:integration-test.properties")
@Slf4j
@ActiveProfiles("medium-integration-test")
public class AbstractMediumContextualTest {
    protected static final String TUPLE_PARAMETERIZED_DISPLAY_NAME = "[" + INDEX_PLACEHOLDER + "] {0}";

    @InjectSoftAssertions
    protected SoftAssertions softly;

    @Autowired
    protected TestableClock clock;
}
