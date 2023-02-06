package ru.yandex.chemodan.test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;

import ru.yandex.chemodan.boot.value.OverridableValuePrefix;
import ru.yandex.chemodan.util.jdbc.DataSourceProperties;
import ru.yandex.chemodan.util.jdbc.JdbcDatabaseConfigurator;
import ru.yandex.chemodan.util.jdbc.JdbcDatabaseConfiguratorContextConfiguration;
import ru.yandex.chemodan.util.test.EmbeddedDBDataSourceProperties;
import ru.yandex.misc.db.embedded.EmbeddedPostgres;
import ru.yandex.misc.db.embedded.ImportEmbeddedPg;
import ru.yandex.misc.db.embedded.PreparedDbProvider;

/**
 * @author vpronto
 */
@Configuration
@Import(JdbcDatabaseConfiguratorContextConfiguration.class)
@ImportEmbeddedPg
@Profile(ActivateBazingaEmbeddedPg.BAZINGA_EMBEDDED_PG)
public class BazingaEmbeddedPgConfiguration {

    @Autowired
    private JdbcDatabaseConfiguratorContextConfiguration dbConfiguratorConfig;

    @Bean
    @OverridableValuePrefix("bazinga")
    public DataSourceProperties bazingaDataSourceProperties(EmbeddedPostgres embeddedPostgres) {
        return new EmbeddedDBDataSourceProperties(PreparedDbProvider.forPreparer("diskqdb", embeddedPostgres).createDatabase());
    }

    @Bean
    public JdbcDatabaseConfigurator bazingaDbConfigurator(DataSourceProperties bazingaDataSourceProperties) {
        return dbConfiguratorConfig.consJdbcConfigurator(bazingaDataSourceProperties);
    }
}
