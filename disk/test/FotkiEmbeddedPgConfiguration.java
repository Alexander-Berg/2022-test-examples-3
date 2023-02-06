package ru.yandex.chemodan.app.fotki.dao.test;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.chemodan.boot.value.OverridableValuePrefix;
import ru.yandex.chemodan.util.jdbc.DataSourceProperties;
import ru.yandex.chemodan.util.test.EmbeddedDBDataSourceProperties;
import ru.yandex.misc.db.embedded.EmbeddedPostgres;
import ru.yandex.misc.db.embedded.ImportEmbeddedPg;
import ru.yandex.misc.db.embedded.PreparedDbProvider;

@Configuration
@Import(FotkiTestJdbcContextConfiguration.class)
@ImportEmbeddedPg
public class FotkiEmbeddedPgConfiguration {

    @Bean
    public PreparedDbProvider.DbInfo fotkiDbInfo(EmbeddedPostgres embeddedPostgres) {
        PreparedDbProvider fotki = PreparedDbProvider.forPreparer("fotki", embeddedPostgres);
        return fotki.createDatabase();
    }

    @Bean
    @OverridableValuePrefix("fotki")
    public DataSourceProperties fotkiDataSourceProperties(PreparedDbProvider.DbInfo fotkiDbInfo) {
        return new EmbeddedDBDataSourceProperties(fotkiDbInfo);
    }
}
