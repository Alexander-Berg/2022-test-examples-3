package ru.yandex.market.deepmind.tracker_approver.config;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.deepmind.tracker_approver.context_setter.DbListenerForHistoryContext;
import ru.yandex.market.deepmind.tracker_approver.context_setter.HistoryContext;
import ru.yandex.market.deepmind.tracker_approver.context_setter.HistoryContextSetter;
import ru.yandex.market.deepmind.tracker_approver.context_setter.HistoryContextSupplier;

@Configuration
@Import({
    DbConfig.class
})
public class HistoryContextSetterConfig {
    private final DbConfig dbConfig;

    @Value("${tracker_approver.schema}")
    private String schema;

    public HistoryContextSetterConfig(DbConfig dbConfig) {
        this.dbConfig = dbConfig;
    }

    @Bean
    public HistoryContextSetter historyContextSetter() {
        return new HistoryContextSetter(schema, dbConfig.jdbcTemplate());
    }

    @Bean
    public HistoryContextSupplier historyContextSupplierService() {
        return () -> new HistoryContext("test_login", "test_context");
    }

    @PostConstruct
    public void setupListener() {
        dbConfig.delegatingJdbcInterceptorSupplier().setSupplier(
            () -> new DbListenerForHistoryContext(
                historyContextSetter(),
                historyContextSupplierService()));
    }
}
