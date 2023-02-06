package ru.yandex.market.crm.triggers.services.segments;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;
import javax.inject.Inject;

import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import ru.yandex.market.crm.core.domain.segment.Segment;
import ru.yandex.market.crm.core.domain.segment.SegmentGroupPart;
import ru.yandex.market.crm.core.domain.segment.SegmentPart;
import ru.yandex.market.crm.core.services.external.pers.basket.PersBasketClient;
import ru.yandex.market.crm.core.services.segmentator.online.OnlineSegmentProcessor;
import ru.yandex.market.crm.core.services.segmentator.online.SegmentAlgorithmStrategySelector;
import ru.yandex.market.crm.core.services.segmentator.online.strategies.EmailListStrategy;
import ru.yandex.market.crm.core.services.segmentator.online.strategies.HasWishesStrategy;
import ru.yandex.market.crm.core.services.segmentator.online.strategies.SubscribedStrategy;
import ru.yandex.market.crm.core.services.subscription.SubscriptionResolver;
import ru.yandex.market.crm.core.services.subscription.SubscriptionService;
import ru.yandex.market.crm.core.suppliers.SubscriptionsTypesSupplier;
import ru.yandex.market.crm.mapreduce.domain.user.IdsGraph;
import ru.yandex.market.crm.mapreduce.domain.user.Uid;
import ru.yandex.market.crm.mapreduce.domain.user.UidType;
import ru.yandex.market.crm.mapreduce.domain.user.User;
import ru.yandex.market.crm.platform.common.Uids;
import ru.yandex.market.crm.platform.models.Subscription;
import ru.yandex.market.crm.triggers.test.AbstractServiceTest;
import ru.yandex.market.crm.triggers.test.helpers.PersBasketTestHelper;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.emailsFilter;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.hasWishesFilter;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.segment;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.subscriptionFilter;

/**
 * @author apershukov
 */
public class OnlineSegmentProcessorTest extends AbstractServiceTest {

    private static final String TEST_EMAIL1 = "test@tesT.ru";
    private static final String TEST_EMAIL2 = "test@Mail.ru";
    private static final String TEST_UUID = "000049c52798c02e893fdadfe93dbd34";

    private static final Uid PUID = Uid.asPuid(111L);
    private static final Uid YUID = Uid.asYuid("123456");

    private static final String SUBSCRIPTION_TEST_EMAIL = "test@test.ru";
    private static final int ADVERTISING = 2;

    private OnlineSegmentProcessor processor;
    @Inject
    private PersBasketClient persBasketClient;
    @Inject
    private PersBasketTestHelper persBasketTestHelper;

    @Inject
    private SubscriptionsTypesSupplier typesSupplier;

    @Mock
    private SubscriptionService subscriptionService;

    private static User user1() {
        return new User()
                .setIdsGraph(
                        new IdsGraph()
                                .addNode(Uid.of(UidType.UUID, TEST_UUID))
                                .addNode(Uid.of(UidType.EMAIL, TEST_EMAIL1))
                                .addNode(Uid.of(UidType.EMAIL, TEST_EMAIL2))
                );
    }

    private static User user2() {
        return new User()
                .setIdsGraph(
                        new IdsGraph()
                                .addNode(Uid.of(UidType.EMAIL, SUBSCRIPTION_TEST_EMAIL))
                );
    }


    @Before
    public void setUp() {
        subscriptionService = mock(SubscriptionService.class);

        SubscriptionResolver resolver = new SubscriptionResolver(subscriptionService, typesSupplier);
        SegmentAlgorithmStrategySelector selector = new SegmentAlgorithmStrategySelector(
                Arrays.asList(
                        new SubscribedStrategy(subscriptionService, typesSupplier, resolver),
                        new HasWishesStrategy(persBasketClient),
                        new EmailListStrategy()
                )
        );

        this.processor = new OnlineSegmentProcessor(selector);
    }

    @Test
    public void testReturnFalseForSegmentWithoutFilters() {
        Segment segment = new Segment();
        SegmentPart segmentPart = new SegmentGroupPart();
        segment.setConfig(segmentPart);

        assertFalse(processor.hasUser(segment, user1()));
    }

    @Test
    public void testReturnTrueForComplexSupportedSegment() {
        Segment segment = segment(
                emailsFilter("test@mail.ru"),
                hasWishesFilter()
        );

        persBasketTestHelper.prepareWishlistCount(Uid.asUuid(TEST_UUID), 1);

        assertTrue(processor.hasUser(segment, user1()));
    }

    @Test
    public void testUserDoesNotBelongToSegmentInSubscriptionIsNotActive() {
        Segment segment = segment(
                subscriptionFilter(Collections.emptyList(), null)
        );
        User user = user2();
        saveSubscription(false, ImmutableSet.of("card"), "desktop");
        assertFalse(processor.hasUser(segment, user));
    }

    @Test
    public void testUserWithDifferentPlaceDoesNotBelongToSegment() {
        Segment segment = segment(
                subscriptionFilter(Collections.singletonList("journal"), null)
        );
        User user = user2();
        saveSubscription(true, ImmutableSet.of("card"), null);
        assertFalse(processor.hasUser(segment, user));
    }

    @Test
    public void testUserWithDifferentPlatformDoesNotBelongToSegment() {
        Segment segment = segment(
                subscriptionFilter(Collections.emptyList(), "touch")
        );
        User user = user2();
        saveSubscription(true, ImmutableSet.of("card", "journal"), "desktop");
        assertFalse(processor.hasUser(segment, user));
    }

    @Test
    public void testUserWithMatchingPlaceBelongsToSegment() {
        Segment segment = segment(
                subscriptionFilter(Arrays.asList("promo", "CARD"), null)
        );
        User user = user2();
        saveSubscription(true, ImmutableSet.of("card", "journal"), null);
        assertTrue(processor.hasUser(segment, user));
    }

    @Test
    public void testUserWithMatchingPlatformBelongsToSegment() {
        Segment segment = segment(
                subscriptionFilter(Collections.emptyList(), "tOuCh")
        );
        User user = user2();
        saveSubscription(true, Collections.emptySet(), "ToUcH");
        assertTrue(processor.hasUser(segment, user));
    }

    @Test
    public void testUserWithSubscriptionBelongsToSegment() {
        Segment segment = segment(
                subscriptionFilter(Collections.emptyList(), null)
        );
        User user = user2();
        saveSubscription(true, ImmutableSet.of("card"), "desktop");
        assertTrue(processor.hasUser(segment, user));
    }

    @Test
    public void testUserWithWishlistItemsOnMarketBelongsToSegment() {
        User user = new User(UUID.randomUUID().toString())
                .setIdsGraph(
                        new IdsGraph()
                                .addNode(PUID)
                                .addNode(YUID)
                                .addEdge(0, 1)
                );

        persBasketTestHelper.prepareWishlistCount(PUID, 0);
        persBasketTestHelper.prepareWishlistCount(YUID, 5);

        Segment segment = segment(hasWishesFilter());
        assertTrue(processor.hasUser(segment, user));
    }

    @Test
    public void testUserWithoutWishliesItemsOnMarketDowsNotBelongToSegment() {
        User user = new User(UUID.randomUUID().toString())
                .setIdsGraph(
                        new IdsGraph()
                                .addNode(PUID)
                                .addNode(YUID)
                                .addEdge(0, 1)
                );

        persBasketTestHelper.prepareWishlistCount(PUID, 0);
        persBasketTestHelper.prepareWishlistCount(YUID, 0);

        Segment segment = segment(hasWishesFilter());
        assertFalse(processor.hasUser(segment, user));
    }

    private void saveSubscription(boolean active, Set<String> places, @Nullable String platform) {
        Subscription.Builder builder =
                Subscription.newBuilder()
                        .setUid(Uids.create(ru.yandex.market.crm.platform.commons.UidType.EMAIL,
                                SUBSCRIPTION_TEST_EMAIL))
                        .setEmailValid(true)
                        .setActive(active)
                        .addAllPlaces(places);

        if (platform != null) {
            builder.setPlatform(platform);
        }

        when(subscriptionService.getSubscriptions(Collections.singleton(SUBSCRIPTION_TEST_EMAIL), ADVERTISING))
                .thenReturn(Collections.singletonList(builder.build()));
    }
}
