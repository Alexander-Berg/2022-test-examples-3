package ru.yandex.market.crm.core.test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableMap;

import ru.yandex.market.crm.core.domain.Color;
import ru.yandex.market.crm.core.domain.CommonDateRange;
import ru.yandex.market.crm.core.domain.coins.BonusType;
import ru.yandex.market.crm.core.domain.coins.PromoType;
import ru.yandex.market.crm.core.domain.crypta.CryptaSegment;
import ru.yandex.market.crm.core.domain.segment.Condition;
import ru.yandex.market.crm.core.domain.segment.Segment;
import ru.yandex.market.crm.core.domain.segment.SegmentAlgorithmPart;
import ru.yandex.market.crm.core.domain.segment.SegmentGroupPart;
import ru.yandex.market.crm.core.domain.segment.SegmentPart;
import ru.yandex.market.crm.core.domain.subscriptions.SubscriptionType;
import ru.yandex.market.crm.core.test.utils.DeviceType;
import ru.yandex.market.crm.core.test.utils.SubscriptionTypes;
import ru.yandex.market.crm.mapreduce.domain.user.UidType;
import ru.yandex.market.crm.platform.commons.SendingType;
import ru.yandex.market.crm.util.LiluStreams;

/**
 * @author apershukov
 */
public class SegmentTestUtils {

    public static Segment segment(String name, Condition condition, SegmentPart... parts) {
        Segment segment = new Segment();
        segment.setId(UUID.randomUUID().toString());
        segment.setName(name);

        SegmentGroupPart groupPart = new SegmentGroupPart();
        groupPart.setParts(Arrays.asList(parts));
        groupPart.setCondition(condition);

        segment.setConfig(groupPart);
        return segment;
    }

    public static Segment segment(String name, SegmentPart... parts) {
        return segment(name, Condition.ALL, parts);
    }

    public static Segment segment(SegmentPart... parts) {
        return segment("Test Segment", parts);
    }

    public static SegmentGroupPart any(SegmentPart... parts) {
        SegmentGroupPart part = new SegmentGroupPart();
        part.setCondition(Condition.ANY);
        part.setParts(List.of(parts));
        return part;
    }

    public static SegmentAlgorithmPart hasWishesFilter(Integer... hids) {
        SegmentAlgorithmPart config = new SegmentAlgorithmPart();
        config.setAlgorithmId("hasWishes");
        config.setProperties(Map.of("hids", List.of(hids)));
        return config;
    }

    public static SegmentAlgorithmPart subscriptionFilter(String type, List<String> places, String platform) {
        SegmentAlgorithmPart config = new SegmentAlgorithmPart();
        config.setAlgorithmId("subscribed");

        ImmutableMap.Builder<String, Object> builder = ImmutableMap.<String, Object>builder()
                .put("subscription_types", Collections.singletonList(type))
                .put("places", places);

        if (platform != null) {
            builder.put("platform", platform);
        }

        config.setProperties(builder.build());
        return config;
    }

    public static SegmentAlgorithmPart subscriptionFilter(List<String> places, String platform) {
        return subscriptionFilter("ADVERTISING", places, platform);
    }

    public static SegmentAlgorithmPart subscriptionFilter(SubscriptionType type) {
        return subscriptionFilter(type.getName(), Collections.emptyList(), null);
    }

    public static SegmentAlgorithmPart subscribedInPeriod() {
        SegmentAlgorithmPart config = new SegmentAlgorithmPart();
        config.setAlgorithmId("subscribed_in_period");

        Map<String, Object> properties = ImmutableMap.of(
                "subscription_type", SubscriptionTypes.ADVERTISING.getName(),
                "subscription_status", "SUBSCRIBED",
                "period", new CommonDateRange(30)
        );

        config.setProperties(properties);
        return config;
    }

    public static SegmentAlgorithmPart emailsFilter(String... emails) {
        SegmentAlgorithmPart part = new SegmentAlgorithmPart();
        part.setAlgorithmId("email_list");
        part.setProperties(
                Collections.singletonMap("test_emails_list", Arrays.asList(emails))
        );
        return part;
    }

    public static SegmentAlgorithmPart allUsers() {
        SegmentAlgorithmPart part = new SegmentAlgorithmPart();
        part.setAlgorithmId("all");
        return part;
    }

    public static SegmentAlgorithmPart passportGender(String gender) {
        SegmentAlgorithmPart part = new SegmentAlgorithmPart();
        part.setAlgorithmId("gender_passport");
        part.setProperties(
                Collections.singletonMap("gender", gender)
        );
        return part;
    }

    public static SegmentAlgorithmPart emailMessagesFilter(String sending,
                                                           SendingType sendingType,
                                                           CommonDateRange period,
                                                           Integer deliveryCount) {
        List<String> sendings = sending == null ? null : List.of(sending);
        return emailMessagesFilter(sendings, List.of(sendingType), period, deliveryCount);
    }

    public static SegmentAlgorithmPart emailMessagesFilter(List<String> sendings,
                                                           List<SendingType> sendingTypes,
                                                           CommonDateRange period,
                                                           Integer deliveryCount) {
        SegmentAlgorithmPart part = new SegmentAlgorithmPart();
        part.setAlgorithmId("messages");

        Map<String, Object> properties = new HashMap<>();
        properties.put("sendingTypes", sendingTypes);
        properties.put("period", period);
        properties.put("sendings", sendings);
        properties.put("color", Color.GREEN.name());
        properties.put("deliveryCount", deliveryCount);
        properties.put("deliveryCountType", 2);

        part.setProperties(properties);

        return part;
    }

    public static SegmentAlgorithmPart ordersFilter() {
        return ordersFilter(false, 0, 0, Collections.emptyList());
    }

    public static SegmentAlgorithmPart ordersFilter(boolean collapseOrders,
                                                    int count,
                                                    int price,
                                                    List<String> productNames,
                                                    Integer... hids) {
        return ordersFilter(collapseOrders, count, price, productNames, List.of(), hids);
    }

    public static SegmentAlgorithmPart ordersFilter(boolean collapseOrders,
                                                    int count,
                                                    int price,
                                                    List<String> productNames,
                                                    List<Long> vendorIds,
                                                    Integer... hids) {
        var categories = Stream.of(hids)
                .map(hid -> Map.<String, Object>of("hid", hid, "name", "Category"))
                .collect(Collectors.toList());

        SegmentAlgorithmPart part = new SegmentAlgorithmPart();
        part.setAlgorithmId("orders");

        part.setProperties(
                ImmutableMap.<String, Object>builder()
                        .put("period", new CommonDateRange(30))
                        .put("count", count)
                        .put("countType", 1)
                        .put("price", price)
                        .put("priceType", 1)
                        .put("categories", categories)
                        .put("color", Color.BLUE.name())
                        .put("collapseOrders", collapseOrders)
                        .put("productNames", productNames)
                        .put("vendors", vendorIds.stream()
                                .map(id -> Map.of("id", id))
                                .collect(Collectors.toList())
                        )
                        .build()
        );

        return part;
    }

    public static SegmentAlgorithmPart serfMarketFilter(List<Long> productIds,
                                                        List<String> productNames,
                                                        List<Long> vendorIds) {
        SegmentAlgorithmPart part = new SegmentAlgorithmPart();
        part.setAlgorithmId("serf_market_platform");

        part.setProperties(Map.of(
                "period", new CommonDateRange(30),
                "productIds", productIds,
                "productNames", productNames,
                "vendors", vendorIds.stream()
                        .map(id -> Map.of("id", id))
                        .collect(Collectors.toList())
        ));

        return part;
    }

    public static SegmentAlgorithmPart pushFilter(Set<SendingType> types,
                                                  CommonDateRange dateRange,
                                                  @Nullable Sending sending,
                                                  int sentCount) {
        return new SegmentAlgorithmPart()
                .setAlgorithmId("pushes")
                .setProperties(
                        getPushAlgorithmBuilder(dateRange, sending, sentCount, types)
                                .build()
                );
    }

    public static SegmentAlgorithmPart pushFilter(CommonDateRange dateRange, @Nullable Sending sending, int sentCount) {
        return pushFilter(Set.of(SendingType.PROMO), dateRange, sending, sentCount);
    }

    public static SegmentAlgorithmPart pushFilterWithReceived(CommonDateRange dateRange,
                                                              Sending sending,
                                                              int sentCount,
                                                              int received) {
        return pushFilterWithReceived(Set.of(SendingType.PROMO), dateRange, sending, sentCount, received);
    }

    public static SegmentAlgorithmPart pushFilterWithReceived(Set<SendingType> sendingTypes,
                                                              CommonDateRange dateRange,
                                                              Sending sending,
                                                              int sentCount,
                                                              int received) {
        return new SegmentAlgorithmPart()
                .setAlgorithmId("pushes")
                .setProperties(
                        getPushAlgorithmBuilder(dateRange, sending, sentCount, sendingTypes)
                                .put("receivedCount", received)
                                .put("receivedCountType", 0)
                                .build()
                );
    }

    public static SegmentAlgorithmPart pushFilterWithOpened(CommonDateRange dateRange,
                                                            Sending sending,
                                                            int sentCount,
                                                            int opened) {
        return new SegmentAlgorithmPart()
                .setAlgorithmId("pushes")
                .setProperties(
                        getPushAlgorithmBuilder(dateRange, sending, sentCount, Set.of(SendingType.PROMO))
                                .put("openedCount", opened)
                                .put("openedCountType", 0)
                                .build()
                );
    }

    public static SegmentAlgorithmPart coinFilter(List<Long> promoIds, int count, CountType countType) {
        SegmentAlgorithmPart part = new SegmentAlgorithmPart();
        part.setAlgorithmId("coins");

        part.setProperties(
                ImmutableMap.of(
                        "count", count,
                        "countType", countType.getCode(),
                        "period", new CommonDateRange(30),
                        "statuses", Collections.singletonList("ACTIVE"),
                        "coin_types", LiluStreams.of(promoIds)
                                .map(id -> new PromoType(id, "Promo " + id, BonusType.COIN))
                                .collect(Collectors.toList())
                )
        );

        return part;
    }

    public static SegmentAlgorithmPart cashbackFilter(List<Long> promoIds, long amount, CountType countType) {
        SegmentAlgorithmPart part = new SegmentAlgorithmPart();
        part.setAlgorithmId("cashback");

        part.setProperties(
                ImmutableMap.of(
                        "amount", amount,
                        "amountType", countType.getCode(),
                        "statuses", Collections.singletonList("CONFIRMED"),
                        "cashback_promos", LiluStreams.of(promoIds)
                                .map(id -> new PromoType(id, "Promo " + id, BonusType.CASHBACK))
                                .collect(Collectors.toList())
                )
        );

        return part;
    }

    public static SegmentAlgorithmPart plusFilter() {
        SegmentAlgorithmPart part = new SegmentAlgorithmPart();
        part.setAlgorithmId("plus");
        return part;
    }

    public static SegmentAlgorithmPart mobilesFilter() {
        return mobilesFilter("ANDROID");
    }

    public static SegmentAlgorithmPart mobilesFilter(String platform) {
        SegmentAlgorithmPart part = new SegmentAlgorithmPart();
        part.setAlgorithmId("mobiles");

        Map<String, Object> properties = ImmutableMap.of(
                "registered", 1,
                "platform", platform,
                "colors", Collections.singletonList(Color.BLUE.name())
        );

        part.setProperties(properties);

        return part;
    }

    public static SegmentAlgorithmPart mobilesFilter(int registered, SubscriptionType subscriptionType) {
        SegmentAlgorithmPart part = new SegmentAlgorithmPart();
        part.setAlgorithmId("mobiles");

        Map<String, Object> properties = ImmutableMap.of(
                "registered", registered,
                "platform", "ANDROID",
                "colors", Collections.singletonList(Color.BLUE.name()),
                "subscription_types", Collections.singletonList(subscriptionType.getName())
        );

        part.setProperties(properties);

        return part;
    }

    public static SegmentAlgorithmPart accessMarketFilter() {
        return accessMarketFilter(DeviceType.DESKTOP);
    }

    public static SegmentAlgorithmPart accessMarketFilter(DeviceType... deviceTypes) {
        SegmentAlgorithmPart part = new SegmentAlgorithmPart();
        part.setAlgorithmId("access_market");

        part.setProperties(
                ImmutableMap.of(
                        "period", new CommonDateRange(30),
                        "color", Color.GREEN.name(),
                        "device_types", deviceTypes
                )
        );

        return part;
    }

    public static SegmentAlgorithmPart userLoginsFilter(String... logins) {
        SegmentAlgorithmPart part = new SegmentAlgorithmPart();
        part.setAlgorithmId("passport_accounts");

        part.setProperties(
                Collections.singletonMap(
                        "logins", Arrays.asList(logins)
                )
        );

        return part;
    }

    public static SegmentAlgorithmPart crypta(CryptaSegment... segments) {
        SegmentAlgorithmPart part = new SegmentAlgorithmPart();
        part.setAlgorithmId("crypta");

        part.setProperties(
                Collections.singletonMap(
                        "crypta_segments", Arrays.asList(segments)
                )
        );

        return part;
    }

    public static SegmentAlgorithmPart periodicAction(String actionKey, CommonDateRange period) {
        Map<String, Object> properties = Map.of(
                "action", actionKey,
                "period", period
        );

        SegmentAlgorithmPart part = new SegmentAlgorithmPart();
        part.setAlgorithmId("periodic_action");
        part.setProperties(properties);
        return part;
    }

    public static SegmentAlgorithmPart pluggableTableFilter(long id, String path, String idColumn, UidType idType) {
        SegmentAlgorithmPart part = new SegmentAlgorithmPart();
        part.setAlgorithmId("pluggable_table");

        Map<String, Object> table = Map.of(
                "id", id,
                "path", path,
                "uidType", idType.name(),
                "uidColumn", idColumn
        );

        part.setProperties(
                Map.of("table", table)
        );

        return part;
    }

    public static SegmentAlgorithmPart emailList(String... emails) {
        SegmentAlgorithmPart part = new SegmentAlgorithmPart();
        part.setAlgorithmId("email_list");
        part.setProperties(Map.of(
                "test_emails_list", List.of(emails)
        ));
        return part;
    }

    public static SegmentAlgorithmPart segmentBuild(long buildId) {
        SegmentAlgorithmPart part = new SegmentAlgorithmPart();
        part.setAlgorithmId("segment_build");
        part.setProperties(Map.of(
                "build", Map.of("id", buildId)
        ));

        return part;
    }

    public static SegmentPart not(SegmentPart part) {
        part.setNot(true);
        return part;
    }

    public static CryptaSegment cryptaSegment(int keywordId, long id) {
        CryptaSegment segment = new CryptaSegment();
        segment.setId(id);
        segment.setKeywordId(keywordId);
        return segment;
    }

    private static ImmutableMap.Builder<String, Object> getPushAlgorithmBuilder(@Nullable CommonDateRange dateRange,
                                                                                @Nullable Sending sending,
                                                                                int sentCount,
                                                                                Set<SendingType> sendingTypes) {
        ImmutableMap.Builder<String, Object> builder = ImmutableMap.<String, Object>builder()
                .put("sendingTypes", sendingTypes.stream().map(Enum::toString).collect(Collectors.toList()))
                .put("color", Color.BLUE.name())
                .put("sentCount", sentCount)
                .put("sentCountType", 0);

        if (sending != null) {
            builder.put("sendings", List.of(sending));
        }
        if (dateRange != null) {
            builder.put("period", dateRange);
        }
        return builder;
    }

    public enum CountType {
        EQUALS(0),
        LESS(-1),
        MORE(1);

        private final int code;

        CountType(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }
    }
}
