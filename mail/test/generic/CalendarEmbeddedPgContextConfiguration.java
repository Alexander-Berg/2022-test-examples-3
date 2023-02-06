package ru.yandex.calendar.test.generic;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.bolts.collection.Either;
import ru.yandex.calendar.boot.DatabaseCredentials;
import ru.yandex.misc.db.embedded.EmbeddedPostgres;
import ru.yandex.misc.db.embedded.ImportEmbeddedPg;
import ru.yandex.misc.db.embedded.PreparedDbProvider;

/**
 * @author dbrylev
 */
@Configuration
@ImportEmbeddedPg
public class CalendarEmbeddedPgContextConfiguration {

    @Bean
    public DatabaseCredentials dbCredentials(EmbeddedPostgres pg) {
        PreparedDbProvider.DbInfo db =
                PreparedDbProvider.forPreparer("calendardb", pg).createDatabase();

        return new DatabaseCredentials(
                Either.left(db.getHost()), db.getPort(), db.getDbName(), db.getUser(), db.getPassword());
    }
}
