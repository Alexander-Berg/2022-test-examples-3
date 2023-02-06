package ru.yandex.market.crm.platform.reducers;

import java.util.Collections;

import com.google.common.collect.Iterables;
import org.junit.Test;

import ru.yandex.market.crm.platform.YieldMock;
import ru.yandex.market.crm.platform.common.Uids;
import ru.yandex.market.crm.platform.commons.UidType;
import ru.yandex.market.crm.platform.models.MetrikaMobileApp;

import static org.junit.Assert.assertEquals;

public class MetrikaMobileAppReducerTest {

    private MetrikaMobileAppReducer reducer = new MetrikaMobileAppReducer();
    private YieldMock collector = new YieldMock();

    @Test
    public void reduceOnlyOneNewFactTest() {
        MetrikaMobileApp newFact = MetrikaMobileApp.newBuilder()
                .setUid(Uids.create(UidType.UUID, "uuid"))
                .build();

        reducer.reduce(Collections.emptyList(), Collections.singleton(newFact), collector);

        MetrikaMobileApp reduced = Iterables.get(collector.getAdded("MetrikaMobileApp"), 0);
        assertEquals(newFact, reduced);
    }

    @Test
    public void reduceOverOldFactTest() {
        MetrikaMobileApp stored = MetrikaMobileApp.newBuilder()
                .setUid(Uids.create(UidType.UUID, "uuid_1"))
                .addUids(Uids.create(UidType.UUID, "uuid_1"))
                .setAppId(123)
                .setUpdateTime("2019-01-29 02:49:05")
                .setDeviceId("device_id_1")
                .build();

        MetrikaMobileApp newFact = MetrikaMobileApp.newBuilder()
                .setUid(Uids.create(UidType.UUID, "uuid_1"))
                .addUids(Uids.create(UidType.UUID, "uuid_1"))
                .setAppId(456)
                .setPushToken("push_token_1")
                .setUpdateTime("2019-01-29 02:49:06")
                .build();

        reducer.reduce(Collections.singletonList(stored), Collections.singleton(newFact), collector);

        MetrikaMobileApp reduced = Iterables.get(collector.getAdded("MetrikaMobileApp"), 0);

        MetrikaMobileApp expected = MetrikaMobileApp.newBuilder()
                .setUid(Uids.create(UidType.UUID, "uuid_1"))
                .addUids(Uids.create(UidType.UUID, "uuid_1"))
                .setPushToken("push_token_1")
                .setAppId(456)
                .setDeviceId("device_id_1")
                .setUpdateTime("2019-01-29 02:49:06")
                .build();

        assertEquals(expected, reduced);
    }

    @Test
    public void testWithRepeatingUids() {
        MetrikaMobileApp stored = MetrikaMobileApp.newBuilder()
                .setUid(Uids.create(UidType.UUID, "uuid_1"))
                .addUids(Uids.create(UidType.PUID, 1))
                .setAppId(123)
                .setUpdateTime("2019-01-29 02:49:05")
                .setDeviceId("device_id_1")
                .build();

        MetrikaMobileApp newFact = MetrikaMobileApp.newBuilder()
                .setUid(Uids.create(UidType.UUID, "uuid_1"))
                .addUids(Uids.create(UidType.PUID, 1))
                .addUids(Uids.create(UidType.PUID, 1))
                .setAppId(456)
                .setPushToken("push_token_1")
                .setUpdateTime("2019-01-29 02:49:06")
                .build();

        reducer.reduce(Collections.singletonList(stored), Collections.singleton(newFact), collector);

        MetrikaMobileApp reduced = Iterables.get(collector.getAdded("MetrikaMobileApp"), 0);
        MetrikaMobileApp expected = MetrikaMobileApp.newBuilder()
                .setUid(Uids.create(UidType.UUID, "uuid_1"))
                .addUids(Uids.create(UidType.PUID, 1))
                .setPushToken("push_token_1")
                .setAppId(456)
                .setDeviceId("device_id_1")
                .setUpdateTime("2019-01-29 02:49:06")
                .build();

        assertEquals(expected, reduced);
    }
}