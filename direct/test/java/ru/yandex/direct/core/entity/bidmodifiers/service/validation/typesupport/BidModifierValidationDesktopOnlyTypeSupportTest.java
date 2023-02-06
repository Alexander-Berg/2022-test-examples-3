package ru.yandex.direct.core.entity.bidmodifiers.service.validation.typesupport;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.bidmodifier.BidModifier;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierDesktopOnly;
import ru.yandex.direct.core.entity.bidmodifiers.container.BidModifierKey;
import ru.yandex.direct.core.entity.bidmodifiers.repository.FakeBidModifierRepository;
import ru.yandex.direct.core.entity.bidmodifiers.service.CachingFeaturesProvider;
import ru.yandex.direct.core.entity.bidmodifiers.validation.typesupport.BidModifierValidationDesktopOnlyTypeSupport;
import ru.yandex.direct.core.entity.bidmodifiers.validation.typesupport.DeviceModifiersConflictChecker;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.testing.FakeShardByClient;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.entity.bidmodifier.BidModifierDesktopOnly.DESKTOP_ONLY_ADJUSTMENT;
import static ru.yandex.direct.core.entity.bidmodifiers.validation.BidModifiersDefects.deviceBidModifiersAllZeros;
import static ru.yandex.direct.core.entity.bidmodifiers.validation.BidModifiersDefects.expressionConditionsIntersection;
import static ru.yandex.direct.core.entity.bidmodifiers.validation.BidModifiersDefects.notSupportedMultiplier;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoErrors;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

public class BidModifierValidationDesktopOnlyTypeSupportTest {
    private ClientId clientId;
    private BidModifierValidationDesktopOnlyTypeSupport service;
    private FeatureService featureService;
    private FakeBidModifierRepository bidModifierRepository;
    private DeviceModifiersConflictChecker conflictChecker;

    @Before
    public void setUp() {
        clientId = ClientId.fromLong(1L);

        bidModifierRepository = new FakeBidModifierRepository(List.of());
        conflictChecker = new DeviceModifiersConflictChecker(new FakeShardByClient(), bidModifierRepository);
        service = new BidModifierValidationDesktopOnlyTypeSupport(conflictChecker);
        featureService = mock(FeatureService.class);
    }

    @Test
    public void validateAddStep1_withNoFeature_errorIsGenerated() {
        when(featureService.isEnabledForClientId(clientId, FeatureName.TABLET_BIDMODIFER_ENABLED)).thenReturn(false);
        var errorPath = path(field(DESKTOP_ONLY_ADJUSTMENT.name()));

        var vr = service.validateAddStep1(
                BidModifierTestHelper.getDesktopOnly(),
                CampaignType.TEXT, new AdGroup().withType(AdGroupType.BASE), clientId,
                new CachingFeaturesProvider(featureService));

        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(
                validationError(errorPath, notSupportedMultiplier()))));
    }

    @Test
    public void validateAddStep1_withFeature_noErrorIsGenerated() {
        when(featureService.isEnabledForClientId(clientId, FeatureName.TABLET_BIDMODIFER_ENABLED)).thenReturn(true);

        var vr = service.validateAddStep1(
                BidModifierTestHelper.getDesktopOnly(),
                CampaignType.TEXT, new AdGroup().withType(AdGroupType.BASE), clientId,
                new CachingFeaturesProvider(featureService));

        assertThat(vr).is(matchedBy(hasNoErrors()));
    }

    private ValidationResult<List<BidModifierDesktopOnly>, Defect> testValidateAddStep2(
            CampaignType campaignType,
            List<BidModifier> modifiersInOperation,
            List<BidModifier> modifiersInDB
    ) {
        bidModifierRepository = new FakeBidModifierRepository(modifiersInDB);
        conflictChecker = new DeviceModifiersConflictChecker(new FakeShardByClient(), bidModifierRepository);
        service = new BidModifierValidationDesktopOnlyTypeSupport(conflictChecker);

        Map<Long, CampaignType> campaignTypes = Map.of(1L, campaignType);
        Map<Long, AdGroup> adGroupsWithType = Map.of(1L, new AdGroup().withType(AdGroupType.BASE));

        var modifiersToValidate = modifiersInOperation.stream()
                .filter(m -> m instanceof BidModifierDesktopOnly)
                .map(m -> (BidModifierDesktopOnly) m)
                .collect(Collectors.toList());
        Map<BidModifierKey, BidModifier> allValidBidModifiersInOperation = modifiersInOperation
                .stream().collect(Collectors.toMap(BidModifierKey::new, m -> m));

        Map<BidModifierKey, BidModifierDesktopOnly> existingModifiers = modifiersInDB.stream()
                .filter(m -> m instanceof BidModifierDesktopOnly)
                .map(m -> (BidModifierDesktopOnly) m)
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
                CampaignType.TEXT,
                List.of(BidModifierTestHelper.getDesktopOnly()),
                Collections.emptyList()
        );

        assertThat(result).is(matchedBy(hasNoErrors()));
    }

    @Test
    public void validateAddStep2_noZeroValues_forTextCampaign() {
        var result = testValidateAddStep2(
                CampaignType.TEXT,
                List.of(BidModifierTestHelper.getZeroDesktopOnly(),
                        BidModifierTestHelper.getZeroMobile(),
                        BidModifierTestHelper.getZeroTablet()),
                Collections.emptyList()
        );

        assertThat(result).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0)), deviceBidModifiersAllZeros()))));
    }

    @Test
    public void validateAddStep2_noZeroValues_forMediaCampaign_noFeature() {
        var result = testValidateAddStep2(
                CampaignType.CPM_BANNER,
                List.of(BidModifierTestHelper.getZeroDesktopOnly()),
                List.of(BidModifierTestHelper.getZeroMobile(),
                        BidModifierTestHelper.getZeroTablet())
        );

        assertThat(result).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0)), deviceBidModifiersAllZeros()))));
    }

    @Test
    public void validateAddStep2_allowsSameZeroValues_forMediaCampaign_withFeature() {
        when(featureService.isEnabledForClientId(clientId, FeatureName.SMARTTV_BID_MODIFIER_ENABLED)).thenReturn(true);

        var result = testValidateAddStep2(
                CampaignType.CPM_BANNER,
                List.of(BidModifierTestHelper.getZeroDesktopOnly()),
                List.of(BidModifierTestHelper.getZeroMobile(),
                        BidModifierTestHelper.getZeroTablet())
        );

        assertThat(result).is(matchedBy(hasNoErrors()));
    }

    @Test
    public void validateAddStep2_noZeroValues_forMediaCampaign_withFeature() {
        when(featureService.isEnabledForClientId(clientId, FeatureName.SMARTTV_BID_MODIFIER_ENABLED)).thenReturn(true);

        var result = testValidateAddStep2(
                CampaignType.CPM_BANNER,
                List.of(BidModifierTestHelper.getZeroDesktopOnly(),
                        BidModifierTestHelper.getZeroMobile()
                ),
                List.of(BidModifierTestHelper.getZeroSmartTv(),
                        BidModifierTestHelper.getZeroTablet())
        );

        assertThat(result).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0)), deviceBidModifiersAllZeros()))));
    }

    @Test
    public void validateAddStep2_noConflictingModifiers() {
        var result = testValidateAddStep2(
                CampaignType.TEXT,
                List.of(BidModifierTestHelper.getZeroDesktopOnly()),
                List.of(BidModifierTestHelper.getZeroDesktop())
        );

        assertThat(result).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0)), expressionConditionsIntersection()))));
    }
}
