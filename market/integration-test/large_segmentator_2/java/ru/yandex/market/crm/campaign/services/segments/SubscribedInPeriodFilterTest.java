package ru.yandex.market.crm.campaign.services.segments;

import java.time.LocalDateTime;
import java.util.Set;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.crm.campaign.test.AbstractServiceLargeTest;
import ru.yandex.market.crm.campaign.test.utils.OfflineSegmentatorTestHelper;
import ru.yandex.market.crm.campaign.test.utils.OfflineSegmentatorTestHelper.UidPair;
import ru.yandex.market.crm.core.domain.segment.LinkingMode;
import ru.yandex.market.crm.core.domain.segment.Segment;
import ru.yandex.market.crm.core.test.utils.SubscriptionsTestHelper;
import ru.yandex.market.crm.core.test.utils.YtSchemaTestHelper;
import ru.yandex.market.crm.mapreduce.domain.user.Uid;
import ru.yandex.market.crm.mapreduce.domain.user.UidType;

import static ru.yandex.market.crm.campaign.test.utils.OfflineSegmentatorTestHelper.pair;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.segment;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.subscribedInPeriod;
import static ru.yandex.market.crm.core.test.utils.SubscriptionsTestHelper.subscription;
import static ru.yandex.market.crm.platform.models.Subscription.Status.SUBSCRIBED;

/**
 * @author apershukov
 */
public class SubscribedInPeriodFilterTest extends AbstractServiceLargeTest {

    private static final Uid EMAIL_1 = Uid.asEmail("email.1@host.ru");
    private static final Uid EMAIL_2 = Uid.asEmail("email.2@host.ru");

    @Inject
    private YtSchemaTestHelper ytSchemaTestHelper;

    @Inject
    private SubscriptionsTestHelper subscriptionsTestHelper;

    @Inject
    private OfflineSegmentatorTestHelper segmentatorTestHelper;

    @Inject
    private SegmentService segmentService;

    @BeforeEach
    public void setUp() {
        ytSchemaTestHelper.prepareSubscriptionFactsTable();
        ytSchemaTestHelper.prepareEmailOwnershipFactsTable();
    }

    @Test
    public void testReturnEmails() throws Exception {
        subscriptionsTestHelper.saveSubscriptions(
                subscription(EMAIL_1.getValue(), SUBSCRIBED, LocalDateTime.now().minusWeeks(1)),
                subscription(EMAIL_2.getValue(), SUBSCRIBED, LocalDateTime.now().minusMonths(6))
        );

        Segment segment = segment(
                subscribedInPeriod()
        );
        segmentService.addSegment(segment);

        Set<UidPair> expected = Set.of(
                pair(EMAIL_1)
        );

        segmentatorTestHelper.assertSegmentPairs(expected, LinkingMode.NONE, Set.of(UidType.EMAIL), segment);
    }
}
