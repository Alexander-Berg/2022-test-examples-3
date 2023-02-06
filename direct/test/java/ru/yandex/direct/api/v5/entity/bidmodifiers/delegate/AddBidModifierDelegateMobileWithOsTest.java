package ru.yandex.direct.api.v5.entity.bidmodifiers.delegate;

import java.util.List;
import java.util.Set;

import com.yandex.direct.api.v5.bidmodifiers.AddRequest;
import com.yandex.direct.api.v5.bidmodifiers.BidModifierAddItem;
import com.yandex.direct.api.v5.bidmodifiers.DesktopAdjustmentAdd;
import com.yandex.direct.api.v5.bidmodifiers.MobileAdjustmentAdd;
import com.yandex.direct.api.v5.bidmodifiers.OperatingSystemTypeEnum;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.api.v5.converter.ResultConverter;
import ru.yandex.direct.api.v5.result.ApiMassResult;
import ru.yandex.direct.api.v5.security.ApiAuthenticationSource;
import ru.yandex.direct.api.v5.testing.configuration.Api5Test;
import ru.yandex.direct.api.v5.validation.DefectType;
import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.core.entity.adgroup.service.AdGroupService;
import ru.yandex.direct.core.entity.bidmodifier.BidModifier;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierDesktop;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierDesktopAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierMobile;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierMobileAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierType;
import ru.yandex.direct.core.entity.bidmodifier.OsType;
import ru.yandex.direct.core.entity.bidmodifiers.repository.BidModifierLevel;
import ru.yandex.direct.core.entity.bidmodifiers.service.BidModifierService;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.user.model.ApiUser;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@Api5Test
@RunWith(SpringRunner.class)
public class AddBidModifierDelegateMobileWithOsTest {
    public static final int VALID_MODIFIER_PERCENT = 120;
    public static final int ZERO_MODIFIER_PERCENT = 0;

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
    private CampaignInfo campaignInfo;

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
        campaignInfo = steps.campaignSteps().createActiveTextCampaign(clientInfo);

    }

    @Test
    public void add_MobileBidModifier_success() {
        var mobileAdjustmentAdd = new MobileAdjustmentAdd()
                .withBidModifier(VALID_MODIFIER_PERCENT)
                .withOperatingSystemType(OperatingSystemTypeEnum.IOS);
        var bidModifierAddItem = new BidModifierAddItem()
                .withCampaignId(campaignInfo.getCampaignId())
                .withMobileAdjustment(mobileAdjustmentAdd);
        AddRequest request = new AddRequest().withBidModifiers(bidModifierAddItem);
        List<BidModifierAddItem> bidModifierAddItems = delegate.convertRequest(request);
        ValidationResult<List<BidModifierAddItem>, DefectType> vr =
                delegate.validateInternalRequest(bidModifierAddItems);

        ApiMassResult<List<Long>> apiResult = delegate.processList(vr.getValue());
        assertThat(apiResult.getErrorCount()).isEqualTo(0);

        List<BidModifier> bidModifiers = bidModifierService.getByCampaignIds(clientInfo.getClientId(),
                Set.of(campaignInfo.getCampaignId()),
                Set.of(BidModifierType.MOBILE_MULTIPLIER),
                Set.of(BidModifierLevel.CAMPAIGN),
                clientInfo.getUid());
        assertThat(bidModifiers).hasSize(1);

        BidModifier expected =
                new BidModifierMobile().withMobileAdjustment(new BidModifierMobileAdjustment()
                                .withOsType(OsType.IOS)
                                .withPercent(VALID_MODIFIER_PERCENT))
                        .withCampaignId(campaignInfo.getCampaignId())
                        .withEnabled(true)
                        .withType(BidModifierType.MOBILE_MULTIPLIER);
        assertThat(bidModifiers.get(0)).is(matchedBy(beanDiffer(expected)
                .useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields())));
    }

    @Test
    public void add_MobileBidModifier_success_onGroup() {
        var mobileAdjustmentAdd = new MobileAdjustmentAdd()
                .withBidModifier(VALID_MODIFIER_PERCENT)
                .withOperatingSystemType(OperatingSystemTypeEnum.IOS);
        var adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(clientInfo);
        var bidModifierAddItem = new BidModifierAddItem()
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withMobileAdjustment(mobileAdjustmentAdd);
        AddRequest request = new AddRequest().withBidModifiers(bidModifierAddItem);
        List<BidModifierAddItem> bidModifierAddItems = delegate.convertRequest(request);
        ValidationResult<List<BidModifierAddItem>, DefectType> vr =
                delegate.validateInternalRequest(bidModifierAddItems);

        ApiMassResult<List<Long>> apiResult = delegate.processList(vr.getValue());
        assertThat(apiResult.getErrorCount()).isEqualTo(0);

        List<BidModifier> bidModifiers = bidModifierService.getByAdGroupIds(clientInfo.getClientId(),
                Set.of(adGroupInfo.getAdGroupId()),
                Set.of(adGroupInfo.getCampaignId()),
                Set.of(BidModifierType.MOBILE_MULTIPLIER),
                Set.of(BidModifierLevel.ADGROUP),
                clientInfo.getUid());
        assertThat(bidModifiers).hasSize(1);

        BidModifier expected = new BidModifierMobile()
                .withMobileAdjustment(new BidModifierMobileAdjustment()
                        .withOsType(OsType.IOS)
                        .withPercent(VALID_MODIFIER_PERCENT))
                .withCampaignId(adGroupInfo.getCampaignId())
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withEnabled(true)
                .withType(BidModifierType.MOBILE_MULTIPLIER);
        assertThat(bidModifiers.get(0)).is(matchedBy(beanDiffer(expected)
                .useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields())));
    }

    @Test
    public void add_MobileBidModifier_success_disableMobile() {
        var mobileAdjustmentAdd = new MobileAdjustmentAdd()
                .withBidModifier(ZERO_MODIFIER_PERCENT);
        var bidModifierAddItem = new BidModifierAddItem()
                .withCampaignId(campaignInfo.getCampaignId())
                .withMobileAdjustment(mobileAdjustmentAdd);
        AddRequest request = new AddRequest().withBidModifiers(bidModifierAddItem);
        List<BidModifierAddItem> bidModifierAddItems = delegate.convertRequest(request);
        ValidationResult<List<BidModifierAddItem>, DefectType> vr =
                delegate.validateInternalRequest(bidModifierAddItems);

        ApiMassResult<List<Long>> apiResult = delegate.processList(vr.getValue());
        assertThat(apiResult.getErrorCount()).isEqualTo(0);

        List<BidModifier> bidModifiers = bidModifierService.getByCampaignIds(clientInfo.getClientId(),
                Set.of(campaignInfo.getCampaignId()),
                Set.of(BidModifierType.MOBILE_MULTIPLIER),
                Set.of(BidModifierLevel.CAMPAIGN),
                clientInfo.getUid());
        assertThat(bidModifiers).hasSize(1);

        BidModifier expected = new BidModifierMobile()
                .withMobileAdjustment(new BidModifierMobileAdjustment()
                        .withPercent(0))
                .withCampaignId(campaignInfo.getCampaignId())
                .withEnabled(true)
                .withType(BidModifierType.MOBILE_MULTIPLIER);
        assertThat(bidModifiers.get(0)).is(matchedBy(beanDiffer(expected)
                .useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields())));
    }

    @Test
    public void add_DesktopBidModifier_success() {
        var desktopAdjustmentAdd = new DesktopAdjustmentAdd()
                .withBidModifier(0);
        var bidModifierAddItem = new BidModifierAddItem()
                .withCampaignId(campaignInfo.getCampaignId())
                .withDesktopAdjustment(desktopAdjustmentAdd);
        AddRequest request = new AddRequest().withBidModifiers(bidModifierAddItem);
        List<BidModifierAddItem> bidModifierAddItems = delegate.convertRequest(request);
        ValidationResult<List<BidModifierAddItem>, DefectType> vr =
                delegate.validateInternalRequest(bidModifierAddItems);

        ApiMassResult<List<Long>> apiResult = delegate.processList(vr.getValue());
        assertThat(apiResult.getErrorCount()).isEqualTo(0);

        List<BidModifier> bidModifiers = bidModifierService.getByCampaignIds(clientInfo.getClientId(),
                Set.of(campaignInfo.getCampaignId()),
                Set.of(BidModifierType.DESKTOP_MULTIPLIER),
                Set.of(BidModifierLevel.CAMPAIGN),
                clientInfo.getUid());
        assertThat(bidModifiers).hasSize(1);

        BidModifier expected = new BidModifierDesktop()
                .withDesktopAdjustment(new BidModifierDesktopAdjustment()
                        .withPercent(0))
                .withCampaignId(campaignInfo.getCampaignId())
                .withEnabled(true)
                .withType(BidModifierType.DESKTOP_MULTIPLIER);
        assertThat(bidModifiers.get(0)).is(matchedBy(beanDiffer(expected)
                .useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields())));
    }
}
