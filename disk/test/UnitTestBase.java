package ru.yandex.chemodan.app.dataapi.test;

import org.joda.time.Duration;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.MapF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.dataapi.api.data.field.DataField;
import ru.yandex.chemodan.app.dataapi.api.data.field.DataFields;
import ru.yandex.chemodan.app.dataapi.api.data.record.DataRecord;
import ru.yandex.chemodan.app.dataapi.api.db.Database;
import ru.yandex.chemodan.app.dataapi.api.db.ref.DatabaseRef;
import ru.yandex.chemodan.app.dataapi.api.user.DataApiUserId;
import ru.yandex.chemodan.app.dataapi.core.dao.support.DataApiRandomValueGenerator;
import ru.yandex.chemodan.app.dataapi.core.generic.DeletionSettings;
import ru.yandex.chemodan.app.dataapi.core.generic.TypeLocation;
import ru.yandex.chemodan.app.dataapi.core.generic.TypeSettings;
import ru.yandex.misc.dataSize.DataSize;
import ru.yandex.misc.io.ClassPathResourceInputStreamSource;

/**
 * @author Denis Bakharev
 */
public class UnitTestBase {

    protected final DataApiRandomValueGenerator randomValueGenerator;

    public UnitTestBase() {
        randomValueGenerator = new DataApiRandomValueGenerator();
    }

    protected DataApiUserId getUserId() {
        return randomValueGenerator.createDataApiUserId();
    }

    protected Database getDatabase(DatabaseRef ref, long size, int recordsCount) {
        return randomValueGenerator
                .createDatabase(ref)
                .withSize(DataSize.fromBytes(size))
                .withRecordsCount(recordsCount);
    }

    protected Database getDatabase(DatabaseRef ref, long size) {
        return getDatabase(ref, size, 0);
    }

    protected Database getDatabase(String appName, long size) {
        Database database = randomValueGenerator.createDatabaseByApp(appName);
        database = database.withSizeInc(size);
        return database;
    }

    protected String readResource(String resourceFileName) {
        ClassPathResourceInputStreamSource iss = new ClassPathResourceInputStreamSource(
                getClass(), resourceFileName);
        return iss.readText();
    }

    protected TypeSettings getTypeSettings() {
        return getTypeSettings("{}");
    }

    protected TypeSettings getTypeSettings(String jsonSchema) {
        DeletionSettings deletionSettings = new DeletionSettings("date", Duration.standardDays(1));
        return new TypeSettings(
                jsonSchema,
                "myType",
                "id",
                true,
                true,
                Cf.list(),
                new TypeLocation(Option.of("app"), "dbId", "colId"),
                Option.of(deletionSettings),
                false);
    }

    protected DataRecord getDataRecord(MapF<String, DataField> data) {
        return new DataRecord(getUserId(), TestDataGenerator.getRecordId(), 1L, new DataFields(data));
    }
}
