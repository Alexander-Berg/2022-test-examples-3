package ru.yandex.direct.core.entity.bidmodifiers.service;

import java.util.List;
import java.util.function.Function;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.bidmodifier.BidModifier;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierDesktop;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierDesktopAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierDesktopOnly;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierDesktopOnlyAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierMobile;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierMobileAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierSmartTV;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierSmartTVAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierTablet;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierTabletAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierType;
import ru.yandex.direct.core.entity.bidmodifier.ComplexBidModifier;
import ru.yandex.direct.core.entity.bidmodifiers.validation.BidModifiersDefectIds;
import ru.yandex.direct.core.entity.bidmodifiers.validation.typesupport.BidModifierValidationTypeSupportDispatcher;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoErrors;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;

@CoreTest
@RunWith(SpringRunner.class)
public class ComplexBidModifierServiceTest {

    @Autowired
    private BidModifierValidationTypeSupportDispatcher validationTypeSupportDispatcher;

    @Mock
    FeatureService featureService;

    private ClientId clientId = ClientId.fromLong(1L);

    private ComplexBidModifierService service;

    private Function<Integer, CampaignType> campaignTypeProvider;
    private Function<Integer, AdGroup> adGroupWithTypeProvider;

    @Before
    public void init() {
        MockitoAnnotations.openMocks(this);
    }

    private BidModifierDesktop getZeroDesktopModifier() {
        return new BidModifierDesktop()
                .withEnabled(true)
                .withType(BidModifierType.DESKTOP_MULTIPLIER)
                .withDesktopAdjustment(new BidModifierDesktopAdjustment().withPercent(0));
    }

    private BidModifierDesktopOnly getZeroDesktopOnlyModifier() {
        return new BidModifierDesktopOnly()
                .withEnabled(true)
                .withType(BidModifierType.DESKTOP_ONLY_MULTIPLIER)
                .withDesktopOnlyAdjustment(new BidModifierDesktopOnlyAdjustment().withPercent(0));
    }

    private BidModifierSmartTV getZeroSmartTVModifier() {
        return new BidModifierSmartTV()
                .withEnabled(true)
                .withType(BidModifierType.SMARTTV_MULTIPLIER)
                .withSmartTVAdjustment(new BidModifierSmartTVAdjustment().withPercent(0));
    }

    private BidModifierMobile getZeroMobileModifier() {
        return new BidModifierMobile()
                .withEnabled(true)
                .withType(BidModifierType.MOBILE_MULTIPLIER)
                .withMobileAdjustment(new BidModifierMobileAdjustment().withPercent(0).withOsType(null));
    }

    private BidModifierTablet getZeroTabletModifier() {
        return new BidModifierTablet()
                .withEnabled(true)
                .withType(BidModifierType.TABLET_MULTIPLIER)
                .withTabletAdjustment(new BidModifierTabletAdjustment().withPercent(0).withOsType(null));
    }

    private ValidationResult<List<BidModifier>, Defect> testComplexModifier(ComplexBidModifier modifier) {
        var flatModifiersFromComplex = service.convertFromComplexModelsForCampaigns(List.of(modifier));
        var bidModifiersToTest = flatModifiersFromComplex.getLeft();
        var complexToFlatIndexMap = flatModifiersFromComplex.getRight();

        return service.validateBidModifiersFlat(
                bidModifiersToTest,
                complexToFlatIndexMap,
                campaignTypeProvider,
                adGroupWithTypeProvider,
                clientId
        );
    }

    @Test
    public void shouldFailValidation_ForZeroDesktopOnlyTabletAndMobile() {
        service = new ComplexBidModifierService(validationTypeSupportDispatcher, featureService);

        doReturn(true).when(featureService).isEnabledForClientId(clientId,
                FeatureName.TABLET_BIDMODIFER_ENABLED);
        doReturn(true).when(featureService).isEnabledForClientId(clientId,
                FeatureName.SMARTTV_BID_MODIFIER_ENABLED);

        campaignTypeProvider = idx -> CampaignType.TEXT;
        adGroupWithTypeProvider = idx -> new AdGroup().withType(AdGroupType.BASE);

        var complexModifier = new ComplexBidModifier()
                .withDesktopOnlyModifier(getZeroDesktopOnlyModifier())
                .withMobileModifier(getZeroMobileModifier())
                .withTabletModifier(getZeroTabletModifier());

        var result = testComplexModifier(complexModifier);

        assertThat("все обнулили, должна быть ошибка валидации",
                result, hasDefectDefinitionWith(
                        validationError(BidModifiersDefectIds.GeneralDefects.DEVICE_BID_MODIFIERS_ALL_ZEROS)));
    }

    @Test
    public void shouldFailValidation_ForZeroDesktopAndMobile() {
        service = new ComplexBidModifierService(validationTypeSupportDispatcher, featureService);

        doReturn(true).when(featureService).isEnabledForClientId(clientId,
                FeatureName.TABLET_BIDMODIFER_ENABLED);
        doReturn(true).when(featureService).isEnabledForClientId(clientId,
                FeatureName.SMARTTV_BID_MODIFIER_ENABLED);

        campaignTypeProvider = idx -> CampaignType.TEXT;
        adGroupWithTypeProvider = idx -> new AdGroup().withType(AdGroupType.BASE);

        var complexModifier = new ComplexBidModifier()
                .withDesktopModifier(getZeroDesktopModifier())
                .withMobileModifier(getZeroMobileModifier());

        var result = testComplexModifier(complexModifier);

        assertThat("все обнулили, должна быть ошибка валидации",
                result, hasDefectDefinitionWith(
                        validationError(BidModifiersDefectIds.GeneralDefects.DEVICE_BID_MODIFIERS_ALL_ZEROS)));
    }

    @Test
    public void shouldFailValidation_ForZeroDesktopSmartTvAndMobile_ForCPM_withFeature() {
        service = new ComplexBidModifierService(validationTypeSupportDispatcher, featureService);

        doReturn(true).when(featureService).isEnabledForClientId(clientId,
                FeatureName.TABLET_BIDMODIFER_ENABLED);
        doReturn(true).when(featureService).isEnabledForClientId(clientId,
                FeatureName.SMARTTV_BID_MODIFIER_ENABLED);

        campaignTypeProvider = idx -> CampaignType.CPM_BANNER;
        adGroupWithTypeProvider = idx -> new AdGroup().withType(AdGroupType.BASE);

        var complexModifier = new ComplexBidModifier()
                .withDesktopModifier(getZeroDesktopModifier())
                .withMobileModifier(getZeroMobileModifier())
                .withSmartTVModifier(getZeroSmartTVModifier());

        var result = testComplexModifier(complexModifier);

        assertThat("все обнулили, должна быть ошибка валидации",
                result, hasDefectDefinitionWith(
                        validationError(BidModifiersDefectIds.GeneralDefects.DEVICE_BID_MODIFIERS_ALL_ZEROS)));
    }
    @Test
    public void shouldNotFailValidation_ForZeroDesktopAndMobile_ForCPM_withFeature() {
        service = new ComplexBidModifierService(validationTypeSupportDispatcher, featureService);

        doReturn(true).when(featureService).isEnabledForClientId(clientId,
                FeatureName.TABLET_BIDMODIFER_ENABLED);
        doReturn(true).when(featureService).isEnabledForClientId(clientId,
                FeatureName.SMARTTV_BID_MODIFIER_ENABLED);

        campaignTypeProvider = idx -> CampaignType.CPM_BANNER;
        adGroupWithTypeProvider = idx -> new AdGroup().withType(AdGroupType.BASE);

        var complexModifier = new ComplexBidModifier()
                .withDesktopModifier(getZeroDesktopModifier())
                .withMobileModifier(getZeroMobileModifier());

        var result = testComplexModifier(complexModifier);

        assertThat("таргетинг на smarttv остался - не должны фейлить",
                result, hasNoErrors());
    }

    @Test
    public void shouldFailValidation_ForZeroDesktopOnly_ForCPM_withFeature() {
        service = new ComplexBidModifierService(validationTypeSupportDispatcher, featureService);

        doReturn(true).when(featureService).isEnabledForClientId(clientId,
                FeatureName.TABLET_BIDMODIFER_ENABLED);

        campaignTypeProvider = idx -> CampaignType.CPM_BANNER;
        adGroupWithTypeProvider = idx -> new AdGroup().withType(AdGroupType.BASE);

        var complexModifier = new ComplexBidModifier()
                .withDesktopOnlyModifier(getZeroDesktopOnlyModifier());

        var result = testComplexModifier(complexModifier);

        assertThat("не поддерживается для cpm, должна быть ошибка валидации",
                result, hasDefectDefinitionWith(
                        validationError(BidModifiersDefectIds.GeneralDefects.NOT_SUPPORTED_MULTIPLIER)));
    }

    @Test
    @Ignore("Temporarily disabled for cpm")
    public void shouldFailValidation_ForZeroDesktopOnlyTabletSmartTvAndMobile_ForCPM_withFeature() {
        service = new ComplexBidModifierService(validationTypeSupportDispatcher, featureService);

        doReturn(true).when(featureService).isEnabledForClientId(clientId,
                FeatureName.TABLET_BIDMODIFER_ENABLED);
        doReturn(true).when(featureService).isEnabledForClientId(clientId,
                FeatureName.SMARTTV_BID_MODIFIER_ENABLED);

        campaignTypeProvider = idx -> CampaignType.CPM_BANNER;
        adGroupWithTypeProvider = idx -> new AdGroup().withType(AdGroupType.BASE);

        var complexModifier = new ComplexBidModifier()
                .withDesktopOnlyModifier(getZeroDesktopOnlyModifier())
                .withTabletModifier(getZeroTabletModifier())
                .withMobileModifier(getZeroMobileModifier())
                .withSmartTVModifier(getZeroSmartTVModifier());

        var result = testComplexModifier(complexModifier);

        assertThat("все обнулили, должна быть ошибка валидации",
                result, hasDefectDefinitionWith(
                        validationError(BidModifiersDefectIds.GeneralDefects.DEVICE_BID_MODIFIERS_ALL_ZEROS)));
    }
    @Test
    @Ignore("Temporarily disabled for cpm")
    public void shouldNotFailValidation_ForZeroDesktopOnlyTabletAndMobile_ForCPM_withFeature() {
        service = new ComplexBidModifierService(validationTypeSupportDispatcher, featureService);

        doReturn(true).when(featureService).isEnabledForClientId(clientId,
                FeatureName.TABLET_BIDMODIFER_ENABLED);
        doReturn(true).when(featureService).isEnabledForClientId(clientId,
                FeatureName.SMARTTV_BID_MODIFIER_ENABLED);

        campaignTypeProvider = idx -> CampaignType.CPM_BANNER;
        adGroupWithTypeProvider = idx -> new AdGroup().withType(AdGroupType.BASE);

        var complexModifier = new ComplexBidModifier()
                .withDesktopOnlyModifier(getZeroDesktopOnlyModifier())
                .withTabletModifier(getZeroTabletModifier())
                .withMobileModifier(getZeroMobileModifier());

        var result = testComplexModifier(complexModifier);

        assertThat("таргетинг на smarttv остался - не должны фейлить",
                result, hasNoErrors());
    }
}
