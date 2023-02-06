package ru.yandex.direct.grid.processing.service.client;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.core.entity.bidmodifiers.service.BidModifierLimits;
import ru.yandex.direct.core.entity.bidmodifiers.service.BidModifierLimitsAdvanced;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.client.service.ClientLimitsService;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.steps.UserSteps;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.grid.model.entity.adgroup.GdAdGroupType;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.model.GdIntRange;
import ru.yandex.direct.grid.processing.model.bidmodifier.GdBidModifierType;
import ru.yandex.direct.grid.processing.model.bidmodifier.GdInventoryType;
import ru.yandex.direct.grid.processing.model.bidmodifier.GdOsType;
import ru.yandex.direct.grid.processing.model.bidmodifier.values.GdAllowedBidModifierValues;
import ru.yandex.direct.grid.processing.model.bidmodifier.values.GdAllowedBidModifierValuesInventory;
import ru.yandex.direct.grid.processing.model.bidmodifier.values.GdAllowedBidModifierValuesMobile;
import ru.yandex.direct.grid.processing.model.client.GdAllowedBidModifiersByAdGroupType;
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.direct.feature.FeatureName.CONTENT_PROMOTION_COLLECTIONS_ON_GRID;
import static ru.yandex.direct.feature.FeatureName.CONTENT_PROMOTION_VIDEO_ON_GRID;
import static ru.yandex.direct.feature.FeatureName.CPM_GEO_PIN_PRODUCT_ENABLED;
import static ru.yandex.direct.grid.processing.service.bidmodifier.BidModifierDataConverter.toBidModifierType;
import static ru.yandex.direct.grid.processing.util.ContextHelper.buildContext;
import static ru.yandex.direct.utils.CommonUtils.nvl;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ClientConstantsDataServiceBidModifiersTest {
    private static final Set<GdBidModifierType> ALLOWABLE_FOR_TEXT_AD_GROUPS = Set.of(
            GdBidModifierType.DEMOGRAPHY_MULTIPLIER,
            GdBidModifierType.MOBILE_MULTIPLIER,
            GdBidModifierType.RETARGETING_MULTIPLIER,
            GdBidModifierType.VIDEO_MULTIPLIER,
            GdBidModifierType.WEATHER_MULTIPLIER,
            GdBidModifierType.DESKTOP_MULTIPLIER);

    private static final Set<GdBidModifierType> ALLOWABLE_FOR_DYNAMIC_AD_GROUPS = Set.of(
            GdBidModifierType.DEMOGRAPHY_MULTIPLIER,
            GdBidModifierType.RETARGETING_MULTIPLIER,
            GdBidModifierType.MOBILE_MULTIPLIER,
            GdBidModifierType.DESKTOP_MULTIPLIER);

    private static final Set<GdBidModifierType> ALLOWABLE_FOR_SMART_AD_GROUPS = Set.of(
            GdBidModifierType.DEMOGRAPHY_MULTIPLIER,
            GdBidModifierType.MOBILE_MULTIPLIER,
            GdBidModifierType.RETARGETING_MULTIPLIER,
            GdBidModifierType.WEATHER_MULTIPLIER,
            GdBidModifierType.SMART_TGO_MULTIPLIER,
            GdBidModifierType.DESKTOP_MULTIPLIER);

    private static final Set<GdBidModifierType> ALLOWABLE_FOR_CPM_BANNER_AD_GROUPS = Set.of(
            GdBidModifierType.DEMOGRAPHY_MULTIPLIER,
            GdBidModifierType.DESKTOP_MULTIPLIER,
            GdBidModifierType.SMARTTV_MULTIPLIER,
            GdBidModifierType.MOBILE_MULTIPLIER,
            GdBidModifierType.RETARGETING_MULTIPLIER,
            GdBidModifierType.WEATHER_MULTIPLIER);

    private static final Set<GdBidModifierType> ALLOWABLE_FOR_CPM_AUDIO_AD_GROUPS_NO_FEATURE = Set.of(
            // Без фичи CPM_AUDIO нет корректировок DESKTOP и MOBILE
            GdBidModifierType.WEATHER_MULTIPLIER
    );

    private static final Set<GdBidModifierType> ALLOWABLE_FOR_CPM_AUDIO_AD_GROUPS_WITH_FEATURE = Set.of(
            GdBidModifierType.DESKTOP_MULTIPLIER,
            GdBidModifierType.MOBILE_MULTIPLIER,
            GdBidModifierType.SMARTTV_MULTIPLIER,
            GdBidModifierType.WEATHER_MULTIPLIER
    );

    private static final Set<GdBidModifierType> ALLOWABLE_FOR_CPM_GEOPRODUCT_AD_GROUPS = Set.of(
            GdBidModifierType.DESKTOP_MULTIPLIER,
            GdBidModifierType.MOBILE_MULTIPLIER);

    private static final Set<GdBidModifierType> ALLOWABLE_FOR_CPM_VIDEO_AD_GROUPS = Set.of(
            GdBidModifierType.DESKTOP_MULTIPLIER,
            GdBidModifierType.MOBILE_MULTIPLIER,
            GdBidModifierType.SMARTTV_MULTIPLIER,
            GdBidModifierType.WEATHER_MULTIPLIER);

    private static final Set<GdBidModifierType> ALLOWABLE_FOR_CPM_OUTDOOR_AD_GROUPS = Set.of(
            GdBidModifierType.EXPRESS_TRAFFIC_MULTIPLIER,
            GdBidModifierType.WEATHER_MULTIPLIER);

    private static final Set<GdBidModifierType> ALLOWABLE_FOR_CPM_INDOOR_AD_GROUPS = Set.of(
            GdBidModifierType.DEMOGRAPHY_MULTIPLIER);

    private static final Set<GdBidModifierType> ALLOWABLE_FOR_MCBANNER_AD_GROUPS = Set.of(
            GdBidModifierType.DEMOGRAPHY_MULTIPLIER,
            GdBidModifierType.RETARGETING_MULTIPLIER);

    private static final Set<GdBidModifierType> ALLOWABLE_FOR_MOBILE_AD_GROUPS = Set.of(
            GdBidModifierType.DEMOGRAPHY_MULTIPLIER,
            GdBidModifierType.RETARGETING_MULTIPLIER);

    private static final Set<GdBidModifierType> ALLOWABLE_FOR_CONTENT_PROMOTION_AD_GROUP = Set.of(
            GdBidModifierType.DEMOGRAPHY_MULTIPLIER,
            GdBidModifierType.MOBILE_MULTIPLIER,
            GdBidModifierType.RETARGETING_MULTIPLIER,
            GdBidModifierType.DESKTOP_MULTIPLIER
    );


    private static final Set<GdBidModifierType> ALLOWABLE_FOR_CPM_GEO_PIN_AD_GROUPS_WITH_FEATURE = Set.of(
            GdBidModifierType.MOBILE_MULTIPLIER);

    private static final Set<GdInventoryType> ALLOWED_INVENTORY_TYPES = Set.of(
            GdInventoryType.INPAGE,
            GdInventoryType.INSTREAM_WEB,
            GdInventoryType.INAPP,
            GdInventoryType.REWARDED
    );

    @Autowired
    UserSteps userSteps;

    @Autowired
    ClientLimitsService clientLimitsService;

    @Mock
    FeatureService featureService;

    @Mock
    GridContextProvider contextProvider;

    @InjectMocks
    ClientConstantsDataService clientConstantsDataService;

    @Autowired
    Steps steps;

    private UserInfo userInfo;

    @Before
    public void init() {
        userInfo = userSteps.createDefaultUser();
        GridGraphQLContext operator = buildContext(userInfo.getUser());
        doReturn(operator)
                .when(contextProvider).getGridContext();
    }

    @Test
    public void check_allowedBidModifierTypesByAgGroupTypesWithFeature() {
        doReturn(true).when(featureService).isEnabledForClientId(userInfo.getClientInfo().getClientId(),
                CPM_GEO_PIN_PRODUCT_ENABLED);

        List<GdAllowedBidModifiersByAdGroupType> actual =
                clientConstantsDataService.getAllowedBidModifierTypesByAgGroupTypes();
        List<GdAllowedBidModifiersByAdGroupType> expected =
                List.of(new GdAllowedBidModifiersByAdGroupType()
                                .withAdGroupType(GdAdGroupType.TEXT)
                                .withAllowedBidModifierTypes(ALLOWABLE_FOR_TEXT_AD_GROUPS),
                        new GdAllowedBidModifiersByAdGroupType()
                                .withAdGroupType(GdAdGroupType.DYNAMIC)
                                .withAllowedBidModifierTypes(ALLOWABLE_FOR_DYNAMIC_AD_GROUPS),
                        new GdAllowedBidModifiersByAdGroupType()
                                .withAdGroupType(GdAdGroupType.PERFORMANCE)
                                .withAllowedBidModifierTypes(ALLOWABLE_FOR_SMART_AD_GROUPS),
                        new GdAllowedBidModifiersByAdGroupType()
                                .withAdGroupType(GdAdGroupType.CPM_BANNER)
                                .withAllowedBidModifierTypes(ALLOWABLE_FOR_CPM_BANNER_AD_GROUPS),
                        new GdAllowedBidModifiersByAdGroupType()
                                .withAdGroupType(GdAdGroupType.CPM_VIDEO)
                                .withAllowedBidModifierTypes(ALLOWABLE_FOR_CPM_VIDEO_AD_GROUPS),
                        new GdAllowedBidModifiersByAdGroupType()
                                .withAdGroupType(GdAdGroupType.CPM_OUTDOOR)
                                .withAllowedBidModifierTypes(ALLOWABLE_FOR_CPM_OUTDOOR_AD_GROUPS),
                        new GdAllowedBidModifiersByAdGroupType()
                                .withAdGroupType(GdAdGroupType.CPM_INDOOR)
                                .withAllowedBidModifierTypes(ALLOWABLE_FOR_CPM_INDOOR_AD_GROUPS),
                        new GdAllowedBidModifiersByAdGroupType()
                                .withAdGroupType(GdAdGroupType.MCBANNER)
                                .withAllowedBidModifierTypes(ALLOWABLE_FOR_MCBANNER_AD_GROUPS),
                        new GdAllowedBidModifiersByAdGroupType()
                                .withAdGroupType(GdAdGroupType.MOBILE_CONTENT)
                                .withAllowedBidModifierTypes(ALLOWABLE_FOR_MOBILE_AD_GROUPS),
                        new GdAllowedBidModifiersByAdGroupType()
                                .withAdGroupType(GdAdGroupType.CPM_AUDIO)
                                .withAllowedBidModifierTypes(ALLOWABLE_FOR_CPM_AUDIO_AD_GROUPS_WITH_FEATURE),
                        new GdAllowedBidModifiersByAdGroupType()
                                .withAdGroupType(GdAdGroupType.CPM_GEOPRODUCT)
                                .withAllowedBidModifierTypes(ALLOWABLE_FOR_CPM_GEOPRODUCT_AD_GROUPS),
                        new GdAllowedBidModifiersByAdGroupType()
                                .withAdGroupType(GdAdGroupType.CPM_GEO_PIN)
                                .withAllowedBidModifierTypes(ALLOWABLE_FOR_CPM_GEO_PIN_AD_GROUPS_WITH_FEATURE)
                );
        assertThat(actual).containsExactlyInAnyOrder(expected.toArray(new GdAllowedBidModifiersByAdGroupType[]{}));
    }

    @Test
    public void check_mobileAllowedBidModifierValuesWithFeature() {
        List<GdAllowedBidModifierValues> allowedBidModifierValues =
                clientConstantsDataService.getAllowedBidModifierValues();
        Optional<GdAllowedBidModifierValues> mobileAllowedBidModifierValues = allowedBidModifierValues.stream()
                .filter(gdAllowedBidModifierValues -> gdAllowedBidModifierValues.getType() == GdBidModifierType.MOBILE_MULTIPLIER)
                .findFirst();
        assertThat(mobileAllowedBidModifierValues.isPresent()).isTrue();

        GdAllowedBidModifierValuesMobile actual =
                (GdAllowedBidModifierValuesMobile) mobileAllowedBidModifierValues.get();
        BidModifierLimits bidModifierLimits =
                BidModifierLimitsAdvanced.getLimits(toBidModifierType(GdBidModifierType.MOBILE_MULTIPLIER),
                        CampaignType.TEXT, null, userInfo.getClientInfo().getClientId(),
                        (clientId, featureName) -> featureService.isEnabledForClientId(clientId, featureName));
        GdAllowedBidModifierValuesMobile expected = new GdAllowedBidModifierValuesMobile()
                .withType(GdBidModifierType.MOBILE_MULTIPLIER)
                .withPercentRange(
                        new GdIntRange()
                                .withMax(bidModifierLimits.percentMax)
                                .withMin(bidModifierLimits.percentMin)
                                .withStep(1))
                .withMaxConditions(nvl(bidModifierLimits.maxConditions, 1))
                .withOsTypes(Set.of(GdOsType.values()));
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void check_SupportedAdGroupsByFeatures_NoBidModifierFeatures() {
        doReturn(true).when(featureService).isEnabledForClientId(userInfo.getClientInfo().getClientId(),
                FeatureName.CONTENT_PROMOTION_VIDEO_ON_GRID);
        doReturn(true).when(featureService).isEnabledForClientId(userInfo.getClientInfo().getClientId(),
                FeatureName.CONTENT_PROMOTION_COLLECTIONS_ON_GRID);
        doReturn(true).when(featureService).isEnabledForClientId(userInfo.getClientInfo().getClientId(),
                FeatureName.CPM_GEO_PIN_PRODUCT_ENABLED);
        doReturn(Set.of(CONTENT_PROMOTION_VIDEO_ON_GRID.getName(), CONTENT_PROMOTION_COLLECTIONS_ON_GRID.getName(),
                CPM_GEO_PIN_PRODUCT_ENABLED.getName()))
                .when(featureService).getEnabledForClientId(userInfo.getClientInfo().getClientId());


        List<GdAllowedBidModifiersByAdGroupType> actual =
                clientConstantsDataService.getAllowedBidModifierTypesByAgGroupTypes();
        List<GdAllowedBidModifiersByAdGroupType> expected =
                List.of(new GdAllowedBidModifiersByAdGroupType()
                                .withAdGroupType(GdAdGroupType.TEXT)
                                .withAllowedBidModifierTypes(ALLOWABLE_FOR_TEXT_AD_GROUPS),
                        new GdAllowedBidModifiersByAdGroupType()
                                .withAdGroupType(GdAdGroupType.DYNAMIC)
                                .withAllowedBidModifierTypes(ALLOWABLE_FOR_DYNAMIC_AD_GROUPS),
                        new GdAllowedBidModifiersByAdGroupType()
                                .withAdGroupType(GdAdGroupType.PERFORMANCE)
                                .withAllowedBidModifierTypes(ALLOWABLE_FOR_SMART_AD_GROUPS),
                        new GdAllowedBidModifiersByAdGroupType()
                                .withAdGroupType(GdAdGroupType.CPM_BANNER)
                                .withAllowedBidModifierTypes(ALLOWABLE_FOR_CPM_BANNER_AD_GROUPS),
                        new GdAllowedBidModifiersByAdGroupType()
                                .withAdGroupType(GdAdGroupType.CPM_VIDEO)
                                .withAllowedBidModifierTypes(ALLOWABLE_FOR_CPM_VIDEO_AD_GROUPS),
                        new GdAllowedBidModifiersByAdGroupType()
                                .withAdGroupType(GdAdGroupType.CPM_OUTDOOR)
                                .withAllowedBidModifierTypes(ALLOWABLE_FOR_CPM_OUTDOOR_AD_GROUPS),
                        new GdAllowedBidModifiersByAdGroupType()
                                .withAdGroupType(GdAdGroupType.CPM_INDOOR)
                                .withAllowedBidModifierTypes(ALLOWABLE_FOR_CPM_INDOOR_AD_GROUPS),
                        new GdAllowedBidModifiersByAdGroupType()
                                .withAdGroupType(GdAdGroupType.MCBANNER)
                                .withAllowedBidModifierTypes(ALLOWABLE_FOR_MCBANNER_AD_GROUPS),
                        new GdAllowedBidModifiersByAdGroupType()
                                .withAdGroupType(GdAdGroupType.MOBILE_CONTENT)
                                .withAllowedBidModifierTypes(ALLOWABLE_FOR_MOBILE_AD_GROUPS),
                        new GdAllowedBidModifiersByAdGroupType()
                                .withAdGroupType(GdAdGroupType.CPM_AUDIO)
                                .withAllowedBidModifierTypes(ALLOWABLE_FOR_CPM_AUDIO_AD_GROUPS_WITH_FEATURE),
                        new GdAllowedBidModifiersByAdGroupType()
                                .withAdGroupType(GdAdGroupType.CPM_GEOPRODUCT)
                                .withAllowedBidModifierTypes(ALLOWABLE_FOR_CPM_GEOPRODUCT_AD_GROUPS),
                        new GdAllowedBidModifiersByAdGroupType()
                                .withAdGroupType(GdAdGroupType.CONTENT_PROMOTION_VIDEO)
                                .withAllowedBidModifierTypes(ALLOWABLE_FOR_CONTENT_PROMOTION_AD_GROUP),
                        new GdAllowedBidModifiersByAdGroupType()
                                .withAdGroupType(GdAdGroupType.CONTENT_PROMOTION_COLLECTION)
                                .withAllowedBidModifierTypes(ALLOWABLE_FOR_CONTENT_PROMOTION_AD_GROUP),
                        new GdAllowedBidModifiersByAdGroupType()
                                .withAdGroupType(GdAdGroupType.CPM_GEO_PIN)
                                .withAllowedBidModifierTypes(ALLOWABLE_FOR_CPM_GEO_PIN_AD_GROUPS_WITH_FEATURE)
                );
        assertThat(actual).containsExactlyInAnyOrder(expected.toArray(new GdAllowedBidModifiersByAdGroupType[]{}));
    }

    @Test
    public void check_mobileAllowedBidModifierValuesWithMobileOsFeature() {
        List<GdAllowedBidModifierValues> allowedBidModifierValues =
                clientConstantsDataService.getAllowedBidModifierValues();
        Optional<GdAllowedBidModifierValues> mobileAllowedBidModifierValues = allowedBidModifierValues.stream()
                .filter(gdAllowedBidModifierValues -> gdAllowedBidModifierValues.getType() == GdBidModifierType.MOBILE_MULTIPLIER)
                .findFirst();
        assertThat(mobileAllowedBidModifierValues.isPresent())
                .isTrue();

        GdAllowedBidModifierValuesMobile actual =
                (GdAllowedBidModifierValuesMobile) mobileAllowedBidModifierValues.get();
        BidModifierLimits bidModifierLimits =
                BidModifierLimitsAdvanced.getLimits(toBidModifierType(GdBidModifierType.MOBILE_MULTIPLIER),
                        CampaignType.TEXT, null, userInfo.getClientInfo().getClientId(),
                        (clientId, featureName) -> featureService.isEnabledForClientId(clientId, featureName));
        GdAllowedBidModifierValuesMobile expected = new GdAllowedBidModifierValuesMobile()
                .withType(GdBidModifierType.MOBILE_MULTIPLIER)
                .withPercentRange(
                        new GdIntRange()
                                .withMax(bidModifierLimits.percentMax)
                                .withMin(0)
                                .withStep(1))
                .withMaxConditions(nvl(bidModifierLimits.maxConditions, 1));
        org.junit.Assert.assertThat(actual, beanDiffer(expected)
                .useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields()
                        .forFields(newPath("osTypes"))
                        .useMatcher(containsInAnyOrder(GdOsType.ANDROID, GdOsType.IOS))));
    }

    @Test
    public void check_inventoryAllowedBidModifierValues() {
        List<GdAllowedBidModifierValues> allowedBidModifierValues =
                clientConstantsDataService.getAllowedBidModifierValues();
        Optional<GdAllowedBidModifierValues> inventoryAllowedBidModifierValues = allowedBidModifierValues.stream()
                .filter(gdAllowedBidModifierValues ->
                        gdAllowedBidModifierValues.getType() == GdBidModifierType.INVENTORY_MULTIPLIER)
                .findFirst();
        assertThat(inventoryAllowedBidModifierValues.isPresent())
                .isTrue();

        GdAllowedBidModifierValuesInventory actual =
                (GdAllowedBidModifierValuesInventory) inventoryAllowedBidModifierValues.get();
        BidModifierLimits bidModifierLimits =
                BidModifierLimitsAdvanced.getLimits(toBidModifierType(GdBidModifierType.INVENTORY_MULTIPLIER),
                        CampaignType.TEXT, null, userInfo.getClientInfo().getClientId(),
                        (clientId, featureName) -> featureService.isEnabledForClientId(clientId, featureName));
        GdAllowedBidModifierValuesInventory expected = new GdAllowedBidModifierValuesInventory()
                .withType(GdBidModifierType.INVENTORY_MULTIPLIER)
                .withPercentRange(
                        new GdIntRange()
                                .withMax(bidModifierLimits.percentMax)
                                .withMin(bidModifierLimits.percentMin)
                                .withStep(1))
                .withMaxConditions(nvl(bidModifierLimits.maxConditions, 1))
                .withInventoryTypes(ALLOWED_INVENTORY_TYPES);
        assertThat(actual).isEqualTo(expected);
    }

}
