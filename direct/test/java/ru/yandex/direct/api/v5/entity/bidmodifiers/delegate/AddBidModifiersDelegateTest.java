package ru.yandex.direct.api.v5.entity.bidmodifiers.delegate;

import java.util.List;
import java.util.Set;

import com.yandex.direct.api.v5.bidmodifiers.AddRequest;
import com.yandex.direct.api.v5.bidmodifiers.BidModifierAddItem;
import com.yandex.direct.api.v5.bidmodifiers.MobileAdjustmentAdd;
import com.yandex.direct.api.v5.bidmodifiers.SmartAdAdjustmentAdd;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.api.v5.converter.ResultConverter;
import ru.yandex.direct.api.v5.result.ApiMassResult;
import ru.yandex.direct.api.v5.security.ApiAuthenticationSource;
import ru.yandex.direct.api.v5.testing.configuration.Api5Test;
import ru.yandex.direct.api.v5.validation.DefectType;
import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.core.entity.adgroup.service.AdGroupService;
import ru.yandex.direct.core.entity.bidmodifier.BidModifier;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierType;
import ru.yandex.direct.core.entity.bidmodifiers.repository.BidModifierLevel;
import ru.yandex.direct.core.entity.bidmodifiers.service.BidModifierService;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.user.model.ApiUser;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.FeedInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.testing.data.TestGroups.activeCpmBannerAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.activePerformanceAdGroup;

@Api5Test
@RunWith(SpringRunner.class)
public class AddBidModifiersDelegateTest {

    @Autowired
    private Steps steps;
    @Autowired
    private BidModifierService bidModifierService;
    @Autowired
    private ResultConverter resultConverter;
    @Autowired
    private AdGroupService adGroupService;
    @Autowired
    private PpcPropertiesSupport ppcPropertiesSupport;
    @Autowired
    private FeatureService featureService;

    @Mock
    private ApiAuthenticationSource auth;

    private AddBidModifiersDelegate delegate;

    private ClientInfo clientInfo;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        clientInfo = steps.clientSteps().createDefaultClient();
        when(auth.getSubclient()).thenReturn(new ApiUser().withClientId(clientInfo.getClientId()));
        when(auth.getChiefSubclient()).thenReturn(new ApiUser().withClientId(clientInfo.getClientId()));
        when(auth.getOperator()).thenReturn(new ApiUser().withUid(clientInfo.getUid()));
        delegate = new AddBidModifiersDelegate(bidModifierService,
                resultConverter,
                auth,
                adGroupService,
                ppcPropertiesSupport,
                featureService);
    }

    @Test
    public void add_MobileBidModifier_success() {
        var mobileAdjustmentAdd = new MobileAdjustmentAdd().withBidModifier(20);
        var campaignInfo = steps.campaignSteps().createActiveCpmBannerCampaign(clientInfo);
        var adGroup = activeCpmBannerAdGroup(campaignInfo.getCampaignId());
        Long adGroupId = steps.adGroupSteps().createAdGroup(adGroup, campaignInfo).getAdGroupId();
        var bidModifierAddItem = new BidModifierAddItem()
                .withAdGroupId(adGroupId)
                .withMobileAdjustment(mobileAdjustmentAdd);
        AddRequest request = new AddRequest().withBidModifiers(bidModifierAddItem);
        List<BidModifierAddItem> bidModifierAddItems = delegate.convertRequest(request);
        ApiMassResult<List<Long>> apiResult = delegate.processList(bidModifierAddItems);
        assertThat(apiResult.getErrorCount()).isEqualTo(0);
        Long bidModifierId = apiResult.get(0).getResult().get(0);
        List<BidModifier> bidModifiers = bidModifierService.getByAdGroupIds(clientInfo.getClientId(),
                Set.of(adGroupId),
                Set.of(campaignInfo.getCampaignId()),
                Set.of(BidModifierType.MOBILE_MULTIPLIER),
                Set.of(BidModifierLevel.ADGROUP),
                clientInfo.getUid());
        var externalId = BidModifierService.getExternalId(bidModifiers.get(0).getId(),
                BidModifierType.MOBILE_MULTIPLIER);
        assertThat(externalId).isEqualTo(bidModifierId);
    }

    @Test
    public void add_SmartBidModifier_success() {
        var smartAdAdjustment = new SmartAdAdjustmentAdd().withBidModifier(20);
        var campaignInfo = steps.campaignSteps().createActivePerformanceCampaign(clientInfo);
        FeedInfo feed = steps.feedSteps().createDefaultFeed(clientInfo);
        var adGroup = activePerformanceAdGroup(campaignInfo.getCampaignId(), feed.getFeedId());
        Long adGroupId = steps.adGroupSteps().createAdGroup(adGroup, campaignInfo).getAdGroupId();
        var bidModifierAddItem = new BidModifierAddItem()
                .withAdGroupId(adGroupId)
                .withSmartAdAdjustment(smartAdAdjustment);
        AddRequest request = new AddRequest().withBidModifiers(bidModifierAddItem);
        List<BidModifierAddItem> bidModifierAddItems = delegate.convertRequest(request);
        ValidationResult<List<BidModifierAddItem>, DefectType> vr =
                delegate.validateInternalRequest(bidModifierAddItems);
        ApiMassResult<List<Long>> apiResult = delegate.processList(vr.getValue());
        assertThat(apiResult.getErrorCount()).isEqualTo(0);
        Long bidModifierId = apiResult.get(0).getResult().get(0);
        List<BidModifier> bidModifiers = bidModifierService.getByAdGroupIds(clientInfo.getClientId(),
                Set.of(adGroupId),
                Set.of(campaignInfo.getCampaignId()),
                Set.of(BidModifierType.PERFORMANCE_TGO_MULTIPLIER),
                Set.of(BidModifierLevel.ADGROUP),
                clientInfo.getUid());
        var externalId = BidModifierService.getExternalId(bidModifiers.get(0).getId(),
                BidModifierType.PERFORMANCE_TGO_MULTIPLIER);
        assertThat(externalId).isEqualTo(bidModifierId);
    }

}
