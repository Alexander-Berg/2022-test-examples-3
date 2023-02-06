package ru.yandex.direct.web.entity.inventori.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import one.util.streamex.StreamEx;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.campaign.service.CampaignWithBrandSafetyService;
import ru.yandex.direct.core.entity.creative.repository.CreativeRepository;
import ru.yandex.direct.core.entity.crypta.repository.CryptaSegmentRepository;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.inventori.service.CampaignInfoCollector;
import ru.yandex.direct.core.entity.pricepackage.service.PricePackageService;
import ru.yandex.direct.core.entity.retargeting.repository.RetargetingConditionRepository;
import ru.yandex.direct.core.entity.user.service.UserService;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.inventori.InventoriClient;
import ru.yandex.direct.inventori.model.request.BlockSize;
import ru.yandex.direct.inventori.model.request.GroupType;
import ru.yandex.direct.inventori.model.request.PlatformCorrections;
import ru.yandex.direct.inventori.model.request.Target;
import ru.yandex.direct.inventori.model.response.ForecastResponse;
import ru.yandex.direct.web.core.entity.inventori.model.MobileOsTypeWeb;
import ru.yandex.direct.web.core.entity.inventori.model.PlatformCorrectionsWeb;
import ru.yandex.direct.web.core.entity.inventori.model.ReachRecommendationResult;
import ru.yandex.direct.web.core.entity.inventori.model.ReachRequest;
import ru.yandex.direct.web.core.entity.inventori.model.VideoCreativeWeb;
import ru.yandex.direct.web.core.entity.inventori.service.CryptaService;
import ru.yandex.direct.web.core.entity.inventori.service.InventoriService;
import ru.yandex.direct.web.core.entity.inventori.service.InventoriWebService;
import ru.yandex.direct.web.core.security.DirectWebAuthenticationSource;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static ru.yandex.direct.core.entity.inventori.service.InventoriServiceCore.ALLOWED_BLOCK_SIZES;
import static ru.yandex.direct.core.entity.inventori.service.InventoriServiceCore.ALLOWED_BLOCK_SIZES_FOR_CPM_GEOPRODUCT;
import static ru.yandex.direct.core.entity.inventori.service.InventoriServiceCore.ALLOWED_BLOCK_SIZES_FOR_FRONTPAGE_DESKTOP;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@Ignore //todo выключил в рамках рефакторинга, переделать или удалить в рамках DIRECT-104384
public class InventoriWebServiceConvertRequestTest {

    @Autowired
    private RetargetingConditionRepository retargetingConditionRepository;

    @Autowired
    private CampaignWithBrandSafetyService campaignWithBrandSafetyService;

    @Autowired
    private AdGroupRepository adGroupRepository;

    @Autowired
    private CryptaSegmentRepository cryptaSegmentRepository;

    private InventoriWebService inventoriWebService;

    @Mock
    private ShardHelper shardHelper;

    @Mock
    private InventoriClient inventoriClient;

    @Mock
    private CryptaService cryptaService;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private DirectWebAuthenticationSource authenticationSource;

    @Mock
    private UserService userService;

    @Mock
    private CampaignInfoCollector campaignInfoCollector;

    @Mock
    private InventoriService inventoriService;

    @Mock
    private CreativeRepository creativeRepository;

    @Mock
    PricePackageService pricePackageService;

    @Mock
    CampaignRepository campaignRepository;

    @Mock
    private FeatureService featureService;

    @Before
    public void setUp() {
        initMocks(this);
        inventoriWebService = new InventoriWebService(shardHelper, inventoriClient, cryptaService,
                authenticationSource, userService, campaignInfoCollector, inventoriService, campaignRepository, pricePackageService,
                featureService, retargetingConditionRepository, campaignWithBrandSafetyService, adGroupRepository, cryptaSegmentRepository);

        when(cryptaService.getSegmentMap())
                .thenReturn(emptyMap());
        when(inventoriClient.getForecast(anyString(), any(), any(), any(), any(), any()))
                .thenReturn(new ForecastResponse().withReach(0L));
        when(creativeRepository.getCreativesByPerformanceAdGroupIds(anyInt(), any(ClientId.class), anyCollection(), anyCollection()))
                .thenReturn(emptyMap());
    }

    @Test
    public void shouldSendAllAllowedBlockSizesOnNullInputBlockSizesAndNullVideoCreatives() {
        ReachRequest nullBlockSizesRequest = new ReachRequest();
        inventoriWebService.getReachForecast(nullBlockSizesRequest);

        ArgumentCaptor<Target> argument = ArgumentCaptor.forClass(Target.class);
        verify(inventoriClient, times(2)).getForecast(anyString(), argument.capture(), any(), any(), any(), any());

        List<BlockSize> basicForecasRequestBlockSizes = argument.getAllValues().get(0).getBlockSizes();
        List<BlockSize> detailedForecastRequestBlockSizes = argument.getAllValues().get(1).getBlockSizes();

        assertTrue(basicForecasRequestBlockSizes.containsAll(ALLOWED_BLOCK_SIZES));
        assertTrue(detailedForecastRequestBlockSizes.containsAll(ALLOWED_BLOCK_SIZES));
    }

    @Test
    public void shouldSendNullBlockSizesWhenHasVideoCreatives() {
        ReachRequest request = new ReachRequest(null, null, null, singletonList(new VideoCreativeWeb(60)), null, null,
                null, null, null, GroupType.VIDEO, null);
        inventoriWebService.getReachForecast(request);

        ArgumentCaptor<Target> argument = ArgumentCaptor.forClass(Target.class);
        verify(inventoriClient, times(2)).getForecast(anyString(), argument.capture(), any(), any(), any(), any());

        List<BlockSize> basicForecasRequestBlockSizes = argument.getAllValues().get(0).getBlockSizes();
        List<BlockSize> detailedForecastRequestBlockSizes = argument.getAllValues().get(1).getBlockSizes();

        assertThat(basicForecasRequestBlockSizes, nullValue());
        assertThat(detailedForecastRequestBlockSizes, nullValue());
    }

    @Test
    public void shouldMakeRecommendationForEachAllowedBlockSizeAndInputBlockSizes() {
        when(inventoriClient.getForecast(anyString(), any(), any(), any(), any(), any()))
                .thenReturn(new ForecastResponse().withReach(1000000L));

        BlockSize allowedBlockSize = ALLOWED_BLOCK_SIZES.iterator().next();
        ru.yandex.direct.web.core.entity.inventori.model.BlockSize inputBlockSize =
                new ru.yandex.direct.web.core.entity.inventori.model.BlockSize(allowedBlockSize.getWidth(),
                        allowedBlockSize.getHeight());

        ReachRequest singleAllowedBlockSizeRequest = new ReachRequest()
                .withBlockSizes(singletonList(inputBlockSize));
        inventoriWebService.getReachRecommendation(singleAllowedBlockSizeRequest);
        ArgumentCaptor<Target> argument = ArgumentCaptor.forClass(Target.class);
        verify(inventoriClient, times(ALLOWED_BLOCK_SIZES.size()))
                .getForecast(anyString(), argument.capture(), any(), any(), any(), any());

        assertTrue(argument.getAllValues().stream()
                // first detailed forecast request with single block size
                .skip(1)
                // all other requests with 2 block sizes
                // one of which is the input one
                .allMatch(q -> q.getBlockSizes().size() == 2 && q.getBlockSizes().contains(allowedBlockSize)));
    }

    @Test
    public void getReachForecast_BlockSizesIsNull_VideoCreativesIsNull_AllAllowedBlocksSent() {
        ReachRequest request = new ReachRequest(null, null, null, null, null, null, null, null, null, null, null);
        inventoriWebService.getReachForecast(request);

        ArgumentCaptor<Target> argument = ArgumentCaptor.forClass(Target.class);
        verify(inventoriClient, times(2)).getForecast(anyString(), argument.capture(), any(), any(), any(), any());

        List<BlockSize> basicForecastRequestBlockSizes = argument.getAllValues().get(0).getBlockSizes();
        List<BlockSize> detailedForecastRequestBlockSizes = argument.getAllValues().get(1).getBlockSizes();

        assertThat(new HashSet<>(basicForecastRequestBlockSizes), is(ALLOWED_BLOCK_SIZES));
        assertThat(new HashSet<>(detailedForecastRequestBlockSizes), is(ALLOWED_BLOCK_SIZES));
    }

    @Test
    public void getReachForecast_BlockSizesIsEmpty_VideoCreativesIsNull_AllAllowedBlocksSent() {
        ReachRequest request = new ReachRequest(null, null, emptyList(), null, null, null, null, null, null, null,
                null);
        inventoriWebService.getReachForecast(request);

        ArgumentCaptor<Target> argument = ArgumentCaptor.forClass(Target.class);
        verify(inventoriClient, times(2)).getForecast(anyString(), argument.capture(), any(), any(), any(), any());

        List<BlockSize> basicForecastRequestBlockSizes = argument.getAllValues().get(0).getBlockSizes();
        List<BlockSize> detailedForecastRequestBlockSizes = argument.getAllValues().get(1).getBlockSizes();

        assertThat(new HashSet<>(basicForecastRequestBlockSizes), is(ALLOWED_BLOCK_SIZES));
        assertThat(new HashSet<>(detailedForecastRequestBlockSizes), is(ALLOWED_BLOCK_SIZES));
    }

    @Test
    public void getReachForecast_BlockSizesIsNotEmpty_VideoCreativesIsNull_AllAllowedBlocksSentForBasicForecast() {
        Set<BlockSize> blockSizes = ImmutableSet.of(new BlockSize(300, 600), new BlockSize(336, 280));

        ReachRequest request = new ReachRequest(null, null,
                StreamEx.of(blockSizes)
                        .map(this::convertToCoreBlockSize)
                        .toList(),
                null, null, null, null, null, null, GroupType.BANNER, null);
        inventoriWebService.getReachForecast(request);

        ArgumentCaptor<Target> argument = ArgumentCaptor.forClass(Target.class);
        verify(inventoriClient, times(2)).getForecast(anyString(), argument.capture(), any(), any(), any(), any());

        List<BlockSize> basicForecastRequestBlockSizes = argument.getAllValues().get(0).getBlockSizes();
        List<BlockSize> detailedForecastRequestBlockSizes = argument.getAllValues().get(1).getBlockSizes();

        assertThat(new HashSet<>(detailedForecastRequestBlockSizes), is(blockSizes));
        assertThat(new HashSet<>(basicForecastRequestBlockSizes), is(ALLOWED_BLOCK_SIZES));
    }

    @Test
    public void getReachForecast_WithGroupCorrections_GroupCorrectionsNotSentForBasicForecast() {
        ReachRequest request = new ReachRequest(null, null, null, null, null, null, null, null,
                new PlatformCorrectionsWeb(100, 100, MobileOsTypeWeb.ANDROID),
                null, null);
        inventoriWebService.getReachForecast(request);

        ArgumentCaptor<Target> argument = ArgumentCaptor.forClass(Target.class);
        verify(inventoriClient, times(2)).getForecast(anyString(), argument.capture(), any(), any(), any(), any());

        PlatformCorrections basicForecastRequestCorrections = argument.getAllValues().get(0).getPlatformCorrections();
        PlatformCorrections detailedForecastRequestCorrections = argument.getAllValues().get(1).getPlatformCorrections();

        assertThat(basicForecastRequestCorrections, nullValue());
        assertThat(detailedForecastRequestCorrections, notNullValue());
    }

    @Test
    public void getReachForecast_WithNoGroupType_BannerGroupTypeSent() {
        ReachRequest request = new ReachRequest(null, null, null, null, null, null, null, null, null, null, null);
        inventoriWebService.getReachForecast(request);

        ArgumentCaptor<Target> argument = ArgumentCaptor.forClass(Target.class);
        verify(inventoriClient, times(2)).getForecast(anyString(), argument.capture(), any(), any(), any(), any());

        GroupType basicForecastRequestGroupType = argument.getAllValues().get(0).getGroupType();
        GroupType detailedForecastRequestGroupType = argument.getAllValues().get(1).getGroupType();

        assertThat(basicForecastRequestGroupType, is(GroupType.BANNER));
        assertThat(detailedForecastRequestGroupType, is(GroupType.BANNER));
    }

    @Test
    public void getReachForecast_WithIndoorGroupType_IndoorGroupTypeSent() {
        ReachRequest request = new ReachRequest(null, null, null, null, null, null, null, null, null, GroupType.INDOOR,
                null);
        inventoriWebService.getReachForecast(request);

        ArgumentCaptor<Target> argument = ArgumentCaptor.forClass(Target.class);
        verify(inventoriClient, times(2)).getForecast(anyString(), argument.capture(), any(), any(), any(), any());

        GroupType basicForecastRequestGroupType = argument.getAllValues().get(0).getGroupType();
        GroupType detailedForecastRequestGroupType = argument.getAllValues().get(1).getGroupType();

        assertThat(basicForecastRequestGroupType, is(GroupType.INDOOR));
        assertThat(detailedForecastRequestGroupType, is(GroupType.INDOOR));
    }

    @Test
    public void getReachForecast_WithBannerGroupType_CpmYndxFrontpageCampaign_MainPageGroupTypeSent() {
        ReachRequest request = new ReachRequest(null, null, null, null, null, null, 1L, null, null, GroupType.BANNER,
                null);
        inventoriWebService.getReachForecast(request);

        ArgumentCaptor<Target> argument = ArgumentCaptor.forClass(Target.class);
        verify(inventoriClient, times(2)).getForecast(anyString(), argument.capture(), any(), any(), any(), any());

        GroupType basicForecastRequestGroupType = argument.getAllValues().get(0).getGroupType();
        GroupType detailedForecastRequestGroupType = argument.getAllValues().get(1).getGroupType();

        assertThat(basicForecastRequestGroupType, is(GroupType.MAIN_PAGE_AND_NTP));
        assertThat(detailedForecastRequestGroupType, is(GroupType.MAIN_PAGE_AND_NTP));
    }

    @Test
    public void getReachForecast_WithGroupTypeVideo_WithVideoCreatives_VideoGroupTypeSent() {
        ReachRequest request = new ReachRequest(null, null, null,
                singletonList(new VideoCreativeWeb()), null, null, null,
                null, null, GroupType.VIDEO, null);
        inventoriWebService.getReachForecast(request);

        ArgumentCaptor<Target> argument = ArgumentCaptor.forClass(Target.class);
        verify(inventoriClient, times(2)).getForecast(anyString(), argument.capture(), any(), any(), any(), any());

        GroupType basicForecastRequestGroupType = argument.getAllValues().get(0).getGroupType();
        GroupType detailedForecastRequestGroupType = argument.getAllValues().get(1).getGroupType();

        assertThat(basicForecastRequestGroupType, is(GroupType.VIDEO));
        assertThat(detailedForecastRequestGroupType, is(GroupType.VIDEO));
    }

    @Test
    public void getReachForecast_VideoCreativesIsEmpty_BlockSizesIsNull_AllAllowedVideoCreativesSent() {
        ReachRequest request = new ReachRequest().withVideoCreatives(emptyList());
        inventoriWebService.getReachForecast(request);

        ArgumentCaptor<Target> argument = ArgumentCaptor.forClass(Target.class);
        verify(inventoriClient, times(2)).getForecast(anyString(), argument.capture(), any(), any(), any(), any());

        List<ru.yandex.direct.inventori.model.request.VideoCreative> basicForecastVideoCreatives =
                argument.getAllValues().get(0).getVideoCreatives();
        List<ru.yandex.direct.inventori.model.request.VideoCreative> detailedForecastVideoCreatives =
                argument.getAllValues().get(1).getVideoCreatives();

        assertNull(basicForecastVideoCreatives);
        assertNull(detailedForecastVideoCreatives);
    }

    @Test
    public void getReachForecast_VideoCreativesIsNotEmpty_BlockSizesIsNull_AllAllowedVideoCreativesSent() {
        int videoDuration = 60;

        ReachRequest request = new ReachRequest()
                .withGroupType(GroupType.VIDEO)
                .withVideoCreatives(singletonList(new VideoCreativeWeb(videoDuration)));
        inventoriWebService.getReachForecast(request);

        ArgumentCaptor<Target> argument = ArgumentCaptor.forClass(Target.class);
        verify(inventoriClient, times(2)).getForecast(anyString(), argument.capture(), any(), any(), any(), any());

        List<ru.yandex.direct.inventori.model.request.VideoCreative> basicForecastVideoCreatives =
                argument.getAllValues().get(0).getVideoCreatives();
        List<ru.yandex.direct.inventori.model.request.VideoCreative> detailedForecastVideoCreatives =
                argument.getAllValues().get(1).getVideoCreatives();

        assertNull(basicForecastVideoCreatives);
        assertThat(detailedForecastVideoCreatives, is(singletonList(buildInventoriVideoCreative(videoDuration))));
    }

    @Test
    public void getReachForecast_WithVideoGroupType_VideoCreativesAndBlockSizesBothNotNull_BlockSizesAndVideoCreativesInTargetAreCorrect() {
        ReachRequest request = new ReachRequest()
                .withGroupType(GroupType.VIDEO)
                .withVideoCreatives(emptyList())
                .withBlockSizes(emptyList());
        inventoriWebService.getReachForecast(request);

        ArgumentCaptor<Target> argument = ArgumentCaptor.forClass(Target.class);
        verify(inventoriClient, times(2)).getForecast(anyString(), argument.capture(), any(), any(), any(), any());

        List<BlockSize> basicForecastBlockSizes = argument.getAllValues().get(0).getBlockSizes();
        List<ru.yandex.direct.inventori.model.request.VideoCreative> basicForecastVideoCreatives =
                argument.getAllValues().get(0).getVideoCreatives();

        List<BlockSize> detailedForecastBlockSizes = argument.getAllValues().get(1).getBlockSizes();
        List<ru.yandex.direct.inventori.model.request.VideoCreative> detailedForecastVideoCreatives =
                argument.getAllValues().get(1).getVideoCreatives();

        assertNull(basicForecastBlockSizes);
        assertNull(detailedForecastBlockSizes);
        assertNull(basicForecastVideoCreatives);
        assertThat(detailedForecastVideoCreatives, empty());
    }

    @Test
    public void getReachRecommendation_BlockSizesContainsNoValidFormat_ValidFormatsWithNullIncreaseValuesReturned() {
        Long campaignId = 1L;

        ReachRequest request = new ReachRequest()
                .withCampaignId(campaignId)
                .withGroupType(GroupType.MAIN_PAGE_AND_NTP)
                .withBlockSizes(mapList(ALLOWED_BLOCK_SIZES_FOR_FRONTPAGE_DESKTOP, this::convertToCoreBlockSize));

        ReachRecommendationResult actualResult = inventoriWebService.getReachRecommendation(request);

        assertThat(actualResult.getBannerFormatIncreasePercent(), empty());
    }

    @Test
    public void getReachForecast_BlockSizesIsNotEmpty_HasAdaptiveCreative_AllBlockSizesUsedInRequests() {
        Set<BlockSize> blockSizes = ImmutableSet.of(new BlockSize(300, 600), new BlockSize(336, 280));

        ReachRequest request = new ReachRequest(null, null,
                StreamEx.of(blockSizes)
                        .map(this::convertToCoreBlockSize)
                        .toList(),
                null, null, null, null, null, null, GroupType.BANNER, null)
                .withHasAdaptiveCreative(true);
        inventoriWebService.getReachForecast(request);

        ArgumentCaptor<Target> argument = ArgumentCaptor.forClass(Target.class);
        verify(inventoriClient, times(2)).getForecast(anyString(), argument.capture(), any(), any(), any(), any());

        List<BlockSize> basicForecastRequestBlockSizes = argument.getAllValues().get(0).getBlockSizes();
        List<BlockSize> detailedForecastRequestBlockSizes = argument.getAllValues().get(1).getBlockSizes();

        assertThat(new HashSet<>(basicForecastRequestBlockSizes), is(ALLOWED_BLOCK_SIZES));
        assertThat(new HashSet<>(detailedForecastRequestBlockSizes), is(ALLOWED_BLOCK_SIZES));
    }

    @Test
    public void getReachRecommendation_BlockSizesIsNotEmpty_HasAdaptiveCreative_BannerFormatIncreasePercentIsEmpty() {
        Set<BlockSize> blockSizes = ImmutableSet.of(new BlockSize(300, 600), new BlockSize(336, 280));

        ReachRequest request = new ReachRequest(null, null,
                StreamEx.of(blockSizes)
                        .map(this::convertToCoreBlockSize)
                        .toList(),
                null, null, null, null, null, null, GroupType.BANNER, null)
                .withHasAdaptiveCreative(true);

        ReachRecommendationResult actual = inventoriWebService.getReachRecommendation(request);

        assertThat(actual.getBannerFormatIncreasePercent(), empty());
    }

    @Test
    public void getReach_ForBannerInMetroGroupType_BannerInMetroGroupTypeSentInRequest() {
        ReachRequest request = new ReachRequest(null, null, emptyList(), null, null, null, null, null, null,
                GroupType.BANNER_IN_GEO_APPS, null);
        inventoriWebService.getReachForecast(request);

        ArgumentCaptor<Target> argument = ArgumentCaptor.forClass(Target.class);
        verify(inventoriClient, times(2)).getForecast(anyString(), argument.capture(), any(), any(), any(), any());

        GroupType basicForecastRequestGroupType = argument.getAllValues().get(0).getGroupType();
        GroupType detailedForecastRequestGroupType = argument.getAllValues().get(1).getGroupType();

        assertThat(basicForecastRequestGroupType, is(GroupType.BANNER_IN_GEO_APPS));
        assertThat(detailedForecastRequestGroupType, is(GroupType.BANNER_IN_GEO_APPS));
    }

    @Test
    public void getReach_ForGeoproductGroupType_BannerInMetroGroupTypeSentInRequest() {
        ReachRequest request = new ReachRequest(null, null, emptyList(), null, null, null, null, null, null,
                GroupType.GEOPRODUCT, null);
        inventoriWebService.getReachForecast(request);

        ArgumentCaptor<Target> argument = ArgumentCaptor.forClass(Target.class);
        verify(inventoriClient, times(2)).getForecast(anyString(), argument.capture(), any(), any(), any(), any());

        GroupType basicForecastRequestGroupType = argument.getAllValues().get(0).getGroupType();
        GroupType detailedForecastRequestGroupType = argument.getAllValues().get(1).getGroupType();

        assertThat(basicForecastRequestGroupType, is(GroupType.BANNER_IN_GEO_APPS));
        assertThat(detailedForecastRequestGroupType, is(GroupType.BANNER_IN_GEO_APPS));
    }

    @Test
    public void getReachForecast_CpmGeoproductAdGroupWithNoCreatives_OnlyAllowedBlockSizesUsedInRequests() {
        ReachRequest request = new ReachRequest(null, null, emptyList(),
                null, null, null, null, null, null, GroupType.BANNER_IN_GEO_APPS, null);
        inventoriWebService.getReachForecast(request);

        ArgumentCaptor<Target> argument = ArgumentCaptor.forClass(Target.class);
        verify(inventoriClient, times(2)).getForecast(anyString(), argument.capture(), any(), any(), any(), any());

        List<BlockSize> basicForecastRequestBlockSizes = argument.getAllValues().get(0).getBlockSizes();
        List<BlockSize> detailedForecastRequestBlockSizes = argument.getAllValues().get(1).getBlockSizes();

        assertThat(new HashSet<>(basicForecastRequestBlockSizes), is(ALLOWED_BLOCK_SIZES_FOR_CPM_GEOPRODUCT));
        assertThat(new HashSet<>(detailedForecastRequestBlockSizes), is(ALLOWED_BLOCK_SIZES_FOR_CPM_GEOPRODUCT));
    }

    @Test
    public void getReach_ForBannerInGeoApps_WithTargetTags_TargetTagsSentInRequest() {
        ReachRequest request = new ReachRequest(null, null, emptyList(), null, null, null, null, null, null,
                GroupType.BANNER_IN_GEO_APPS, List.of("metro-app", "app-navi"));

        inventoriWebService.getReachForecast(request);

        ArgumentCaptor<Target> argument = ArgumentCaptor.forClass(Target.class);
        verify(inventoriClient, times(2)).getForecast(anyString(), argument.capture(), any(), any(), any(), any());

        List<String> basicForecastRequestTargetTags = argument.getAllValues().get(0).getTargetTags();
        List<String> detailedForecastRequestTargetTags = argument.getAllValues().get(1).getTargetTags();

        assertThat(basicForecastRequestTargetTags, containsInAnyOrder("metro-app", "app-navi"));
        assertThat(detailedForecastRequestTargetTags, empty());
    }

    @Test
    public void getReach_ForGeoproductGroupType_TargetTagsSentInRequest() {
        ReachRequest request = new ReachRequest(null, null, emptyList(), null, null, null, null, null, null,
                GroupType.GEOPRODUCT, List.of("metro-app", "app-navi"));
        inventoriWebService.getReachForecast(request);

        ArgumentCaptor<Target> argument = ArgumentCaptor.forClass(Target.class);
        verify(inventoriClient, times(2)).getForecast(anyString(), argument.capture(), any(), any(), any(), any());

        List<String> basicForecastRequestTargetTags = argument.getAllValues().get(0).getTargetTags();
        List<String> detailedForecastRequestTargetTags = argument.getAllValues().get(1).getTargetTags();

        assertThat(basicForecastRequestTargetTags, containsInAnyOrder("metro-app", "app-navi"));
        assertThat(detailedForecastRequestTargetTags, empty());
    }

    @Test
    public void getReachRecommendation_CpmGeoproductAdGroupWithValidCreative_NoRecommendationReturned() {
        Set<BlockSize> blockSizes = ImmutableSet.of(new BlockSize(640, 100));

        ReachRequest request = new ReachRequest(null, null,
                StreamEx.of(blockSizes)
                        .map(this::convertToCoreBlockSize)
                        .toList(),
                null, null, null, null, null, null, GroupType.BANNER_IN_GEO_APPS, null);
        inventoriWebService.getReachRecommendation(request);

        ArgumentCaptor<Target> argument = ArgumentCaptor.forClass(Target.class);
        verify(inventoriClient, times(1)).getForecast(anyString(), argument.capture(), any(), any(), any(), any());

        List<BlockSize> basicForecastRequestBlockSizes = argument.getAllValues().get(0).getBlockSizes();

        assertThat(new HashSet<>(basicForecastRequestBlockSizes), is(ALLOWED_BLOCK_SIZES_FOR_CPM_GEOPRODUCT));
    }

    @Test
    public void getReachRecommendation_CpmGeoproductAdGroupWithNoCreative_NoRecommendationReturned() {
        ReachRequest request = new ReachRequest(null, null,
                emptyList(), null, null, null, null, null, null, GroupType.BANNER_IN_GEO_APPS, null);
        inventoriWebService.getReachRecommendation(request);

        ArgumentCaptor<Target> argument = ArgumentCaptor.forClass(Target.class);
        verify(inventoriClient, times(1)).getForecast(anyString(), argument.capture(), any(), any(), any(), any());

        List<BlockSize> basicForecastRequestBlockSizes = argument.getAllValues().get(0).getBlockSizes();

        assertThat(new HashSet<>(basicForecastRequestBlockSizes), is(ALLOWED_BLOCK_SIZES_FOR_CPM_GEOPRODUCT));
    }

    private ru.yandex.direct.web.core.entity.inventori.model.BlockSize convertToCoreBlockSize(BlockSize blockSize) {
        return new ru.yandex.direct.web.core.entity.inventori.model.BlockSize(blockSize.getWidth(), blockSize.getHeight());
    }

    private ru.yandex.direct.inventori.model.request.VideoCreative buildInventoriVideoCreative(int videoDuration) {
        return new ru.yandex.direct.inventori.model.request.VideoCreative(
                videoDuration * 1000, null, singleton(new ru.yandex.direct.inventori.model.request.BlockSize(16, 9)));
    }
}

