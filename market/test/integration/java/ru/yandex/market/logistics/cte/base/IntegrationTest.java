package ru.yandex.market.logistics.cte.base;


import java.io.File;

import com.github.springtestdbunit.TransactionDbUnitTestExecutionListener;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.util.Files;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.web.AnnotationConfigWebContextLoader;
import org.springframework.test.context.web.WebAppConfiguration;

import ru.yandex.common.util.text.Charsets;
import ru.yandex.market.logistics.cte.config.IntegrationTestConfig;
import ru.yandex.market.mboc.http.DeliveryParams;

@ContextConfiguration(
        loader = AnnotationConfigWebContextLoader.class,
        classes = IntegrationTestConfig.class)
@TestExecutionListeners({
        DependencyInjectionTestExecutionListener.class,
        DirtiesContextTestExecutionListener.class,
        DbUnitTestExecutionListener.class,
        TransactionDbUnitTestExecutionListener.class,
        HibernateQueriesExecutionListener.class,
})
@WebAppConfiguration
@ExtendWith(SpringExtension.class)
@DbUnitConfiguration(
        databaseConnection = {"dbUnitDatabaseConnection", "dbqueueDatabaseConnection"},
        dataSetLoader = NullableColumnsDataSetLoader.class)
public abstract class IntegrationTest {

    @Autowired
    protected DeliveryParams deliveryParams;

    @Autowired
    protected CacheManager cacheManager;

    protected SoftAssertions assertions;

    @BeforeEach
    public void createAssertions() {
        assertions = new SoftAssertions();
    }

    @AfterEach
    public void triggerAssertions() {
        assertions.assertAll();
    }

    protected String readFromFile(String filename) {
        return Files.contentOf(new File(ClassLoader.getSystemResource(filename).getPath()), Charsets.UTF_8);
    }

    @AfterEach
    public void resetMocks() {
        Mockito.reset(
            deliveryParams
        );
    }
}
