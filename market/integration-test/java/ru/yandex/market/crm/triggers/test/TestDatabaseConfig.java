package ru.yandex.market.crm.triggers.test;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.mcrm.db.ChangelogProvider;
import ru.yandex.market.mcrm.db.test.TestMasterReadOnlyDataSourceConfiguration;

/**
 * @author apershukov
 */
@Configuration
@Import(TestMasterReadOnlyDataSourceConfiguration.class)
public class TestDatabaseConfig {

    @Bean
    public ChangelogProvider coreIntTestChangelogProvider() {
        return () -> "/sql/changelog.xml";
    }
}
