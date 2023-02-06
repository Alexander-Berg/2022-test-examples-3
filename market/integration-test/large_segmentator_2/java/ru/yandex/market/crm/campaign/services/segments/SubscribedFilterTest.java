package ru.yandex.market.crm.campaign.services.segments;

import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;

import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.crm.campaign.test.AbstractServiceLargeTest;
import ru.yandex.market.crm.campaign.test.utils.OfflineSegmentatorTestHelper;
import ru.yandex.market.crm.campaign.test.utils.OfflineSegmentatorTestHelper.UidPair;
import ru.yandex.market.crm.core.domain.segment.LinkingMode;
import ru.yandex.market.crm.core.domain.segment.Segment;
import ru.yandex.market.crm.core.test.utils.SubscriptionTypes;
import ru.yandex.market.crm.core.test.utils.SubscriptionsTestHelper;
import ru.yandex.market.crm.core.test.utils.UserTestHelper;
import ru.yandex.market.crm.core.test.utils.YtSchemaTestHelper;
import ru.yandex.market.crm.mapreduce.domain.user.IdsGraph;
import ru.yandex.market.crm.mapreduce.domain.user.Uid;
import ru.yandex.market.crm.mapreduce.domain.user.UidType;
import ru.yandex.market.crm.mapreduce.domain.user.User;

import static ru.yandex.market.crm.campaign.test.utils.OfflineSegmentatorTestHelper.pair;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.segment;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.subscriptionFilter;
import static ru.yandex.market.crm.core.test.utils.SubscriptionsTestHelper.subscription;
import static ru.yandex.market.crm.platform.models.Subscription.Status.SUBSCRIBED;
import static ru.yandex.market.crm.platform.models.Subscription.Status.UNSUBSCRIBED;

/**
 * @author apershukov
 */
public class SubscribedFilterTest extends AbstractServiceLargeTest {

    private static final Uid EMAIL_1 = Uid.asEmail("email.1@host.ru");
    private static final Uid EMAIL_2 = Uid.asEmail("email.2@host.ru");
    private static final Uid YUID = Uid.asYuid("1234567");

    @Inject
    private YtSchemaTestHelper ytSchemaTestHelper;

    @Inject
    private UserTestHelper userTestHelper;

    @Inject
    private SubscriptionsTestHelper subscriptionsTestHelper;

    @Inject
    private OfflineSegmentatorTestHelper segmentatorTestHelper;

    @BeforeEach
    public void setUp() {
        ytSchemaTestHelper.prepareSubscriptionFactsTable();
        ytSchemaTestHelper.prepareMobileAppInfoFactsTable();
        ytSchemaTestHelper.prepareEmailsGeoInfo();
        ytSchemaTestHelper.prepareUserTables();
    }

    @Test
    public void testSubscribedSegment() throws Exception {
        Segment segment = segment(
                subscriptionFilter(SubscriptionTypes.ADVERTISING)
        );

        subscriptionsTestHelper.saveSubscriptions(
                subscription(EMAIL_1.getValue(), SUBSCRIBED),
                subscription(EMAIL_2.getValue(), UNSUBSCRIBED)
        );

        Set<UidPair> expected = ImmutableSet.of(
                pair(EMAIL_1)
        );

        segmentatorTestHelper.assertSegmentPairs(expected, LinkingMode.ALL, Set.of(UidType.EMAIL), segment);
    }

    /**
     * В случае если указана подписка WISHLIST в сегмент попадают адреса которые не были явно отписаны
     */
    @Test
    public void testNotUnsubscribed() throws Exception {
        Segment segment = segment(
                subscriptionFilter(SubscriptionTypes.WISHLIST)
        );

        subscriptionsTestHelper.saveSubscriptions(
                subscription(EMAIL_2.getValue(), UNSUBSCRIBED, SubscriptionTypes.WISHLIST)
        );

        userTestHelper.addUsers(
            new User(UUID.randomUUID().toString())
                    .setIdsGraph(
                            new IdsGraph()
                                    .addNode(EMAIL_1)
                                    .addNode(YUID)
                                    .addEdge(0, 1)
                    ),
            new User(UUID.randomUUID().toString())
                    .setIdsGraph(
                            new IdsGraph()
                                    .addNode(EMAIL_2)
                                    .addNode(Uid.asYuid("yuid-2"))
                                    .addEdge(0, 1)
                    )
        );
        userTestHelper.finishUsersPreparation();

        segmentatorTestHelper.assertSegmentPairs(
                Set.of(pair(EMAIL_1)),
                LinkingMode.NONE,
                Set.of(UidType.EMAIL),
                segment
        );
    }
}
