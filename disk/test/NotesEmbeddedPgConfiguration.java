package ru.yandex.chemodan.app.notes.dao.test;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.chemodan.app.notes.dao.configuration.NotesJdbcContextConfiguration;
import ru.yandex.chemodan.boot.value.OverridableValuePrefix;
import ru.yandex.chemodan.util.jdbc.DataSourceProperties;
import ru.yandex.chemodan.util.test.EmbeddedDBDataSourceProperties;
import ru.yandex.misc.db.embedded.EmbeddedPostgres;
import ru.yandex.misc.db.embedded.ImportEmbeddedPg;
import ru.yandex.misc.db.embedded.PreparedDbProvider;

/**
 * @author vpronto
 */
@Configuration
@Import({
        NotesJdbcContextConfiguration.class,
})
@ImportEmbeddedPg
public class NotesEmbeddedPgConfiguration {

    @Bean
    public PreparedDbProvider.DbInfo notesDbInfo(EmbeddedPostgres embeddedPostgres) {
        PreparedDbProvider notes = PreparedDbProvider.forPreparer("disk_notes", embeddedPostgres);
        return notes.createDatabase();
    }

    @Bean
    @OverridableValuePrefix("notes")
    public DataSourceProperties notesDataSourceProperties(PreparedDbProvider.DbInfo notesDbInfo) {
        return new EmbeddedDBDataSourceProperties(notesDbInfo);
    }

}
