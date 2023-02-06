package ru.yandex.market.crm.platform.mappers;

import java.util.List;

import org.junit.Test;

import ru.yandex.market.crm.platform.common.Uids;
import ru.yandex.market.crm.platform.commons.UidType;
import ru.yandex.market.crm.platform.models.MobileAppInfo;

import static org.junit.Assert.assertEquals;

public class MobileAppInfoMapperTest {

    private final MobileAppInfoMapper mapper = new MobileAppInfoMapper();

    @Test
    public void testMap() {
        String line = "tskv\tUUID=0f20afe0568e320df8e6743dec1a7849\tPUSH_TOKEN=dQ1xYLMgVMw" +
                ":APA91bHExglCrkpx5U3W0zupe3ECa" +
                "8aKjHn7_hSeDc0cEIQPFlw7dSUqhXkFZqzMhMyGQP0s5tz_CDPjtb9_BFyylZb6s9UB-UJkajKwc94bCOs63e45t5_HIEo7f" +
                "-3i6wuZYRVCX8Z1\t" +
                "UNREGISTERED=false\tMODIFICATION_TIME=2019-02-01 14:13:05" +
                ".0\tPUID=816389024\tGEO_ID=21\tPLATFORM=ANDROID\t" +
                "APP_NAME=ru.yandex.market.fulfillment\tMUID=1152921504661349643\tDISABLED_BY_SYSTEM=true";

        List<MobileAppInfo> parsed = mapper.apply(line.getBytes());

        assertEquals(1, parsed.size());

        MobileAppInfo expected = MobileAppInfo.newBuilder()
                .setKeyUid(Uids.create(UidType.UUID, "0f20afe0568e320df8e6743dec1a7849"))
                .setUuid("0f20afe0568e320df8e6743dec1a7849")
                .addUid(Uids.create(UidType.UUID, "0f20afe0568e320df8e6743dec1a7849"))
                .addUid(Uids.create(UidType.MUID, 1152921504661349643L))
                .addUid(Uids.create(UidType.PUID, 816389024))
                .setPushToken("dQ1xYLMgVMw" +
                        ":APA91bHExglCrkpx5U3W0zupe3ECa8aKjHn7_hSeDc0cEIQPFlw7dSUqhXkFZqzMhMyGQP0s5tz_CDPjt" +
                        "b9_BFyylZb6s9UB-UJkajKwc94bCOs63e45t5_HIEo7f-3i6wuZYRVCX8Z1")
                .setRegistered(true)
                .setTriggersRegistered(true)
                .setModificationTime("2019-02-01 14:13:05.0")
                .setPlatform("ANDROID")
                .setAppName("ru.yandex.market.fulfillment")
                .setGeoId(21)
                .setEventType(MobileAppInfo.EventType.APPINFO_CHANGES)
                .setDisabledBySystem(true)
                .build();

        assertEquals(expected, parsed.get(0));
    }

    @Test
    public void testIgnoreNumberIdsWithZeroValue() {
        String line = "tskv\t" +
                "UUID=0f20afe0568e320df8e6743dec1a7849\t" +
                "PUSH_TOKEN=dQ1xY\t" +
                "UNREGISTERED=false\t" +
                "MODIFICATION_TIME=2019-02-01 14:13:05.0\t" +
                "PUID=0\t" +
                "GEO_ID=21\t" +
                "PLATFORM=ANDROID\t" +
                "APP_NAME=ru.yandex.market.fulfillment\t" +
                "MUID=0\t" +
                "DISABLED_BY_SYSTEM=false";

        List<MobileAppInfo> parsed = mapper.apply(line.getBytes());

        assertEquals(1, parsed.size());

        MobileAppInfo expected = MobileAppInfo.newBuilder()
                .setKeyUid(Uids.create(UidType.UUID, "0f20afe0568e320df8e6743dec1a7849"))
                .setUuid("0f20afe0568e320df8e6743dec1a7849")
                .addUid(Uids.create(UidType.UUID, "0f20afe0568e320df8e6743dec1a7849"))
                .setPushToken("dQ1xY")
                .setRegistered(true)
                .setTriggersRegistered(true)
                .setModificationTime("2019-02-01 14:13:05.0")
                .setPlatform("ANDROID")
                .setAppName("ru.yandex.market.fulfillment")
                .setGeoId(21)
                .setEventType(MobileAppInfo.EventType.APPINFO_CHANGES)
                .setDisabledBySystem(false)
                .build();

        assertEquals(expected, parsed.get(0));
    }
}
