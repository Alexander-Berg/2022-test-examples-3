package ru.yandex.chemodan.app.djfs.core.db.pg;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.chemodan.util.jdbc.DataSourceProperties;
import ru.yandex.chemodan.util.sharpei.SharpeiClient;
import ru.yandex.misc.db.embedded.ActivateEmbeddedPg;
import ru.yandex.misc.db.embedded.EmbeddedPostgres;
import ru.yandex.misc.db.embedded.ImportEmbeddedPg;
import ru.yandex.misc.db.embedded.PreparedDbProvider;

/**
 * @author eoshch
 */
@Configuration
@ActivateEmbeddedPg
@ImportEmbeddedPg
public class DjfsEmbeddedPgContextConfiguration {
    @Bean
    public SharpeiClient sharpeiClient(List<PreparedDbProvider.DbInfo> dbInfos) {
        InMemorySharpeiClient result = new InMemorySharpeiClient();
        for (PreparedDbProvider.DbInfo dbInfo : dbInfos) {
            result.addShard(dbInfo);
        }
        return result;
    }

    @Bean
    public PreparedDbProvider.DbInfo dbInfoShard1(EmbeddedPostgres embeddedPostgres) {
        return PreparedDbProvider.forPreparer("diskdb", embeddedPostgres).createDatabase();
    }

    @Bean
    public PreparedDbProvider.DbInfo dbInfoShard2(EmbeddedPostgres embeddedPostgres) {
        return PreparedDbProvider.forPreparer("diskdb", embeddedPostgres).createDatabase();
    }

    @Bean
    public PreparedDbProvider.DbInfo dbInfoCommonShard(EmbeddedPostgres embeddedPostgres) {
        return PreparedDbProvider.forPreparer("disk_commondb", embeddedPostgres).createDatabase();
    }

    @Bean
    public DataSourceProperties dataSourceProperties(List<PreparedDbProvider.DbInfo> dbInfos) {
        return new DjfsEmbeddedPgDataSourceProperties(dbInfos.get(0));
    }
}
