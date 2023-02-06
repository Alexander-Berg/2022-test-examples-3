package ru.yandex.market.sc.core.test;

import javax.sql.DataSource;

import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * @author valter
 */
public class CleanupEmbeddedDbExtension implements BeforeEachCallback {

    @Override
    public void beforeEach(ExtensionContext context) {
        var dataSource = SpringExtension.getApplicationContext(context).getBean("dataSource", DataSource.class);
        truncateTables(dataSource);
    }

    private void truncateTables(DataSource dataSource) {
        ResourceDatabasePopulator scriptLauncher = new ResourceDatabasePopulator();
        scriptLauncher.addScript(new ClassPathResource("truncate.sql"));
        scriptLauncher.execute(dataSource);

    }
}
