package ru.yandex.market.pers.feedback.config;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import ru.yandex.market.pers.feedback.PersFeedbackApplication;
import ru.yandex.market.pers.feedback.mock.CheckouterMockConfigurer;
import ru.yandex.market.pers.service.common.PageMatcherController;
import ru.yandex.market.pers.test.common.AbstractPersWebTest;
import ru.yandex.market.pers.test.common.PersTestMocksHolder;

@Import({
    MockConfiguration.class,
    CoreConfig.class,
    CheckouterClientConfig.class,
    TestDbConfig.class,
    PageMatcherController.class
})
@ActiveProfiles(value = {"local-testing"})
@TestPropertySource("classpath:int-test.properties")
@TestConfiguration
@ComponentScan(
    basePackageClasses = {PersFeedbackApplication.class},
    excludeFilters = @ComponentScan.Filter(Configuration.class)
)
public abstract class AbstractPersFeedbackTest extends AbstractPersWebTest {
    @Autowired
    protected CheckouterMockConfigurer checkouterMockConfigurer;

    @Qualifier("feedbackDataSource")
    @Autowired
    private DataSource dataSource;

    @BeforeEach
    public void setUpBase() {
        truncateDatabase();
        resetMocks();
    }

    protected void truncateDatabase() {
        applySqlScript(dataSource, "/files/truncate-db.sql");
    }

    public void resetMocks() {
        PersTestMocksHolder.resetMocks();
    }
}
