package ru.yandex.direct.grid.processing.service.aggregatedstatuses;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.core.aggregatedstatuses.FakeAggregatedStatuses;
import ru.yandex.direct.core.entity.aggregatedstatuses.GdEntityLevel;
import ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum;
import ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusReason;
import ru.yandex.direct.core.entity.aggregatedstatuses.ad.AggregatedStatusAdData;
import ru.yandex.direct.core.entity.aggregatedstatuses.adgroup.AggregatedStatusAdGroupData;
import ru.yandex.direct.core.entity.aggregatedstatuses.campaign.AggregatedStatusCampaignData;
import ru.yandex.direct.core.entity.aggregatedstatuses.keyword.AggregatedStatusKeywordData;
import ru.yandex.direct.core.entity.banner.FakeBannerAdGroupRelation;
import ru.yandex.direct.core.entity.moderationreason.model.ModerationReasonObjectType;
import ru.yandex.direct.core.entity.moderationreason.model.ModerationReasonWithDetails;
import ru.yandex.direct.core.entity.moderationreason.service.FakeModerationReasonText;
import ru.yandex.direct.core.entity.moderationreason.service.FakeModerationReasons;
import ru.yandex.direct.dbutil.testing.FakeShardByClient;
import ru.yandex.direct.grid.model.aggregatedstatuses.GdPopupEntityEnum;
import ru.yandex.direct.grid.model.aggregatedstatuses.GdStatusCounter;
import ru.yandex.direct.grid.processing.util.ContextHelper;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class AggregatedStatusesGraphQlServiceTest {
    private AggregatedStatusesGraphQlService aggregatedStatusesGraphQlService;

    @Before
    public void init() {
        final var aggregatedStatusesViewService = new FakeAggregatedStatuses(
                Map.of(
                        3L, new AggregatedStatusCampaignData(GdSelfStatusEnum.STOP_CRIT,
                                GdSelfStatusReason.CAMPAIGN_WAIT_PAYMENT)
                ),
                Map.of(1L, Map.of(
                                1L, new AggregatedStatusAdGroupData(GdSelfStatusEnum.STOP_WARN),
                                2L, new AggregatedStatusAdGroupData(GdSelfStatusEnum.STOP_CRIT)),
                        2L, Map.of(
                                3L, new AggregatedStatusAdGroupData(GdSelfStatusEnum.STOP_CRIT),
                                4L, new AggregatedStatusAdGroupData(GdSelfStatusEnum.STOP_CRIT)),
                        3L, Map.of(
                                5L, new AggregatedStatusAdGroupData(GdSelfStatusEnum.STOP_CRIT,
                                        List.of(GdSelfStatusReason.ADGROUP_ADS_SUSPENDED_BY_USER,
                                                GdSelfStatusReason.CAMPAIGN_WAIT_PAYMENT))
                        )),
                Map.of(
                        1L, new AggregatedStatusAdGroupData(GdSelfStatusEnum.STOP_WARN),
                        2L, new AggregatedStatusAdGroupData(GdSelfStatusEnum.STOP_CRIT),
                        3L, new AggregatedStatusAdGroupData(GdSelfStatusEnum.STOP_CRIT),
                        4L, new AggregatedStatusAdGroupData(GdSelfStatusEnum.STOP_CRIT),
                        5L, new AggregatedStatusAdGroupData(GdSelfStatusEnum.STOP_CRIT,
                                List.of(GdSelfStatusReason.ADGROUP_ADS_SUSPENDED_BY_USER,
                                        GdSelfStatusReason.CAMPAIGN_WAIT_PAYMENT))
                ),
                Map.of(1L, Map.of(
                                1L, new AggregatedStatusAdData(List.of(), GdSelfStatusEnum.STOP_CRIT,
                                        List.of(GdSelfStatusReason.REJECTED_ON_MODERATION)),
                                2L, new AggregatedStatusAdData(List.of(), GdSelfStatusEnum.STOP_CRIT,
                                        List.of(GdSelfStatusReason.REJECTED_ON_MODERATION))),
                        3L, Map.of(3L, new AggregatedStatusAdData(List.of(), GdSelfStatusEnum.STOP_CRIT,
                                GdSelfStatusReason.AD_HAS_REJECTED_SITELINKS)),
                        4L, Map.of(4L, new AggregatedStatusAdData(List.of(), GdSelfStatusEnum.STOP_CRIT,
                                GdSelfStatusReason.AD_HAS_REJECTED_SITELINKS)),
                        5L, Map.of(5L, new AggregatedStatusAdData(List.of(), GdSelfStatusEnum.STOP_CRIT,
                                List.of(GdSelfStatusReason.ADGROUP_ADS_SUSPENDED_BY_USER,
                                        GdSelfStatusReason.CAMPAIGN_WAIT_PAYMENT,
                                        GdSelfStatusReason.REJECTED_ON_MODERATION)))),
                Map.of(
                        1L, Map.of(1L, new AggregatedStatusKeywordData(GdSelfStatusEnum.STOP_CRIT,
                                GdSelfStatusReason.REJECTED_ON_MODERATION)),
                        5L, Map.of(5L, new AggregatedStatusKeywordData(GdSelfStatusEnum.STOP_CRIT,
                                GdSelfStatusReason.REJECTED_ON_MODERATION))),
                Map.of()
        );

        final var shardHelper = new FakeShardByClient(Map.of());
        final var reasons = List.of(new ModerationReasonWithDetails(
                1L, 1L, 1L, ModerationReasonObjectType.BANNER, "text1", "text1", "token1", "comment1", List.of()
        ), new ModerationReasonWithDetails(
                2L, 2L, 1L, ModerationReasonObjectType.PHRASES, "text2", "text2", "token2", "comment2", List.of()
        ), new ModerationReasonWithDetails(
                1L, 1L, 2L, ModerationReasonObjectType.BANNER, "text1", "text1", "token1", "comment1", List.of()
        ), new ModerationReasonWithDetails(
                2L, 2L, 2L, ModerationReasonObjectType.PHRASES, "text2", "text2", "token2", "comment2", List.of()
        ));

        final var moderationReason = new FakeModerationReasons(Map.of(), reasons);
        final var bannerAdGroupRelation = new FakeBannerAdGroupRelation(Map.of(1L, 1L, 2L, 1L,
                3L, 2L, 4L, 2L));

        final var moderationReasonText = new FakeModerationReasonText(Map.of("1", "text1", "2", "text2"));

        aggregatedStatusesGraphQlService = new AggregatedStatusesGraphQlService(aggregatedStatusesViewService,
                shardHelper,
                bannerAdGroupRelation,
                moderationReason,
                moderationReasonText);
    }

    @Test
    public void campaignStatusPopupNew() {
        final var gridGraphQLContext = ContextHelper.buildDefaultContext();
        final var popupData = aggregatedStatusesGraphQlService.campaignStatusPopupNew(gridGraphQLContext, 1L, null,
                null);

        assertThat(popupData.getTotalCounters().get(GdEntityLevel.GROUP), equalTo(2));
        assertThat(popupData.getTotalCounters().get(GdEntityLevel.BANNER), equalTo(2));
        assertThat(popupData.getTotalCounters().get(GdEntityLevel.KEYWORD), equalTo(1));
        assertThat(popupData.getTotalCounters().get(GdEntityLevel.RETARGETING), equalTo(0));

        final var bannersStatus =
                popupData.getStatuses().stream().filter(s -> s.getEntityLevel() == GdEntityLevel.BANNER).findFirst();
        final var keywordsStatus =
                popupData.getStatuses().stream().filter(s -> s.getEntityLevel() == GdEntityLevel.KEYWORD).findFirst();

        assertThat(bannersStatus.get().getCount(), equalTo(2));
        assertThat(bannersStatus.get().getModerationDiags().size(), equalTo(1));
        assertThat(keywordsStatus.get().getModerationDiags().size(), equalTo(1));
    }

    @Test
    public void campaignStatusPopupNewWithoutDuplicates() {
        final var gridGraphQLContext = ContextHelper.buildDefaultContext();
        final var popupData = aggregatedStatusesGraphQlService.campaignStatusPopupNew(gridGraphQLContext, 2L, null,
                null);
        final var statuses = popupData.getStatuses();

        assertThat(statuses.size(), equalTo(2));
        assertThat(statuses.get(0), equalTo(new GdStatusCounter()
                .withEntityType(GdPopupEntityEnum.BANNER)
                .withEntityLevel(GdEntityLevel.BANNER)
                .withStatus(GdSelfStatusEnum.STOP_CRIT)
                .withReasons(List.of(GdSelfStatusReason.AD_HAS_REJECTED_SITELINKS))
                .withModerationDiags(List.of())
                .withCount(2)));
    }

    @Test
    public void bannerStatusPopupNew() {
        final var gridGraphQLContext = ContextHelper.buildDefaultContext();
        final var popupData = aggregatedStatusesGraphQlService.bannerStatusPopupNew(gridGraphQLContext, 1L, null, null);

        assertThat(popupData.getTotalCounters().get(GdEntityLevel.BANNER), equalTo(1));
        assertThat(popupData.getTotalCounters().get(GdEntityLevel.KEYWORD), equalTo(1));

        final var bannersStatus =
                popupData.getStatuses().stream().filter(s -> s.getEntityLevel() == GdEntityLevel.BANNER).findFirst();
        final var keywordsStatus =
                popupData.getStatuses().stream().filter(s -> s.getEntityLevel() == GdEntityLevel.KEYWORD).findFirst();

        System.out.println(popupData);
        assertThat(bannersStatus.isPresent(), equalTo(false));
        assertThat(keywordsStatus.get().getModerationDiags().size(), equalTo(1));
        assertThat(popupData.getCurrentEntityStatus().getModerationDiags().size(), equalTo(1));
        assertThat(popupData.getCurrentEntityStatus().getModerationDiags().get(0).getJson(), equalTo("text1"));
        assertThat(popupData.getCurrentEntityStatus().getReasons().size(), equalTo(1));
    }

    @Test
    public void filterParentReasonsFromChildrenInCampaign() {
        final var gridGraphQLContext = ContextHelper.buildDefaultContext();
        final var popupData = aggregatedStatusesGraphQlService.campaignStatusPopupNew(gridGraphQLContext, 3L, null,
                null);

        assertThat(popupData.getTotalCounters().get(GdEntityLevel.GROUP), equalTo(1));
        assertThat(popupData.getTotalCounters().get(GdEntityLevel.BANNER), equalTo(1));
        assertThat(popupData.getTotalCounters().get(GdEntityLevel.KEYWORD), equalTo(1));

        final var statusByType =
                popupData.getStatuses().stream().collect(Collectors.toMap(GdStatusCounter::getEntityLevel, e -> e));

        assertThat(statusByType.get(GdEntityLevel.GROUP), equalTo(new GdStatusCounter()
                .withEntityType(GdPopupEntityEnum.GROUP)
                .withEntityLevel(GdEntityLevel.GROUP).withStatus(GdSelfStatusEnum.STOP_CRIT)
                .withReasons(List.of(GdSelfStatusReason.ADGROUP_ADS_SUSPENDED_BY_USER))
                .withModerationDiags(List.of())
                .withCount(1)));
        assertThat(statusByType.get(GdEntityLevel.BANNER), equalTo(new GdStatusCounter()
                .withEntityType(GdPopupEntityEnum.BANNER)
                .withEntityLevel(GdEntityLevel.BANNER)
                .withStatus(GdSelfStatusEnum.STOP_CRIT)
                .withReasons(List.of(GdSelfStatusReason.REJECTED_ON_MODERATION))
                .withModerationDiags(List.of())
                .withCount(1)));
        assertThat(statusByType.get(GdEntityLevel.KEYWORD), equalTo(new GdStatusCounter()
                .withEntityType(GdPopupEntityEnum.KEYWORD)
                .withEntityLevel(GdEntityLevel.KEYWORD)
                .withStatus(GdSelfStatusEnum.STOP_CRIT)
                .withReasons(List.of(GdSelfStatusReason.REJECTED_ON_MODERATION))
                .withModerationDiags(List.of())
                .withCount(1)));
    }

    @Test
    public void filterParentReasonsFromChildrenInGroup() {
        final var gridGraphQLContext = ContextHelper.buildDefaultContext();
        final var popupData = aggregatedStatusesGraphQlService.adgroupStatusPopupNew(gridGraphQLContext, 5L, null,
                null);

        assertThat(popupData.getTotalCounters().get(GdEntityLevel.BANNER), equalTo(1));
        assertThat(popupData.getTotalCounters().get(GdEntityLevel.KEYWORD), equalTo(1));

        final var statusByType =
                popupData.getStatuses().stream().collect(Collectors.toMap(GdStatusCounter::getEntityLevel, e -> e));

        assertThat(statusByType.get(GdEntityLevel.BANNER), equalTo(new GdStatusCounter()
                .withEntityType(GdPopupEntityEnum.BANNER)
                .withEntityLevel(GdEntityLevel.BANNER)
                .withStatus(GdSelfStatusEnum.STOP_CRIT)
                .withReasons(List.of(GdSelfStatusReason.REJECTED_ON_MODERATION))
                .withModerationDiags(List.of())
                .withCount(1)));
        assertThat(statusByType.get(GdEntityLevel.KEYWORD), equalTo(new GdStatusCounter()
                .withEntityType(GdPopupEntityEnum.KEYWORD)
                .withEntityLevel(GdEntityLevel.KEYWORD)
                .withStatus(GdSelfStatusEnum.STOP_CRIT)
                .withReasons(List.of(GdSelfStatusReason.REJECTED_ON_MODERATION))
                .withModerationDiags(List.of())
                .withCount(1)));
    }
}
