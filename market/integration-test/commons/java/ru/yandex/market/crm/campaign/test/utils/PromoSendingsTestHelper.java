package ru.yandex.market.crm.campaign.test.utils;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.market.crm.mapreduce.domain.user.Uid;
import ru.yandex.market.crm.mapreduce.domain.user.UidType;
import ru.yandex.market.crm.yt.client.YtClient;

@Component
public class PromoSendingsTestHelper {
    public static class DeviceId {
        private final String uuid;
        private final String origUuid;
        private final String platform;

        public DeviceId(String uuid, String origUuid, String platform) {
            this.uuid = uuid;
            this.origUuid = origUuid;
            this.platform = platform;
        }
    }

    public static YTreeMapNode userIdsRow(Uid userUid) {
        return YTree.mapBuilder()
                .key("id_value").value(userUid.getValue())
                .key("id_type").value(userUid.getType().name())
                .key("original_id_value").value(userUid.getValue())
                .key("original_id_type").value(userUid.getType().name())
                .key("distribution_id").value(userUid.getValue())
                .buildMap();
    }

    public static YTreeMapNode deviceIdsRow(DeviceId deviceId) {
        return YTree.mapBuilder()
                .key("id_value").value(deviceId.uuid)
                .key("id_type").value(UidType.UUID.name())
                .key("original_id_value").value(deviceId.origUuid)
                .key("original_id_type").value(UidType.UUID.name())
                .key("distribution_id").value(deviceId.uuid)
                .key("platform").value(deviceId.platform)
                .key("device_id_hash").value(deviceId.uuid + "_hash")
                .key("device_id").value("")
                .key("tz_offset").value("")
                .buildMap();
    }

    private final YtClient ytClient;

    public PromoSendingsTestHelper(YtClient ytClient) {
        this.ytClient = ytClient;
    }

    public void prepareUserIdsTable(List<Uid> uids, YPath path) {
        ytClient.createTable(path, "promo_user_ids.yson");

        ytClient.write(
                path,
                uids.stream()
                        .map(PromoSendingsTestHelper::userIdsRow)
                        .collect(Collectors.toUnmodifiableList())
        );
    }

    public void prepareUserDeviceIdsTable(List<DeviceId> deviceIds, YPath path) {
        ytClient.createTable(path, "promo_user_device_ids.yson");

        ytClient.write(
                path,
                deviceIds.stream()
                        .map(PromoSendingsTestHelper::deviceIdsRow)
                        .collect(Collectors.toUnmodifiableList())
        );
    }
}
