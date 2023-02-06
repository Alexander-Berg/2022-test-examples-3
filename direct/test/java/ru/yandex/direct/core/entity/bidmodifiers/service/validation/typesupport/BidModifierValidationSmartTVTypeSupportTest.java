package ru.yandex.direct.core.entity.bidmodifiers.service.validation.typesupport;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.adgroup.model.TextAdGroup;
import ru.yandex.direct.core.entity.bidmodifier.BidModifier;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierSmartTV;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierSmartTVAdjustment;
import ru.yandex.direct.core.entity.bidmodifiers.container.BidModifierKey;
import ru.yandex.direct.core.entity.bidmodifiers.repository.FakeBidModifierRepository;
import ru.yandex.direct.core.entity.bidmodifiers.service.CachingFeaturesProvider;
import ru.yandex.direct.core.entity.bidmodifiers.validation.typesupport.BidModifierValidationSmartTVTypeSupport;
import ru.yandex.direct.core.entity.bidmodifiers.validation.typesupport.DeviceModifiersConflictChecker;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.testing.FakeShardByClient;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.Path;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.entity.bidmodifier.BidModifierSmartTV.SMART_T_V_ADJUSTMENT;
import static ru.yandex.direct.core.entity.bidmodifiers.validation.BidModifiersDefects.deviceBidModifiersAllZeros;
import static ru.yandex.direct.core.entity.bidmodifiers.validation.BidModifiersDefects.notSupportedMultiplier;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoErrors;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

public class BidModifierValidationSmartTVTypeSupportTest {
    private static final Path errorPath = path(field(SMART_T_V_ADJUSTMENT.name()));
    private ClientId clientId;
    private BidModifierSmartTV modifier;
    private AdGroup adGroup;
    private BidModifierValidationSmartTVTypeSupport service;
    private FeatureService featureService;
    private FakeBidModifierRepository bidModifierRepository;
    private DeviceModifiersConflictChecker conflictChecker;

    @Before
    public void setUp() {
        clientId = ClientId.fromLong(1L);
        modifier = new BidModifierSmartTV().withSmartTVAdjustment(
                new BidModifierSmartTVAdjustment().withPercent(120));

        bidModifierRepository = new FakeBidModifierRepository(List.of());
        conflictChecker = new DeviceModifiersConflictChecker(new FakeShardByClient(), bidModifierRepository);
        service = new BidModifierValidationSmartTVTypeSupport(conflictChecker);
        featureService = mock(FeatureService.class);

        when(featureService.isEnabledForClientId(clientId, FeatureName.SMARTTV_BID_MODIFIER_ENABLED)).thenReturn(true);
    }

    @Test
    public void validateAddStep1_TextCampaign_errorIsGenerated() {
        adGroup = new TextAdGroup().withType(AdGroupType.BASE);
        ValidationResult<BidModifierSmartTV, Defect> vr = service.validateAddStep1(modifier.withCampaignId(1L),
                CampaignType.TEXT, adGroup, clientId,
                new CachingFeaturesProvider(featureService));
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(
                validationError(errorPath, notSupportedMultiplier()))));
    }

    @Test
    public void validateAddStep1_CpmCampaignCpmBannerAdGroup_success() {
        adGroup = new TextAdGroup().withType(AdGroupType.CPM_BANNER);
        ValidationResult<BidModifierSmartTV, Defect> vr = service.validateAddStep1(modifier.withCampaignId(1L),
                CampaignType.CPM_BANNER, adGroup, clientId, new CachingFeaturesProvider(featureService));
        assertThat(vr).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void validateAddStep1_CpmCampaignCpmVideoAdGroup_success() {
        adGroup = new TextAdGroup().withType(AdGroupType.CPM_VIDEO);
        ValidationResult<BidModifierSmartTV, Defect> vr = service.validateAddStep1(modifier.withCampaignId(1L),
                CampaignType.CPM_BANNER, adGroup, clientId, new CachingFeaturesProvider(featureService));
        assertThat(vr).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void validateAddStep1_cpmCpmPriceCampaign_errorIsGenerated() {
        ValidationResult<BidModifierSmartTV, Defect> vr = service.validateAddStep1(modifier.withCampaignId(1L),
                CampaignType.CPM_PRICE, null, clientId,
                new CachingFeaturesProvider(featureService));
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(
                validationError(errorPath, notSupportedMultiplier()))));
    }

    private ValidationResult<List<BidModifierSmartTV>, Defect> testValidateAddStep2(
            List<BidModifier> modifiersInOperation,
            List<BidModifier> modifiersInDB
    ) {
        bidModifierRepository = new FakeBidModifierRepository(modifiersInDB);
        conflictChecker = new DeviceModifiersConflictChecker(new FakeShardByClient(), bidModifierRepository);
        service = new BidModifierValidationSmartTVTypeSupport(conflictChecker);

        Map<Long, CampaignType> campaignTypes = Map.of(1L, CampaignType.CPM_BANNER);
        Map<Long, AdGroup> adGroupsWithType = Map.of(1L, new AdGroup().withType(AdGroupType.BASE));

        var modifiersToValidate = modifiersInOperation.stream()
                .filter(m -> m instanceof BidModifierSmartTV)
                .map(m -> (BidModifierSmartTV) m)
                .collect(Collectors.toList());
        Map<BidModifierKey, BidModifier> allValidBidModifiersInOperation = modifiersInOperation
                .stream().collect(Collectors.toMap(BidModifierKey::new, m -> m));

        Map<BidModifierKey, BidModifierSmartTV> existingModifiers = modifiersInDB.stream()
                .filter(m -> m instanceof BidModifierSmartTV)
                .map(m -> (BidModifierSmartTV) m)
                .collect(Collectors.toMap(BidModifierKey::new, m -> m));

        return service.validateAddStep2(clientId,
                modifiersToValidate,
                existingModifiers,
                campaignTypes,
                adGroupsWithType,
                new CachingFeaturesProvider(featureService),
                allValidBidModifiersInOperation);
    }

    @Test
    public void validateAddStep2_happyPath() {
        var result = testValidateAddStep2(
                List.of(BidModifierTestHelper.getSmartTv()),
                Collections.emptyList()
        );

        assertThat(result).is(matchedBy(hasNoErrors()));
    }

    @Test
    public void validateAddStep2_noZeroValues() {
        var result = testValidateAddStep2(
                List.of(BidModifierTestHelper.getZeroSmartTv(),
                        BidModifierTestHelper.getZeroMobile(),
                        BidModifierTestHelper.getZeroDesktop()),
                Collections.emptyList()
        );

        assertThat(result).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0)), deviceBidModifiersAllZeros()))));
    }

    @Test
    public void validateAddStep2_noZeroValues_forDesktopOnly() {
        var result = testValidateAddStep2(
                List.of(BidModifierTestHelper.getZeroSmartTv(),
                        BidModifierTestHelper.getZeroMobile(),
                        BidModifierTestHelper.getZeroDesktopOnly()),
                List.of(BidModifierTestHelper.getZeroTablet())
        );

        assertThat(result).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0)), deviceBidModifiersAllZeros()))));
    }

}
