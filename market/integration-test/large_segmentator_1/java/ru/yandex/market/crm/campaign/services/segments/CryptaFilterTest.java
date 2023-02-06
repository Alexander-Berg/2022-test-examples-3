package ru.yandex.market.crm.campaign.services.segments;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import javax.inject.Inject;

import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.Test;

import ru.yandex.market.crm.campaign.test.AbstractServiceLargeTest;
import ru.yandex.market.crm.campaign.test.utils.CryptaProfilesTestHelper;
import ru.yandex.market.crm.campaign.test.utils.CryptaProfilesTestHelper.ProfileBuilder;
import ru.yandex.market.crm.campaign.test.utils.OfflineSegmentatorTestHelper;
import ru.yandex.market.crm.campaign.test.utils.OfflineSegmentatorTestHelper.UidPair;
import ru.yandex.market.crm.core.domain.segment.LinkingMode;
import ru.yandex.market.crm.core.domain.segment.Segment;
import ru.yandex.market.crm.mapreduce.domain.user.Uid;
import ru.yandex.market.crm.mapreduce.domain.user.UidType;

import static ru.yandex.market.crm.campaign.test.utils.CryptaProfilesTestHelper.exactSocdem;
import static ru.yandex.market.crm.campaign.test.utils.CryptaProfilesTestHelper.profile;
import static ru.yandex.market.crm.campaign.test.utils.OfflineSegmentatorTestHelper.pair;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.crypta;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.cryptaSegment;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.segment;

/**
 * @author apershukov
 */
public class CryptaFilterTest extends AbstractServiceLargeTest {

    @Inject
    private OfflineSegmentatorTestHelper segmentatorTestHelper;

    @Inject
    private CryptaProfilesTestHelper cryptaProfilesTestHelper;

    /**
     * Фильтрация по сегменту из колонки "user_age_6s"
     */
    @Test
    public void testFilterByAgeSegment() throws Exception {
        prepareProfiles(
                profile(111).exactSocdem(exactSocdem("0_17", null, null, null)),
                profile(222),
                profile(333).exactSocdem(exactSocdem(null, null, null, null)),
                profile(444).exactSocdem(exactSocdem("25_34", null, null, null)),
                profile(555).exactSocdem(exactSocdem("45_54", null, null, null))
        );

        Segment segment = segment(
                crypta(
                        cryptaSegment(543, 2),
                        cryptaSegment(543, 4)
                )
        );

        Set<UidPair> pairs = ImmutableSet.of(
                pair(Uid.asYuid("444")),
                pair(Uid.asYuid("555"))
        );

        assertSegment(segment, pairs);
    }

    /**
     * Фильтрация по сегменту из колонки "heuristic_common"
     */
    @Test
    public void testFilterByHeuristicCommonSegment() throws Exception {
        prepareProfiles(
                profile(111).heuristicCommon(Collections.singletonList(1058)),
                profile(222),
                profile(333).heuristicCommon(Collections.emptyList()),
                profile(444).heuristicCommon(Arrays.asList(1024, 1048)),
                profile(555).heuristicCommon(Collections.singletonList(1039))
        );

        Segment segment = segment(
                crypta(
                        cryptaSegment(547, 1058),
                        cryptaSegment(547, 1048)
                )
        );

        Set<UidPair> pairs = ImmutableSet.of(
                pair(Uid.asYuid("111")),
                pair(Uid.asYuid("444"))
        );

        assertSegment(segment, pairs);
    }

    /**
     * Фильтрация по сегменту из колонки "audience_segments"
     */
    @Test
    public void testFilterByAudienceSegments() throws Exception {
        prepareProfiles(
                profile(111),
                profile(222).audienceSegments(Collections.emptyList()),
                profile(333).audienceSegments(Collections.singletonList(6521188)),
                profile(444).audienceSegments(Arrays.asList(5994874, 3997067)),
                profile(555).audienceSegments(Collections.singletonList(5994937))
        );

        Segment segment = segment(
                crypta(
                        cryptaSegment(557, 6521188),
                        cryptaSegment(557, 3997067)
                )
        );

        Set<UidPair> pairs = ImmutableSet.of(
                pair(Uid.asYuid("333")),
                pair(Uid.asYuid("444"))
        );

        assertSegment(segment, pairs);
    }

    /**
     * Фильтрация по сегменту из колонки "heuristic_private"
     */
    @Test
    public void testFilterByPrivateHeuristicSegments() throws Exception {
        prepareProfiles(
                profile(111),
                profile(222).heuristicPrivate(Collections.emptyList()),
                profile(333).heuristicPrivate(Collections.singletonList(6521188)),
                profile(444).heuristicPrivate(Arrays.asList(5994874, 3997067)),
                profile(555).heuristicPrivate(Collections.singletonList(5994937))
        );

        Segment segment = segment(
                crypta(
                        cryptaSegment(548, 6521188),
                        cryptaSegment(548, 3997067)
                )
        );

        Set<UidPair> pairs = ImmutableSet.of(
                pair(Uid.asYuid("333")),
                pair(Uid.asYuid("444"))
        );

        assertSegment(segment, pairs);
    }

    /**
     * Фильтрация по сегменту из колонки "income_5_segments"
     */
    @Test
    public void testFilterByIncome5Segments() throws Exception {
        prepareProfiles(
                profile(111).exactSocdem(exactSocdem(null, null, null, null)),
                profile(222),
                profile(333).exactSocdem(exactSocdem(null, "A", null, null)),
                profile(444).exactSocdem(exactSocdem(null, "C1", null, null)),
                profile(555).exactSocdem(exactSocdem(null, "B1", null, null))
        );

        Segment segment = segment(
                crypta(
                        cryptaSegment(614, 3)
                )
        );

        Set<UidPair> pairs = ImmutableSet.of(
                pair(Uid.asYuid("444"))
        );

        assertSegment(segment, pairs);
    }

    /**
     * Фильтрация по сегменту из колонки "lal_internal"
     */
    @Test
    public void testFilterByLalInternal() throws Exception {
        prepareProfiles(
                profile(111),
                profile(222).lalInternal(Collections.emptyList()),
                profile(333).lalInternal(Collections.singletonList(1119)),
                profile(444).lalInternal(Arrays.asList(1134, 1735)),
                profile(555).lalInternal(Arrays.asList(1534, 1543))
        );

        Segment segment = segment(
                crypta(
                        cryptaSegment(546, 1119),
                        cryptaSegment(546, 1534)
                )
        );

        Set<UidPair> pairs = ImmutableSet.of(
                pair(Uid.asYuid("333")),
                pair(Uid.asYuid("555"))
        );

        assertSegment(segment, pairs);
    }

    /**
     * Фильтрация по сегменту из колонки "lal_common"
     */
    @Test
    public void testFilterByLalCommon() throws Exception {
        prepareProfiles(
                profile(111),
                profile(222).lalCommon(Collections.emptyList()),
                profile(333).lalCommon(Collections.singletonList(1119)),
                profile(444).lalCommon(Arrays.asList(1134, 1735)),
                profile(555).lalCommon(Arrays.asList(1534, 1543))
        );

        Segment segment = segment(
                crypta(
                        cryptaSegment(544, 1119),
                        cryptaSegment(544, 1534)
                )
        );

        Set<UidPair> pairs = ImmutableSet.of(
                pair(Uid.asYuid("333")),
                pair(Uid.asYuid("555"))
        );

        assertSegment(segment, pairs);
    }

    /**
     * Фильтрация по сегменту из колонки "income_segments"
     */
    @Test
    public void testFilterByIncomeSegment() throws Exception {
        prepareProfiles(
                profile(111).exactSocdem(exactSocdem(null, null, null, null)),
                profile(222),
                profile(333).exactSocdem(exactSocdem(null, null, "A", null)),
                profile(444).exactSocdem(exactSocdem(null, null, "B", null)),
                profile(555).exactSocdem(exactSocdem(null, null, "C", null))
        );

        Segment segment = segment(
                crypta(
                        cryptaSegment(176, 1)
                )
        );

        Set<UidPair> pairs = ImmutableSet.of(
                pair(Uid.asYuid("444"))
        );

        assertSegment(segment, pairs);
    }

    /**
     * Фильтрация по сегменту из колонки "longterm_interests"
     */
    @Test
    public void testFilterByLongtermInterests() throws Exception {
        prepareProfiles(
                profile(111),
                profile(222).longtermInterests(),
                profile(333).longtermInterests(24),
                profile(444).longtermInterests(192, 146),
                profile(555).longtermInterests(55)
        );

        Segment segment = segment(
                crypta(
                        cryptaSegment(601, 192),
                        cryptaSegment(601, 55)
                )
        );

        Set<UidPair> pairs = ImmutableSet.of(
                pair(Uid.asYuid("444")),
                pair(Uid.asYuid("555"))
        );

        assertSegment(segment, pairs);
    }

    /**
     * Фильтрация по сегменту из колонки "shortterm_interests"
     */
    @Test
    public void testFilterByShorttermInterests() throws Exception {
        prepareProfiles(
                profile(111),
                profile(222).shorttermInterests(),
                profile(333).shorttermInterests(24),
                profile(444).shorttermInterests(192, 146),
                profile(555).shorttermInterests(55)
        );

        Segment segment = segment(
                crypta(
                        cryptaSegment(602, 192),
                        cryptaSegment(602, 55)
                )
        );

        Set<UidPair> pairs = ImmutableSet.of(
                pair(Uid.asYuid("444")),
                pair(Uid.asYuid("555"))
        );

        assertSegment(segment, pairs);
    }

    /**
     * Фильтрация по сегменту из колонки "heuristic_segments"
     */
    @Test
    public void testFilterByHeuristicSegments() throws Exception {
        prepareProfiles(
                profile(111),
                profile(222).heuristicSegments(),
                profile(333).heuristicSegments(596),
                profile(444).heuristicSegments(661, 548),
                profile(555).heuristicSegments(595)
        );

        Segment segment = segment(
                crypta(
                        cryptaSegment(216, 661),
                        cryptaSegment(216, 595)
                )
        );

        Set<UidPair> pairs = ImmutableSet.of(
                pair(Uid.asYuid("444")),
                pair(Uid.asYuid("555"))
        );

        assertSegment(segment, pairs);
    }

    /**
     * Фильтрация по сегменту из колонки "marketing_segments"
     */
    @Test
    public void testFilterByMarketingSegments() throws Exception {
        prepareProfiles(
                profile(111),
                profile(222).marketingSegments(),
                profile(333).marketingSegments(297),
                profile(444).marketingSegments(314, 317),
                profile(555).marketingSegments(318)
        );

        Segment segment = segment(
                crypta(
                        cryptaSegment(281, 314),
                        cryptaSegment(281, 318)
                )
        );

        Set<UidPair> pairs = ImmutableSet.of(
                pair(Uid.asYuid("444")),
                pair(Uid.asYuid("555"))
        );

        assertSegment(segment, pairs);
    }

    /**
     * Фильтрация по сегменту из колонки "gender"
     */
    @Test
    public void testFilterByGender() throws Exception {
        prepareProfiles(
                profile(111).exactSocdem(exactSocdem(null, null, null, "f")),
                profile(222),
                profile(333).exactSocdem(exactSocdem(null, null, null, "m")),
                profile(444).exactSocdem(exactSocdem(null, null, null, null))
        );

        Segment segment = segment(
                crypta(
                        cryptaSegment(174, 0)
                )
        );

        Set<UidPair> pairs = ImmutableSet.of(
                pair(Uid.asYuid("333"))
        );

        assertSegment(segment, pairs);
    }

    /**
     * Фильтрация одновреммено по нескольким колонкам
     */
    @Test
    public void testFilterByMultipleColumns() throws Exception {
        prepareProfiles(
                profile(111).shorttermInterests(24),
                profile(222).shorttermInterests(192, 146),
                profile(333).longtermInterests(55)
        );

        Segment segment = segment(
                crypta(
                        cryptaSegment(602, 192),
                        cryptaSegment(601, 55)
                )
        );

        Set<UidPair> pairs = ImmutableSet.of(
                pair(Uid.asYuid("222")),
                pair(Uid.asYuid("333"))
        );

        assertSegment(segment, pairs);
    }

    private void assertSegment(Segment segment, Set<UidPair> pairs) throws Exception {
        segmentatorTestHelper.assertSegmentPairs(pairs, LinkingMode.NONE, Set.of(UidType.YUID), segment);
    }

    private void prepareProfiles(ProfileBuilder... profileBuilders) {
        cryptaProfilesTestHelper.prepareProfiles(profileBuilders);
    }
}
