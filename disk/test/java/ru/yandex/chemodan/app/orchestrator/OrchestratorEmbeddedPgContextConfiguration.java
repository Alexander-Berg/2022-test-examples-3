package ru.yandex.chemodan.app.orchestrator;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.chemodan.boot.value.OverridableValuePrefix;
import ru.yandex.chemodan.util.jdbc.DataSourceProperties;
import ru.yandex.chemodan.util.test.EmbeddedDBDataSourceProperties;
import ru.yandex.misc.db.embedded.EmbeddedPostgres;
import ru.yandex.misc.db.embedded.ImportEmbeddedPg;
import ru.yandex.misc.db.embedded.PreparedDbProvider;

/**
 * @author yashunsky
 */
@Configuration
@Import({
        OrchestratorJdbcContextConfiguration.class,
})
@ImportEmbeddedPg
public class OrchestratorEmbeddedPgContextConfiguration {
    @Bean
    public PreparedDbProvider.DbInfo orchestratorDbInfo(EmbeddedPostgres embeddedPostgres) {
        PreparedDbProvider notes = PreparedDbProvider.forPreparer("orchestrator", embeddedPostgres);
        return notes.createDatabase();
    }

    @Bean
    @OverridableValuePrefix("orchestrator")
    public DataSourceProperties dataSourceProperties(EmbeddedPostgres embeddedPostgres) {
        return new EmbeddedDBDataSourceProperties(
                PreparedDbProvider.forPreparer("orchestrator", embeddedPostgres)
                        .createDatabase()
        );
    }

}
