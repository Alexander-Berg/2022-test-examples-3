package ru.yandex.market.crm.campaign.services.segments;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import javax.inject.Inject;

import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;

import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.market.crm.campaign.test.AbstractServiceLargeTest;
import ru.yandex.market.crm.campaign.test.utils.OfflineSegmentatorTestHelper;
import ru.yandex.market.crm.campaign.test.utils.OfflineSegmentatorTestHelper.UidPair;
import ru.yandex.market.crm.core.domain.segment.LinkingMode;
import ru.yandex.market.crm.core.domain.segment.Segment;
import ru.yandex.market.crm.core.test.SegmentTestUtils.CountType;
import ru.yandex.market.crm.core.test.utils.UserTestHelper;
import ru.yandex.market.crm.core.test.utils.YtSchemaTestHelper;
import ru.yandex.market.crm.mapreduce.domain.user.IdsGraph;
import ru.yandex.market.crm.mapreduce.domain.user.Uid;
import ru.yandex.market.crm.mapreduce.domain.user.UidType;
import ru.yandex.market.crm.mapreduce.domain.user.User;
import ru.yandex.market.crm.yt.client.YtClient;

import static ru.yandex.market.crm.campaign.test.utils.OfflineSegmentatorTestHelper.pair;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.coinFilter;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.segment;

/**
 * @author apershukov
 */
public class CoinFilterTest extends AbstractServiceLargeTest {

    @Inject
    private YtSchemaTestHelper ytSchemaTestHelper;
    @Inject
    private YtClient ytClient;
    @Inject
    private UserTestHelper userTestHelper;
    @Inject
    private OfflineSegmentatorTestHelper segmentatorTestHelper;
    @Value("${var.created_coins}")
    private String coinsTablePath;

    private static YTreeMapNode coin(long id, long puid, long promoId) {
        return YTree.mapBuilder()
                .key("coin_id").value(id)
                .key("coin_nominal").value(1000)
                .key("promo_id").value(promoId)
                .key("uid").value(puid)
                .key("status").value("ACTIVE")
                .key("end_date").value(ZonedDateTime.now().minusDays(1).toString())
                .buildMap();
    }

    private static YTreeMapNode coin(long puid, long promoId) {
        return coin(RandomUtils.nextLong(), puid, promoId);
    }

    private static User user(long puid) {
        return new User("user-" + puid)
                .setIdsGraph(
                        new IdsGraph()
                                .addNode(Uid.asPuid(puid))
                                .addNode(Uid.asYuid(String.valueOf(System.currentTimeMillis())))
                                .addEdge(0, 1)
                );
    }

    @BeforeEach
    public void setUp() {
        ytSchemaTestHelper.prepareEmailOwnershipFactsTable();
        ytSchemaTestHelper.prepareMobileAppInfoFactsTable();
        ytSchemaTestHelper.prepareEmailsGeoInfo();
        ytSchemaTestHelper.prepareUserTables();
        ytClient.createTable(YPath.simple(coinsTablePath), null);
    }

    @AfterEach
    public void tearDown() {
        ytClient.remove(YPath.simple(coinsTablePath));
    }

    @Test
    public void testCoinOfCertainPromo() throws Exception {
        insertCoins(
                coin(1, 10999),
                coin(2, 11000)
        );

        Set<UidPair> expected = ImmutableSet.of(
                pair(Uid.asPuid(1L))
        );

        Segment segment = segment(
                coinFilter(Collections.singletonList(10999L), 0, CountType.MORE)
        );
        assertSegment(expected, segment);
    }

    @Test
    public void testCoinOfAnyPromo() throws Exception {
        insertCoins(
                coin(1, 10999),
                coin(2, 11000)
        );

        Set<UidPair> expected = ImmutableSet.of(
                pair(Uid.asPuid(1L)),
                pair(Uid.asPuid(2L))
        );

        Segment segment = segment(
                coinFilter(Collections.emptyList(), 0, CountType.MORE)
        );
        assertSegment(expected, segment);
    }

    @Test
    public void testMoreThanTwoCoins() throws Exception {
        insertCoins(
                coin(1, 10999),
                coin(1, 10999),
                coin(1, 10999),
                coin(2, 11000),
                coin(2, 11000)
        );

        Set<UidPair> expected = ImmutableSet.of(
                pair(Uid.asPuid(1L))
        );

        Segment segment = segment(
                coinFilter(Collections.emptyList(), 2, CountType.MORE)
        );
        assertSegment(expected, segment);
    }

    @Test
    public void testWithoutCoinsOfCertainType() throws Exception {
        addUsers(
                user(1),
                user(2),
                user(3)
        );

        insertCoins(
                coin(1, 10999),
                coin(1, 11000),
                coin(2, 11000)
        );

        Set<UidPair> expected = ImmutableSet.of(
                pair(Uid.asPuid(2L)),
                pair(Uid.asPuid(3L))
        );

        Segment segment = segment(
                coinFilter(Collections.singletonList(10999L), 0, CountType.EQUALS)
        );
        assertSegment(expected, segment);
    }

    @Test
    public void testDoNotCountSameCoinTwice() throws Exception {
        insertCoins(
                coin(111, 1, 10999),
                coin(111, 1, 10999)
        );

        Set<UidPair> expected = ImmutableSet.of(
                pair(Uid.asPuid(1L))
        );

        Segment segment = segment(
                coinFilter(Collections.emptyList(), 1, CountType.EQUALS)
        );
        assertSegment(expected, segment);
    }

    private void assertSegment(Set<UidPair> expected, Segment segment) throws Exception {
        segmentatorTestHelper.assertSegmentPairs(expected, LinkingMode.NONE, Set.of(UidType.PUID), segment);
    }

    private void insertCoins(YTreeMapNode... coins) {
        ytClient.write(YPath.simple(coinsTablePath), Arrays.asList(coins));
    }

    private void addUsers(User... user) {
        userTestHelper.addUsers(user);
        userTestHelper.finishUsersPreparation();
    }
}
