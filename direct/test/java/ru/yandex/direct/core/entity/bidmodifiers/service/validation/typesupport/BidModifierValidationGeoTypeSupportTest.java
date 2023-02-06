package ru.yandex.direct.core.entity.bidmodifiers.service.validation.typesupport;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.adgroup.model.CpmBannerAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.CriterionType;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierGeo;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierRegionalAdjustment;
import ru.yandex.direct.core.entity.bidmodifiers.validation.typesupport.BidModifierValidationGeoTypeSupport;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.regions.GeoTree;
import ru.yandex.direct.regions.GeoTreeFactory;
import ru.yandex.direct.regions.GeoTreeType;
import ru.yandex.direct.regions.SimpleGeoTreeFactory;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.Path;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.entity.bidmodifier.BidModifierGeo.REGIONAL_ADJUSTMENTS;
import static ru.yandex.direct.core.entity.bidmodifiers.validation.BidModifiersDefects.geoBidModifiersNotSupportedOnAdGroups;
import static ru.yandex.direct.core.entity.bidmodifiers.validation.BidModifiersDefects.notSupportedMultiplier;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

public class BidModifierValidationGeoTypeSupportTest {
    private static final Path errorPath = path(field(REGIONAL_ADJUSTMENTS.name()), index(0));
    private ClientId clientId;
    private CpmBannerAdGroup adGroupCpmBanner;
    private BidModifierGeo modifier;
    private BidModifierValidationGeoTypeSupport service;

    @Before
    public void setUp() {
        clientId = ClientId.fromLong(1L);
        adGroupCpmBanner = new CpmBannerAdGroup().withType(AdGroupType.CPM_BANNER);
        modifier = new BidModifierGeo().withRegionalAdjustments(singletonList(
                new BidModifierRegionalAdjustment().withPercent(120).withRegionId(100L).withHidden(false)));

        GeoTree mockGeoTree = mock(GeoTree.class);
        when(mockGeoTree.hasRegion(100L)).thenReturn(true);
        GeoTreeFactory mockGeoTreeFactory = new SimpleGeoTreeFactory(Map.of(GeoTreeType.GLOBAL, mockGeoTree));

        service = new BidModifierValidationGeoTypeSupport(mockGeoTreeFactory);
    }

    @Test
    public void validateAddStep1_adGroup_errorIsGenerated() {
        ValidationResult<BidModifierGeo, Defect> vr = service.validateAddStep1(modifier.withAdGroupId(1L),
                CampaignType.CPM_BANNER, adGroupCpmBanner.withCriterionType(CriterionType.KEYWORD), clientId, null);
        // Geo-корректировка никогда не устанавливалась на группах
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(
                validationError(path(), geoBidModifiersNotSupportedOnAdGroups()))));
    }

    @Test
    public void validateAddStep1_cpmBannerCampaign_errorIsGenerated() {
        ValidationResult<BidModifierGeo, Defect> vr = service.validateAddStep1(modifier.withCampaignId(1L),
                CampaignType.CPM_BANNER, null, clientId, null);
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(
                validationError(errorPath, notSupportedMultiplier()))));
    }

    @Test
    public void validateAddStep1_cpmDealsCampaign_errorIsGenerated() {
        ValidationResult<BidModifierGeo, Defect> vr = service.validateAddStep1(modifier.withCampaignId(1L),
                CampaignType.CPM_DEALS, null, clientId, null);
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(
                validationError(errorPath, notSupportedMultiplier()))));
    }

    @Test
    public void validateAddStep1_cpmYndxFrontpageCampaign_errorIsGenerated() {
        ValidationResult<BidModifierGeo, Defect> vr = service.validateAddStep1(modifier.withCampaignId(1L),
                CampaignType.CPM_YNDX_FRONTPAGE, null, clientId, null);
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(
                validationError(errorPath, notSupportedMultiplier()))));
    }

    @Test
    public void validateAddStep1_contentPromotionCampaign_errorIsGenerated() {
        ValidationResult<BidModifierGeo, Defect> vr = service.validateAddStep1(modifier.withCampaignId(1L),
                CampaignType.CONTENT_PROMOTION, null, clientId, null);
        assertThat(vr).is(matchedBy(hasNoDefectsDefinitions()));
    }
}
