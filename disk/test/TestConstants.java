package ru.yandex.chemodan.app.dataapi.test;

import ru.yandex.chemodan.app.dataapi.api.context.DatabaseAppContext;
import ru.yandex.chemodan.app.dataapi.api.db.ref.AppDatabaseRef;
import ru.yandex.chemodan.app.dataapi.api.db.ref.external.ExternalDatabaseAlias;

/**
 * @author Denis Bakharev
 * @author Dmitriy Amelin (lemeh)
 */
public class TestConstants {
    public static final String DATAAPI = "dataapi";
    public static final DatabaseAppContext CLIENT_CTX = new DatabaseAppContext("client-app");
    public static final AppDatabaseRef CLIENT_DB_REF = new AppDatabaseRef(CLIENT_CTX, "database_id");
    public static final DatabaseAppContext ORIGINAL_APP = new DatabaseAppContext("original-app");

    public static final ExternalDatabaseAlias EXT_DB_ALIAS_RO =
            new ExternalDatabaseAlias(CLIENT_CTX.appName(), ORIGINAL_APP.appName(), "database-id-read-only");
    public static final ExternalDatabaseAlias EXT_DB_ALIAS_RW =
            new ExternalDatabaseAlias(CLIENT_CTX.appName(), ORIGINAL_APP.appName(), "database-id-read-write");
}
