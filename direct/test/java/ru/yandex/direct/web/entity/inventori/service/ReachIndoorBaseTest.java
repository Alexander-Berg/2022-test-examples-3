package ru.yandex.direct.web.entity.inventori.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeoutException;

import one.util.streamex.StreamEx;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.asynchttp.AsyncHttpExecuteException;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.campaign.service.CampaignService;
import ru.yandex.direct.core.entity.campaign.service.CampaignWithBrandSafetyService;
import ru.yandex.direct.core.entity.creative.model.AdditionalData;
import ru.yandex.direct.core.entity.creative.model.VideoFormat;
import ru.yandex.direct.core.entity.creative.repository.CreativeRepository;
import ru.yandex.direct.core.entity.crypta.repository.CryptaSegmentRepository;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.inventori.service.CampaignInfoCollector;
import ru.yandex.direct.core.entity.placements.model1.IndoorPlacement;
import ru.yandex.direct.core.entity.pricepackage.service.PricePackageService;
import ru.yandex.direct.core.entity.retargeting.repository.RetargetingConditionRepository;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.entity.user.service.UserService;
import ru.yandex.direct.core.security.DirectAuthentication;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.CreativeInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.inventori.InventoriClient;
import ru.yandex.direct.inventori.model.response.IndoorPredictionResponse;
import ru.yandex.direct.inventori.model.response.error.ErrorsEntry;
import ru.yandex.direct.web.configuration.DirectWebTest;
import ru.yandex.direct.web.configuration.mock.auth.DirectWebAuthenticationSourceMock;
import ru.yandex.direct.web.core.entity.inventori.model.BidModifierDemographicWeb;
import ru.yandex.direct.web.core.entity.inventori.model.PageBlockWeb;
import ru.yandex.direct.web.core.entity.inventori.model.ReachIndoorRequest;
import ru.yandex.direct.web.core.entity.inventori.service.CryptaService;
import ru.yandex.direct.web.core.entity.inventori.service.InventoriService;
import ru.yandex.direct.web.core.entity.inventori.service.InventoriWebService;
import ru.yandex.direct.web.core.security.DirectWebAuthenticationSource;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultCpmIndoorVideoAddition;
import static ru.yandex.direct.core.testing.data.TestPlacements.indoorBlockWithOneSize;
import static ru.yandex.direct.core.testing.data.TestPlacements.indoorPlacementWithBlocks;

@DirectWebTest
@RunWith(SpringRunner.class)
public abstract class ReachIndoorBaseTest {

    protected static final CompareStrategy REACH_INDOOR_RESULT_STRATEGY = DefaultCompareStrategies.allFields()
            .forFields(newPath("requestId")).useMatcher(notNullValue());

    protected static final Long PAGE_ID_1 = 1L;
    protected static final Long PAGE_ID_2 = 2L;
    protected static final Long PAGE_1_BLOCK_ID_1 = 11L;
    protected static final Long PAGE_2_BLOCK_ID_1 = 21L;
    protected static final Long PAGE_2_BLOCK_ID_2 = 22L;

    @Autowired
    private RetargetingConditionRepository retargetingConditionRepository;

    @Autowired
    private CampaignWithBrandSafetyService campaignWithBrandSafetyService;

    @Autowired
    private AdGroupRepository adGroupRepository;

    @Autowired
    private CryptaSegmentRepository cryptaSegmentRepository;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    protected InventoriClient inventoriClient;

    @Mock
    private InventoriService inventoriService;

    protected InventoriWebService inventoriWebService;

    @Autowired
    protected Steps steps;

    @Autowired
    private CryptaService cryptaService;

    @Autowired
    private ShardHelper shardHelper;

    @Autowired
    private CreativeRepository creativeRepository;

    @Autowired
    private CampaignService campaignService;

    @Autowired
    private UserService userService;

    @Autowired
    private CampaignInfoCollector campaignInfoCollector;

    @Autowired
    PricePackageService pricePackageService;

    @Autowired
    CampaignRepository campaignRepository;

    @Autowired
    protected DirectWebAuthenticationSource authenticationSource;

    @Autowired
    private FeatureService featureService;

    protected ClientInfo clientInfo;
    protected CampaignInfo campaignInfo;
    protected AdGroupInfo adGroupInfo;
    protected CreativeInfo creativeInfo1;
    protected CreativeInfo creativeInfo2;
    protected ClientId clientId;
    protected User operator;
    protected User user;
    protected int shard;

    @Before
    public void before() {
        campaignInfo = steps.campaignSteps().createActiveCpmBannerCampaign();
        adGroupInfo = steps.adGroupSteps().createActiveCpmIndoorAdGroup(campaignInfo);
        clientInfo = campaignInfo.getClientInfo();
        clientId = clientInfo.getClientId();
        shard = campaignInfo.getShard();

        creativeInfo1 = createCreative(60, Pair.of(1920, 1080), Pair.of(3840, 2160), Pair.of(null, null));
        creativeInfo2 = createCreative(1.5, Pair.of(640, 480));

        IndoorPlacement indoorPlacement1 = indoorPlacementWithBlocks(PAGE_ID_1,
                singletonList(
                        indoorBlockWithOneSize(PAGE_ID_1, PAGE_1_BLOCK_ID_1)));
        IndoorPlacement indoorPlacement2 = indoorPlacementWithBlocks(PAGE_ID_2,
                asList(
                        indoorBlockWithOneSize(PAGE_ID_2, PAGE_2_BLOCK_ID_1),
                        indoorBlockWithOneSize(PAGE_ID_2, PAGE_2_BLOCK_ID_2)));
        steps.placementSteps().clearPlacements();
        steps.placementSteps().addPlacements(indoorPlacement1, indoorPlacement2);

        inventoriWebService = new InventoriWebService(shardHelper, inventoriClient, cryptaService,
                authenticationSource, userService, campaignInfoCollector, inventoriService, campaignRepository, pricePackageService,
                featureService, retargetingConditionRepository, campaignWithBrandSafetyService, adGroupRepository, cryptaSegmentRepository);

        setAuthData();
    }

    private void setAuthData() {
        operator = steps.userSteps().createDefaultUser().getUser();
        user = clientInfo.getChiefUserInfo().getUser();

        DirectWebAuthenticationSourceMock authSource =
                (DirectWebAuthenticationSourceMock) authenticationSource;
        authSource.withOperator(operator);
        authSource.withSubjectUser(user);

        SecurityContextHolder.getContext()
                .setAuthentication(new DirectAuthentication(operator, user));
    }

    private CreativeInfo createCreative(double duration, Pair<Integer, Integer>... sizes) {
        List<VideoFormat> formats = StreamEx.of(sizes)
                .map(size -> new VideoFormat().withWidth(size.getLeft()).withHeight(size.getRight()))
                .toList();
        return steps.creativeSteps().createCreative(new CreativeInfo()
                .withClientInfo(clientInfo)
                .withCreative(defaultCpmIndoorVideoAddition(null, null)
                        .withAdditionalData(new AdditionalData()
                                .withDuration(BigDecimal.valueOf(duration))
                                .withFormats(formats))));
    }

    protected void inventoriSuccessResponse() {
        when(inventoriClient.getIndoorPrediction(any(), any(), any(), any(), any(), any()))
                .thenReturn(new IndoorPredictionResponse()
                        .withReach(1000L)
                        .withOtsCapacity(2000L));
    }

    protected void inventoriSuccessLessThanResponse() {
        when(inventoriClient.getIndoorPrediction(any(), any(), any(), any(), any(), any()))
                .thenReturn(new IndoorPredictionResponse()
                        .withReachLessThan(3000L));
    }

    protected void inventoriBadResponse() {
        when(inventoriClient.getIndoorPrediction(any(), any(), any(), any(), any(), any()))
                .thenReturn(new IndoorPredictionResponse()
                        .withErrors(singletonList(new ErrorsEntry().withType("INVALID_REQUEST"))));
    }

    protected void inventoriExceptionResponse() {
        when(inventoriClient.getIndoorPrediction(any(), any(), any(), any(), any(), any()))
                .thenThrow(new AsyncHttpExecuteException("timeout", new TimeoutException()));
    }

    protected ReachIndoorRequest defaultRequest() {
        return new ReachIndoorRequest()
                .withCampaignId(campaignInfo.getCampaignId())
                .withAdgroupId(adGroupInfo.getAdGroupId())
                .withVideoCreativeIds(asList(creativeInfo1.getCreativeId(), creativeInfo2.getCreativeId()))
                .withPageBlocks(asList(
                        new PageBlockWeb(PAGE_ID_1, singletonList(PAGE_1_BLOCK_ID_1)),
                        new PageBlockWeb(PAGE_ID_2, asList(PAGE_2_BLOCK_ID_1, PAGE_2_BLOCK_ID_2))
                ))
                .withBidModifierDemographics(asList(
                        new BidModifierDemographicWeb("male", "0-17", 110),
                        new BidModifierDemographicWeb("all", "18-24", 120)
                ));
    }
}
