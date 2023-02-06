package ru.yandex.direct.grid.processing.service.group;

import java.util.Collections;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import graphql.ExecutionResult;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.TextAdGroup;
import ru.yandex.direct.core.entity.adgroup.service.AdGroupService;
import ru.yandex.direct.core.entity.banner.model.old.OldPerformanceBanner;
import ru.yandex.direct.core.entity.creative.model.Creative;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.BannerInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.CreativeInfo;
import ru.yandex.direct.core.testing.info.PerformanceAdGroupInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.api.GdValidationResult;
import ru.yandex.direct.grid.processing.model.group.mutation.GdUpdateAdGroupPayload;
import ru.yandex.direct.grid.processing.model.group.mutation.GdUpdateAdGroupPayloadItem;
import ru.yandex.direct.grid.processing.model.group.mutation.GdUpdateAdGroupRegions;
import ru.yandex.direct.grid.processing.model.group.mutation.GdUpdateAdGroupRegionsAction;
import ru.yandex.direct.grid.processing.util.GraphQLUtils;
import ru.yandex.direct.grid.processing.util.GraphQlTestExecutor;
import ru.yandex.direct.grid.processing.util.GraphQlTestExecutor.TemplateMutation;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.regions.Region;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.inconsistentCreativeGeoToAdGroupGeo;
import static ru.yandex.direct.core.entity.region.validation.RegionIdDefects.geoEmptyRegions;
import static ru.yandex.direct.core.entity.region.validation.RegionIdDefects.geoIncorrectUseOfZeroRegion;
import static ru.yandex.direct.core.testing.data.TestBanners.activePerformanceBanner;
import static ru.yandex.direct.core.testing.data.TestClients.defaultClient;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultPerformanceCreative;
import static ru.yandex.direct.core.testing.data.TestGroups.activeTextAdGroup;
import static ru.yandex.direct.grid.processing.service.group.AdGroupGraphQlService.UPDATE_AD_GROUP_REGIONS_MUTATION_NAME;
import static ru.yandex.direct.grid.processing.service.group.mutation.UpdateAdGroupRegionsMutationService.VALIDATION_RESULT_PATH;
import static ru.yandex.direct.grid.processing.service.validation.GridDefectDefinitions.mutuallyExclusive;
import static ru.yandex.direct.grid.processing.util.validation.GridValidationHelper.toGdValidationResult;
import static ru.yandex.direct.grid.processing.util.validation.GridValidationMatchers.gridDefect;
import static ru.yandex.direct.grid.processing.util.validation.GridValidationMatchers.hasErrorsWith;
import static ru.yandex.direct.regions.Region.CRIMEA_REGION_ID;
import static ru.yandex.direct.regions.Region.GLOBAL_REGION_ID;
import static ru.yandex.direct.regions.Region.KYIV_REGION_ID;
import static ru.yandex.direct.regions.Region.MOSCOW_REGION_ID;
import static ru.yandex.direct.regions.Region.RUSSIA_REGION_ID;
import static ru.yandex.direct.regions.Region.UKRAINE_REGION_ID;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.validation.defect.CommonDefects.objectNotFound;
import static ru.yandex.direct.validation.defect.NumberDefects.greaterThanOrEqualTo;
import static ru.yandex.direct.validation.result.PathHelper.concat;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@GridProcessingTest
@RunWith(JUnitParamsRunner.class)
@ParametersAreNonnullByDefault
public class UpdateAdGroupRegionsMutationTest {

    private static final String UPDATE_AD_GROUP_REGIONS_MUTATION_TEMPLATE = ""
            + "mutation {\n"
            + "  %s (input: %s) {\n"
            + "    updatedAdGroupItems {\n"
            + "      adGroupId\n"
            + "    }\n"
            + "    validationResult {\n"
            + "      errors {\n"
            + "        code\n"
            + "        path\n"
            + "        params\n"
            + "      }\n"
            + "    }\n"
            + "  }\n"
            + "}";

    private static final TemplateMutation<GdUpdateAdGroupRegions, GdUpdateAdGroupPayload> UPDATE_REGIONS_MUTATION =
            new TemplateMutation<>(UPDATE_AD_GROUP_REGIONS_MUTATION_NAME, UPDATE_AD_GROUP_REGIONS_MUTATION_TEMPLATE,
                    GdUpdateAdGroupRegions.class, GdUpdateAdGroupPayload.class);
    private static final long NOT_EXIST_AD_GROUP_ID = Long.MAX_VALUE;

    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    private UserInfo userInfo;
    private User operator;
    private AdGroupInfo adGroup;
    private GdUpdateAdGroupRegions input;
    private List<Long> campaignIds;
    private List<Long> adGroupIds;

    @Autowired
    private GraphQlTestExecutor graphQlTestExecutor;

    @Autowired
    private Steps steps;

    @Autowired
    private AdGroupService adGroupService;

    @Before
    public void before() {
        userInfo = steps.userSteps().createDefaultUser();
        operator = userInfo.getUser();
        TestAuthHelper.setDirectAuthentication(operator);

        CampaignInfo campaignInfo = steps.campaignSteps().createActiveCampaign(userInfo.getClientInfo());
        adGroup = steps.adGroupSteps().createDefaultAdGroup(campaignInfo);
        campaignIds = List.of(campaignInfo.getCampaignId());
        adGroupIds = List.of(adGroup.getAdGroupId());

        input = new GdUpdateAdGroupRegions();
    }

    public static Object[][] params() {
        return new Object[][]{
                {GdUpdateAdGroupRegionsAction.REPLACE,
                        List.of(Region.MOSCOW_AND_MOSCOW_PROVINCE_REGION_ID, -MOSCOW_REGION_ID),
                        List.of(Region.MOSCOW_AND_MOSCOW_PROVINCE_REGION_ID, -MOSCOW_REGION_ID)},
                {GdUpdateAdGroupRegionsAction.ADD,
                        List.of(Region.KAZAKHSTAN_REGION_ID, MOSCOW_REGION_ID),
                        List.of(RUSSIA_REGION_ID, Region.KAZAKHSTAN_REGION_ID)},
                {GdUpdateAdGroupRegionsAction.REMOVE,
                        List.of(Region.MOSCOW_AND_MOSCOW_PROVINCE_REGION_ID, Region.CHUVASH_REPUBLIC_REGION_ID),
                        List.of(RUSSIA_REGION_ID, -Region.MOSCOW_AND_MOSCOW_PROVINCE_REGION_ID,
                                -Region.CHUVASH_REPUBLIC_REGION_ID)},
        };
    }

    public static Object[][] paramsTestCrimea() {
        return new Object[][]{
                // добавляем Россию
                {"Россия-Крым. Добавляем Россию русскому клиенту. Крым добавляется", RUSSIA_REGION_ID,
                        List.of(RUSSIA_REGION_ID),
                        List.of(RUSSIA_REGION_ID),
                        List.of(RUSSIA_REGION_ID, CRIMEA_REGION_ID),
                },
                {"Россия-Крым. Добавляем Россию украинскому клиенту. Крым не добавляется", UKRAINE_REGION_ID,
                        List.of(RUSSIA_REGION_ID),
                        List.of(RUSSIA_REGION_ID),
                        List.of(RUSSIA_REGION_ID),
                },

                {"Россия,Украина-Крым. Добавляем Россию русскому клиенту. Крым добавляется", RUSSIA_REGION_ID,
                        List.of(UKRAINE_REGION_ID, RUSSIA_REGION_ID),
                        List.of(RUSSIA_REGION_ID),
                        List.of(UKRAINE_REGION_ID, RUSSIA_REGION_ID, CRIMEA_REGION_ID),
                },
                {"Россия,Украина-Крым. Добавляем Россию украинскому клиенту. Крым не добавляется", UKRAINE_REGION_ID,
                        List.of(UKRAINE_REGION_ID, RUSSIA_REGION_ID),
                        List.of(RUSSIA_REGION_ID),
                        List.of(UKRAINE_REGION_ID, RUSSIA_REGION_ID),
                },

                {"Россия,Украина+Крым. Добавляем Россию русскому клиенту. Россия+Крым+Украина", RUSSIA_REGION_ID,
                        List.of(UKRAINE_REGION_ID, CRIMEA_REGION_ID, RUSSIA_REGION_ID),
                        List.of(RUSSIA_REGION_ID),
                        List.of(UKRAINE_REGION_ID, RUSSIA_REGION_ID, CRIMEA_REGION_ID),
                },
                {"Россия,Украина+Крым. Добавляем Россию украинскому клиенту. Россия+Крым+Украина", UKRAINE_REGION_ID,
                        List.of(UKRAINE_REGION_ID, CRIMEA_REGION_ID, RUSSIA_REGION_ID),
                        List.of(RUSSIA_REGION_ID),
                        List.of(UKRAINE_REGION_ID, RUSSIA_REGION_ID, CRIMEA_REGION_ID),
                },

                {"Москва,Киев. Добавляем Россию русскому клиенту. Россия+Крым+Киев", RUSSIA_REGION_ID,
                        List.of(MOSCOW_REGION_ID, KYIV_REGION_ID),
                        List.of(RUSSIA_REGION_ID),
                        List.of(KYIV_REGION_ID, RUSSIA_REGION_ID, CRIMEA_REGION_ID),
                },

                {"Москва,Киев. Добавляем Россию украинскому клиенту. Россия+Киев", UKRAINE_REGION_ID,
                        List.of(MOSCOW_REGION_ID, KYIV_REGION_ID),
                        List.of(RUSSIA_REGION_ID),
                        List.of(KYIV_REGION_ID, RUSSIA_REGION_ID),
                },

                // добавляем Украину
                {"Россия,Украина-Крым. Добавляем Украину русскому клиенту. Россия,Украина", RUSSIA_REGION_ID,
                        List.of(UKRAINE_REGION_ID, RUSSIA_REGION_ID),
                        List.of(UKRAINE_REGION_ID),
                        List.of(RUSSIA_REGION_ID, UKRAINE_REGION_ID),
                },
                {"Россия,Украина-Крым. Добавляем Украину украинскому клиенту. Россия+Украина+Крым", UKRAINE_REGION_ID,
                        List.of(UKRAINE_REGION_ID, RUSSIA_REGION_ID),
                        List.of(UKRAINE_REGION_ID),
                        List.of(RUSSIA_REGION_ID, UKRAINE_REGION_ID, CRIMEA_REGION_ID),
                },

                {"Россия,Украина+Крым. Добавляем Украину русскому клиенту. Россия+Крым+Украина", RUSSIA_REGION_ID,
                        List.of(UKRAINE_REGION_ID, RUSSIA_REGION_ID, CRIMEA_REGION_ID),
                        List.of(UKRAINE_REGION_ID),
                        List.of(RUSSIA_REGION_ID, UKRAINE_REGION_ID, CRIMEA_REGION_ID),
                },
                {"Россия,Украина+Крым. Добавляем Украину украинскому клиенту. Россия,Украина+Крым", UKRAINE_REGION_ID,
                        List.of(UKRAINE_REGION_ID, RUSSIA_REGION_ID, CRIMEA_REGION_ID),
                        List.of(UKRAINE_REGION_ID),
                        List.of(RUSSIA_REGION_ID, UKRAINE_REGION_ID, CRIMEA_REGION_ID),
                },

                {"Москва,Киев. Добавляем Украину русскому клиенту. Москва+Украина", RUSSIA_REGION_ID,
                        List.of(MOSCOW_REGION_ID, KYIV_REGION_ID),
                        List.of(UKRAINE_REGION_ID),
                        List.of(MOSCOW_REGION_ID, UKRAINE_REGION_ID),
                },

                {"Москва,Киев. Добавляем Украину украинскому клиенту. Москва,Украина,Крым", UKRAINE_REGION_ID,
                        List.of(MOSCOW_REGION_ID, KYIV_REGION_ID),
                        List.of(UKRAINE_REGION_ID),
                        List.of(MOSCOW_REGION_ID, UKRAINE_REGION_ID, CRIMEA_REGION_ID),
                },
        };
    }

    @Test
    @Parameters(method = "params")
    @TestCaseName("action = {0}")
    public void updateAdGroupRegionsByAdGroupIds(GdUpdateAdGroupRegionsAction action,
                                     List<Long> regionsToUpdate, List<Long> expectedRegions) {
        input.withAction(action)
                .withAdGroupIds(adGroupIds)
                .withRegionIds(regionsToUpdate);

        updateAdGroupRegions(action, regionsToUpdate, expectedRegions);
    }

    @Test
    @Parameters(method = "params")
    @TestCaseName("action = {0}")
    public void updateAdGroupRegionsByCampaignId(GdUpdateAdGroupRegionsAction action,
                                                 List<Long> regionsToUpdate, List<Long> expectedRegions) {
        input.withAction(action)
                .withCampaignIds(campaignIds)
                .withRegionIds(regionsToUpdate);

        updateAdGroupRegions(action, regionsToUpdate, expectedRegions);
    }

    private void updateAdGroupRegions(GdUpdateAdGroupRegionsAction action,
                                     List<Long> regionsToUpdate, List<Long> expectedRegions) {
        GdUpdateAdGroupPayload payload =
                graphQlTestExecutor.doMutationAndGetPayload(UPDATE_REGIONS_MUTATION, input, operator);

        //noinspection ConstantConditions
        List<Long> regions = adGroupService.getAdGroup(adGroup.getAdGroupId()).getGeo();

        var expectedPayload = new GdUpdateAdGroupPayload()
                .withUpdatedAdGroupItems(List.of(new GdUpdateAdGroupPayloadItem()
                        .withAdGroupId(this.adGroup.getAdGroupId())))
                .withValidationResult(null);
        SoftAssertions.assertSoftly(softAssertions -> {
            softAssertions.assertThat(payload)
                    .is(matchedBy(beanDiffer(expectedPayload)));

            softAssertions.assertThat(regions)
                    .is(matchedBy(beanDiffer(expectedRegions)));
        });
    }

    @Test
    @Parameters(method = "paramsTestCrimea")
    @TestCaseName("action = {0}")
    public void updateAdGroupRegions_AddRussiaToRussianClient(String testName, Long countryRegionId,
                                                              List<Long> initialRegions,
                                                              List<Long> regionsToAdd,
                                                              List<Long> expectedRegions) {

        ClientInfo clientInfo = steps.clientSteps().createClient(defaultClient()
                .withCountryRegionId(countryRegionId));

        operator = clientInfo.getChiefUserInfo().getUser();
        TestAuthHelper.setDirectAuthentication(operator);

        CampaignInfo campaignInfo = steps.campaignSteps().createActiveCampaign(clientInfo);

        TextAdGroup textAdGroup = activeTextAdGroup()
                .withGeo(initialRegions);

        AdGroupInfo adGroupGeoGlobal = steps.adGroupSteps().createAdGroup(textAdGroup, campaignInfo);

        Long adGroupId = adGroupGeoGlobal.getAdGroupId();
        AdGroup adGroup = adGroupService.getAdGroup(adGroupId);

        input.withAction(GdUpdateAdGroupRegionsAction.ADD)
                .withAdGroupIds(List.of(adGroupId))
                .withRegionIds(regionsToAdd);

        GdUpdateAdGroupPayload payload =
                graphQlTestExecutor.doMutationAndGetPayload(UPDATE_REGIONS_MUTATION, input, operator);

        var expectedPayload = new GdUpdateAdGroupPayload()
                .withUpdatedAdGroupItems(List.of(new GdUpdateAdGroupPayloadItem()
                        .withAdGroupId(adGroupId)))
                .withValidationResult(null);

        //noinspection ConstantConditions
        List<Long> actualRegions = adGroupService.getAdGroup(adGroupId).getGeo();

        SoftAssertions.assertSoftly(softAssertions -> {
            softAssertions.assertThat(payload)
                    .is(matchedBy(beanDiffer(expectedPayload)));

            softAssertions.assertThat(actualRegions)
                    .is(matchedBy(beanDiffer(expectedRegions)));
        });
    }

    @Test
    public void updateAdGroupRegions_requestValidation() {
        input.withAction(GdUpdateAdGroupRegionsAction.ADD)
                .withAdGroupIds(adGroupIds)
                .withCampaignIds(campaignIds)
                .withRegionIds(List.of(-1L));
        //noinspection ConstantConditions
        List<Long> regionsBeforeUpdate = adGroupService.getAdGroup(adGroup.getAdGroupId()).getGeo();

        ExecutionResult executionResult = graphQlTestExecutor.doMutation(UPDATE_REGIONS_MUTATION, input, operator);
        assertThat(executionResult.getErrors()).hasSize(1);
        List<GdValidationResult> gdValidationResults = GraphQLUtils.getGdValidationResults(executionResult.getErrors());
        assertThat(gdValidationResults).hasSize(1);

        //noinspection ConstantConditions
        List<Long> regions = adGroupService.getAdGroup(adGroup.getAdGroupId()).getGeo();

        assertThat(gdValidationResults.get(0))
                .is(matchedBy(hasErrorsWith(gridDefect(
                        path(field(GdUpdateAdGroupRegions.REGION_IDS), index(0)), greaterThanOrEqualTo(0L)))))
                .is(matchedBy(hasErrorsWith(gridDefect(path(), mutuallyExclusive()))));
        assertThat(regions)
                .is(matchedBy(beanDiffer(regionsBeforeUpdate)));
    }

    @Test
    public void updateAdGroupRegions_preValidation_removeFromGlobal_AllError() {
        AdGroupInfo adGroupGeoRussia = this.adGroup;

        TextAdGroup textAdGroup = activeTextAdGroup()
                .withGeo(List.of(GLOBAL_REGION_ID));

        AdGroupInfo adGroupGeoGlobal = steps.adGroupSteps().createAdGroup(textAdGroup,
                adGroupGeoRussia.getCampaignInfo());

        input.withAction(GdUpdateAdGroupRegionsAction.REMOVE)
                .withAdGroupIds(List.of(adGroupGeoGlobal.getAdGroupId()))
                .withRegionIds(List.of(Region.KIROV_OBLAST_REGION_ID));

        GdUpdateAdGroupPayload payload =
                graphQlTestExecutor.doMutationAndGetPayload(UPDATE_REGIONS_MUTATION, input, operator);
        GdValidationResult expectedGdValidationResult =
                toGdValidationResult(concat(VALIDATION_RESULT_PATH, index(0)), geoIncorrectUseOfZeroRegion())
                        .withWarnings(null);
        var expectedPayload = new GdUpdateAdGroupPayload()
                .withUpdatedAdGroupItems(List.of())
                .withValidationResult(expectedGdValidationResult);

        //noinspection ConstantConditions
        List<Long> regionsAdGroupGeoGlobal = adGroupService.getAdGroup(adGroupGeoGlobal.getAdGroupId()).getGeo();

        SoftAssertions.assertSoftly(softAssertions -> {
            softAssertions.assertThat(payload)
                    .is(matchedBy(beanDiffer(expectedPayload)));

            softAssertions.assertThat(regionsAdGroupGeoGlobal)
                    .is(matchedBy(beanDiffer(List.of(GLOBAL_REGION_ID))));
        });
    }

    @Test
    public void updateAdGroupRegions_preValidation_removeFromGlobal_OneSuccess_OneError() {
        AdGroupInfo adGroupGeoRussia = this.adGroup;

        TextAdGroup textAdGroup = activeTextAdGroup()
                .withGeo(List.of(GLOBAL_REGION_ID));

        AdGroupInfo adGroupGeoGlobal = steps.adGroupSteps().createAdGroup(textAdGroup,
                adGroupGeoRussia.getCampaignInfo());

        input.withAction(GdUpdateAdGroupRegionsAction.REMOVE)
                .withAdGroupIds(List.of(adGroupGeoRussia.getAdGroupId(), adGroupGeoGlobal.getAdGroupId()))
                .withRegionIds(List.of(Region.KIROV_OBLAST_REGION_ID));

        GdUpdateAdGroupPayload payload =
                graphQlTestExecutor.doMutationAndGetPayload(UPDATE_REGIONS_MUTATION, input, operator);
        GdValidationResult expectedGdValidationResult =
                toGdValidationResult(concat(VALIDATION_RESULT_PATH, index(1)), geoIncorrectUseOfZeroRegion())
                        .withWarnings(null);
        var expectedPayload = new GdUpdateAdGroupPayload()
                .withUpdatedAdGroupItems(List.of(new GdUpdateAdGroupPayloadItem()
                        .withAdGroupId(adGroupGeoRussia.getAdGroupId())))
                .withValidationResult(expectedGdValidationResult);

        //noinspection ConstantConditions
        List<Long> regionsAdGroupGeoRussia = adGroupService.getAdGroup(adGroupGeoRussia.getAdGroupId()).getGeo();
        List<Long> regionsAdGroupGeoGlobal = adGroupService.getAdGroup(adGroupGeoGlobal.getAdGroupId()).getGeo();

        SoftAssertions.assertSoftly(softAssertions -> {
            softAssertions.assertThat(payload)
                    .is(matchedBy(beanDiffer(expectedPayload)));

            softAssertions.assertThat(regionsAdGroupGeoRussia)
                    .is(matchedBy(beanDiffer(List.of(RUSSIA_REGION_ID, -Region.KIROV_OBLAST_REGION_ID))));

            softAssertions.assertThat(regionsAdGroupGeoGlobal)
                    .is(matchedBy(beanDiffer(List.of(GLOBAL_REGION_ID))));
        });
    }

    @Test
    public void updateAdGroupRegions_preValidation_performanceAdGroup() {
        PerformanceAdGroupInfo adGroupInfo =
                steps.adGroupSteps().createDefaultPerformanceAdGroup(userInfo.getClientInfo());

        Creative creative = defaultPerformanceCreative(adGroupInfo.getClientId(), null)
                .withSumGeo(List.of(MOSCOW_REGION_ID));
        CreativeInfo creativeInfo = steps.creativeSteps().createCreative(creative, adGroupInfo.getClientInfo());

        OldPerformanceBanner banner = activePerformanceBanner(adGroupInfo.getCampaignId(), adGroupInfo.getAdGroupId(),
                creativeInfo.getCreativeId());
        steps.bannerSteps().createBanner(new BannerInfo()
                .withBanner(banner)
                .withAdGroupInfo(adGroupInfo)
                .withCampaignInfo(adGroupInfo.getCampaignInfo())
                .withClientInfo(adGroupInfo.getClientInfo()));

        input = new GdUpdateAdGroupRegions()
                .withAdGroupIds(List.of(adGroupInfo.getAdGroupId()))
                .withAction(GdUpdateAdGroupRegionsAction.REPLACE)
                .withRegionIds(List.of(RUSSIA_REGION_ID));

        //noinspection ConstantConditions
        List<Long> regionsBeforeUpdate = adGroupService.getAdGroup(adGroupInfo.getAdGroupId()).getGeo();

        GdUpdateAdGroupPayload payload =
                graphQlTestExecutor.doMutationAndGetPayload(UPDATE_REGIONS_MUTATION, input, operator);

        GdValidationResult expectedGdValidationResult =
                toGdValidationResult(concat(VALIDATION_RESULT_PATH, index(0)), inconsistentCreativeGeoToAdGroupGeo())
                        .withWarnings(null);
        var expectedPayload = new GdUpdateAdGroupPayload()
                .withUpdatedAdGroupItems(Collections.emptyList())
                .withValidationResult(expectedGdValidationResult);

        //noinspection ConstantConditions
        List<Long> regions = adGroupService.getAdGroup(adGroupInfo.getAdGroupId()).getGeo();

        SoftAssertions.assertSoftly(softAssertions -> {
            softAssertions.assertThat(payload)
                    .is(matchedBy(beanDiffer(expectedPayload)));
            softAssertions.assertThat(regions)
                    .is(matchedBy(beanDiffer(regionsBeforeUpdate)));
        });
    }

    @Test
    public void updateAdGroupRegions_validation() {
        input.withAction(GdUpdateAdGroupRegionsAction.REMOVE)
                .withAdGroupIds(adGroupIds)
                .withRegionIds(adGroup.getAdGroup().getGeo());
        //noinspection ConstantConditions
        List<Long> regionsBeforeUpdate = adGroupService.getAdGroup(adGroup.getAdGroupId()).getGeo();

        GdUpdateAdGroupPayload payload =
                graphQlTestExecutor.doMutationAndGetPayload(UPDATE_REGIONS_MUTATION, input, operator);
        GdValidationResult expectedGdValidationResult =
                toGdValidationResult(concat(VALIDATION_RESULT_PATH, index(0)), geoEmptyRegions())
                        .withWarnings(null);
        var expectedPayload = new GdUpdateAdGroupPayload()
                .withUpdatedAdGroupItems(Collections.emptyList())
                .withValidationResult(expectedGdValidationResult);

        //noinspection ConstantConditions
        List<Long> regions = adGroupService.getAdGroup(adGroup.getAdGroupId()).getGeo();

        SoftAssertions.assertSoftly(softAssertions -> {
            softAssertions.assertThat(payload)
                    .is(matchedBy(beanDiffer(expectedPayload)));

            softAssertions.assertThat(regions)
                    .is(matchedBy(beanDiffer(regionsBeforeUpdate)));
        });
    }

    @Test
    public void updateAdGroupRegions_validationWithNotExistAdGroup() {
        input.withAction(GdUpdateAdGroupRegionsAction.REPLACE)
                .withAdGroupIds(List.of(adGroup.getAdGroupId(), NOT_EXIST_AD_GROUP_ID))
                .withRegionIds(List.of(Region.KIROV_OBLAST_REGION_ID));

        GdUpdateAdGroupPayload payload =
                graphQlTestExecutor.doMutationAndGetPayload(UPDATE_REGIONS_MUTATION, input, operator);
        GdValidationResult expectedGdValidationResult =
                toGdValidationResult(concat(VALIDATION_RESULT_PATH, index(1)), objectNotFound())
                        .withWarnings(null);
        var expectedPayload = new GdUpdateAdGroupPayload()
                .withUpdatedAdGroupItems(List.of(new GdUpdateAdGroupPayloadItem()
                        .withAdGroupId(this.adGroup.getAdGroupId())))
                .withValidationResult(expectedGdValidationResult);

        //noinspection ConstantConditions
        List<Long> regions = adGroupService.getAdGroup(adGroup.getAdGroupId()).getGeo();

        SoftAssertions.assertSoftly(softAssertions -> {
            softAssertions.assertThat(payload)
                    .is(matchedBy(beanDiffer(expectedPayload)));

            softAssertions.assertThat(regions)
                    .is(matchedBy(beanDiffer(input.getRegionIds())));
        });
    }

}
