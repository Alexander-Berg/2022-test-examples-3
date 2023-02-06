package ru.yandex.market.cashier;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;

public class DBCleanerTestExecutionListener extends AbstractTestExecutionListener {
    private static final Log LOG = LogFactory.getLog(DBCleanerTestExecutionListener.class);
    private Resource truncateScript = new ClassPathResource("clean_db.sql");
    private ResourceDatabasePopulator populator = new ResourceDatabasePopulator(truncateScript);

    @Override
    public void afterTestMethod(TestContext testContext) throws Exception {
        LOG.info("Starting clean db");
        final ApplicationContext context = testContext.getApplicationContext();
        final DataSource masterDatasource = context.getBean(DataSource.class);
        final PlatformTransactionManager transactionManager = context.getBean(PlatformTransactionManager.class);

        TransactionTemplate transactionTemplate = new TransactionTemplate();
        transactionTemplate.setTransactionManager(transactionManager);

        transactionTemplate.execute(ts -> {
            DatabasePopulatorUtils.execute(populator, masterDatasource);
            return null;
        });
    }
}
