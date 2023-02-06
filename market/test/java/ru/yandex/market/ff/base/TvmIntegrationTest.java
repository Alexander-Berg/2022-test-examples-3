package ru.yandex.market.ff.base;

import com.github.springtestdbunit.TransactionDbUnitTestExecutionListener;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.web.AnnotationConfigWebContextLoader;
import org.springframework.test.context.web.WebAppConfiguration;

import ru.yandex.market.ff.configuration.TvmIntegrationTestConfiguration;
import ru.yandex.market.ff.util.HibernateQueriesExecutionListener;

@ContextConfiguration(
    loader = AnnotationConfigWebContextLoader.class,
    classes = TvmIntegrationTestConfiguration.class)
@TestExecutionListeners({
    DependencyInjectionTestExecutionListener.class,
    DirtiesContextTestExecutionListener.class,
    DbUnitTestExecutionListener.class,
    TransactionDbUnitTestExecutionListener.class,
    HibernateQueriesExecutionListener.class,
})
@WebAppConfiguration
@ExtendWith(SpringExtension.class)
@TestPropertySource("classpath:servant-integration-test.properties")
public abstract class TvmIntegrationTest {
}
