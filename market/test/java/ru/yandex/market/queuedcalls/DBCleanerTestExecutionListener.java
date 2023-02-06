package ru.yandex.market.queuedcalls;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;
import org.springframework.transaction.support.TransactionTemplate;

public class DBCleanerTestExecutionListener extends AbstractTestExecutionListener {

    private static final Log LOG = LogFactory.getLog(DBCleanerTestExecutionListener.class);
    private final Resource truncateScript = new ClassPathResource("clean_db.sql");
    private final ResourceDatabasePopulator populator = new ResourceDatabasePopulator(truncateScript);

    @Override
    public void afterTestMethod(TestContext testContext) {
        LOG.info("Starting clean db");
        final ApplicationContext context = testContext.getApplicationContext();
        final DataSource masterDatasource = context.getBean(EnvironmentConfig.class).queuedCallsDataSource();
        final TransactionTemplate transactionTemplate = context.getBean(TransactionTemplate.class);

        transactionTemplate.execute(ts -> {
            DatabasePopulatorUtils.execute(populator, masterDatasource);
            return null;
        });
    }
}
