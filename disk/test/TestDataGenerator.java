package ru.yandex.chemodan.app.dataapi.test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.MapF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.dataapi.api.data.field.DataField;
import ru.yandex.chemodan.app.dataapi.api.data.record.DataRecord;
import ru.yandex.chemodan.app.dataapi.api.data.record.DataRecordId;
import ru.yandex.chemodan.app.dataapi.api.db.ref.AppDatabaseRef;
import ru.yandex.chemodan.app.dataapi.api.user.DataApiUserId;
import ru.yandex.chemodan.app.dataapi.core.generic.loader.appdata.model.AppCollectionData;
import ru.yandex.chemodan.app.dataapi.core.generic.loader.appdata.model.AppData;
import ru.yandex.chemodan.app.dataapi.core.generic.loader.appdata.model.AppDatabaseData;
import ru.yandex.chemodan.app.dataapi.core.generic.loader.appdata.model.AppId;
import ru.yandex.misc.random.Random2;

/**
 * @author Denis Bakharev
 */
public class TestDataGenerator {
    public static DataRecord getDataRecord(MapF<String, DataField> data) {
        return new DataRecord(
                getUid(),
                getRecordId(),
                10101L,
                data);
    }

    public static DataApiUserId getUid() {
        return DataApiUserId.parse(String.valueOf(Random2.R.nextNonNegativeInt()));
    }

    public static AppData getAppData(AppId appId) {
        DataRecord record = getDataRecord(Cf.map("key", DataField.string("value")));
        AppCollectionData appCollectionData = new AppCollectionData("colId", Cf.list(record));
        AppDatabaseData appDatabaseData = new AppDatabaseData("dbId", "handle", Cf.list(appCollectionData));
        return new AppData(appId, Cf.list(appDatabaseData));
    }

    public static DataRecordId getRecordId() {
        return new DataRecordId(new AppDatabaseRef("app", "dbId").consHandle("h"), "colId", "recId");
    }

    public static AppId getAppId() {
        return new AppId(getUid(), Option.of("app"));
    }
}
