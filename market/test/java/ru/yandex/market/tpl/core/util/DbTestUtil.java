package ru.yandex.market.tpl.core.util;

import javax.sql.DataSource;

import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

public final class DbTestUtil {

    public static void truncateTables(DataSource dataSource) {
        ResourceDatabasePopulator scriptLauncher = new ResourceDatabasePopulator();
        scriptLauncher.addScript(new ClassPathResource("truncate.sql"));
        scriptLauncher.execute(dataSource);
    }

    private DbTestUtil() {}
}
