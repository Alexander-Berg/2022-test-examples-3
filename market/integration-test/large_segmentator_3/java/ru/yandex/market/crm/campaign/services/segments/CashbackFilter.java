package ru.yandex.market.crm.campaign.services.segments;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import javax.inject.Inject;

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
import ru.yandex.market.crm.core.test.SegmentTestUtils;
import ru.yandex.market.crm.core.test.utils.YtSchemaTestHelper;
import ru.yandex.market.crm.mapreduce.domain.user.Uid;
import ru.yandex.market.crm.mapreduce.domain.user.UidType;

import static ru.yandex.market.crm.campaign.test.utils.OfflineSegmentatorTestHelper.pair;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.cashbackFilter;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.segment;

public class CashbackFilter extends AbstractServiceLargeTest {

    private static final long PROMO_ID = 123456L;

    @Value("${var.issued_cashback}")
    private String cashbackTablePath;

    @Inject
    private YtSchemaTestHelper ytSchemaTestHelper;
    @Inject
    private OfflineSegmentatorTestHelper segmentatorTestHelper;

    @BeforeEach
    public void setUp() {
        ytSchemaTestHelper.prepareEmailOwnershipFactsTable();
        ytSchemaTestHelper.prepareUserTables();
        ytClient.createTable(YPath.simple(cashbackTablePath), null);
    }

    @AfterEach
    public void tearDown() {
        ytClient.remove(YPath.simple(cashbackTablePath));
    }

    private static YTreeMapNode cashback(long amount, long puid, long promoId) {
        return YTree.mapBuilder()
                .key("amount").value(amount)
                .key("puid").value(puid)
                .key("promo_id").value(promoId)
                .key("status").value("CONFIRMED")
                .buildMap();
    }

    @Test
    public void testWithAmountCondition() throws Exception {
        insertCashback(
                cashback(1, 1L, PROMO_ID),
                cashback(2, 2L, PROMO_ID)
        );

        Set<UidPair> expected = Set.of(
                pair(Uid.asPuid(2L))
        );

        Segment segment = segment(
                cashbackFilter(Collections.emptyList(), 1L, SegmentTestUtils.CountType.MORE)
        );

        assertSegment(expected, segment);
    }

    @Test
    public void testCertainPromoCondition() throws Exception {
        insertCashback(
                cashback(1, 1L, 666L),
                cashback(1, 2L, PROMO_ID)
        );

        Set<UidPair> expected = Set.of(
                pair(Uid.asPuid(2L))
        );

        Segment segment = segment(
                cashbackFilter(Collections.singletonList(PROMO_ID), 0, SegmentTestUtils.CountType.MORE)
        );

        assertSegment(expected, segment);
    }

    @Test
    public void testAmountSum() throws Exception {
        insertCashback(
                cashback(1, 1L, PROMO_ID),
                cashback(1, 1L, PROMO_ID),
                cashback(1, 2L, PROMO_ID),
                cashback(1, 2L, PROMO_ID),
                cashback(1, 2L, PROMO_ID)
        );

        Set<UidPair> expected = Set.of(
                pair(Uid.asPuid(1L)),
                pair(Uid.asPuid(2L))
        );

        Segment segment = segment(
                cashbackFilter(Collections.emptyList(), 1, SegmentTestUtils.CountType.MORE)
        );

        assertSegment(expected, segment);
    }

    private void assertSegment(Set<UidPair> expected, Segment segment) throws Exception {
        segmentatorTestHelper.assertSegmentPairs(expected, LinkingMode.NONE, Set.of(UidType.PUID), segment);
    }

    private void insertCashback(YTreeMapNode... cashback) {
        ytClient.write(YPath.simple(cashbackTablePath), Arrays.asList(cashback));
    }
}
