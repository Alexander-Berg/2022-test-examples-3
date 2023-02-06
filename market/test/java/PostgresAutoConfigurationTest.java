package ru.yandex.market.starter.postgres;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.market.starter.postgres.config.PostgresAutoConfiguration;
import ru.yandex.market.starter.postgres.processors.DataSourceTraceBeanPostProcessor;

import static org.assertj.core.api.Assertions.assertThat;

public class PostgresAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(PostgresAutoConfiguration.class));

    private final ApplicationContextRunner prodPostgresContextRunner = contextRunner.withPropertyValues(
        "postgresql.hosts=localhost",
        "postgresql.database.name=pg",
        "postgresql.username=pg",
        "postgresql.password=pg"
    );

    private final ApplicationContextRunner embeddedPostgresContextRunner = contextRunner
        .withPropertyValues("mj.postgres.embedded.enabled=true");

    @Test
    void productionPostgresTest() {
        prodPostgresContextRunner
            .run(context -> {
                    assertThat(context.getEnvironment().getProperty("spring.datasource.url"))
                        .isNotNull();
                    assertThat(context.getEnvironment().getProperty("spring.datasource.username"))
                        .isNotNull();
                    assertThat(context.getEnvironment().getProperty("spring.datasource.password"))
                        .isNotNull();
                    assertThat(context).hasSingleBean(DataSourceTraceBeanPostProcessor.class);
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
                "spring.datasource.url=" + url,
                "spring.datasource.username=" + username,
                "spring.datasource.password=" + password
            )
            .run(context -> {
                    assertThat(context.getEnvironment().getProperty("spring.datasource.url")).isEqualTo(url);
                    assertThat(context.getEnvironment().getProperty("spring.datasource.username")).isEqualTo(username);
                    assertThat(context.getEnvironment().getProperty("spring.datasource.password")).isEqualTo(password);
                }
            );
    }

    @Test
    void embeddedPostgresTest() {
        embeddedPostgresContextRunner
            .run(context -> {
                    assertThat(context.getEnvironment().getProperty("spring.liquibase.change-log"))
                        .isEqualTo("classpath:liquibase/db-changelog.xml");
                    assertThat(context).hasSingleBean(EmbeddedPostgres.class);
                    assertThat(context).hasSingleBean(DataSource.class);
                }
            );
    }

    @Test
    void zonkyPropertiesTest() {
        final int port = 4543;
        embeddedPostgresContextRunner
            .withPropertyValues("mj.zonky.port=" + port)
            .run(context -> assertThat(context.getBean(EmbeddedPostgres.class).getPort()).isEqualTo(port));
    }

    @Test
    void embeddedDatasourceOverridingTest() {
        embeddedPostgresContextRunner
            .withUserConfiguration(UserDataSourceConfiguration.class)
            .run(context -> {
                assertThat(context).doesNotHaveBean(EmbeddedPostgres.class);
                assertThat(context).hasSingleBean(DataSource.class);
            });
    }

    @Test
    void recipeConfigurationTest() {
        final int port = 8786;
        final String dbName = "fsdfdsf";
        final String username = "qweqw";
        final String password = "oiutr";

        embeddedPostgresContextRunner
            .withPropertyValues("mj.postgres.embedded.type=recipe")
            .withSystemProperties(
                "PG_LOCAL_PORT=" + port,
                "PG_LOCAL_DATABASE=" + dbName,
                "PG_LOCAL_USER=" + username,
                "PG_LOCAL_PASSWORD=" + password
            )
            .run(context -> {
                assertThat(context.getEnvironment().getProperty("spring.datasource.url"))
                    .isEqualTo("jdbc:postgresql://localhost:" + port + "/" + dbName);
                assertThat(context.getEnvironment().getProperty("spring.datasource.username")).isEqualTo(username);
                assertThat(context.getEnvironment().getProperty("spring.datasource.password")).isEqualTo(password);
            });
    }

    @Configuration
    static class UserDataSourceConfiguration {

        @Bean
        DataSource dataSource() {
            return new HikariDataSource();
        }

    }
}
