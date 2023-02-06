package ru.yandex.market.crm.core.test.utils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.base.Strings;
import org.springframework.stereotype.Component;

import ru.yandex.inside.yt.kosher.impl.ytree.YTreeStringNodeImpl;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.market.crm.core.domain.Color;
import ru.yandex.market.crm.core.domain.mobile.MetricaMobileApp;
import ru.yandex.market.crm.core.domain.subscriptions.SubscriptionType;
import ru.yandex.market.crm.core.util.MobileAppInfoUtil;
import ru.yandex.market.crm.yt.client.YtClient;
import ru.yandex.market.crm.platform.common.Uids;
import ru.yandex.market.crm.platform.commons.Uid;
import ru.yandex.market.crm.platform.commons.UidType;
import ru.yandex.market.crm.platform.models.GenericSubscription;
import ru.yandex.market.crm.platform.models.MetrikaMobileApp;
import ru.yandex.market.crm.platform.models.MobileAppInfo;
import ru.yandex.market.crm.platform.models.PushTokenStatuses;
import ru.yandex.market.crm.platform.models.PushTokenStatuses.TokenStatus;

import static java.util.Comparator.comparing;
import static ru.yandex.market.crm.core.test.utils.SubscriptionTypes.STORE_PUSH_GENERAL_ADVERTISING;

/**
 * @author apershukov
 */
@Component
public class MobileTablesHelper {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final YtClient ytClient;
    private final YtTestTables ytTestTables;

    public MobileTablesHelper(YtClient ytClient, YtTestTables ytTestTables) {
        this.ytClient = ytClient;
        this.ytTestTables = ytTestTables;
    }

    private static long getAppIdFor(Color color) {
        return MetricaMobileApp.getMobileAppsFor(color, false).get(0).getId();
    }

    public static MetrikaMobileApp metrikaMobileApp(Color color, String uuid, String deviceId, String deviceIdHash) {
        return metrikaMobileAppBuilder()
                .setUid(Uids.create(UidType.UUID, uuid))
                .setDeviceId(deviceId)
                .setAppId(getAppIdFor(color))
                .setDeviceIdHash(deviceIdHash)
                .build();
    }

    /**
     * Создаёт билдер для MetricaMobileApp и заполняет дефолтными данными. После этого поля можно переопределить
     * нужным для теста образом и построить DTO
     */
    public static MetrikaMobileApp.Builder metrikaMobileAppBuilder() {
        return MetrikaMobileApp.newBuilder()
                .setUid(Uids.create(UidType.UUID, UUID.randomUUID().toString()))
                .setDeviceId(UUID.randomUUID().toString())
                .setAppId(getAppIdFor(Color.BLUE))
                .setDeviceIdHash(UUID.randomUUID().toString())
                .setPushToken(UUID.randomUUID().toString())
                .setUpdateTime(DATE_TIME_FORMATTER.format(LocalDateTime.now().minusDays(1)));
    }

    public static MobileAppInfo mobileAppInfo(String uuid) {
        return mobileAppInfo(uuid, Color.BLUE);
    }

    public static MobileAppInfo mobileAppInfo(String uuid, String platform) {
        return mobileAppInfo(true, uuid, null, Color.BLUE, platform);
    }

    public static MobileAppInfo mobileAppInfo(String uuid, Color color) {
        return mobileAppInfo(true, uuid, null, color, MobileAppInfoUtil.APP_INFO_PLATFORM_ANDROID);
    }

    public static MobileAppInfo mobileAppInfo(boolean registered, String uuid, Long puid) {
        return mobileAppInfo(registered, uuid, puid, Color.BLUE, MobileAppInfoUtil.APP_INFO_PLATFORM_ANDROID);
    }

    public static MobileAppInfo mobileAppInfo(boolean registered, String uuid, Long puid, Color color) {
        return mobileAppInfo(registered, uuid, puid, color, MobileAppInfoUtil.APP_INFO_PLATFORM_ANDROID);
    }

    public static MobileAppInfo mobileAppInfo(boolean registered,
                                              String uuid,
                                              Long puid,
                                              Color color,
                                              String platform) {
        List<Uid> uids = new ArrayList<>();
        uids.add(Uids.create(UidType.UUID, uuid));

        if (puid != null) {
            uids.add(Uids.create(UidType.PUID, puid));
        }

        MobileAppInfo.Builder builder = MobileAppInfo.newBuilder()
                .setKeyUid(Uids.create(UidType.UUID, uuid))
                .addAllUid(uids)
                .setUuid(uuid)
                .setPlatform(platform)
                .setRegistered(registered)
                .setPushToken(UUID.randomUUID().toString())
                .setModificationTime(DATE_TIME_FORMATTER.format(LocalDateTime.now().minusDays(1)));

        String appName = MobileAppInfoUtil.getAppNamesByColor(color).iterator().next();
        if (appName != null) {
            builder.setAppName(appName);
        }

        return builder.build();
    }

    public static GenericSubscription genericSubscription(String uuid) {
        return genericSubscription(uuid, STORE_PUSH_GENERAL_ADVERTISING, true);
    }

    public static GenericSubscription genericSubscription(String uuid, SubscriptionType type, boolean subscribed) {
        GenericSubscription.Channel channel = GenericSubscription.Channel.PUSH;
        GenericSubscription.Status status = subscribed
                ? GenericSubscription.Status.SUBSCRIBED
                : GenericSubscription.Status.UNSUBSCRIBED;
        long now = Instant.now().toEpochMilli();

        return GenericSubscription
                .newBuilder()
                .setUid(Uids.create(UidType.UUID, uuid))
                .setId(channel.getNumber() + "$" + type)
                .setChannel(channel)
                .setType(type.getId())
                .setStatus(status)
                .setCreatedAt(now)
                .setModifiedAt(now)
                .build();
    }

    public static PushTokenStatuses tokenStatus(String deviceIdHash, boolean isValid) {
        return tokenStatus(Color.BLUE, deviceIdHash, isValid);
    }

    public static PushTokenStatuses tokenStatus(Color color, String deviceIdHash, boolean isValid) {
        return PushTokenStatuses.newBuilder()
                .setUid(Uids.create(UidType.MM_DEVICE_ID_HASH, deviceIdHash))
                .setAppId(String.valueOf(getAppIdFor(color)))
                .addStatuses(
                        TokenStatus.newBuilder()
                                .setType("1")
                                .setIsValid(isValid)
                )
                .build();
    }

    public void prepareMobileAppInfos(MobileAppInfo... facts) {
        List<YTreeMapNode> rows = Stream.of(facts)
                .flatMap(MobileTablesHelper::multiplyMobileAppInfos)
                .map(fact -> YTree.mapBuilder()
                        .key("id").value(
                                Strings.isNullOrEmpty(fact.getKeyUid().getStringValue()) ?
                                        String.valueOf(fact.getKeyUid().getIntValue()) :
                                        fact.getKeyUid().getStringValue()
                        )
                        .key("id_type").value(fact.getKeyUid().getType().name().toLowerCase())
                        .key("fact_id").value(fact.getUuid())
                        .key("fact").value(new YTreeStringNodeImpl(fact.toByteArray(), null))
                        .key("platform").value(fact.getPlatform())
                        .buildMap()
                )
                .sorted(comparing(x -> x.getString("id")))
                .collect(Collectors.toList());

        ytClient.write(ytTestTables.getMobileAppInfoFacts(), rows);
    }

    public void prepareGenericSubscriptions(GenericSubscription... facts) {
        List<YTreeMapNode> rows = Stream.of(facts)
                .map(fact -> YTree.mapBuilder()
                        .key("id").value(
                                Strings.isNullOrEmpty(fact.getUid().getStringValue()) ?
                                        String.valueOf(fact.getUid().getIntValue()) :
                                        fact.getUid().getStringValue()
                        )
                        .key("id_type").value(fact.getUid().getType().name().toLowerCase())
                        .key("fact_id").value(fact.getId())
                        .key("fact").value(new YTreeStringNodeImpl(fact.toByteArray(), null))
                        .buildMap()
                )
                .sorted(comparing(x -> x.getString("id")))
                .collect(Collectors.toList());

        ytClient.write(ytTestTables.getGenericSubscriptionFacts(), rows);
    }

    private static Stream<MobileAppInfo> multiplyMobileAppInfos(MobileAppInfo fact) {
        return fact.getUidList().stream()
                .map(uid -> MobileAppInfo.newBuilder()
                        .mergeFrom(fact)
                        .setKeyUid(uid)
                        .build()
                );
    }
}
