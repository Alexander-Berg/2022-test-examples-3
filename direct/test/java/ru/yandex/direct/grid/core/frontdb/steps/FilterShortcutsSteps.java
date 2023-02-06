package ru.yandex.direct.grid.core.frontdb.steps;

import java.time.Instant;

import com.yandex.ydb.table.TableClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.ydb.YdbPath;
import ru.yandex.direct.ydb.builder.QueryAndParams;
import ru.yandex.direct.ydb.builder.querybuilder.FromBuilder;
import ru.yandex.direct.ydb.builder.querybuilder.InsertBuilder;
import ru.yandex.direct.ydb.client.YdbClient;
import ru.yandex.direct.ydb.table.temptable.TempTableDescription;
import ru.yandex.direct.ydb.testutils.ydbinfo.YdbInfo;
import ru.yandex.direct.ydb.testutils.ydbinfo.YdbInfoFactory;

import static ru.yandex.direct.common.configuration.FrontDbYdbConfiguration.FRONTDB_YDB_CLIENT_BEAN;
import static ru.yandex.direct.common.configuration.FrontDbYdbConfiguration.FRONTDB_YDB_PATH_BEAN;
import static ru.yandex.direct.grid.core.frontdb.tables.Tables.FILTER_SHORTCUT_INSTANCES;
import static ru.yandex.direct.utils.HashingUtils.getMd5HashUtf8AsHexString;
import static ru.yandex.direct.ydb.table.temptable.TempTable.tempTable;

@Service
public class FilterShortcutsSteps {
    private final YdbPath path;
    private final YdbClient ydbClient;

    public FilterShortcutsSteps(@Qualifier(FRONTDB_YDB_CLIENT_BEAN) YdbClient ydbClient,
                                @Qualifier(FRONTDB_YDB_PATH_BEAN) YdbPath path) {
        this.path = path;
        this.ydbClient = ydbClient;
        createTables();
    }

    public String getHashForJsonFilter(String jsonFilter) {
        return getMd5HashUtf8AsHexString(jsonFilter);
    }

    public String saveFilter(ClientId clientId, String jsonFilter) {
        String hash = getHashForJsonFilter(jsonFilter);

        var tempTable = tempTable(new TempTableDescription(),
                FILTER_SHORTCUT_INSTANCES.HASH,
                FILTER_SHORTCUT_INSTANCES.CLIENT_ID,
                FILTER_SHORTCUT_INSTANCES.SAVE_TIME,
                FILTER_SHORTCUT_INSTANCES.FILTER).createValues();

        tempTable.fill(hash, clientId.asLong(), Instant.now(), jsonFilter);

        FromBuilder queryBuilder = InsertBuilder.replaceInto(FILTER_SHORTCUT_INSTANCES)
                .selectAll()
                .from(tempTable);

        QueryAndParams queryAndParams = queryBuilder.queryAndParams(path);
        ydbClient.executeQuery(queryAndParams);

        return hash;
    }

    private static void createTables() {
        YdbInfo ydbInfo = YdbInfoFactory.getExecutor();
        TableClient tableClient = ydbInfo.getClient();
        YdbPath db = ydbInfo.getDb();
        tableClient.createSession().thenAccept(
                sessionResult -> {
                    var session = sessionResult.expect("Cannot create session");
                    var tableDescription = FILTER_SHORTCUT_INSTANCES.getDescription();
                    YdbPath ydbPath = YdbPath.of(db.getPath(), FILTER_SHORTCUT_INSTANCES.getRealName());
                    session.createTable(ydbPath.getPath(), tableDescription).join().expect("Cannot " +
                            "create " +
                            "table " +
                            "filter_shortcuts");
                    session.close().join();
                }).join();
    }
}
