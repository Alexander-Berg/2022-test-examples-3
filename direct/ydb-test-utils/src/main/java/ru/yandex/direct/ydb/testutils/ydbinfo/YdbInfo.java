package ru.yandex.direct.ydb.testutils.ydbinfo;

import com.yandex.ydb.table.TableClient;

import ru.yandex.direct.ydb.YdbPath;

public interface YdbInfo {
    void init();

    TableClient getClient();

    YdbPath getDb();
}
