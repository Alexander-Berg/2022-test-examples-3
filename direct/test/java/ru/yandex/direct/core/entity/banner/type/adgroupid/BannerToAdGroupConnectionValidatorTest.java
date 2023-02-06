package ru.yandex.direct.core.entity.banner.type.adgroupid;

import java.util.Map;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdgroupType;
import ru.yandex.direct.core.entity.banner.model.BannerWithAdGroupId;
import ru.yandex.direct.core.entity.banner.model.ContentPromotionBanner;
import ru.yandex.direct.core.entity.banner.model.InternalBanner;
import ru.yandex.direct.core.entity.banner.service.validation.type.BannerTypeValidationPredicates;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.service.accesschecker.CampaignSubObjectAccessConstraint;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.emptyMap;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static ru.yandex.direct.core.entity.banner.service.validation.BannerConstants.MAX_BANNERS_IN_ADGROUP;
import static ru.yandex.direct.core.entity.banner.service.validation.BannerConstants.MAX_BANNERS_IN_INTERNAL_CAMPAIGN;
import static ru.yandex.direct.core.entity.banner.service.validation.BannerConstants.MAX_BANNERS_IN_UNIVERSAL_APP_CAMPAIGN;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.maxBannersInAdGroup;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.maxBannersInInternalCampaign;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.maxBannersInUniversalAppCampaign;
import static ru.yandex.direct.core.testing.data.adgroup.TestContentPromotionAdGroups.fullContentPromotionAdGroup;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.path;

@RunWith(JUnitParamsRunner.class)
public class BannerToAdGroupConnectionValidatorTest {

    private static final Long CAMPAIGN_ID = 1L;
    private static final Long ADGROUP_ID = 2L;

    private CampaignSubObjectAccessConstraint campaignLevelAccessConstraint;

    @Before
    public void setUp() {
        campaignLevelAccessConstraint = mock(CampaignSubObjectAccessConstraint.class);
    }

    @Test
    public void invalidBanner_add_limitBannersInAdGroup() {
        var validationContainer = BannerWithAdGroupIdValidationContainer.create(
                Map.of(ADGROUP_ID, fullContentPromotionAdGroup(CAMPAIGN_ID, ContentPromotionAdgroupType.VIDEO)),
                Map.of(ADGROUP_ID, MAX_BANNERS_IN_ADGROUP.longValue() + 1),
                emptyMap(),
                emptyMap(),
                emptyMap());
        var bannerToAdd = new ContentPromotionBanner()
                .withAdGroupId(ADGROUP_ID);
        var validator = getConnectionValidator(validationContainer, true);
        ValidationResult<BannerWithAdGroupId, Defect> validationResult = validator.apply(bannerToAdd);
        assertThat(validationResult, hasDefectDefinitionWith(validationError(
                path(), maxBannersInAdGroup(MAX_BANNERS_IN_ADGROUP))));
    }

    @SuppressWarnings("unused")
    private Object[] campaignTypesParameters() {
        return new Object[][]{
                {"РМП", CampaignType.MOBILE_CONTENT},
                {"ТГО", CampaignType.TEXT},
        };
    }

    @Test
    @Parameters(method = "campaignTypesParameters")
    @TestCaseName("{0}")
    public void banner_add_limitBannersInAdGroupOfUniversalAppCampaign(@SuppressWarnings("unused") String description,
                                                                       CampaignType campaignType) {
        var validationContainer = BannerWithAdGroupIdValidationContainer.create(
                Map.of(ADGROUP_ID, fullContentPromotionAdGroup(CAMPAIGN_ID, ContentPromotionAdgroupType.VIDEO)),
                Map.of(ADGROUP_ID, MAX_BANNERS_IN_ADGROUP.longValue() + 1),
                Map.of(ADGROUP_ID, MAX_BANNERS_IN_ADGROUP.longValue() + 1),
                Map.of(CAMPAIGN_ID, MAX_BANNERS_IN_UNIVERSAL_APP_CAMPAIGN),
                Map.of(CAMPAIGN_ID, campaignType));
        var bannerToAdd = new ContentPromotionBanner()
                .withAdGroupId(ADGROUP_ID);
        var validator = getConnectionValidator(validationContainer, true);
        ValidationResult<BannerWithAdGroupId, Defect> validationResult = validator.apply(bannerToAdd);
        assertThat(validationResult, hasNoDefectsDefinitions());
    }

    @Test
    @Parameters(method = "campaignTypesParameters")
    @TestCaseName("{0}")
    public void invalidBanner_add_limitBannersInUniversalCampaign(@SuppressWarnings("unused") String description,
                                                                  CampaignType campaignType) {
        var validationContainer = BannerWithAdGroupIdValidationContainer.create(
                Map.of(ADGROUP_ID, fullContentPromotionAdGroup(CAMPAIGN_ID, ContentPromotionAdgroupType.VIDEO)),
                Map.of(ADGROUP_ID, 1L),
                Map.of(ADGROUP_ID, 1L),
                Map.of(CAMPAIGN_ID, MAX_BANNERS_IN_UNIVERSAL_APP_CAMPAIGN + 1),
                Map.of(CAMPAIGN_ID, campaignType));
        var bannerToAdd = new ContentPromotionBanner()
                .withAdGroupId(ADGROUP_ID);
        var validator = getConnectionValidator(validationContainer, true);
        ValidationResult<BannerWithAdGroupId, Defect> validationResult = validator.apply(bannerToAdd);
        assertThat(validationResult, hasDefectDefinitionWith(validationError(
                path(), maxBannersInUniversalAppCampaign(MAX_BANNERS_IN_UNIVERSAL_APP_CAMPAIGN))));
    }

    @Test
    public void invalidBanner_add_limitBannersInInternalCampaign() {
        var validationContainer = BannerWithAdGroupIdValidationContainer.create(
                Map.of(ADGROUP_ID, fullContentPromotionAdGroup(CAMPAIGN_ID, ContentPromotionAdgroupType.VIDEO)),
                Map.of(ADGROUP_ID, 1L),
                emptyMap(),
                Map.of(CAMPAIGN_ID, MAX_BANNERS_IN_INTERNAL_CAMPAIGN + 1),
                emptyMap()
        );
        var bannerToAdd = new InternalBanner()
                .withAdGroupId(ADGROUP_ID);
        var validator = getConnectionValidator(validationContainer, true);
        ValidationResult<BannerWithAdGroupId, Defect> validationResult = validator.apply(bannerToAdd);
        assertThat(validationResult, hasDefectDefinitionWith(validationError(
                path(), maxBannersInInternalCampaign(MAX_BANNERS_IN_INTERNAL_CAMPAIGN))));
    }

    @Test
    public void banner_update_noLimitBannersInAdGroup() {
        var validationContainer = BannerWithAdGroupIdValidationContainer.create(
                Map.of(ADGROUP_ID, fullContentPromotionAdGroup(CAMPAIGN_ID, ContentPromotionAdgroupType.VIDEO)),
                Map.of(ADGROUP_ID, MAX_BANNERS_IN_ADGROUP.longValue() + 1),
                emptyMap(),
                emptyMap(),
                emptyMap());
        var bannerToAdd = new ContentPromotionBanner()
                .withAdGroupId(ADGROUP_ID);
        var validator = getConnectionValidator(validationContainer, false);
        ValidationResult<BannerWithAdGroupId, Defect> validationResult = validator.apply(bannerToAdd);
        assertThat(validationResult, hasNoDefectsDefinitions());
    }

    private BannerToAdGroupConnectionValidator<BannerWithAdGroupId> getConnectionValidator(
            BannerWithAdGroupIdValidationContainer validationContainer, boolean isAddOperation) {
        return new BannerToAdGroupConnectionValidator<>(
                BannerWithAdGroupId.AD_GROUP_ID,
                BannerTypeValidationPredicates::isInternalBanner,
                BannerTypeValidationPredicates::isPerformanceBannerMain,
                validationContainer,
                campaignLevelAccessConstraint,
                isAddOperation);
    }
}
