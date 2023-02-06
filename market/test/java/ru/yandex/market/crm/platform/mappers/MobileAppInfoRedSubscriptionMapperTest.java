package ru.yandex.market.crm.platform.mappers;

import java.util.List;

import org.junit.Test;

import ru.yandex.market.crm.platform.common.Uids;
import ru.yandex.market.crm.platform.commons.Uid;
import ru.yandex.market.crm.platform.commons.UidType;
import ru.yandex.market.crm.platform.models.MobileAppInfo;

import static org.junit.Assert.assertEquals;
import static ru.yandex.market.crm.platform.models.MobileAppInfo.EventType.*;

public class MobileAppInfoRedSubscriptionMapperTest {

    private final MobileAppInfoRedSubscriptionMapper mapper = new MobileAppInfoRedSubscriptionMapper();

    @Test
    public void testMap() {
        String line = "tskv\temail=astakhova@clarissbaby.com\tuuid=96cf2adb3b79e9adc78e4659ee725187\t" +
                "puid=1130000035006687\tsubscription_type=42\t" +
                "subscription_status=1\tcreation_date=2019-01-11 14:10:43.0\tconfirmed_date=2019-01-11 14:10:44.0\t" +
                "modification_date=2019-01-11 14:10:43" +
                ".0\tactive_email=false\tsubscription_parameter_uid_19=1130000035006687\t" +
                "subscription_parameter_yandexUid_20=4120750961294154853";

        List<MobileAppInfo> parsed = mapper.apply(line.getBytes());

        assertEquals(1, parsed.size());

        Uid uuid = Uids.create(UidType.UUID, "96cf2adb3b79e9adc78e4659ee725187");

        MobileAppInfo expected = MobileAppInfo.newBuilder()
                .setKeyUid(uuid)
                .setUuid(uuid.getStringValue())
                .addUid(uuid)
                .addUid(Uids.create(UidType.PUID, 1130000035006687L))
                .setModificationTime("2019-01-11 14:10:43.0")
                .setTriggersRegistered(true)
                .setAppName("ru.yandex.red.market")
                .setEventType(SUBS_TRIGGER)
                .build();

        assertEquals(expected, parsed.get(0));
    }

    @Test
    public void testIgnoreNumericIdsWithZeroValue() {
        String line = "tskv\t" +
                "email=astakhova@clarissbaby.com\t" +
                "uuid=96cf2adb3b79e9adc78e4659ee725187\t" +
                "puid=0\t" +
                "subscription_type=43\t" +
                "subscription_status=1\t" +
                "creation_date=2019-01-11 14:10:43.0\t" +
                "confirmed_date=2019-01-11 14:10:44.0\t" +
                "modification_date=2019-01-11 14:10:43.0\t" +
                "active_email=false\t" +
                "subscription_parameter_uid_19=1130000035006687\t" +
                "subscription_parameter_yandexUid_20=4120750961294154853";

        List<MobileAppInfo> parsed = mapper.apply(line.getBytes());

        assertEquals(1, parsed.size());

        Uid uuid = Uids.create(UidType.UUID, "96cf2adb3b79e9adc78e4659ee725187");

        MobileAppInfo expected = MobileAppInfo.newBuilder()
                .setKeyUid(uuid)
                .setUuid(uuid.getStringValue())
                .addUid(uuid)
                .setModificationTime("2019-01-11 14:10:43.0")
                .setRegistered(true)
                .setAppName("ru.yandex.red.market")
                .setEventType(SUBS_ADVERTIZING)
                .build();

        assertEquals(expected, parsed.get(0));
    }
}
