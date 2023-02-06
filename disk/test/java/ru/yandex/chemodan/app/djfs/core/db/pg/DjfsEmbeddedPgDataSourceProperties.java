package ru.yandex.chemodan.app.djfs.core.db.pg;

import ru.yandex.chemodan.util.test.EmbeddedDBDataSourceProperties;
import ru.yandex.misc.db.embedded.PreparedDbProvider;

/**
 * @author eoshch
 */
public class DjfsEmbeddedPgDataSourceProperties extends EmbeddedDBDataSourceProperties {
    public DjfsEmbeddedPgDataSourceProperties(PreparedDbProvider.DbInfo dbInfo) {
        super(dbInfo);
    }

    @Override
    public String getUrlSuffix() {
        return "?prepareThreshold=0&defaultTz=UTC";
    }

    @Override
    public String getUsername() {
        return "disk_mpfs";
    }
}
