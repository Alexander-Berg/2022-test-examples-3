package ru.yandex.market.tpl.carrier.core;

import javax.sql.DataSource;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.junit.jupiter.SpringExtension;

public class CleanupAfterEachEmbeddedDbExtension implements AfterEachCallback {

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        var dataSource = SpringExtension.getApplicationContext(context).getBean("dataSource", DataSource.class);
        truncateTables(dataSource);
    }

    private void truncateTables(DataSource dataSource) {
        ResourceDatabasePopulator scriptLauncher = new ResourceDatabasePopulator();
        scriptLauncher.addScript(new ClassPathResource("truncate.sql"));
        scriptLauncher.execute(dataSource);
    }
}
