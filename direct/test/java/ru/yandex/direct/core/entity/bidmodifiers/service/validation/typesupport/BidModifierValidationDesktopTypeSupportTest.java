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
import ru.yandex.direct.core.entity.bidmodifier.BidModifierDesktop;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierDesktopAdjustment;
import ru.yandex.direct.core.entity.bidmodifiers.container.BidModifierKey;
import ru.yandex.direct.core.entity.bidmodifiers.repository.FakeBidModifierRepository;
import ru.yandex.direct.core.entity.bidmodifiers.service.CachingFeaturesProvider;
import ru.yandex.direct.core.entity.bidmodifiers.validation.typesupport.BidModifierValidationDesktopTypeSupport;
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
import static ru.yandex.direct.core.entity.bidmodifier.BidModifierDesktop.DESKTOP_ADJUSTMENT;
import static ru.yandex.direct.core.entity.bidmodifiers.validation.BidModifiersDefects.deviceBidModifiersAllZeros;
import static ru.yandex.direct.core.entity.bidmodifiers.validation.BidModifiersDefects.expressionConditionsIntersection;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoErrors;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

public class BidModifierValidationDesktopTypeSupportTest {
    private static final Path errorPath = path(field(DESKTOP_ADJUSTMENT.name()));

    private ClientId clientId;
    private BidModifierDesktop modifier;
    private BidModifierValidationDesktopTypeSupport service;
    private FeatureService featureService;
    private FakeBidModifierRepository bidModifierRepository;
    private DeviceModifiersConflictChecker conflictChecker;

    @Before
    public void setUp() {
        clientId = ClientId.fromLong(1L);
        modifier = new BidModifierDesktop().withDesktopAdjustment(
                new BidModifierDesktopAdjustment().withPercent(120));

        bidModifierRepository = new FakeBidModifierRepository(List.of());
        conflictChecker = new DeviceModifiersConflictChecker(new FakeShardByClient(), bidModifierRepository);
        service = new BidModifierValidationDesktopTypeSupport(conflictChecker);
        featureService = mock(FeatureService.class);
    }

    @Test
    public void validateAddStep1_contentPromotionCampaign_success() {
        ValidationResult<BidModifierDesktop, Defect> vr = service.validateAddStep1(modifier.withCampaignId(1L),
                CampaignType.CONTENT_PROMOTION, new AdGroup().withType(AdGroupType.BASE), clientId,
                new CachingFeaturesProvider(featureService));
        assertThat(vr).is(matchedBy(hasNoErrors()));
    }

    @Test
    public void validateAddStep1_cpmYndxFrontpageCampaign_success() {
        ValidationResult<BidModifierDesktop, Defect> vr = service.validateAddStep1(modifier.withCampaignId(1L),
                CampaignType.CPM_YNDX_FRONTPAGE, new AdGroup().withType(AdGroupType.BASE), clientId,
                new CachingFeaturesProvider(featureService));
        assertThat(vr).is(matchedBy(hasNoErrors()));
    }

    private ValidationResult<List<BidModifierDesktop>, Defect> testValidateAddStep2(
            CampaignType campaignType,
            List<BidModifier> modifiersInOperation,
            List<BidModifier> modifiersInDB
    ) {
        bidModifierRepository = new FakeBidModifierRepository(modifiersInDB);
        conflictChecker = new DeviceModifiersConflictChecker(new FakeShardByClient(), bidModifierRepository);
        service = new BidModifierValidationDesktopTypeSupport(conflictChecker);

        Map<Long, CampaignType> campaignTypes = Map.of(1L, campaignType);
        Map<Long, AdGroup> adGroupsWithType = Map.of(1L, new AdGroup().withType(AdGroupType.BASE));

        var modifiersToValidate = modifiersInOperation.stream()
                .filter(m -> m instanceof BidModifierDesktop)
                .map(m -> (BidModifierDesktop) m)
                .collect(Collectors.toList());
        Map<BidModifierKey, BidModifier> allValidBidModifiersInOperation = modifiersInOperation
                .stream().collect(Collectors.toMap(BidModifierKey::new, m -> m));

        Map<BidModifierKey, BidModifierDesktop> existingModifiers = modifiersInDB.stream()
                .filter(m -> m instanceof BidModifierDesktop)
                .map(m -> (BidModifierDesktop) m)
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
                List.of(BidModifierTestHelper.getDesktop()),
                Collections.emptyList()
        );

        assertThat(result).is(matchedBy(hasNoErrors()));
    }

    @Test
    public void validateAddStep2_noZeroValues_forTextCampaign() {
        var result = testValidateAddStep2(
                CampaignType.TEXT,
                List.of(BidModifierTestHelper.getZeroDesktop(), BidModifierTestHelper.getZeroMobile()),
                Collections.emptyList()
        );

        assertThat(result).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0)), deviceBidModifiersAllZeros()))));
    }

    @Test
    public void validateAddStep2_noZeroValues_forMediaCampaign_noFeature() {
        var result = testValidateAddStep2(
                CampaignType.CPM_BANNER,
                List.of(BidModifierTestHelper.getZeroDesktop(), BidModifierTestHelper.getZeroMobile()),
                Collections.emptyList()
        );

        assertThat(result).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0)), deviceBidModifiersAllZeros()))));
    }

    @Test
    public void validateAddStep2_allowsSameZeroValues_forMediaCampaign_withFeature() {
        when(featureService.isEnabledForClientId(clientId, FeatureName.SMARTTV_BID_MODIFIER_ENABLED)).thenReturn(true);

        var result = testValidateAddStep2(
                CampaignType.CPM_BANNER,
                List.of(BidModifierTestHelper.getZeroDesktop(), BidModifierTestHelper.getZeroMobile()),
                Collections.emptyList()
        );

        assertThat(result).is(matchedBy(hasNoErrors()));
    }

    @Test
    public void validateAddStep2_noZeroValues_forMediaCampaign_withFeature() {
        when(featureService.isEnabledForClientId(clientId, FeatureName.SMARTTV_BID_MODIFIER_ENABLED)).thenReturn(true);

        var result = testValidateAddStep2(
                CampaignType.CPM_BANNER,
                List.of(BidModifierTestHelper.getZeroDesktop(),
                        BidModifierTestHelper.getZeroMobile()
                ),
                List.of(BidModifierTestHelper.getZeroSmartTv())
        );

        assertThat(result).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0)), deviceBidModifiersAllZeros()))));
    }

    @Test
    public void validateAddStep2_noConflictingModifiers() {
        var result = testValidateAddStep2(
                CampaignType.TEXT,
                List.of(BidModifierTestHelper.getZeroDesktop()),
                List.of(BidModifierTestHelper.getZeroDesktopOnly())
        );

        assertThat(result).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0)), expressionConditionsIntersection()))));
    }
}
