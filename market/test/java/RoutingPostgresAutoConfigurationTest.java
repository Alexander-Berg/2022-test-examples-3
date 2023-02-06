package ru.yandex.market.starter.postgres;

import java.sql.SQLException;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import ru.yandex.market.starter.postgres.config.PostgresAutoConfiguration;
import ru.yandex.market.starter.postgres.config.PostgresRoutingConfiguration;
import ru.yandex.market.starter.postgres.datasources.ReadWriteRoutingDateSource;
import ru.yandex.market.starter.postgres.processors.DataSourceTraceBeanPostProcessor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class RoutingPostgresAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(PostgresAutoConfiguration.class));

    private final ApplicationContextRunner prodPostgresContextRunner = contextRunner.withPropertyValues(
        "mj.postgres.datasource.routing.enabled=true",
        "postgresql.hosts=localhost",
        "postgresql.database.name=pg",
        "postgresql.username=pg",
        "postgresql.password=pg"
    );

    @Test
    void productionPostgresTest() {
        prodPostgresContextRunner
            .run(context -> {
                    assertThat(context.getEnvironment().getProperty("mj.postgres.datasource.routing.enabled"))
                        .isEqualTo("true");
                    assertThat(context.getEnvironment().getProperty("mj.postgres.datasource.routing.write.url"))
                        .isNotNull();
                    assertThat(context.getEnvironment().getProperty("mj.postgres.datasource.routing.write.username"))
                        .isNotNull();
                    assertThat(context.getEnvironment().getProperty("mj.postgres.datasource.routing.write.password"))
                        .isNotNull();
                    assertThat(context.getEnvironment().getProperty("mj.postgres.datasource.routing.read.url"))
                        .isNotNull();
                    assertThat(context.getEnvironment().getProperty("mj.postgres.datasource.routing.read.username"))
                        .isNotNull();
                    assertThat(context.getEnvironment().getProperty("mj.postgres.datasource.routing.read.password"))
                        .isNotNull();
                    assertThat(context).hasSingleBean(DataSourceTraceBeanPostProcessor.class);
                    assertThat(context).getBean(PostgresRoutingConfiguration.WRITE_DATASOURCE_BEAN_NAME)
                        .isInstanceOf(HikariDataSource.class);
                    assertThat(context).getBean(PostgresRoutingConfiguration.READ_DATASOURCE_BEAN_NAME)
                        .isInstanceOf(HikariDataSource.class);

                    try {
                        context.getBean(DataSource.class).unwrap(ReadWriteRoutingDateSource.class);
                     } catch (SQLException exc) {
                        fail("primary datasource is not instance of ReadWriteRoutingDateSource.class");
                    }
                }
            );
    }

    @Test
    void propertiesOverridingTest() {
        final String url = "jdbc:postgresql://erewrwe/rttrg";
        final String username = "qweqwe";
        final String password = "iuyuiyui";

        prodPostgresContextRunner
            .withPropertyValues(
                "mj.postgres.datasource.routing.write.url=" + url,
                "mj.postgres.datasource.routing.write.username=" + username,
                "mj.postgres.datasource.routing.write.password=" + password
            )
            .run(context -> {
                    assertThat(context.getEnvironment().getProperty("mj.postgres.datasource.routing.write.url"))
                        .isEqualTo(url);
                    assertThat(context.getEnvironment().getProperty("mj.postgres.datasource.routing.write.username"))
                        .isEqualTo(username);
                    assertThat(context.getEnvironment().getProperty("mj.postgres.datasource.routing.write.password"))
                        .isEqualTo(password);
                }
            );
    }

}
