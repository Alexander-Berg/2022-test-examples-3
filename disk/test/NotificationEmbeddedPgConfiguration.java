package ru.yandex.chemodan.app.notifier.admin.dao.test;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;

import ru.yandex.chemodan.app.notifier.admin.dao.NotifierAdminJdbcContextConfiguration;
import ru.yandex.chemodan.boot.value.OverridableValuePrefix;
import ru.yandex.chemodan.util.jdbc.DataSourceProperties;
import ru.yandex.chemodan.util.test.EmbeddedDBDataSourceProperties;
import ru.yandex.misc.db.embedded.EmbeddedPostgres;
import ru.yandex.misc.db.embedded.ImportEmbeddedPg;
import ru.yandex.misc.db.embedded.PreparedDbProvider;

import static ru.yandex.chemodan.app.notifier.admin.dao.test.ActivateNotificationEmbeddedPg.NOTIFICATION_EMBEDDED_PG;

/**
 * @author vpronto
 */
@Configuration
@Import(NotifierAdminJdbcContextConfiguration.class)
@ImportEmbeddedPg
@Profile(NOTIFICATION_EMBEDDED_PG)
public class NotificationEmbeddedPgConfiguration {

    @Bean
    public PreparedDbProvider.DbInfo notifierDbInfo(EmbeddedPostgres embeddedPostgres) {
        PreparedDbProvider dataapi = PreparedDbProvider.forPreparer("notifier", embeddedPostgres);
        return dataapi.createDatabase();
    }

    @Bean
    @OverridableValuePrefix("notifier-admin")
    public DataSourceProperties notifierAdminDataSourceProperties(PreparedDbProvider.DbInfo notifierDbInfo) {
        return new EmbeddedDBDataSourceProperties(notifierDbInfo);
    }

}
