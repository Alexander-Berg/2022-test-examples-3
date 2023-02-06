package ru.yandex.market.fulfillment.wrap.marschroute.functional.configuration;


import com.github.springtestdbunit.TransactionDbUnitTestExecutionListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import ru.yandex.market.fulfillment.wrap.core.configuration.database.LiquibaseConfiguration;
import ru.yandex.market.logistics.test.integration.db.listener.ResetDatabaseTestExecutionListener;


@Import({LiquibaseConfiguration.class, EmbeddedPostgresConfiguration.class})
@TestExecutionListeners(value = {
        DependencyInjectionTestExecutionListener.class,
        TransactionDbUnitTestExecutionListener.class,
        ResetDatabaseTestExecutionListener.class
})
@EnableJpaRepositories(basePackages = "ru.yandex.market.fulfillment.wrap.marschroute.repository")
@Configuration
public class DatabaseTestConfiguration {
}
