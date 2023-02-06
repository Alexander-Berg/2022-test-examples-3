package ru.yandex.market.pers.comparison.config;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.application.monitoring.ComplexMonitoring;
import ru.yandex.market.pers.comparison.PersComparison;
import ru.yandex.market.pers.comparison.controller.TransactionalLogEventTestConsumer;
import ru.yandex.market.pers.comparison.logging.TransactionalLogEventConsumer;
import ru.yandex.market.pers.comparison.model.ComparisonItemsLogEntity;
import ru.yandex.market.pers.test.db.EmbeddedPostgreFactory;

import static org.mockito.Mockito.mock;

/**
 * @author varvara
 * 28.01.2020
 */
@Import({
    CoreConfig.class,
    InternalConfig.class
})
@Configuration
@ComponentScan(
        basePackageClasses = {PersComparison.class},
        excludeFilters = @ComponentScan.Filter(Configuration.class)
)
public class CoreMockConfiguration {

    @Bean(destroyMethod = "close")
    public Object embeddedPostgres() {
        return EmbeddedPostgreFactory.embeddedPostgres(x -> x);
    }

    @Bean
    public DataSource embeddedDatasource() {
        return EmbeddedPostgreFactory.embeddedDatasource(embeddedPostgres(), Map.of());
    }

    @Bean
    public ComplexMonitoring complexMonitoring() {
        return mock(ComplexMonitoring.class);
    }

    @Bean
    @Qualifier("pgDataSource")
    public DataSource pgDataSource() {
        return embeddedDatasource();
    }

    @Bean
    @Qualifier("pgJdbcTemplate")
    public JdbcTemplate pgJdbcTemplate(@Qualifier("pgDataSource") DataSource pgDataSource) {
        return new JdbcTemplate(pgDataSource);
    }

    @Bean
    @Qualifier("transactionalLogEventConsumer")
    public TransactionalLogEventConsumer<ComparisonItemsLogEntity> transactionalLogEventConsumer() {
        return new TransactionalLogEventTestConsumer(ComparisonItemsLogEntity.class);
    }

    @Bean
    @Qualifier("transactionalLogEventConsumerList")
    public List<TransactionalLogEventConsumer> transactionalLogEventConsumerList(
        @Qualifier("transactionalLogEventConsumer") TransactionalLogEventConsumer consumer
    ) {
        return Collections.singletonList(consumer);
    }

}
