package ru.yandex.market.crm.core.test.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Component;

import ru.yandex.inside.yt.kosher.impl.ytree.YTreeStringNodeImpl;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.market.crm.core.domain.subscriptions.SubscriptionType;
import ru.yandex.market.crm.yt.client.YtClient;
import ru.yandex.market.crm.core.yt.paths.CrmYtTables;
import ru.yandex.market.crm.platform.common.Uids;
import ru.yandex.market.crm.platform.commons.UidType;
import ru.yandex.market.crm.platform.models.Subscription;

/**
 * @author apershukov
 */
@Component
public class SubscriptionsTestHelper {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final YtClient ytClient;
    private final CrmYtTables ytTables;

    public SubscriptionsTestHelper(YtClient ytClient, CrmYtTables ytTables) {
        this.ytClient = ytClient;
        this.ytTables = ytTables;
    }

    public static Subscription subscription(String email, Subscription.Status status) {
        return subscription(email, status, LocalDateTime.now().minusDays(1));
    }

    public static Subscription subscription(String email, Subscription.Status status, LocalDateTime modificationTime) {
        return subscription(
                email,
                status,
                SubscriptionTypes.ADVERTISING,
                modificationTime
        );
    }

    public static Subscription subscription(String email,
                                            Subscription.Status status,
                                            SubscriptionType subscriptionType) {
        return subscription(
                email,
                status,
                subscriptionType,
                LocalDateTime.now()
        );
    }

    public static Subscription subscription(String email,
                                            Subscription.Status status,
                                            SubscriptionType subscriptionType,
                                            LocalDateTime modificationTime) {
        String modificationDate = modificationTime.format(DATE_TIME_FORMATTER);

        return Subscription.newBuilder()
                .setUid(Uids.create(UidType.EMAIL, email))
                .setType(subscriptionType.getId())
                .setStatus(status)
                .setActive(status == Subscription.Status.SUBSCRIBED)
                .setEmailValid(true)
                .setModificationDate(modificationDate)
                .setStatusModificationDate(modificationDate)
                .build();
    }

    public static Subscription subscription(String email) {
        return subscription(email, Subscription.Status.SUBSCRIBED);
    }

    public static Subscription subscription(String email, SubscriptionType type) {
        return subscription(email, Subscription.Status.SUBSCRIBED, type);
    }

    public static Subscription subscription(String email, long linkedPuid) {
        return subscription(email).toBuilder()
                .setLinkedPuid(linkedPuid)
                .build();
    }

    public static Subscription subscription(String email, SubscriptionType type, long linkedPuid) {
        return subscription(email, type).toBuilder()
                .setLinkedPuid(linkedPuid)
                .build();
    }

    public void saveSubscriptions(Subscription... subscriptions) {
        List<YTreeMapNode> rows = Stream.of(subscriptions)
                .sorted(Comparator.comparing(x -> x.getUid().getStringValue()))
                .map(subscription -> YTree.mapBuilder()
                        .key("id").value(subscription.getUid().getStringValue())
                        .key("id_type").value("email")
                        .key("fact_id").value(subscription.getId())
                        .key("fact").value(new YTreeStringNodeImpl(
                                subscription.toByteArray(),
                                null
                        ))
                        .buildMap())
                .collect(Collectors.toList());

        ytClient.write(ytTables.getSubscriptions(), rows);
    }
}
