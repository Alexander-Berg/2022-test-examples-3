package ru.yandex.market.tpl.core;

import javax.persistence.EntityManager;
import javax.sql.DataSource;

import org.apache.commons.collections.CollectionUtils;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.core.test.TplAbstractTest;

public class CleanupAfterEachV2Extension implements AfterEachCallback {

    public static final String TRUNCATE_SQL_SCRIPT = "truncate.sql";

    @Override
    public void afterEach(ExtensionContext context) {
        var dataSource = getBeanFromExtensionTestContext(context, DataSource.class);
        truncateTables(dataSource);
        clearDBEntities(context);
    }

    /**
     * Удаление конкретных сущностей теста, которые предварительно были помещены в
     * в removingEntities.
     * @param context
     */
    private void clearDBEntities(ExtensionContext context) {
        context.getTestInstance()
                .filter(TplAbstractTest.class::isInstance)
                .map(TplAbstractTest.class::cast)
                .map(TplAbstractTest::getRemovingEntities)
                .filter(CollectionUtils::isNotEmpty)
                .ifPresent(entities -> {
                    EntityManager em = getBeanFromExtensionTestContext(context, EntityManager.class);
                    TransactionTemplate transactionTemplate = getBeanFromExtensionTestContext(context,
                            TransactionTemplate.class);

                    transactionTemplate.execute(status -> {
                        entities
                                .stream()
                                .map(em::merge)
                                .forEach(em::remove);
                        return true;
                    });
                    entities.clear();
                });
    }

    private void truncateTables(DataSource dataSource) {
        ResourceDatabasePopulator scriptLauncher = new ResourceDatabasePopulator();
        scriptLauncher.addScript(new ClassPathResource(TRUNCATE_SQL_SCRIPT));
        scriptLauncher.execute(dataSource);
    }

    private <T> T getBeanFromExtensionTestContext(ExtensionContext context, Class<T> beanClass) {
        return SpringExtension.getApplicationContext(context).getBean(beanClass);
    }

}
