package ru.yandex.direct.jobs.campaignstatuscorrect;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.annotation.Nullable;

import one.util.streamex.StreamEx;
import org.assertj.core.api.SoftAssertions;
import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerStatusPostModerate;
import ru.yandex.direct.core.entity.banner.model.CpmAudioBanner;
import ru.yandex.direct.core.entity.banner.model.CpmBanner;
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository;
import ru.yandex.direct.core.entity.banner.type.creative.model.CreativeSize;
import ru.yandex.direct.core.entity.campaign.model.CpmPriceCampaign;
import ru.yandex.direct.core.entity.campaign.model.PriceFlightReasonIncorrect;
import ru.yandex.direct.core.entity.campaign.model.PriceFlightStatusCorrect;
import ru.yandex.direct.core.entity.campaign.model.PriceFlightTargetingsSnapshot;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.creative.model.Creative;
import ru.yandex.direct.core.entity.creative.repository.CreativeRepository;
import ru.yandex.direct.core.entity.pricepackage.model.ViewType;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.jobs.configuration.JobsTest;
import ru.yandex.direct.logicprocessor.processors.campaignstatuscorrect.CampaignStatusCorrectCheckService;
import ru.yandex.direct.logicprocessor.processors.campaignstatuscorrect.PriceCampaignsMinusGeoCheckService;

import static com.google.common.collect.Iterables.getFirst;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static ru.yandex.direct.core.entity.adgroup.service.AdGroupCpmPriceUtils.PRIORITY_DEFAULT;
import static ru.yandex.direct.core.entity.adgroup.service.AdGroupCpmPriceUtils.PRIORITY_SPECIFIC;
import static ru.yandex.direct.core.entity.campaign.service.CampaignWithPricePackageUtils.VIEW_TYPE_TO_CREATIVE_SIZES;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@JobsTest
@ExtendWith(SpringExtension.class)
public class CampaignStatusCorrectCheckServiceTest {

    private static final int SHARD = 1;

    @Autowired
    private DslContextProvider dslContextProvider;

    @Mock
    private CampaignTypedRepository campaignTypedRepository;
    @Mock
    private CampaignRepository campaignRepository;
    @Mock
    private AdGroupRepository adGroupRepository;
    @Mock
    private BannerTypedRepository bannerTypedRepository;
    @Mock
    private CreativeRepository creativeRepository;


    @Mock
    private PriceCampaignsMinusGeoCheckService priceCampaignsMinusGeoCheckService;

    private CampaignStatusCorrectCheckService campaignStatusCorrectCheckService;

    private List<Long> campaignIds = List.of(144L);
    private List<Long> adGroupIds = List.of(145L, 146L, 147L);
    private List<Long> bannerIds = List.of(155L, 166L, 177L, 178L, 179L);
    private List<Long> creativeIds = List.of(188L, 199L, 1101L);
    private Answer<List<CpmPriceCampaign>> campaigns = invocation -> singletonList(new CpmPriceCampaign()
            .withId(campaignIds.get(0))
            .withFlightTargetingsSnapshot(new PriceFlightTargetingsSnapshot()
                    .withViewTypes(asList(ViewType.MOBILE, ViewType.NEW_TAB))
                    .withAllowExpandedDesktopCreative(false))
            .withFlightStatusCorrect(PriceFlightStatusCorrect.NEW)
    );

    private final CreativeSize sigularFormatCreative = new CreativeSize(0L, 0L);
    private final Set<CreativeSize> setWithSigularFormat = Set.of(sigularFormatCreative);

    private final CreativeSize mobileSize =
            getFirst(Objects.requireNonNull(getFirst(VIEW_TYPE_TO_CREATIVE_SIZES.get(ViewType.MOBILE),
                    setWithSigularFormat)), sigularFormatCreative);

    private final CreativeSize newTabSize =
            getFirst(Objects.requireNonNull(getFirst(VIEW_TYPE_TO_CREATIVE_SIZES.get(ViewType.NEW_TAB),
                    setWithSigularFormat)), sigularFormatCreative);


    @BeforeEach
    public void before() {
        initMocks(this);

        campaignStatusCorrectCheckService = new CampaignStatusCorrectCheckService(
                dslContextProvider,
                campaignTypedRepository,
                campaignRepository,
                adGroupRepository,
                bannerTypedRepository,
                creativeRepository, priceCampaignsMinusGeoCheckService);

        when(campaignTypedRepository.getTypedCampaigns(SHARD, campaignIds)).thenAnswer(campaigns);
        when(priceCampaignsMinusGeoCheckService.filterBannersWithoutOverlappingMinusRegions(anyInt(), anyList())).thenAnswer(e -> e.getArgument(1));
    }

    @Test
    public void checkCampaignsStatusCorrect_NotCorrect_NoAdGroups() {
        when(adGroupRepository.getAdGroupIdsByCampaignIds(SHARD, campaignIds)).thenReturn(emptyMap());

        campaignStatusCorrectCheckService.verifyCampaignsStatusCorrect(SHARD, campaignIds);
        checkStatusCorrect(PriceFlightStatusCorrect.NO, PriceFlightReasonIncorrect.NO_DEFAULT_GROUP);
        checkStatusBsSynced(campaignIds);
    }

    @Test
    public void checkCampaignsStatusCorrect_NotCorrectAndNoBsSynced_NoAdGroups() {
        when(campaignTypedRepository.getTypedCampaigns(SHARD, campaignIds)).thenAnswer(invocation -> singletonList(new CpmPriceCampaign()
                .withId(campaignIds.get(0))
                .withFlightStatusCorrect(PriceFlightStatusCorrect.NO)
        ));
        when(adGroupRepository.getAdGroupIdsByCampaignIds(SHARD, campaignIds)).thenReturn(Map.of(campaignIds.get(0),
                emptyList()));

        campaignStatusCorrectCheckService.verifyCampaignsStatusCorrect(SHARD, campaignIds);
        checkStatusCorrect(PriceFlightStatusCorrect.NO, PriceFlightReasonIncorrect.NO_DEFAULT_GROUP);
        checkStatusBsSynced(emptyList());
    }

    @Test
    public void checkCampaignsStatusCorrect_NotCorrect_NoDefaultAdGroup() {
        when(adGroupRepository.getAdGroupIdsByCampaignIds(SHARD, campaignIds)).thenReturn(Map.of(campaignIds.get(0),
                adGroupIds));
        when(adGroupRepository.getAdGroupsPriority(SHARD, adGroupIds)).thenReturn(
                StreamEx.of(adGroupIds)
                        .mapToEntry(id -> PRIORITY_SPECIFIC)
                        .toMap()
        );

        campaignStatusCorrectCheckService.verifyCampaignsStatusCorrect(SHARD, campaignIds);
        checkStatusCorrect(PriceFlightStatusCorrect.NO, PriceFlightReasonIncorrect.NO_DEFAULT_GROUP);
        checkStatusBsSynced(campaignIds);
    }

    @Test
    public void checkCampaignsStatusCorrect_NotCorrect_MoreThanOneDefaultAdGroup() {
        when(adGroupRepository.getAdGroupIdsByCampaignIds(SHARD, campaignIds)).thenReturn(Map.of(campaignIds.get(0),
                adGroupIds));
        when(adGroupRepository.getAdGroupTypesByIds(SHARD, adGroupIds)).thenReturn(
                Map.of(adGroupIds.get(0), AdGroupType.CPM_YNDX_FRONTPAGE));
        when(adGroupRepository.getAdGroupsPriority(SHARD, adGroupIds)).thenReturn(
                StreamEx.of(adGroupIds)
                        .mapToEntry(id -> PRIORITY_DEFAULT)
                        .toMap()
        );

        campaignStatusCorrectCheckService.verifyCampaignsStatusCorrect(SHARD, campaignIds);
        checkStatusCorrect(PriceFlightStatusCorrect.NO, PriceFlightReasonIncorrect.MORE_THAN_ONE_DEFAULT_GROUP);
        checkStatusBsSynced(campaignIds);
    }

    @Test
    public void checkCampaignsStatusCorrect_NotCorrect_DefaultAdGroupNotModerated() {
        when(adGroupRepository.getAdGroupIdsByCampaignIds(SHARD, campaignIds)).thenReturn(Map.of(campaignIds.get(0),
                adGroupIds));
        when(adGroupRepository.getAdGroups(SHARD, singletonList(adGroupIds.get(0)))).thenReturn(singletonList(new AdGroup()
                .withId(adGroupIds.get(0))
                .withType(AdGroupType.CPM_YNDX_FRONTPAGE)
                .withStatusModerate(ru.yandex.direct.core.entity.adgroup.model.StatusModerate.READY)
                .withStatusPostModerate(ru.yandex.direct.core.entity.adgroup.model.StatusPostModerate.YES)));
        when(adGroupRepository.getAdGroupTypesByIds(SHARD, adGroupIds)).thenReturn(
                Map.of(adGroupIds.get(0), AdGroupType.CPM_YNDX_FRONTPAGE));
        when(adGroupRepository.getAdGroupsPriority(SHARD, adGroupIds)).thenReturn(Map.of(adGroupIds.get(0),
                PRIORITY_DEFAULT));

        campaignStatusCorrectCheckService.verifyCampaignsStatusCorrect(SHARD, campaignIds);
        verify(creativeRepository, Mockito.times(0)).getCreativesByBannerIds(anyInt(), any());
        checkStatusCorrect(PriceFlightStatusCorrect.NO, PriceFlightReasonIncorrect.NOT_MODERATED_DEFAULT_GROUP);
        checkStatusBsSynced(campaignIds);
    }

    @Test
    public void checkCampaignsStatusCorrect_NotCorrect_NoBanners() {
        when(adGroupRepository.getAdGroupIdsByCampaignIds(SHARD, campaignIds)).thenReturn(Map.of(campaignIds.get(0),
                adGroupIds));
        when(adGroupRepository.getAdGroups(SHARD, singletonList(adGroupIds.get(0)))).thenReturn(singletonList(new AdGroup()
                .withId(adGroupIds.get(0))
                .withType(AdGroupType.CPM_YNDX_FRONTPAGE)
                .withStatusModerate(ru.yandex.direct.core.entity.adgroup.model.StatusModerate.YES)
                .withStatusPostModerate(ru.yandex.direct.core.entity.adgroup.model.StatusPostModerate.YES)));
        when(adGroupRepository.getAdGroupTypesByIds(SHARD, adGroupIds)).thenReturn(Map.of(adGroupIds.get(0),
                AdGroupType.CPM_YNDX_FRONTPAGE));
        when(adGroupRepository.getAdGroupsPriority(SHARD, adGroupIds)).thenReturn(Map.of(adGroupIds.get(0),
                PRIORITY_DEFAULT));

        campaignStatusCorrectCheckService.verifyCampaignsStatusCorrect(SHARD, campaignIds);
        verify(creativeRepository, Mockito.times(0)).getCreativesByBannerIds(anyInt(), any());
        checkStatusCorrect(PriceFlightStatusCorrect.NO, PriceFlightReasonIncorrect.NO_ACTIVE_BANNERS);
        checkStatusBsSynced(campaignIds);
    }

    @Test
    public void checkCampaignsStatusCorrect_NotCorrect_NoActiveBanners() {
        when(adGroupRepository.getAdGroupIdsByCampaignIds(SHARD, campaignIds)).thenReturn(Map.of(campaignIds.get(0),
                adGroupIds));
        when(adGroupRepository.getAdGroupTypesByIds(SHARD, adGroupIds)).thenReturn(
                Map.of(adGroupIds.get(0), AdGroupType.CPM_YNDX_FRONTPAGE));
        when(adGroupRepository.getAdGroupsPriority(SHARD, adGroupIds)).thenReturn(Map.of(adGroupIds.get(0),
                PRIORITY_DEFAULT));
        when(adGroupRepository.getAdGroups(SHARD, singletonList(adGroupIds.get(0)))).thenReturn(singletonList(new AdGroup()
                .withId(adGroupIds.get(0))
                .withType(AdGroupType.CPM_YNDX_FRONTPAGE)
                .withStatusModerate(ru.yandex.direct.core.entity.adgroup.model.StatusModerate.YES)
                .withStatusPostModerate(ru.yandex.direct.core.entity.adgroup.model.StatusPostModerate.YES)));
        when(bannerTypedRepository.getBannersByGroupIds(SHARD, singletonList(adGroupIds.get(0))))
                .thenReturn(singletonList(
                        new CpmBanner()
                                .withId(bannerIds.get(0))
                                .withStatusShow(false)
                                .withStatusModerate(BannerStatusModerate.YES)
                                .withStatusPostModerate(BannerStatusPostModerate.YES)
                                .withStatusArchived(false)
                ));

        campaignStatusCorrectCheckService.verifyCampaignsStatusCorrect(SHARD, campaignIds);
        verify(bannerTypedRepository, Mockito.times(1))
                .getBannersByGroupIds(SHARD, singletonList(adGroupIds.get(0)));
        verify(creativeRepository, Mockito.times(0))
                .getCreativesByBannerIds(anyInt(), any());
        checkStatusCorrect(PriceFlightStatusCorrect.NO, PriceFlightReasonIncorrect.NO_ACTIVE_BANNERS);
        checkStatusBsSynced(campaignIds);
    }

    @Test
    public void checkCampaignsStatusCorrect_NotCorrect_NotAllRelevantCreatives() {
        when(adGroupRepository.getAdGroupIdsByCampaignIds(SHARD, campaignIds)).thenReturn(Map.of(campaignIds.get(0),
                adGroupIds));
        when(adGroupRepository.getAdGroupTypesByIds(SHARD, adGroupIds)).thenReturn(
                Map.of(adGroupIds.get(0), AdGroupType.CPM_YNDX_FRONTPAGE));
        when(adGroupRepository.getAdGroupsPriority(SHARD, adGroupIds)).thenReturn(Map.of(adGroupIds.get(0),
                PRIORITY_DEFAULT));
        when(adGroupRepository.getAdGroups(SHARD, singletonList(adGroupIds.get(0)))).thenReturn(singletonList(new AdGroup()
                .withId(adGroupIds.get(0))
                .withType(AdGroupType.CPM_YNDX_FRONTPAGE)
                .withStatusModerate(ru.yandex.direct.core.entity.adgroup.model.StatusModerate.YES)
                .withStatusPostModerate(ru.yandex.direct.core.entity.adgroup.model.StatusPostModerate.YES)));
        when(bannerTypedRepository.getBannersByGroupIds(SHARD, singletonList(adGroupIds.get(0))))
                .thenReturn(singletonList(
                        new CpmBanner()
                                .withId(bannerIds.get(0))
                                .withStatusShow(true)
                                .withStatusModerate(BannerStatusModerate.YES)
                                .withStatusPostModerate(BannerStatusPostModerate.YES)
                                .withStatusArchived(false)
                                .withStatusActive(true)
                ));
        when(creativeRepository.getCreativesByBannerIds(SHARD, singletonList(bannerIds.get(0))))
                .thenReturn(Map.of(bannerIds.get(0),
                        new Creative()
                                .withId(creativeIds.get(0))
                                .withWidth(mobileSize.getWidth())
                                .withHeight(mobileSize.getHeight())
                ));

        campaignStatusCorrectCheckService.verifyCampaignsStatusCorrect(SHARD, campaignIds);
        verify(bannerTypedRepository, Mockito.times(1))
                .getBannersByGroupIds(SHARD, singletonList(adGroupIds.get(0)));
        verify(creativeRepository, Mockito.times(1))
                .getCreativesByBannerIds(SHARD, singletonList(bannerIds.get(0)));
        checkStatusCorrect(PriceFlightStatusCorrect.NO, PriceFlightReasonIncorrect.NOT_FULL);
        checkStatusBsSynced(campaignIds);
    }

    @Test
    public void checkCampaignsStatusCorrect_Correct() {
        when(adGroupRepository.getAdGroupIdsByCampaignIds(SHARD, campaignIds)).thenReturn(Map.of(campaignIds.get(0),
                adGroupIds));
        when(adGroupRepository.getAdGroupTypesByIds(SHARD, adGroupIds)).thenReturn(
                Map.of(adGroupIds.get(0), AdGroupType.CPM_YNDX_FRONTPAGE));
        when(adGroupRepository.getAdGroupsPriority(SHARD, adGroupIds)).thenReturn(Map.of(adGroupIds.get(0),
                PRIORITY_DEFAULT));
        when(adGroupRepository.getAdGroups(SHARD, singletonList(adGroupIds.get(0)))).thenReturn(singletonList(new AdGroup()
                .withId(adGroupIds.get(0))
                .withType(AdGroupType.CPM_YNDX_FRONTPAGE)
                .withStatusModerate(ru.yandex.direct.core.entity.adgroup.model.StatusModerate.YES)
                .withStatusPostModerate(ru.yandex.direct.core.entity.adgroup.model.StatusPostModerate.YES)));
        when(bannerTypedRepository.getBannersByGroupIds(SHARD, singletonList(adGroupIds.get(0))))
                .thenReturn(asList(
                        new CpmBanner()
                                .withId(bannerIds.get(0))
                                .withStatusShow(true)
                                .withStatusModerate(BannerStatusModerate.YES)
                                .withStatusPostModerate(BannerStatusPostModerate.YES)
                                .withStatusArchived(false)
                                .withStatusActive(true),
                        new CpmBanner()
                                .withId(bannerIds.get(1))
                                .withStatusShow(true)
                                .withStatusModerate(BannerStatusModerate.YES)
                                .withStatusPostModerate(BannerStatusPostModerate.YES)
                                .withStatusArchived(false)
                                .withStatusActive(true)
                ));
        when(creativeRepository.getCreativesByBannerIds(SHARD, asList(bannerIds.get(0), bannerIds.get(1))))
                .thenReturn(Map.of(bannerIds.get(0),
                        new Creative()
                                .withId(creativeIds.get(0))
                                .withWidth(newTabSize.getWidth())
                                .withHeight(newTabSize.getHeight()),
                        bannerIds.get(1),
                        new Creative()
                                .withId(creativeIds.get(1))
                                .withWidth(mobileSize.getWidth())
                                .withHeight(mobileSize.getHeight())
                ));

        campaignStatusCorrectCheckService.verifyCampaignsStatusCorrect(SHARD, campaignIds);
        checkStatusCorrect(PriceFlightStatusCorrect.YES, null);
        checkStatusBsSynced(campaignIds);
    }

    @Test
    public void checkCampaignsStatusCorrect_Correct_AdGroupNotModeratedWithActiveBanners() {
        when(adGroupRepository.getAdGroupIdsByCampaignIds(SHARD, campaignIds)).thenReturn(Map.of(campaignIds.get(0),
                adGroupIds));
        when(adGroupRepository.getAdGroupTypesByIds(SHARD, adGroupIds)).thenReturn(
                Map.of(adGroupIds.get(0), AdGroupType.CPM_YNDX_FRONTPAGE));
        when(adGroupRepository.getAdGroupsPriority(SHARD, adGroupIds)).thenReturn(Map.of(adGroupIds.get(0),
                PRIORITY_DEFAULT));
        when(adGroupRepository.getAdGroups(SHARD, singletonList(adGroupIds.get(0)))).thenReturn(singletonList(new AdGroup()
                .withId(adGroupIds.get(0))
                .withType(AdGroupType.CPM_YNDX_FRONTPAGE)
                .withStatusModerate(ru.yandex.direct.core.entity.adgroup.model.StatusModerate.READY)
                .withStatusPostModerate(ru.yandex.direct.core.entity.adgroup.model.StatusPostModerate.NO)));
        when(bannerTypedRepository.getBannersByGroupIds(SHARD, singletonList(adGroupIds.get(0))))
                .thenReturn(asList(
                        new CpmBanner()
                                .withId(bannerIds.get(0))
                                .withStatusShow(true)
                                .withStatusModerate(BannerStatusModerate.YES)
                                .withStatusPostModerate(BannerStatusPostModerate.YES)
                                .withStatusArchived(false)
                                .withStatusActive(false),
                        new CpmBanner()
                                .withId(bannerIds.get(1))
                                .withStatusShow(true)
                                .withStatusModerate(BannerStatusModerate.YES)
                                .withStatusPostModerate(BannerStatusPostModerate.YES)
                                .withStatusArchived(false)
                                .withStatusActive(true)
                ));
        when(creativeRepository.getCreativesByBannerIds(SHARD, asList(bannerIds.get(0), bannerIds.get(1))))
                .thenReturn(Map.of(bannerIds.get(0),
                        new Creative()
                                .withId(creativeIds.get(0))
                                .withWidth(newTabSize.getWidth())
                                .withHeight(newTabSize.getHeight()),
                        bannerIds.get(1),
                        new Creative()
                                .withId(creativeIds.get(1))
                                .withWidth(mobileSize.getWidth())
                                .withHeight(mobileSize.getHeight())
                ));

        campaignStatusCorrectCheckService.verifyCampaignsStatusCorrect(SHARD, campaignIds);
        checkStatusCorrect(PriceFlightStatusCorrect.YES, null);
        checkStatusBsSynced(campaignIds);
    }

    private void checkStatusCorrect(PriceFlightStatusCorrect expectedStatusCorrect,
                                    @Nullable PriceFlightReasonIncorrect expectedReasonIncorrect) {
        ArgumentCaptor<Configuration> conf = ArgumentCaptor.forClass(Configuration.class);
        ArgumentCaptor<Long> campaignId = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<PriceFlightStatusCorrect> statusCorrect =
                ArgumentCaptor.forClass(PriceFlightStatusCorrect.class);
        ArgumentCaptor<PriceFlightReasonIncorrect> reasonIncorrect =
                ArgumentCaptor.forClass(PriceFlightReasonIncorrect.class);

        verify(campaignRepository, Mockito.times(1))
                .setCampaignsCpmPriceStatusCorrect(conf.capture(), campaignId.capture(),
                        statusCorrect.capture(), reasonIncorrect.capture());
        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(statusCorrect.getValue()).isEqualTo(expectedStatusCorrect);
        soft.assertThat(reasonIncorrect.getValue()).isEqualTo(expectedReasonIncorrect);
        soft.assertAll();
    }

    private void checkStatusBsSynced(List<Long> expectedCampaignIds) {
        ArgumentCaptor<DSLContext> context = ArgumentCaptor.forClass(DSLContext.class);
        ArgumentCaptor<List<Long>> campaignIds = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<StatusBsSynced> statusBsSynced = ArgumentCaptor.forClass(StatusBsSynced.class);

        verify(campaignRepository, Mockito.times(1))
                .updateStatusBsSynced(context.capture(), campaignIds.capture(), statusBsSynced.capture());
        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(campaignIds.getValue()).isEqualTo(expectedCampaignIds);
        soft.assertThat(statusBsSynced.getValue()).isEqualTo(StatusBsSynced.NO);
    }

    @Test
    public void checkCampaignsStatusCorrect_NotCorrect_CpmVideoAdGroupNotModerated() {
        when(adGroupRepository.getAdGroupIdsByCampaignIds(SHARD, campaignIds)).thenReturn(Map.of(campaignIds.get(0),
                adGroupIds));
        when(adGroupRepository.getAdGroups(SHARD, adGroupIds)).thenReturn(mapList(adGroupIds,
                adGroupId -> new AdGroup()
                        .withId(adGroupId)
                        .withType(AdGroupType.CPM_VIDEO)
                        .withStatusModerate(ru.yandex.direct.core.entity.adgroup.model.StatusModerate.READY)
                        .withStatusPostModerate(ru.yandex.direct.core.entity.adgroup.model.StatusPostModerate.YES)));
        when(adGroupRepository.getAdGroupTypesByIds(SHARD, adGroupIds)).thenReturn(
                Map.of(adGroupIds.get(0), AdGroupType.CPM_VIDEO,
                        adGroupIds.get(1), AdGroupType.CPM_VIDEO,
                        adGroupIds.get(2), AdGroupType.CPM_VIDEO));

        campaignStatusCorrectCheckService.verifyCampaignsStatusCorrect(SHARD, campaignIds);
        checkStatusCorrect(PriceFlightStatusCorrect.NO, PriceFlightReasonIncorrect.NOT_MODERATED_GROUP);
        checkStatusBsSynced(campaignIds);
    }

    @Test
    public void checkCampaignsStatusCorrect_Correct_CpmVideoAdGroupOnly() {
        when(adGroupRepository.getAdGroupIdsByCampaignIds(SHARD, campaignIds)).thenReturn(Map.of(campaignIds.get(0),
                adGroupIds));
        when(adGroupRepository.getAdGroups(SHARD, adGroupIds)).thenReturn(mapList(adGroupIds,
                adGroupId -> new AdGroup()
                        .withId(adGroupId)
                        .withType(AdGroupType.CPM_VIDEO)
                        .withStatusModerate(ru.yandex.direct.core.entity.adgroup.model.StatusModerate.YES)
                        .withStatusPostModerate(ru.yandex.direct.core.entity.adgroup.model.StatusPostModerate.YES)));
        when(adGroupRepository.getAdGroupTypesByIds(SHARD, adGroupIds)).thenReturn(
                Map.of(adGroupIds.get(0), AdGroupType.CPM_VIDEO,
                        adGroupIds.get(1), AdGroupType.CPM_VIDEO,
                        adGroupIds.get(2), AdGroupType.CPM_VIDEO));

        when(bannerTypedRepository.getBannersByGroupIds(SHARD, singletonList(adGroupIds.get(0))))
                .thenReturn(asList(
                        new CpmBanner()
                                .withId(bannerIds.get(0))
                                .withStatusShow(true)
                                .withStatusModerate(BannerStatusModerate.YES)
                                .withStatusPostModerate(BannerStatusPostModerate.YES)
                                .withStatusArchived(false)
                                .withStatusActive(true),
                        new CpmBanner()
                                .withId(bannerIds.get(1))
                                .withStatusShow(true)
                                .withStatusModerate(BannerStatusModerate.YES)
                                .withStatusPostModerate(BannerStatusPostModerate.YES)
                                .withStatusArchived(false)
                                .withStatusActive(true)
                ));
        when(creativeRepository.getCreativesByBannerIds(SHARD, asList(bannerIds.get(0), bannerIds.get(1))))
                .thenReturn(Map.of(bannerIds.get(0),
                        new Creative()
                                .withId(creativeIds.get(0))
                                .withWidth(newTabSize.getWidth())
                                .withHeight(newTabSize.getHeight()),
                        bannerIds.get(1),
                        new Creative()
                                .withId(creativeIds.get(1))
                                .withWidth(mobileSize.getWidth())
                                .withHeight(mobileSize.getHeight())
                ));

        campaignStatusCorrectCheckService.verifyCampaignsStatusCorrect(SHARD, campaignIds);

        checkStatusCorrect(PriceFlightStatusCorrect.YES, null);
        checkStatusBsSynced(campaignIds);
    }

    @Test
    public void checkCampaignsStatusCorrect_Correct_CpmAudioAdGroupOnly() {
        when(adGroupRepository.getAdGroupIdsByCampaignIds(SHARD, campaignIds)).thenReturn(Map.of(campaignIds.get(0),
                adGroupIds));
        when(adGroupRepository.getAdGroups(SHARD, adGroupIds)).thenReturn(mapList(adGroupIds,
                adGroupId -> new AdGroup()
                        .withId(adGroupId)
                        .withType(AdGroupType.CPM_AUDIO)
                        .withStatusModerate(ru.yandex.direct.core.entity.adgroup.model.StatusModerate.YES)
                        .withStatusPostModerate(ru.yandex.direct.core.entity.adgroup.model.StatusPostModerate.YES)));
        when(adGroupRepository.getAdGroupTypesByIds(SHARD, adGroupIds)).thenReturn(
                Map.of(adGroupIds.get(0), AdGroupType.CPM_AUDIO,
                        adGroupIds.get(1), AdGroupType.CPM_AUDIO,
                        adGroupIds.get(2), AdGroupType.CPM_AUDIO));

        when(bannerTypedRepository.getBannersByGroupIds(SHARD, singletonList(adGroupIds.get(0))))
                .thenReturn(asList(
                        new CpmAudioBanner()
                                .withId(bannerIds.get(0))
                                .withStatusShow(true)
                                .withStatusModerate(BannerStatusModerate.YES)
                                .withStatusPostModerate(BannerStatusPostModerate.YES)
                                .withStatusArchived(false)
                                .withStatusActive(true),
                        new CpmAudioBanner()
                                .withId(bannerIds.get(1))
                                .withStatusShow(true)
                                .withStatusModerate(BannerStatusModerate.YES)
                                .withStatusPostModerate(BannerStatusPostModerate.YES)
                                .withStatusArchived(false)
                                .withStatusActive(true)
                ));
        when(creativeRepository.getCreativesByBannerIds(SHARD, asList(bannerIds.get(0), bannerIds.get(1))))
                .thenReturn(Map.of(bannerIds.get(0),
                        new Creative()
                                .withId(creativeIds.get(0))
                                .withWidth(newTabSize.getWidth())
                                .withHeight(newTabSize.getHeight()),
                        bannerIds.get(1),
                        new Creative()
                                .withId(creativeIds.get(1))
                                .withWidth(mobileSize.getWidth())
                                .withHeight(mobileSize.getHeight())
                ));

        campaignStatusCorrectCheckService.verifyCampaignsStatusCorrect(SHARD, campaignIds);

        checkStatusCorrect(PriceFlightStatusCorrect.YES, null);
        checkStatusBsSynced(campaignIds);
    }

    @Test
    public void checkCampaignsStatusCorrect_Correct_CpmVideoAdGroupNullCreativeSize() {
        List<Long> localAdGroupIds = singletonList(adGroupIds.get(0));
        when(adGroupRepository.getAdGroupIdsByCampaignIds(SHARD, campaignIds)).thenReturn(Map.of(campaignIds.get(0),
                localAdGroupIds));
        when(adGroupRepository.getAdGroups(SHARD, localAdGroupIds)).thenReturn(mapList(localAdGroupIds,
                adGroupId -> new AdGroup()
                        .withId(adGroupId)
                        .withType(AdGroupType.CPM_VIDEO)
                        .withStatusModerate(ru.yandex.direct.core.entity.adgroup.model.StatusModerate.YES)
                        .withStatusPostModerate(ru.yandex.direct.core.entity.adgroup.model.StatusPostModerate.YES)));
        when(adGroupRepository.getAdGroupTypesByIds(SHARD, localAdGroupIds)).thenReturn(
                Map.of(localAdGroupIds.get(0), AdGroupType.CPM_VIDEO));

        when(bannerTypedRepository.getBannersByGroupIds(SHARD, localAdGroupIds))
                .thenReturn(asList(
                        new CpmBanner()
                                .withId(bannerIds.get(0))
                                .withStatusShow(true)
                                .withStatusModerate(BannerStatusModerate.YES)
                                .withStatusPostModerate(BannerStatusPostModerate.YES)
                                .withStatusArchived(false)
                                .withStatusActive(true),
                        new CpmBanner()
                                .withId(bannerIds.get(1))
                                .withStatusShow(true)
                                .withStatusModerate(BannerStatusModerate.YES)
                                .withStatusPostModerate(BannerStatusPostModerate.YES)
                                .withStatusArchived(false)
                                .withStatusActive(true)
                ));
        when(creativeRepository.getCreativesByBannerIds(SHARD, asList(bannerIds.get(0), bannerIds.get(1))))
                .thenReturn(Map.of(bannerIds.get(0),
                        new Creative()
                                .withId(creativeIds.get(0))
                                .withWidth(null)
                                .withHeight(null),
                        bannerIds.get(1),
                        new Creative()
                                .withId(creativeIds.get(1))
                                .withWidth(null)
                                .withHeight(null)
                ));

        campaignStatusCorrectCheckService.verifyCampaignsStatusCorrect(SHARD, campaignIds);

        checkStatusCorrect(PriceFlightStatusCorrect.YES, null);
        checkStatusBsSynced(campaignIds);
    }

    @Test
    public void checkCampaignsStatusCorrect_NotCorrect_CpmVideoAdGroupNoActiveBanners() {
        when(adGroupRepository.getAdGroupIdsByCampaignIds(SHARD, campaignIds)).thenReturn(Map.of(campaignIds.get(0),
                adGroupIds));
        when(adGroupRepository.getAdGroups(SHARD, adGroupIds)).thenReturn(mapList(adGroupIds,
                adGroupId -> new AdGroup()
                        .withId(adGroupId)
                        .withStatusModerate(ru.yandex.direct.core.entity.adgroup.model.StatusModerate.YES)
                        .withStatusPostModerate(ru.yandex.direct.core.entity.adgroup.model.StatusPostModerate.YES)));
        when(adGroupRepository.getAdGroupTypesByIds(SHARD, adGroupIds)).thenReturn(
                Map.of(adGroupIds.get(0), AdGroupType.CPM_VIDEO,
                        adGroupIds.get(1), AdGroupType.CPM_YNDX_FRONTPAGE,
                        adGroupIds.get(2), AdGroupType.CPM_VIDEO));

        when(bannerTypedRepository.getBannersByGroupIds(SHARD, singletonList(adGroupIds.get(0))))
                .thenReturn(asList(
                        new CpmBanner()
                                .withId(bannerIds.get(0))
                                .withStatusShow(false)
                                .withStatusModerate(BannerStatusModerate.YES)
                                .withStatusPostModerate(BannerStatusPostModerate.YES)
                                .withStatusArchived(false)
                                .withStatusActive(false),
                        new CpmBanner()
                                .withId(bannerIds.get(1))
                                .withStatusShow(true)
                                .withStatusModerate(BannerStatusModerate.YES)
                                .withStatusPostModerate(BannerStatusPostModerate.YES)
                                .withStatusArchived(true)
                                .withStatusActive(false)
                ));
        when(creativeRepository.getCreativesByBannerIds(SHARD, asList(bannerIds.get(0), bannerIds.get(1))))
                .thenReturn(Map.of(bannerIds.get(0),
                        new Creative()
                                .withId(creativeIds.get(0))
                                .withWidth(newTabSize.getWidth())
                                .withHeight(newTabSize.getHeight()),
                        bannerIds.get(1),
                        new Creative()
                                .withId(creativeIds.get(1))
                                .withWidth(mobileSize.getWidth())
                                .withHeight(mobileSize.getHeight())
                ));

        campaignStatusCorrectCheckService.verifyCampaignsStatusCorrect(SHARD, campaignIds);

        checkStatusCorrect(PriceFlightStatusCorrect.NO, PriceFlightReasonIncorrect.NO_ACTIVE_BANNERS);
        checkStatusBsSynced(campaignIds);
    }

    @Test
    public void checkCampaignsStatusCorrect_Correct_MidexAdGroup() {
        when(adGroupRepository.getAdGroupIdsByCampaignIds(SHARD, campaignIds)).thenReturn(Map.of(campaignIds.get(0),
                adGroupIds));
        when(adGroupRepository.getAdGroups(SHARD, adGroupIds)).thenReturn(mapList(adGroupIds,
                adGroupId -> new AdGroup()
                        .withId(adGroupId)
                        .withStatusModerate(ru.yandex.direct.core.entity.adgroup.model.StatusModerate.READY)
                        .withStatusPostModerate(ru.yandex.direct.core.entity.adgroup.model.StatusPostModerate.NO)));
        when(adGroupRepository.getAdGroupTypesByIds(SHARD, adGroupIds)).thenReturn(
                Map.of(adGroupIds.get(0), AdGroupType.CPM_YNDX_FRONTPAGE,
                        adGroupIds.get(1), AdGroupType.CPM_VIDEO,
                        adGroupIds.get(2), AdGroupType.CPM_VIDEO));

        when(bannerTypedRepository.getBannersByGroupIds(SHARD, singletonList(adGroupIds.get(0))))
                .thenReturn(asList(
                        new CpmBanner()
                                .withId(bannerIds.get(0))
                                .withStatusShow(true)
                                .withStatusModerate(BannerStatusModerate.YES)
                                .withStatusPostModerate(BannerStatusPostModerate.YES)
                                .withStatusArchived(false)
                                .withStatusActive(true),
                        new CpmBanner()
                                .withId(bannerIds.get(1))
                                .withStatusShow(true)
                                .withStatusModerate(BannerStatusModerate.YES)
                                .withStatusPostModerate(BannerStatusPostModerate.YES)
                                .withStatusArchived(false)
                                .withStatusActive(true)
                ));
        when(creativeRepository.getCreativesByBannerIds(SHARD, asList(bannerIds.get(0), bannerIds.get(1))))
                .thenReturn(Map.of(bannerIds.get(0),
                        new Creative()
                                .withId(creativeIds.get(0))
                                .withWidth(newTabSize.getWidth())
                                .withHeight(newTabSize.getHeight()),
                        bannerIds.get(1),
                        new Creative()
                                .withId(creativeIds.get(1))
                                .withWidth(mobileSize.getWidth())
                                .withHeight(mobileSize.getHeight())
                ));

        campaignStatusCorrectCheckService.verifyCampaignsStatusCorrect(SHARD, campaignIds);

        checkStatusCorrect(PriceFlightStatusCorrect.YES, null);
        checkStatusBsSynced(campaignIds);
    }
}
