package ru.yandex.market.fulfillment.wrap.marschroute.functional.configuration;

import java.io.FileInputStream;
import java.sql.Timestamp;
import java.time.LocalDateTime;

import com.github.springtestdbunit.DbUnitTestExecutionListener;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.dataset.ReplacementDataSetModifier;
import org.dbunit.IDatabaseTester;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.ReplacementDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockitoTestExecutionListener;
import org.springframework.boot.test.mock.mockito.ResetMocksTestExecutionListener;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.fulfillment.wrap.core.configuration.database.LiquibaseConfiguration;
import ru.yandex.market.logistics.test.integration.db.DbUnitTestConfiguration;
import ru.yandex.market.logistics.test.integration.db.listener.CleanDatabase;
import ru.yandex.market.logistics.test.integration.db.listener.ResetDatabaseTestExecutionListener;

@Import(value = {LiquibaseConfiguration.class,
    EmbeddedPostgresConfiguration.class,
    DbUnitTestConfiguration.class}
)
@TestExecutionListeners(value = {
    DependencyInjectionTestExecutionListener.class,
    ResetDatabaseTestExecutionListener.class,
    DbUnitTestExecutionListener.class,
    MockitoTestExecutionListener.class,
    ResetMocksTestExecutionListener.class
})
@CleanDatabase
@EnableJpaRepositories(basePackages = "ru.yandex.market.fulfillment.wrap.marschroute.repository")
@DbUnitConfiguration(databaseConnection = "dbUnitDatabaseConnection")
public abstract class RepositoryTest extends MarschrouteWrapTest {

    private static final Timestamp EXACTLY_90_DAYS_BEFORE_TODAY = Timestamp.valueOf(LocalDateTime.now());

    private static final Timestamp MORE_THEN_90_DAYS_BEFORE_TODAY = Timestamp.valueOf(LocalDateTime.now()
        .minusDays(91));

    @Autowired
    protected TransactionTemplate transactionTemplate;

    @Autowired
    protected IDatabaseTester databaseTester;

    @Autowired
    protected ResourceLoader resourceLoader;

    protected void setupCreatedDateInDataSet(String configLocation) {
        try {
            IDataSet dataSet = new FlatXmlDataSetBuilder()
                .build(new FileInputStream(resourceLoader.getResource(configLocation).getFile()));
            ReplacementDataSet rDataSet = new ReplacementDataSet(dataSet);
            rDataSet.addReplacementObject("[created_90_days_ago]", EXACTLY_90_DAYS_BEFORE_TODAY);
            rDataSet.addReplacementObject("[created_more_then_90_days_ago]", MORE_THEN_90_DAYS_BEFORE_TODAY);
            databaseTester.setDataSet(rDataSet);
            databaseTester.onSetup();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    protected static class DateSetterModifier extends ReplacementDataSetModifier {

        @Override
        protected void addReplacements(ReplacementDataSet rDataSet) {
            rDataSet.addReplacementObject("[created_90_days_ago]", EXACTLY_90_DAYS_BEFORE_TODAY);
            rDataSet.addReplacementObject("[created_more_then_90_days_ago]", MORE_THEN_90_DAYS_BEFORE_TODAY);
        }

    }

}

