package ru.yandex.market.robot.db.liquibase;

import liquibase.integration.spring.SpringLiquibase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import ru.yandex.market.robot.db.AutoGenerationSettingsDao;
import ru.yandex.market.robot.db.RawModelStorage;
import ru.yandex.market.test.db.embeded.postgres.TransactionalEmbeddedPostgresConfig;

import javax.sql.DataSource;

/**
 * @author jkt on 01.12.17.
 */
@Configuration
@Import({
    TransactionalEmbeddedPostgresConfig.class
})
public class LiquibaseTestConfig {

    private static final String MARKET_RAW_MODEL_CHANGELOG =
        "classpath:ru/yandex/market/robot/db/liquibase/market_raw_model/market_raw_model.changelog.xml";


    @Autowired
    TransactionalEmbeddedPostgresConfig postgres;


    @Bean
    public JdbcTemplate jdbcTemplate() {
        return new JdbcTemplate(postgres.dataSource());
    }


    @Bean
    public SpringLiquibase liquibase() {
        return createLiquibaseForChangeLog(postgres.dataSource(), MARKET_RAW_MODEL_CHANGELOG);
    }

    private SpringLiquibase createLiquibaseForChangeLog(DataSource dataSource, String changelog) {
        SpringLiquibase result = new SpringLiquibase();
        result.setDataSource(dataSource);
        result.setChangeLog(changelog);
        return result;
    }
}
