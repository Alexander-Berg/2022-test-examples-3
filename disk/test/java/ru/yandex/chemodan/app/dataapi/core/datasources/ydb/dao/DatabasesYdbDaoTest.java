package ru.yandex.chemodan.app.dataapi.core.datasources.ydb.dao;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.yandex.ydb.table.transaction.TransactionMode;
import org.joda.time.Instant;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.dao.IncorrectResultSizeDataAccessException;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.dataapi.api.context.DatabaseContext;
import ru.yandex.chemodan.app.dataapi.api.db.Database;
import ru.yandex.chemodan.app.dataapi.api.db.DatabaseExistsException;
import ru.yandex.chemodan.app.dataapi.api.db.DatabaseMeta;
import ru.yandex.chemodan.app.dataapi.api.db.handle.DatabaseHandle;
import ru.yandex.chemodan.app.dataapi.core.dao.data.DatabaseRevisionMismatchException;
import ru.yandex.chemodan.app.dataapi.core.datasources.ydb.DataapiYdbTestBase;
import ru.yandex.chemodan.ydb.dao.ThreadLocalYdbTransactionManager;
import ru.yandex.chemodan.ydb.dao.YdbTimeoutSettings;
import ru.yandex.chemodan.ydb.dao.pojo.YdbTestUtils;
import ru.yandex.misc.ExceptionUtils;
import ru.yandex.misc.dataSize.DataSize;
import ru.yandex.misc.random.Random2;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.thread.ThreadUtils;

/**
 * @author tolmalev
 */
@Ignore
public class DatabasesYdbDaoTest extends DataapiYdbTestBase {
    private DatabasesYdbDao dao;

    @Before
    public void init() throws ExecutionException, InterruptedException {
        super.init();

        dao = new DatabasesYdbDao(transactionManager);
    }

    @Test
    public void insertDatabaseWithDescription() {
        Database database = new Database(uid,
                new DatabaseHandle("app_1", "db_1", Random2.R.nextAlnum(10)),
                0,
                new DatabaseMeta(Instant.now(), Instant.now(), DataSize.ZERO, 0, Option.of("descr")));

        dao.insert(database);
        Assert.some(database, dao.find(uid, database.dbRef()));
    }

    @Test
    public void insertDatabaseWithoutDescription() {
        Database database = new Database(uid,
                new DatabaseHandle("app_1_1", "db_1_1", Random2.R.nextAlnum(10)),
                0,
                new DatabaseMeta(Instant.now(), Instant.now(), DataSize.ZERO, 0, Option.empty()));

        dao.insert(database);
        Assert.some(database, dao.find(uid, database.dbRef()));
    }

    @Test
    public void updateDatabase() {
        Database database = new Database(uid,
                new DatabaseHandle("app_1", "db_1", Random2.R.nextAlnum(10)),
                0,
                new DatabaseMeta(Instant.now(), Instant.now(), DataSize.ZERO, 0, Option.of("descr")));

        dao.insert(database);

        dao.save(database.withRev(10), database.rev);
    }

    @Test(expected = DatabaseRevisionMismatchException.class)
    public void updateNonExistingDatabase() {
        Database database = new Database(uid,
                new DatabaseHandle("app_1", "db_1", Random2.R.nextAlnum(10)),
                0,
                new DatabaseMeta(Instant.now(), Instant.now(), DataSize.ZERO, 0, Option.of("descr")));

        dao.save(database, database.rev);
    }

    @Test(expected = IncorrectResultSizeDataAccessException.class)
    public void deleteNonExictingDatabase() {
        dao.delete(uid, DatabaseContext.fromDbAppId(Option.of("app_1_1")), Cf.list("asaf"));
    }

    @Test
    public void deleteExictingDatabase() {
        Database database = new Database(uid,
                new DatabaseHandle("app_1", "db_1", Random2.R.nextAlnum(10)),
                0,
                new DatabaseMeta(Instant.now(), Instant.now(), DataSize.ZERO, 0, Option.of("descr")));

        dao.insert(database);
        Assert.some(database, dao.find(uid, database.dbRef()));

        dao.delete(uid, database.dbContext(), database.getDatabaseIds());
    }

    @Test(expected = DatabaseRevisionMismatchException.class)
    public void outdatedUpdateDatabase() {
        Database database = new Database(uid,
                new DatabaseHandle("app_1", "db_1", Random2.R.nextAlnum(10)),
                0,
                new DatabaseMeta(Instant.now(), Instant.now(), DataSize.ZERO, 0, Option.of("descr")));

        dao.insert(database);

        dao.save(database.withRev(10), database.rev + 5);
    }
/*

    @Test(expected = RetriableTransactionException.class)
    public void concurrent_read_commit_after_insert_fails() {
        ThreadLocalYdbTransactionManager transactionManager2 = new ThreadLocalYdbTransactionManager(tableClient);
        DatabasesYdbDao dao2 = new DatabasesYdbDao(transactionManager2);

        transactionManager.startTransaction(TransactionMode.SERIALIZABLE_READ_WRITE);
        transactionManager2.startTransaction(TransactionMode.SERIALIZABLE_READ_WRITE);

        Database database = new Database(uid,
                new DatabaseHandle("app_1_1", "db_1_1", Random2.R.nextAlnum(10)),
                0,
                new DatabaseMeta(Instant.now(), Instant.now(), DataSize.ZERO, 0, Option.empty()));

        dao.insert(database);
        dao2.find(uid, database.dbRef());

        transactionManager.commit();
        transactionManager2.commit();
    }

    @Test
    public void concurrent_insert_commit_after_read_ok() {
        ThreadLocalYdbTransactionManager transactionManager2 = new ThreadLocalYdbTransactionManager(tableClient);
        DatabasesYdbDao dao2 = new DatabasesYdbDao(transactionManager2);

        transactionManager.startTransaction(TransactionMode.SERIALIZABLE_READ_WRITE);
        transactionManager2.startTransaction(TransactionMode.SERIALIZABLE_READ_WRITE);

        Database database = new Database(uid,
                new DatabaseHandle("app_1_1", "db_1_1", Random2.R.nextAlnum(10)),
                0,
                new DatabaseMeta(Instant.now(), Instant.now(), DataSize.ZERO, 0, Option.empty()));

        dao2.find(uid, database.dbRef());
        dao.insert(database);

        transactionManager2.commit();
        transactionManager.commit();
    }

    @Test(expected = RetriableTransactionException.class)
    public void concurrent_insert_and_insert() {
        ThreadLocalYdbTransactionManager transactionManager2 = new ThreadLocalYdbTransactionManager(tableClient);
        DatabasesYdbDao dao2 = new DatabasesYdbDao(transactionManager2);

        transactionManager.startTransaction(TransactionMode.SERIALIZABLE_READ_WRITE);
        transactionManager2.startTransaction(TransactionMode.SERIALIZABLE_READ_WRITE);

        Database database = new Database(uid,
                new DatabaseHandle("app_1_1", "db_1_1", Random2.R.nextAlnum(10)),
                0,
                new DatabaseMeta(Instant.now(), Instant.now(), DataSize.ZERO, 0, Option.empty()));

        dao.insert(database);
        dao2.insert(database);

        transactionManager2.commit();
        transactionManager.commit();
    }
*/

    @Test
    public void findByHandle() {
        Database database = new Database(uid,
                new DatabaseHandle("app_1_1", "db_1_1", Random2.R.nextAlnum(10)),
                0,
                new DatabaseMeta(Instant.now(), Instant.now(), DataSize.ZERO, 0, Option.empty()));

        dao.insert(database);

        Assert.some(database, dao.find(uid, database.dbRef()));
        Assert.some(database, dao.findByHandle(uid, database.dbHandle));
    }

    @Test
    public void findAll() {
        Assert.assertEmpty(dao.find(uid));

        Database database = new Database(uid,
                new DatabaseHandle("app_1_1", "db_1_1", Random2.R.nextAlnum(10)),
                0,
                new DatabaseMeta(Instant.now(), Instant.now(), DataSize.ZERO, 0, Option.empty()));

        dao.insert(database);

        Assert.sizeIs(1, dao.find(uid));

        database = new Database(uid,
                new DatabaseHandle("app_1_2", "db_1_1", Random2.R.nextAlnum(10)),
                0,
                new DatabaseMeta(Instant.now(), Instant.now(), DataSize.ZERO, 0, Option.empty()));

        dao.insert(database);

        Assert.sizeIs(2, dao.find(uid));

        database = new Database(uid,
                new DatabaseHandle("app_1_1", "db_1_2", Random2.R.nextAlnum(10)),
                0,
                new DatabaseMeta(Instant.now(), Instant.now(), DataSize.ZERO, 0, Option.empty()));

        dao.insert(database);

        Assert.sizeIs(3, dao.find(uid));
    }

    @Test
    public void findAllForApp() {
        DatabaseContext dbContext = DatabaseContext.fromDbAppId(Option.of("app_1_1"));

        Assert.assertEmpty(dao.find(uid, dbContext));

        Database database = new Database(uid,
                new DatabaseHandle("app_1_1", "db_1_1", Random2.R.nextAlnum(10)),
                0,
                new DatabaseMeta(Instant.now(), Instant.now(), DataSize.ZERO, 0, Option.empty()));

        dao.insert(database);

        Assert.sizeIs(1, dao.find(uid, dbContext));

        database = new Database(uid,
                new DatabaseHandle("app_1_2", "db_1_1", Random2.R.nextAlnum(10)),
                0,
                new DatabaseMeta(Instant.now(), Instant.now(), DataSize.ZERO, 0, Option.empty()));

        dao.insert(database);

        Assert.sizeIs(1, dao.find(uid, dbContext));

        database = new Database(uid,
                new DatabaseHandle("app_1_1", "db_1_2", Random2.R.nextAlnum(10)),
                0,
                new DatabaseMeta(Instant.now(), Instant.now(), DataSize.ZERO, 0, Option.empty()));

        dao.insert(database);

        Assert.sizeIs(2, dao.find(uid, dbContext));
    }

    @Test
    public void findByDatabaseIds() {
        DatabaseContext dbContext = DatabaseContext.fromDbAppId(Option.of("app_1_1"));

        Database database1 = new Database(uid,
                new DatabaseHandle("app_1_1", "db_1_1", Random2.R.nextAlnum(10)),
                0,
                new DatabaseMeta(Instant.now(), Instant.now(), DataSize.ZERO, 0, Option.empty()));

        dao.insert(database1);

        Database database2 = new Database(uid,
                new DatabaseHandle("app_1_1", "db_1_2", Random2.R.nextAlnum(10)),
                0,
                new DatabaseMeta(Instant.now(), Instant.now(), DataSize.ZERO, 0, Option.empty()));

        dao.insert(database2);

        Database database3 = new Database(uid,
                new DatabaseHandle("app_1_1", "db_1_3", Random2.R.nextAlnum(10)),
                0,
                new DatabaseMeta(Instant.now(), Instant.now(), DataSize.ZERO, 0, Option.empty()));

        dao.insert(database3);

        ListF<Database> databases = dao.find(uid, dbContext, Cf.list("db_1_1", "db_1_2"));
        Assert.sizeIs(2, databases);
        Assert.some(database1, databases.find(db -> db.databaseId().equals(database1.databaseId())));
        Assert.some(database2, databases.find(db -> db.databaseId().equals(database2.databaseId())));
    }

    @Test
    public void concurrentCreateAndDeleteInLongTransactions() throws InterruptedException {
        YdbTimeoutSettings timeoutSettings = YdbTestUtils.getTestTimeoutSettings();
        ThreadLocalYdbTransactionManager transactionManager2 = new ThreadLocalYdbTransactionManager(tableClient, timeoutSettings);
        DatabasesYdbDao dao2 = new DatabasesYdbDao(transactionManager2);

        Database database = new Database(uid,
                new DatabaseHandle("app_1_1", "db_1_1", Random2.R.nextAlnum(10)),
                0,
                new DatabaseMeta(Instant.now(), Instant.now(), DataSize.ZERO, 0, Option.empty()));

        ListF<RuntimeException> e1 = Cf.arrayList();
        ListF<RuntimeException> e2 = Cf.arrayList();

        Callable<Void> r1 = () -> {
            try {
                transactionManager.executeInTx(() -> {
                    dao.insert(database);
                    ThreadUtils.sleep(Random2.R.nextInt(100));
                    return null;
                }, TransactionMode.SERIALIZABLE_READ_WRITE);
            } catch (DatabaseExistsException ignored) {
            } catch (RuntimeException e) {
                e1.add(e);
            }
            return null;
        };

        Callable<Void> r2 = () -> {
            try {
                transactionManager2.executeInTx(() -> {
                    dao2.delete(uid, database.dbContext(), database.getDatabaseIds());
                    ThreadUtils.sleep(Random2.R.nextInt(100));
                    return null;
                }, TransactionMode.SERIALIZABLE_READ_WRITE);
            } catch (IncorrectResultSizeDataAccessException ignored) {
            } catch (RuntimeException e) {
                e2.add(e);
            }
            return null;
        };

        ExecutorService executorService = Executors.newFixedThreadPool(5);
        executorService.invokeAll(Cf.range(0, 100).flatMap(i -> Cf.list(r1, r2)));
        executorService.shutdown();
        executorService.awaitTermination(100, TimeUnit.SECONDS);

        checkAllSessionsIdle(transactionManager.getTableClientForTests());
        checkAllSessionsIdle(transactionManager2.getTableClientForTests());

        e1.forEach(ExceptionUtils::throwException);
        e2.forEach(ExceptionUtils::throwException);
    }

    @Test
    public void concurrentCreateAndDeleteWithShortTransactions() throws InterruptedException {
        YdbTimeoutSettings timeoutSettings = YdbTestUtils.getTestTimeoutSettings();
        ThreadLocalYdbTransactionManager transactionManager2 = new ThreadLocalYdbTransactionManager(tableClient, timeoutSettings);
        DatabasesYdbDao dao2 = new DatabasesYdbDao(transactionManager2);

        Database database = new Database(uid,
                new DatabaseHandle("app_1_1", "db_1_1", Random2.R.nextAlnum(10)),
                0,
                new DatabaseMeta(Instant.now(), Instant.now(), DataSize.ZERO, 0, Option.empty()));

        ListF<RuntimeException> e1 = Cf.arrayList();
        ListF<RuntimeException> e2 = Cf.arrayList();

        Callable<Void> r1 = () -> {
            try {
                dao.insert(database);
            } catch (DatabaseExistsException ignored) {
            } catch (RuntimeException e) {
                e1.add(e);
            }
            return null;
        };

        Callable<Void> r2 = () -> {
            try {
                dao2.delete(uid, database.dbContext(), database.getDatabaseIds());
            } catch (IncorrectResultSizeDataAccessException ignored) {
            } catch (RuntimeException e) {
                e2.add(e);
            }
            return null;
        };

        ExecutorService executorService = Executors.newFixedThreadPool(3);
        executorService.invokeAll(Cf.range(0, 1000).flatMap(i -> Cf.list(r1, r2)));
        executorService.shutdown();
        executorService.awaitTermination(100, TimeUnit.SECONDS);

        checkAllSessionsIdle(transactionManager.getTableClientForTests());
        checkAllSessionsIdle(transactionManager2.getTableClientForTests());

        e1.forEach(ExceptionUtils::throwException);
        e2.forEach(ExceptionUtils::throwException);
    }
}
