package ru.yandex.chemodan.app.dataapi.core.datasources.ydb.dao;

import java.util.concurrent.ExecutionException;

import org.joda.time.Instant;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.SetF;
import ru.yandex.chemodan.app.dataapi.api.data.field.DataField;
import ru.yandex.chemodan.app.dataapi.api.data.filter.condition.CollectionIdCondition;
import ru.yandex.chemodan.app.dataapi.api.data.filter.condition.RecordCondition;
import ru.yandex.chemodan.app.dataapi.api.data.filter.condition.RecordIdCondition;
import ru.yandex.chemodan.app.dataapi.api.data.filter.ordering.RecordOrder;
import ru.yandex.chemodan.app.dataapi.api.data.record.DataRecord;
import ru.yandex.chemodan.app.dataapi.api.data.record.DataRecordId;
import ru.yandex.chemodan.app.dataapi.api.db.Database;
import ru.yandex.chemodan.app.dataapi.api.db.DatabaseMeta;
import ru.yandex.chemodan.app.dataapi.api.db.handle.DatabaseHandle;
import ru.yandex.chemodan.app.dataapi.core.datasources.ydb.DataapiYdbTestBase;
import ru.yandex.misc.dataSize.DataSize;
import ru.yandex.misc.db.q.SqlLimits;
import ru.yandex.misc.random.Random2;
import ru.yandex.misc.test.Assert;

/**
 * @author tolmalev
 */
public class DataRecordsYdbDaoTest extends DataapiYdbTestBase {
    private DataRecordsYdbDao dao;

    @Before
    public void init() throws ExecutionException, InterruptedException {
        super.init();

        dao = new DataRecordsYdbDao(transactionManager);
    }

    @Test
    public void insertAndDelete() {
        Database database = new Database(uid,
                new DatabaseHandle("app_1", "db_1", Random2.R.nextAlnum(10)),
                0,
                new DatabaseMeta(Instant.now(), Instant.now(), DataSize.ZERO, 0, Option.of("descr")));

        ListF<DataRecord> newRecords = Cf.list(
                new DataRecord(uid, new DataRecordId(database.dbHandle, "c1", "r1"), 0, Cf.map()),
                new DataRecord(uid, new DataRecordId(database.dbHandle, "c1", "r2"), 0, Cf.map()),
                new DataRecord(uid, new DataRecordId(database.dbHandle, "c1", "r3"), 0, Cf.map()),
                new DataRecord(uid, new DataRecordId(database.dbHandle, "c1", "r4"), 0, Cf.map()),
                new DataRecord(uid, new DataRecordId(database.dbHandle, "c1", "r5"), 0, Cf.map()),
                new DataRecord(uid, new DataRecordId(database.dbHandle, "c1", "r6"), 0, Cf.map())
        );

        dao.bulkInsertDeleteUpdate(uid, database.dbRef(), newRecords, Cf.set(), Cf.list());

        SetF<DataRecordId> deleteIds = Cf.set(
                newRecords.get(0).id,
                newRecords.get(1).id,
                newRecords.get(2).id
        );

        newRecords = Cf.list(
                new DataRecord(uid, new DataRecordId(database.dbHandle, "c1", "r7"), 0, Cf.map()),
                new DataRecord(uid, new DataRecordId(database.dbHandle, "c1", "r8"), 0, Cf.map())
        );

        dao.bulkInsertDeleteUpdate(uid, database.dbRef(), newRecords, deleteIds, Cf.list());

        ListF<DataRecord> allInDb = dao.find(uid, database.dbHandle, CollectionIdCondition.all(), RecordIdCondition.all(),
                RecordCondition.all(), RecordOrder.defaultOrder(), SqlLimits.all());

        Assert.sizeIs(6 - 3 + 2, allInDb);
    }

    @Test
    public void insertAndUpdate() {
        Database database = new Database(uid,
                new DatabaseHandle("app_1", "db_1", Random2.R.nextAlnum(10)),
                0,
                new DatabaseMeta(Instant.now(), Instant.now(), DataSize.ZERO, 0, Option.of("descr")));

        ListF<DataRecord> newRecords = Cf.list(
                new DataRecord(uid, new DataRecordId(database.dbHandle, "c1", "r1"), 0, Cf.map()),
                new DataRecord(uid, new DataRecordId(database.dbHandle, "c1", "r2"), 0, Cf.map()),
                new DataRecord(uid, new DataRecordId(database.dbHandle, "c1", "r3"), 0, Cf.map()),
                new DataRecord(uid, new DataRecordId(database.dbHandle, "c1", "r4"), 0, Cf.map()),
                new DataRecord(uid, new DataRecordId(database.dbHandle, "c1", "r5"), 0, Cf.map()),
                new DataRecord(uid, new DataRecordId(database.dbHandle, "c1", "r6"), 0, Cf.map())
        );

        dao.bulkInsertDeleteUpdate(uid, database.dbRef(), newRecords, Cf.set(), Cf.list());

        newRecords = Cf.list(
                new DataRecord(uid, new DataRecordId(database.dbHandle, "c1", "r7"), 0, Cf.map()),
                new DataRecord(uid, new DataRecordId(database.dbHandle, "c1", "r8"), 0, Cf.map())
        );

        ListF<DataRecord> updateRecords = Cf.list(
                new DataRecord(uid, new DataRecordId(database.dbHandle, "c1", "r3"), 1, Cf.map("a", DataField.string("1"))),
                new DataRecord(uid, new DataRecordId(database.dbHandle, "c1", "r4"), 1, Cf.map("b", DataField.string("2")))
        );

        dao.bulkInsertDeleteUpdate(uid, database.dbRef(), newRecords, Cf.set(), updateRecords);

        ListF<DataRecord> allInDb = dao.find(uid, database.dbHandle, CollectionIdCondition.all(), RecordIdCondition.all(),
                RecordCondition.all(), RecordOrder.defaultOrder(), SqlLimits.all());

        Assert.sizeIs(6 + 2, allInDb);
    }

    @Test
    public void insertAndDeleteAndUpdate() {
        Database database = new Database(uid,
                new DatabaseHandle("app_1", "db_1", Random2.R.nextAlnum(10)),
                0,
                new DatabaseMeta(Instant.now(), Instant.now(), DataSize.ZERO, 0, Option.of("descr")));

        ListF<DataRecord> newRecords = Cf.list(
                new DataRecord(uid, new DataRecordId(database.dbHandle, "c1", "r1"), 0, Cf.map()),
                new DataRecord(uid, new DataRecordId(database.dbHandle, "c1", "r2"), 0, Cf.map()),
                new DataRecord(uid, new DataRecordId(database.dbHandle, "c1", "r3"), 0, Cf.map()),
                new DataRecord(uid, new DataRecordId(database.dbHandle, "c1", "r4"), 0, Cf.map()),
                new DataRecord(uid, new DataRecordId(database.dbHandle, "c1", "r5"), 0, Cf.map()),
                new DataRecord(uid, new DataRecordId(database.dbHandle, "c1", "r6"), 0, Cf.map())
        );

        dao.bulkInsertDeleteUpdate(uid, database.dbRef(), newRecords, Cf.set(), Cf.list());

        SetF<DataRecordId> deleteIds = Cf.set(
                newRecords.get(0).id,
                newRecords.get(1).id,
                newRecords.get(2).id
        );

        newRecords = Cf.list(
                new DataRecord(uid, new DataRecordId(database.dbHandle, "c1", "r7"), 0, Cf.map()),
                new DataRecord(uid, new DataRecordId(database.dbHandle, "c1", "r8"), 0, Cf.map())
        );

        ListF<DataRecord> updateRecords = Cf.list(
                new DataRecord(uid, new DataRecordId(database.dbHandle, "c1", "r4"), 1, Cf.map("a", DataField.string("1"))),
                new DataRecord(uid, new DataRecordId(database.dbHandle, "c1", "r5"), 1, Cf.map("b", DataField.string("2")))
        );

        dao.bulkInsertDeleteUpdate(uid, database.dbRef(), newRecords, deleteIds, updateRecords);

        ListF<DataRecord> allInDb = dao.find(uid, database.dbHandle, CollectionIdCondition.all(), RecordIdCondition.all(),
                RecordCondition.all(), RecordOrder.defaultOrder(), SqlLimits.all());

        Assert.sizeIs(6 - 3 + 2, allInDb);
    }

    @Test(expected = IllegalArgumentException.class)
    public void badIds1() {
        DatabaseHandle handle = new DatabaseHandle("app_1", "db_1", Random2.R.nextAlnum(10));

        ListF<DataRecord> records = Cf.list(
                new DataRecord(uid, new DataRecordId(handle, "c1", "r1"), 0, Cf.map()),
                new DataRecord(uid, new DataRecordId(handle, "c1", "r1"), 0, Cf.map()));

        dao.bulkInsertDeleteUpdate(uid, handle.dbRef, records, Cf.set(), Cf.list());
    }

    @Test(expected = IllegalArgumentException.class)
    public void badIds2() {
        DatabaseHandle handle = new DatabaseHandle("app_1", "db_1", Random2.R.nextAlnum(10));

        ListF<DataRecord> records = Cf.list(
                new DataRecord(uid, new DataRecordId(handle, "c1", "r1"), 0, Cf.map()),
                new DataRecord(uid, new DataRecordId(handle, "c1", "r2"), 0, Cf.map()));

        SetF<DataRecordId> deleteIds = Cf.set(new DataRecordId(handle, "c1", "r1"));

        dao.bulkInsertDeleteUpdate(uid, handle.dbRef, records, deleteIds, Cf.list());
    }

    @Test(expected = IllegalArgumentException.class)
    public void badIds3() {
        DatabaseHandle handle = new DatabaseHandle("app_1", "db_1", Random2.R.nextAlnum(10));

        ListF<DataRecord> records = Cf.list(
                new DataRecord(uid, new DataRecordId(handle, "c1", "r1"), 0, Cf.map()),
                new DataRecord(uid, new DataRecordId(handle, "c1", "r2"), 0, Cf.map()));

        SetF<DataRecordId> deleteIds = Cf.set(new DataRecordId(handle, "c1", "r0"));

        ListF<DataRecord> updateRecords = Cf.list(
                new DataRecord(uid, new DataRecordId(handle, "c1", "r1"), 0, Cf.map()));

        dao.bulkInsertDeleteUpdate(uid, handle.dbRef, records, deleteIds, updateRecords);
    }
}
