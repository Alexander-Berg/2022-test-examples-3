package {root_package}.config;

import java.io.IOException;

import javax.sql.DataSource;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@Configuration
public class EmbeddedDbConfig {

    @Value("${postgres.embedded.port:0}")
    private int port;

    @Bean
    public NamedParameterJdbcTemplate postgresJdbcTemplate(DataSource dataSource) {
        return new NamedParameterJdbcTemplate(dataSource);
    }

    @Bean(destroyMethod = "close")
    public EmbeddedPostgres embeddedPostgres() throws IOException {
        return EmbeddedPostgres.builder()
            .setErrorRedirector(ProcessBuilder.Redirect.INHERIT)
            .setOutputRedirector(ProcessBuilder.Redirect.INHERIT)
            .setPort(port)
            .start();
    }

    @Bean
    public DataSource createPostgresDataSource(EmbeddedPostgres embeddedPostgres) {
        return embeddedPostgres.getPostgresDatabase();
    }
}
