package ru.yandex.market.ff.base;

import com.github.springtestdbunit.TransactionDbUnitTestExecutionListener;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.web.AnnotationConfigWebContextLoader;
import org.springframework.test.context.web.WebAppConfiguration;

import ru.yandex.market.ff.configuration.IntegrationTestWithDbQueueConsumersConfiguration;
import ru.yandex.market.ff.repository.helper.NullableColumnsDataSetLoader;
import ru.yandex.market.ff.service.DbQueueLogCache;
import ru.yandex.market.ff.util.HibernateQueriesExecutionListener;

@ContextConfiguration(
        loader = AnnotationConfigWebContextLoader.class,
        classes = IntegrationTestWithDbQueueConsumersConfiguration.class)
@TestExecutionListeners({
        DependencyInjectionTestExecutionListener.class,
        DirtiesContextTestExecutionListener.class,
        DbUnitTestExecutionListener.class,
        TransactionDbUnitTestExecutionListener.class,
        HibernateQueriesExecutionListener.class,
})
@WebAppConfiguration("classpath:resources")
@ExtendWith(SpringExtension.class)
@TestPropertySource("classpath:servant-integration-test.properties")
@DbUnitConfiguration(dataSetLoader = NullableColumnsDataSetLoader.class)
public abstract class IntegrationTestWithDbQueueConsumers extends IntegrationTest {
    @Autowired
    private DbQueueLogCache dbQueueLogCache;

    @AfterEach
    void syncDbQueueLogCache() {
        dbQueueLogCache.saveToPersistenceStorage();
    }
}
