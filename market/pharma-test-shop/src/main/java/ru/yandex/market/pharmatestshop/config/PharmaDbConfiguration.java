package ru.yandex.market.pharmatestshop.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@Import({
        PharmaLiquibaseConfiguration.class,
        HibernateJpaAutoConfiguration.class,
})

@EntityScan("ru.yandex.market.pharmatestshop.domain")
@EnableJpaRepositories("ru.yandex.market.pharmatestshop.domain")
@EnableTransactionManagement(proxyTargetClass = true)
@EnableJpaAuditing

@ImportAutoConfiguration({
        JdbcTemplateAutoConfiguration.class,
        TransactionAutoConfiguration.class,
})
@RequiredArgsConstructor
public class PharmaDbConfiguration {

}
