package ru.yandex.market.robot.db.raw_model.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import ru.yandex.market.robot.db.AutoGenerationSettingsDao;
import ru.yandex.market.robot.db.RawModelStorage;
import ru.yandex.market.robot.db.TitleProcessorDao;
import ru.yandex.market.robot.db.liquibase.LiquibaseTestConfig;
import ru.yandex.market.test.db.embeded.postgres.TransactionalEmbeddedPostgresConfig;

/**
 * @author jkt on 01.12.17.
 */
@Configuration
@Import({
    TransactionalEmbeddedPostgresConfig.class,
    LiquibaseTestConfig.class
})
public class MarketRawModelAccessorsTestConfig {

    // todo сейчас база поднимается для каждого тестового класса. Это + 7-10 секунд на тестовый класс.
    // Надо сделать чтобы тесты переиспользовали один контект и он не инициализировался для каждого тестового класса
    @Autowired
    TransactionalEmbeddedPostgresConfig postgres;


    @Bean
    public JdbcTemplate jdbcTemplate() {
        return new JdbcTemplate(postgres.dataSource());
    }

    @Bean
    DataSourceTransactionManager rawModelTransactionManager() {
        return new DataSourceTransactionManager(postgres.dataSource());
    }

    @Bean
    TransactionTemplate rawModelTransactionTemplate() {
        return new TransactionTemplate(rawModelTransactionManager());
    }

    @Bean
    TitleProcessorDao titleProcessorDao() {
        TitleProcessorDao dao = new TitleProcessorDao();
        dao.setJdbcTemplate(jdbcTemplate());
        dao.setTransactionTemplate(rawModelTransactionTemplate());
        return dao;
    }

    @Bean
    RawModelStorage rawModelStorage() {
        RawModelStorage storage = new RawModelStorage();
        storage.setJdbcTemplate(jdbcTemplate());
        storage.setTransactionTemplate(rawModelTransactionTemplate());
        storage.setTitleProcessorDao(titleProcessorDao());
        return storage;
    }

    @Bean
    AutoGenerationSettingsDao autoGenerationSettingsDao() {
        AutoGenerationSettingsDao result = new AutoGenerationSettingsDao();
        result.setJdbcTemplate(jdbcTemplate());
        result.setRawModelStorage(rawModelStorage());
        return result;
    }

}
