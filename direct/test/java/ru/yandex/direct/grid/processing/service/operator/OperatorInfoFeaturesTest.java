package ru.yandex.direct.grid.processing.service.operator;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import ru.yandex.direct.common.TranslationService;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.grid.processing.model.client.GdCoreFeatureWithDescription;
import ru.yandex.direct.grid.processing.model.client.GdOperatorFeatures;
import ru.yandex.direct.grid.processing.service.client.converter.GdFeatureWithDescriptionConverterService;
import ru.yandex.direct.rbac.RbacRole;

import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(JUnitParamsRunner.class)
public class OperatorInfoFeaturesTest {

    private GdFeatureWithDescriptionConverterService gdFeatureWithDescriptionConverterService;

    @Before
    public void before() {
        TranslationService translationService = mock(TranslationService.class);
        when(translationService.translate(any())).thenReturn("mocked description");
        gdFeatureWithDescriptionConverterService = new GdFeatureWithDescriptionConverterService(translationService);
    }


    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                {
                        "Все фичи",
                        ImmutableSet.of(FeatureName.GRID.getName(),
                                FeatureName.CAMPAIGN_CREATION_STEPS_ENABLED_FOR_DNA.getName(),
                                FeatureName.ADS_MASS_EDIT_FOR_DNA.getName(),
                                FeatureName.BANNER_UPDATE_IN_JAVA_FOR_DNA.getName(),
                                FeatureName.CPM_BANNER_UPDATE_IN_JAVA_FOR_DNA.getName(),
                                FeatureName.CPM_ADGROUP_UPDATE_IN_JAVA_FOR_DNA.getName(),
                                FeatureName.PERFORMANCE_AD_EDIT_ALLOWED_IN_DNA.getName(),
                                FeatureName.BANNER_AIMING_ALLOWED.getName(),
                                FeatureName.BANNER_AIMING_CPM_ALLOWED.getName(),
                                FeatureName.BANNER_AIMING_CPM_YNDX_FRONTPAGE_ALLOWED.getName(),
                                FeatureName.SHOW_RETARGETING_GRID.getName(),
                                FeatureName.USER_PROFILE_PAGE_ALLOWED.getName(),
                                FeatureName.SHOW_RETARGETING_GRID.getName(),
                                FeatureName.SHOW_AGGREGATED_STATUS.getName(),
                                FeatureName.SHOW_AGGREGATED_STATUS_DEBUG.getName(),
                                FeatureName.SHOW_AGGREGATED_STATUS_OPEN_BETA.getName(),
                                FeatureName.HIDE_OLD_TEXT_SMART_EDIT_FOR_DNA.getName(),
                                FeatureName.SERVICE_WORKER_ALLOWED.getName(),
                                FeatureName.CPM_VIDEO_GROUPS_EDIT_FOR_DNA.getName(),
                                FeatureName.CPM_AUDIO_GROUPS_EDIT_FOR_DNA.getName(),
                                FeatureName.CPM_OUTDOOR_GROUPS_EDIT_FOR_DNA.getName(),
                                FeatureName.CPM_INDOOR_GROUPS_EDIT_FOR_DNA.getName(),
                                FeatureName.CPM_GEOPRODUCT_GROUPS_EDIT_FOR_DNA.getName(),
                                FeatureName.WEBVISOR_ENABLED_FOR_DNA.getName(),
                                FeatureName.ENABLE_PRELOAD_ASSETS.getName(),
                                FeatureName.ENABLE_PREFETCH_ASSETS.getName(),
                                FeatureName.ENABLE_LONG_TERM_CACHING.getName(),
                                FeatureName.MCBANNER_DNA.getName(),
                                FeatureName.MCBANNER_CAMPAIGN_DNA.getName(),
                                FeatureName.RMP_GROUPS_EDIT_FOR_DNA.getName(),
                                FeatureName.RMP_BANNER_UPDATE_IN_JAVA_FOR_DNA.getName(),
                                FeatureName.TARGET_TAGS_ALLOWED.getName(),
                                FeatureName.KEYWORDS_EDIT_ON_BANNERS_PAGE_ALLOWED_FOR_DNA.getName(),
                                FeatureName.PERFORMANCE_CAMPAINGS_EDIT_ALLOWED_IN_DNA.getName(),
                                FeatureName.DYNAMIC_CAMPAIGNS_GROUPS_AND_ADS_EDIT_ALLOWED_IN_DNA.getName(),
                                FeatureName.GOALS_ONLY_WITH_CAMPAIGN_COUNTERS_USED.getName(),
                                FeatureName.DEFAULT_AUTOBUDGET_AVG_CPA.getName(),
                                FeatureName.DEFAULT_AUTOBUDGET_AVG_CLICK_WITH_WEEK_BUDGET.getName(),
                                FeatureName.DEFAULT_AUTOBUDGET_ROI.getName(),
                                FeatureName.HIDE_OLD_SHOW_CAMPS_FOR_DNA.getName(),
                                FeatureName.SHOW_CAMP_LINK_BY_CELL_HOVER_ENABLED_FOR_DNA.getName(),
                                FeatureName.SHOW_CAMP_LINK_BY_NAME_CLICK_ENABLED_FOR_DNA.getName(),
                                FeatureName.SHOW_CAMP_LINK_IN_GRID_CELL_ENABLED_FOR_DNA.getName(),
                                FeatureName.SHOW_CAMP_LINK_IN_POPUP_ENABLED_FOR_DNA.getName(),
                                FeatureName.BULK_OPS_CAMPAIGNS_EDIT_STRATEGIES.getName(),
                                FeatureName.BULK_OPS_CAMPAIGNS_EDIT_ORGANIZATION.getName(),
                                FeatureName.BULK_OPS_CAMPAIGNS_EDIT_DAY_BUDGET.getName(),
                                FeatureName.YDB_FILTER_SHORTCUTS_ENABLED.getName(),
                                FeatureName.YDB_FILTER_SHORTCUTS_FOR_CAMPAIGN_ENABLED.getName(),
                                FeatureName.SHOW_DNA_BY_DEFAULT.getName(),
                                FeatureName.SUGGEST_GENERATED_GROUP_PHRASES_FOR_OPERATOR.getName(),
                                FeatureName.SUGGEST_GENERATED_TITLE_AND_SNIPPET_FOR_OPERATOR.getName(),
                                FeatureName.SUGGEST_GENERATED_PHRASES_BY_SNIPPET_FOR_OPERATOR.getName(),
                                FeatureName.SUGGEST_GENERATED_IMAGES_FOR_OPERATOR.getName(),
                                FeatureName.SUGGEST_GENERATED_SITELINKS_FOR_OPERATOR.getName(),
                                FeatureName.SUGGEST_GENERATED_REGIONS_FOR_OPERATOR.getName(),
                                FeatureName.SHOW_SIDEBAR_FOR_ALL_FOR_DNA.getName(),
                                FeatureName.LOADING_DNA_SCRIPTS_BEFORE_OLD_INTERFACE_SCRIPTS_ENABLED.getName(),
                                FeatureName.ENABLE_SPELLER_ON_AD_EDIT.getName(),
                                FeatureName.INTERCLIENT_CAMPAIGN_COPY_ALLOWED.getName(),
                                FeatureName.ENABLE_HOVERABLE_MENU.getName(),
                                FeatureName.ENABLE_INTERNAL_LINKS_IN_SAME_WINDOW.getName(),
                                FeatureName.UC_DESIGN_FOR_DNA_EDIT_ENABLED.getName(),
                                FeatureName.UC_DESIGN_FOR_DNA_GRID_ENABLED.getName(),
                                FeatureName.SET_CAMPAIGN_DISALLOWED_PAGE_IDS.getName(),
                                FeatureName.BRAND_LIFT_HIDDEN.getName()
                        ),
                        new GdOperatorFeatures().withIsGridEnabled(true)
                                .withIsCampaignCreationStepsEnabledForDna(true)
                                .withIsAdsMassEditAllowed(true)
                                .withIsRmpGroupsUpdateAllowed(true)
                                .withIsRmpBannersUpdateAllowed(true)
                                .withIsBannerUpdateAllowed(true)
                                .withIsCpmBannerUpdateAllowed(true)
                                .withIsBannerAimingAllowed(true)
                                .withIsCpmAdGroupUpdateAllowed(true)
                                .withIsPerformanceAdEditAllowed(true)
                                .withIsBannerAimingCpmAllowed(true)
                                .withIsBannerAimingCpmYndxFrontpageAllowed(true)
                                .withIsRetargetingGridEnabled(true)
                                .withIsAggregatedStatusAllowed(true)
                                .withIsAggregatedStatusDebugAllowed(true)
                                .withIsAggregatedStatusOpenBetaAllowed(true)
                                .withIsUserProfilePageAllowed(true)
                                .withIsOldTextSmartEditHidden(true)
                                .withIsServiceWorkerAllowed(true)
                                .withIsVideoGroupsEditAllowed(true)
                                .withIsAudioGroupsEditAllowed(true)
                                .withIsOutdoorGroupsEditAllowed(true)
                                .withIsIndoorGroupsEditAllowed(true)
                                .withIsGeoproductGroupsEditAllowed(true)
                                .withIsWebvisorEnabledForDna(true)
                                .withUcDesignForDnaEditEnabled(true)
                                .withUcDesignForDnaGridEnabled(true)
                                .withEnablePreloadAssets(true)
                                .withEnablePrefetchAssets(true)
                                .withEnableLongTermCaching(true)
                                .withIsMcBannerDnaEnabled(true)
                                .withIsMcBannerCampaignDnaEnabled(true)
                                .withIsTargetTagsAllowed(true)
                                .withIsKeywordsEditOnBannersPageAllowed(true)
                                .withIsPerformanceCampaignsEditAllowed(true)
                                .withIsDynamicCampaignsGroupsAndAdsEditAllowed(true)
                                .withIsGoalsOnlyWithCampaignCountersUsed(true)
                                .withIsDefaultAutobudgetAvgCpaEnabled(true)
                                .withIsDefaultAutobudgetAvgClickWithWeekBudgetEnabled(true)
                                .withIsDefaultAutobudgetRoiEnabled(true)
                                .withIsOldShowCampsHidden(true)
                                .withIsShowDnaByDefaultEnabled(true)
                                .withIsShowCampLinkByCellHoverEnabledForDna(true)
                                .withIsShowCampLinkByNameClickEnabledForDna(true)
                                .withIsShowCampLinkInGridCellEnabledForDna(true)
                                .withIsShowCampLinkInPopupEnabledForDna(true)
                                .withIsBulkOpsCampaignsEditOrganizationEnabled(true)
                                .withIsBulkOpsCampaignsEditStrategiesEnabled(true)
                                .withIsBulkOpsCampaignsEditDayBudgetEnabled(true)
                                .withIsFilterShortcutsEnabled(true)
                                .withIsFilterShortcutsForCampaignEnabled(true)
                                .withIsShowSidebarForAllEnabledForDna(true)
                                .withIsSuggestGeneratedGroupPhrasesForOperator(true)
                                .withIsSuggestGeneratedTitleAndSnippetForOperator(true)
                                .withIsSuggestGeneratedPhrasesBySnippetForOperator(true)
                                .withIsSuggestGeneratedImagesForOperator(true)
                                .withIsSuggestGeneratedSitelinksForOperator(true)
                                .withIsSuggestGeneratedRegionsForOperator(true)
                                .withIsLoadingDnaScriptsBeforeOldInterfaceScriptsEnabled(true)
                                .withIsSpellerOnEditAdEnabled(true)
                                .withIsInterClientCampaignCopyAllowed(true)
                                .withIsHoverableMenuEnabled(true)
                                .withIsInternalLinksInSameWindowEnabled(true)
                                .withIsSetCampaignDisallowedPageIdsEnabled(true)
                                .withIsBrandLiftCpmYndxFrontpageAllowed(true)
                },
                {
                        "нет фич",
                        emptySet(),
                        new GdOperatorFeatures().withIsGridEnabled(false)
                                .withIsCampaignCreationStepsEnabledForDna(false)
                                .withIsAdsMassEditAllowed(false)
                                .withIsRmpGroupsUpdateAllowed(false)
                                .withIsRmpBannersUpdateAllowed(false)
                                .withIsBannerUpdateAllowed(false)
                                .withIsCpmBannerUpdateAllowed(false)
                                .withIsBannerAimingAllowed(false)
                                .withIsCpmAdGroupUpdateAllowed(false)
                                .withIsPerformanceAdEditAllowed(false)
                                .withIsBannerAimingCpmAllowed(false)
                                .withIsBannerAimingCpmYndxFrontpageAllowed(false)
                                .withIsRetargetingGridEnabled(false)
                                .withIsUserProfilePageAllowed(false)
                                .withIsOldTextSmartEditHidden(false)
                                .withIsServiceWorkerAllowed(false)
                                .withIsVideoGroupsEditAllowed(false)
                                .withIsAudioGroupsEditAllowed(false)
                                .withIsOutdoorGroupsEditAllowed(false)
                                .withIsIndoorGroupsEditAllowed(false)
                                .withIsWebvisorEnabledForDna(false)
                                .withUcDesignForDnaEditEnabled(false)
                                .withUcDesignForDnaGridEnabled(false)
                                .withEnablePreloadAssets(false)
                                .withEnablePrefetchAssets(false)
                                .withEnableLongTermCaching(false)
                                .withIsGeoproductGroupsEditAllowed(false)
                                .withIsAggregatedStatusAllowed(false)
                                .withIsAggregatedStatusDebugAllowed(false)
                                .withIsAggregatedStatusOpenBetaAllowed(false)
                                .withIsMcBannerDnaEnabled(false)
                                .withIsMcBannerCampaignDnaEnabled(false)
                                .withIsTargetTagsAllowed(false)
                                .withIsKeywordsEditOnBannersPageAllowed(false)
                                .withIsPerformanceCampaignsEditAllowed(false)
                                .withIsDynamicCampaignsGroupsAndAdsEditAllowed(false)
                                .withIsGoalsOnlyWithCampaignCountersUsed(false)
                                .withIsDefaultAutobudgetAvgCpaEnabled(false)
                                .withIsDefaultAutobudgetAvgClickWithWeekBudgetEnabled(false)
                                .withIsDefaultAutobudgetRoiEnabled(false)
                                .withIsOldShowCampsHidden(false)
                                .withIsShowDnaByDefaultEnabled(false)
                                .withIsShowCampLinkByCellHoverEnabledForDna(false)
                                .withIsShowCampLinkByNameClickEnabledForDna(false)
                                .withIsShowCampLinkInGridCellEnabledForDna(false)
                                .withIsShowCampLinkInPopupEnabledForDna(false)
                                .withIsBulkOpsCampaignsEditOrganizationEnabled(false)
                                .withIsBulkOpsCampaignsEditStrategiesEnabled(false)
                                .withIsBulkOpsCampaignsEditDayBudgetEnabled(false)
                                .withIsFilterShortcutsEnabled(false)
                                .withIsFilterShortcutsForCampaignEnabled(false)
                                .withIsShowSidebarForAllEnabledForDna(false)
                                .withIsSuggestGeneratedGroupPhrasesForOperator(false)
                                .withIsSuggestGeneratedTitleAndSnippetForOperator(false)
                                .withIsSuggestGeneratedPhrasesBySnippetForOperator(false)
                                .withIsSuggestGeneratedImagesForOperator(false)
                                .withIsSuggestGeneratedSitelinksForOperator(false)
                                .withIsSuggestGeneratedRegionsForOperator(false)
                                .withIsLoadingDnaScriptsBeforeOldInterfaceScriptsEnabled(false)
                                .withIsSpellerOnEditAdEnabled(false)
                                .withIsInterClientCampaignCopyAllowed(false)
                                .withIsHoverableMenuEnabled(false)
                                .withIsInternalLinksInSameWindowEnabled(false)
                                .withIsSetCampaignDisallowedPageIdsEnabled(false)
                                .withIsBrandLiftCpmYndxFrontpageAllowed(false)
                },
                {
                        "частично есть фичи",
                        ImmutableSet.of(FeatureName.GRID.getName(),
                                FeatureName.BANNER_AIMING_ALLOWED.getName()),
                        new GdOperatorFeatures().withIsGridEnabled(true)
                                .withIsCampaignCreationStepsEnabledForDna(false)
                                .withIsAdsMassEditAllowed(false)
                                .withIsRmpGroupsUpdateAllowed(false)
                                .withIsRmpBannersUpdateAllowed(false)
                                .withIsBannerUpdateAllowed(false)
                                .withIsCpmBannerUpdateAllowed(false)
                                .withIsBannerAimingAllowed(true)
                                .withIsCpmAdGroupUpdateAllowed(false)
                                .withIsPerformanceAdEditAllowed(false)
                                .withIsBannerAimingCpmAllowed(false)
                                .withIsBannerAimingCpmYndxFrontpageAllowed(false)
                                .withIsRetargetingGridEnabled(false)
                                .withIsUserProfilePageAllowed(false)
                                .withIsOldTextSmartEditHidden(false)
                                .withIsWebvisorEnabledForDna(false)
                                .withUcDesignForDnaEditEnabled(false)
                                .withUcDesignForDnaGridEnabled(false)
                                .withEnablePreloadAssets(false)
                                .withEnablePrefetchAssets(false)
                                .withEnableLongTermCaching(false)
                                .withIsServiceWorkerAllowed(false)
                                .withIsVideoGroupsEditAllowed(false)
                                .withIsAudioGroupsEditAllowed(false)
                                .withIsOutdoorGroupsEditAllowed(false)
                                .withIsIndoorGroupsEditAllowed(false)
                                .withIsGeoproductGroupsEditAllowed(false)
                                .withIsMcBannerDnaEnabled(false)
                                .withIsMcBannerCampaignDnaEnabled(false)
                                .withIsAggregatedStatusAllowed(false)
                                .withIsAggregatedStatusDebugAllowed(false)
                                .withIsAggregatedStatusOpenBetaAllowed(false)
                                .withIsTargetTagsAllowed(false)
                                .withIsKeywordsEditOnBannersPageAllowed(false)
                                .withIsPerformanceCampaignsEditAllowed(false)
                                .withIsDynamicCampaignsGroupsAndAdsEditAllowed(false)
                                .withIsGoalsOnlyWithCampaignCountersUsed(false)
                                .withIsDefaultAutobudgetAvgCpaEnabled(false)
                                .withIsDefaultAutobudgetAvgClickWithWeekBudgetEnabled(false)
                                .withIsDefaultAutobudgetRoiEnabled(false)
                                .withIsOldShowCampsHidden(false)
                                .withIsShowDnaByDefaultEnabled(false)
                                .withIsShowCampLinkByCellHoverEnabledForDna(false)
                                .withIsShowCampLinkByNameClickEnabledForDna(false)
                                .withIsShowCampLinkInGridCellEnabledForDna(false)
                                .withIsShowCampLinkInPopupEnabledForDna(false)
                                .withIsBulkOpsCampaignsEditOrganizationEnabled(false)
                                .withIsBulkOpsCampaignsEditStrategiesEnabled(false)
                                .withIsBulkOpsCampaignsEditDayBudgetEnabled(false)
                                .withIsFilterShortcutsEnabled(false)
                                .withIsFilterShortcutsForCampaignEnabled(false)
                                .withIsShowSidebarForAllEnabledForDna(false)
                                .withIsSuggestGeneratedGroupPhrasesForOperator(false)
                                .withIsSuggestGeneratedTitleAndSnippetForOperator(false)
                                .withIsSuggestGeneratedPhrasesBySnippetForOperator(false)
                                .withIsSuggestGeneratedImagesForOperator(false)
                                .withIsSuggestGeneratedSitelinksForOperator(false)
                                .withIsSuggestGeneratedRegionsForOperator(false)
                                .withIsLoadingDnaScriptsBeforeOldInterfaceScriptsEnabled(false)
                                .withIsSpellerOnEditAdEnabled(false)
                                .withIsInterClientCampaignCopyAllowed(false)
                                .withIsHoverableMenuEnabled(false)
                                .withIsInternalLinksInSameWindowEnabled(false)
                                .withIsSetCampaignDisallowedPageIdsEnabled(false)
                                .withIsBrandLiftCpmYndxFrontpageAllowed(false)
                },
        });
    }

    @Test
    @Parameters(method = "parameters")
    @TestCaseName("{0}")
    public void testGetOperatorFeatures(@SuppressWarnings("unused") String testName,
                                        Set<String> availableFeatures, GdOperatorFeatures expected) {

        Set<GdCoreFeatureWithDescription> availableOperatorCoreFeatures =
                gdFeatureWithDescriptionConverterService.convertToCore(availableFeatures);

        expected
                .withAvailableOperatorCoreFeatures(availableOperatorCoreFeatures);

        GdOperatorFeatures actual = OperatorConverter.
                toGdOperatorFeatures(availableFeatures, RbacRole.CLIENT, availableOperatorCoreFeatures);
        assertThat(actual).isEqualTo(expected);
    }
}
