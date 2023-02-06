package ru.yandex.market.crm.campaign.test.utils;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Component;

import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.market.crm.core.domain.mobile.MetricaMobileApp;
import ru.yandex.market.crm.core.domain.subscriptions.SubscriptionType;
import ru.yandex.market.crm.core.test.utils.YtTestTables;
import ru.yandex.market.crm.core.util.EmailUtil;
import ru.yandex.market.crm.yt.client.YtClient;

import static ru.yandex.market.crm.core.test.utils.SubscriptionTypes.STORE_PUSH_GENERAL_ADVERTISING;
import static ru.yandex.market.crm.core.util.MobileAppInfoUtil.APP_INFO_PLATFORM_ANDROID;

/**
 * @author apershukov
 */
@Component
public class ChytDataTablesHelper {

    public static YTreeMapNode chytPassportEmail(long puid, String email) {
        return YTree.mapBuilder()
                .key("puid").value(String.valueOf(puid))
                .key("email").value(EmailUtil.normalizeEmail(email))
                .key("not_found").value(false)
                .buildMap();
    }

    public static YTreeMapNode chytPassportUuid(long puid, String uuid) {
        return YTree.mapBuilder()
                .key("puid").value(String.valueOf(puid))
                .key("uuid").value(uuid)
                .buildMap();
    }

    public static YTreeMapNode chytUuidWithToken(String uuid, String deviceId, String deviceIdHash) {
        return chytUuidWithToken(uuid, deviceId, deviceIdHash, APP_INFO_PLATFORM_ANDROID, 0);
    }

    public static YTreeMapNode chytUuidWithToken(String uuid, String deviceId, String deviceIdHash, String platform) {
        return chytUuidWithToken(uuid, deviceId, deviceIdHash, platform, 0);
    }

    public static YTreeMapNode chytUuidWIthToken(String uuid, String deviceId, String deviceIdHash, int tzOffset) {
        return chytUuidWithToken(uuid, deviceId, deviceIdHash, APP_INFO_PLATFORM_ANDROID, tzOffset);
    }

    private static YTreeMapNode chytUuidWithToken(String uuid,
                                                  String deviceId,
                                                  String deviceIdHash,
                                                  String platform,
                                                  int tzOffset) {
        return YTree.mapBuilder()
                .key("uuid").value(uuid)
                .key("device_id").value(deviceId)
                .key("device_id_hash").value(deviceIdHash)
                .key("platform").value(platform)
                .key("app_id").value(MetricaMobileApp.BERU.getId())
                .key("tz_offset").value(tzOffset)
                .buildMap();
    }

    public static YTreeMapNode chytUuidWithSubscription(String uuid) {
        return chytUuidWithSubscription(uuid, STORE_PUSH_GENERAL_ADVERTISING, true);
    }

    public static YTreeMapNode chytUuidWithSubscription(String uuid, SubscriptionType type, boolean subscribed) {
        return YTree.mapBuilder()
                .key("uuid").value(uuid)
                .key("type").value(type.getId())
                .key("status").value(subscribed ? 1 : 2)
                .buildMap();
    }

    private final YtClient ytClient;
    private final YtTestTables ytTestTables;

    public ChytDataTablesHelper(YtClient ytClient, YtTestTables ytTestTables) {
        this.ytClient = ytClient;
        this.ytTestTables = ytTestTables;
    }

    public void preparePassportEmails(YTreeMapNode... rows) {
        ytClient.write(ytTestTables.getChytPassportEmails(), List.of(rows));
    }

    public void preparePassportUuids(YTreeMapNode... rows) {
        ytClient.write(ytTestTables.getChytPassportUuids(), List.of(rows));
    }

    public void prepareUuidsWithTokens(YTreeMapNode... rows) {
        prepareUuidsWithTokens(ytTestTables.getChytUuidsWithTokens(), rows);
    }

    public void prepareUuidsWithTokens(YPath tablePath, YTreeMapNode... rows) {
        ytClient.write(tablePath, sortByUuid(rows));
    }

    public void prepareUuidsWithSubscriptions(YTreeMapNode... rows) {
        ytClient.write(ytTestTables.getChytUuidsWithSubscriptions(), sortByUuid(rows));
    }

    private List<YTreeMapNode> sortByUuid(YTreeMapNode... rows) {
        return Stream.of(rows)
                .sorted(Comparator.comparing(x -> x.getString("uuid")))
                .collect(Collectors.toList());
    }
}
