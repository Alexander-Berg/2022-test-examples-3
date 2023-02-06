package ru.yandex.chemodan.app.urlshortener.test;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import ru.yandex.chemodan.app.urlshortener.dao.UrlShortenerJdbcContextConfiguration;
import ru.yandex.chemodan.boot.value.OverridableValuePrefix;
import ru.yandex.chemodan.util.jdbc.DataSourceProperties;
import ru.yandex.chemodan.util.test.EmbeddedDBDataSourceProperties;
import ru.yandex.misc.db.embedded.EmbeddedPostgres;
import ru.yandex.misc.db.embedded.ImportEmbeddedPg;
import ru.yandex.misc.db.embedded.PreparedDbProvider;

import static ru.yandex.chemodan.app.urlshortener.test.ActivateUrlShortenerEmbeddedPg.URL_SHORTENER_EMBEDDED_PG;

/**
 * @author vpronto
 */
@Configuration
@Import(UrlShortenerJdbcContextConfiguration.class)
@ImportEmbeddedPg
@Profile(URL_SHORTENER_EMBEDDED_PG)
public class UrlShortenerEmbeddedPgConfiguration {

    @Bean
    @Primary
    public PreparedDbProvider.DbInfo dbInfoShard1(EmbeddedPostgres embeddedPostgres) {
        return PreparedDbProvider.forPreparer("urlsdb", embeddedPostgres).createDatabase();
    }

    @Bean
    @Primary
    @OverridableValuePrefix("urlshortener")
    public DataSourceProperties urlshortenerDataSourceProperties(PreparedDbProvider.DbInfo dbInfo) {
        return new EmbeddedDBDataSourceProperties(dbInfo);
    }

}
