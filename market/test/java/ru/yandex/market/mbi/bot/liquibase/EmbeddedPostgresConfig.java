package ru.yandex.market.mbi.bot.liquibase;

import javax.sql.DataSource;

import com.opentable.db.postgres.embedded.EmbeddedPostgres;
import com.opentable.db.postgres.embedded.LiquibasePreparer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EmbeddedPostgresConfig {

    @Bean
    public DataSource dataSource() throws Exception {
        DataSource dataSource = EmbeddedPostgres.builder().start().getPostgresDatabase();
        LiquibasePreparer.forClasspathLocation("changelog.xml").prepare(dataSource);
        return dataSource;
    }
}
