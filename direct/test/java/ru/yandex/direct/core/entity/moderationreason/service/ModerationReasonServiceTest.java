package ru.yandex.direct.core.entity.moderationreason.service;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository;
import ru.yandex.direct.core.entity.banner.repository.ModerateBannerPagesRepository;
import ru.yandex.direct.core.entity.creative.model.Creative;
import ru.yandex.direct.core.entity.creative.repository.CreativeRepository;
import ru.yandex.direct.core.entity.moderationdiag.model.ModerationDiag;
import ru.yandex.direct.core.entity.moderationdiag.model.ModerationDiagType;
import ru.yandex.direct.core.entity.moderationdiag.service.ModerationDiagService;
import ru.yandex.direct.core.entity.moderationreason.model.BannerAssetType;
import ru.yandex.direct.core.entity.moderationreason.model.ModerationReason;
import ru.yandex.direct.core.entity.moderationreason.model.ModerationReasonDetailed;
import ru.yandex.direct.core.entity.moderationreason.model.ModerationReasonObjectType;
import ru.yandex.direct.core.entity.moderationreason.repository.ModerationReasonRepository;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.entity.moderationreason.model.ModerationReasonObjectType.BANNER;
import static ru.yandex.direct.core.entity.moderationreason.model.ModerationReasonObjectType.PERF_CREATIVE;

@RunWith(MockitoJUnitRunner.class)
public class ModerationReasonServiceTest {
    private static final int SHARD = 88;
    private static final ClientId CLIENT_ID = ClientId.fromLong(737L);
    private static final long OBJECT_ID1 = 3L;
    private static final long OBJECT_ID2 = 7L;
    private static final long PERF_CREATIVE_ID = 11L;
    private static final long DIAG_ID1 = 20001L;
    private static final long DIAG_ID2 = 20002L;
    private static final long DIAG_ID3 = 20003L;
    private static final long PERF_CREATIVE_DIAG_ID = 20004L;
    private static final long ASSET_DIAG_ID1 = 20005L;
    private static final long ASSET_DIAG_ID2 = 20006L;
    private static final List<Long> OBJECT_IDS = asList(OBJECT_ID1, OBJECT_ID2);
    private static final Long BANNER_ID = 111L;
    private static final ModerationReasonObjectType OBJECT_TYPE = ModerationReasonObjectType.CALLOUT;
    private static final ModerationDiag MODERATION_DIAG1 = new ModerationDiag();
    private static final ModerationDiag MODERATION_DIAG2 = new ModerationDiag();
    private static final ModerationDiag MODERATION_DIAG3 = new ModerationDiag();
    private static final ModerationDiag PERF_CREATIVE_MODERATION_DIAG = new ModerationDiag();
    private static final ModerationDiag ASSET_MODERATION_DIAG = new ModerationDiag();
    private static final ModerationReason MODERATION_REASON1 = new ModerationReason()
            .withObjectId(OBJECT_ID1)
            .withReasons(asList(new ModerationReasonDetailed().withId(DIAG_ID1),
                    new ModerationReasonDetailed().withId(DIAG_ID2)))
            .withObjectType(OBJECT_TYPE);
    private static final ModerationReason MODERATION_REASON2 = new ModerationReason()
            .withObjectId(OBJECT_ID2)
            .withReasons(singletonList(new ModerationReasonDetailed().withId(DIAG_ID3)))
            .withObjectType(OBJECT_TYPE);
    private static final ModerationReason PERF_CREATIVE_MODERATION_REASON = new ModerationReason()
            .withObjectId(PERF_CREATIVE_ID)
            .withReasons(singletonList(new ModerationReasonDetailed().withId(PERF_CREATIVE_DIAG_ID)))
            .withObjectType(PERF_CREATIVE);
    private static final ModerationReason ASSET_MODERATION_REASON = new ModerationReason()
            .withObjectId(BANNER_ID)
            .withReasons(singletonList(new ModerationReasonDetailed().withId(DIAG_ID1)))
            .withAssetsReasons(Map.of(
                    BannerAssetType.TITLE, Set.of(ASSET_DIAG_ID1),
                    BannerAssetType.BODY, Set.of(ASSET_DIAG_ID2)
            ))
            .withObjectType(BANNER);

    private ModerationReasonService serviceUnderTest;
    @Mock
    private ModerationReasonRepository moderationReasonRepository;
    @Mock
    private PpcPropertiesSupport ppcPropertiesSupport;
    @Mock
    private ModerationDiagService moderationDiagService;
    @Mock
    private ShardHelper shardHelper;

    @Mock
    private BannerTypedRepository bannerTypedRepository;
    @Mock
    private CreativeRepository creativeRepository;
    @Mock
    private ModerateBannerPagesRepository moderateBannerPagesRepository;

    @Before
    public void setUp() throws Exception {
        moderationDiagService.invalidateAll();
        when(shardHelper.getShardByClientIdStrictly(eq(CLIENT_ID))).thenReturn(SHARD);
        when(moderationDiagService.get(any())).thenReturn(Map.of(
                DIAG_ID1, MODERATION_DIAG1,
                DIAG_ID2, MODERATION_DIAG2,
                DIAG_ID3, MODERATION_DIAG3,
                PERF_CREATIVE_DIAG_ID, PERF_CREATIVE_MODERATION_DIAG,
                ASSET_DIAG_ID1, ASSET_MODERATION_DIAG,
                ASSET_DIAG_ID2, ASSET_MODERATION_DIAG
        ));
        when(moderationReasonRepository.fetchRejected(eq(SHARD), eq(OBJECT_TYPE), eq(OBJECT_IDS))).thenReturn(
                asList(MODERATION_REASON1, MODERATION_REASON2));
        when(moderationReasonRepository.fetchRejected(SHARD, Map.of(PERF_CREATIVE, List.of(PERF_CREATIVE_ID)))).thenReturn(
                List.of(PERF_CREATIVE_MODERATION_REASON));
        when(moderationReasonRepository.fetchRejected(eq(SHARD), eq(BANNER), eq(List.of(BANNER_ID)))).thenReturn(
                List.of(ASSET_MODERATION_REASON));
        when(creativeRepository.getCreatives(SHARD, Set.of(PERF_CREATIVE_ID))).thenReturn(
                List.of(new Creative().withId(PERF_CREATIVE_ID).withClientId(CLIENT_ID.asLong())));
        serviceUnderTest = new ModerationReasonService(shardHelper, moderationReasonRepository, moderationDiagService,
                bannerTypedRepository, creativeRepository, moderateBannerPagesRepository, ppcPropertiesSupport);
    }

    @Test
    public void getRejectReasonDiags_CheckCalls() {
        serviceUnderTest.getRejectReasonDiags(CLIENT_ID, OBJECT_TYPE, OBJECT_IDS);
        verify(shardHelper).getShardByClientIdStrictly(eq(CLIENT_ID));
        verify(moderationDiagService).get(eq(ModerationDiagType.COMMON));
        verify(moderationReasonRepository).fetchRejected(eq(SHARD), eq(OBJECT_TYPE), eq(OBJECT_IDS));
    }

    @Test
    public void getRejectReasonDiags_ReturnsCorrectResult() {
        Map<Long, List<ModerationDiag>> reasons =
                serviceUnderTest.getRejectReasonDiags(CLIENT_ID, OBJECT_TYPE, OBJECT_IDS);

        assertThat(reasons).isEqualTo(ImmutableMap.of(
                OBJECT_ID1, asList(MODERATION_DIAG1, MODERATION_DIAG2),
                OBJECT_ID2, singletonList(MODERATION_DIAG3)
        ));
    }

    @Test
    public void getRejectReasonDiags_PerfCreative() {
        Map<Long, Map<ModerationReasonObjectType, List<ModerationDiag>>> reasons =
                serviceUnderTest.getRejectReasonDiags(CLIENT_ID, Map.of(PERF_CREATIVE, List.of(PERF_CREATIVE_ID)));

        assertThat(reasons).isEqualTo(Map.of(
                PERF_CREATIVE_ID, Map.of(
                        PERF_CREATIVE, List.of(PERF_CREATIVE_MODERATION_DIAG)
                )
        ));
    }

    @Test
    public void getBannerAssetsRejectReasonDiags() {
        Map<Long, Map<BannerAssetType, List<ModerationDiag>>> reasonsByBannerId =
                serviceUnderTest.getBannerAssetsRejectReasonDiags(CLIENT_ID, List.of(BANNER_ID));

        var expected = Map.of(
                BannerAssetType.TITLE, List.of(ASSET_MODERATION_DIAG),
                BannerAssetType.BODY, List.of(ASSET_MODERATION_DIAG));
        assertThat(reasonsByBannerId).containsEntry(BANNER_ID, expected);
    }
}
