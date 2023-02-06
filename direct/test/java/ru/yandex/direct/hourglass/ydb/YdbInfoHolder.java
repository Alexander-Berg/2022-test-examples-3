package ru.yandex.direct.hourglass.ydb;

import com.yandex.ydb.table.TableClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.direct.ydb.YdbPath;
import ru.yandex.direct.ydb.testutils.ydbinfo.YdbInfo;
import ru.yandex.direct.ydb.testutils.ydbinfo.YdbInfoFactory;

import static ru.yandex.direct.hourglass.ydb.storage.Tables.SCHEDULED_TASKS;
import static ru.yandex.direct.hourglass.ydb.storage.Tables.SCHEDULER_INSTANCES;

public class YdbInfoHolder {
    private static final Logger logger = LoggerFactory.getLogger(YdbInfoHolder.class);
    private volatile static boolean tablesCreated = false;


    public static YdbInfo getYdbInfo() {
        YdbInfo ydbInfo = YdbInfoFactory.getExecutor();
        if (!tablesCreated) {
            synchronized (YdbInfoHolder.class) {
                if (!tablesCreated) {
                    logger.info("Ydb: start create tables");
                    createScheduledTasksTable(ydbInfo.getClient(), ydbInfo.getDb());
                    createSchedulerInstancesTable(ydbInfo.getClient(), ydbInfo.getDb());
                    logger.info("Ydb inited, db name: {}", ydbInfo.getDb().getPath());
                    tablesCreated = true;
                }
            }
        }
        return ydbInfo;
    }

    private static void createScheduledTasksTable(TableClient tableClient, YdbPath db) {
        tableClient.createSession().thenAccept(
                sessionResult -> {
                    var session = sessionResult.expect("Cannot create session");
                    var tableDescription = SCHEDULED_TASKS.getDescription();
                    YdbPath ydbPath = YdbPath.of(db.getPath(), SCHEDULED_TASKS.getRealName());
                    session.createTable(ydbPath.getPath(), tableDescription).join().expect("Cannot " +
                            "create " +
                            "table " +
                            "scheduled_task");
                    session.close().join();
                }).join();
    }

    private static void createSchedulerInstancesTable(TableClient tableClient, YdbPath db) {
        tableClient.createSession().thenAccept(
                sessionResult -> {
                    var session = sessionResult.expect("Cannot create session");
                    var tableDescription = SCHEDULER_INSTANCES.getDescription();
                    YdbPath ydbPath = YdbPath.of(db.getPath(), SCHEDULER_INSTANCES.getRealName());
                    session.createTable(ydbPath.getPath(), tableDescription).join().expect("Cannot " +
                            "create table " +
                            "scheduler_instances");
                    session.close().join();
                }).join();
    }
}
