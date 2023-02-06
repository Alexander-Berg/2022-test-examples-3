package ru.yandex.chemodan.app.dataapi.apps.profile.test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.chemodan.app.dataapi.api.data.record.DataRecordId;
import ru.yandex.chemodan.app.dataapi.api.data.snapshot.PatchableSnapshot;
import ru.yandex.chemodan.app.dataapi.api.data.snapshot.Snapshot;
import ru.yandex.chemodan.app.dataapi.api.db.Database;
import ru.yandex.chemodan.app.dataapi.api.deltas.RecordChange;
import ru.yandex.chemodan.app.dataapi.apps.profile.ProfileUtils;
import ru.yandex.chemodan.app.dataapi.apps.profile.address.Address;
import ru.yandex.chemodan.app.dataapi.test.UnitTestBase;

/**
 * @author Denis Bakharev
 */
public class ProfileUnitTestBase extends UnitTestBase {
    protected Database getAddressDatabase() {
        return randomValueGenerator.createDatabase(ProfileUtils.ADDRESSES_DB_REF);
    }

    protected PatchableSnapshot createEventWithAddress(Address address) {
        Database database = getAddressDatabase();
        DataRecordId recordId = ProfileUtils.ADDRESSES_COL_REF.consRecordRef(address.getAddressId().get())
                .toRecordId(ProfileUtils.ADDRESSES_COL_REF.dbRef().consHandle("handle"));
        return new Snapshot(database, Cf.list())
                .toPatchable()
                .patch(
                        RecordChange.insert(recordId, address.toDataMap())
                                .toDelta()
                );
    }
}
