package ru.yandex.direct.grid.processing.service.client;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import one.util.streamex.EntryStream;
import org.apache.commons.lang.math.RandomUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.client.model.ClientLimits;
import ru.yandex.direct.core.entity.client.service.ClientLimitsService;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.steps.UserSteps;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.grid.model.campaign.GdCampaignType;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.model.bidmodifier.GdBidModifierType;
import ru.yandex.direct.grid.processing.model.bidmodifier.values.GdAllowedBidModifierValues;
import ru.yandex.direct.grid.processing.model.client.GdAllowedBidModifiersByAdGroupType;
import ru.yandex.direct.grid.processing.model.client.GdAllowedBidModifiersByCampaignType;
import ru.yandex.direct.grid.processing.model.client.GdClientLimits;
import ru.yandex.direct.grid.processing.model.client.GdClientMetrikaCounters;
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.testing.data.TestUsers.generateNewUser;
import static ru.yandex.direct.grid.processing.model.bidmodifier.GdBidModifierType.GEO_MULTIPLIER;
import static ru.yandex.direct.grid.processing.model.bidmodifier.GdBidModifierType.PRISMA_INCOME_GRADE_MULTIPLIER;
import static ru.yandex.direct.grid.processing.model.bidmodifier.GdBidModifierType.TRAFARET_POSITION_MULTIPLIER;
import static ru.yandex.direct.grid.processing.service.client.ClientConstantsDataService.DEFAULT_METRIKA_AVALIABLE;
import static ru.yandex.direct.grid.processing.service.client.converter.ClientConstantsConverter.toGdClientLimits;
import static ru.yandex.direct.grid.processing.util.ContextHelper.buildContext;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.utils.FunctionalUtils.listToMap;
import static ru.yandex.direct.utils.FunctionalUtils.listToSet;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ClientConstantsDataServiceTest {
    private final static List<Long> COUNTERS = List.of(RandomUtils.nextLong());

    @Autowired
    UserSteps userSteps;

    @Autowired
    GridContextProvider gridContextProvider;

    @Autowired
    ClientLimitsService clientLimitsService;

    @Autowired
    ClientConstantsDataService clientConstantsDataService;

    @Autowired
    Steps steps;

    private UserInfo userInfo;

    @Before
    public void init() {
        userInfo = userSteps.createUser(generateNewUser());
        GridGraphQLContext operator = buildContext(userInfo.getUser());
        gridContextProvider.setGridContext(operator);
    }

    @Test
    public void check_clientLimits() {
        UserInfo operator = steps.userSteps().createDefaultUser();
        GridGraphQLContext context = buildContext(operator.getUser());
        List<ClientLimits> clientLimits = clientLimitsService.massGetClientLimits(
                List.of(operator.getClientInfo().getClientId()));
        assumeThat(clientLimits, hasSize(1));
        GdClientLimits expected = toGdClientLimits(clientLimits.get(0));
        GdClientLimits actual = clientConstantsDataService.getClientLimits(context);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void check_clientCommonMetrikaCounters() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        steps.clientSteps().addCommonMetrikaCounters(clientInfo, COUNTERS);
        GdClientMetrikaCounters expected =
                new GdClientMetrikaCounters()
                        .withCounters(listToSet(COUNTERS, Long::intValue))
                        .withIsMetrikaAvailable(DEFAULT_METRIKA_AVALIABLE);

        GdClientMetrikaCounters actual =
                clientConstantsDataService.getCommonMetrikaCounters(clientInfo.getClientId());
        assertThat(actual).is(matchedBy(beanDiffer(expected)));
    }

    @Test
    public void getAllowedBidModifierTypesByAgGroupTypes_gdAdGroupType_Unique() {
        List<GdAllowedBidModifiersByAdGroupType> bidModifiersByAdGroupType = clientConstantsDataService
                .getAllowedBidModifierTypesByAgGroupTypes();
        assertThat(bidModifiersByAdGroupType.stream()
                .map(GdAllowedBidModifiersByAdGroupType::getAdGroupType)
                .distinct()
                .count())
                .isEqualTo(bidModifiersByAdGroupType.size());
    }

    @Test
    public void getAllowedBidModifierTypesByCampaignTypes_gdCampaignType_Unique() {
        steps.featureSteps().addClientFeature(userInfo.getClientId(),
                FeatureName.GEO_RATE_CORRECTIONS_ALLOWED_FOR_DNA, true);
        List<GdAllowedBidModifiersByCampaignType> bidModifiersByCampaignType = clientConstantsDataService
                .getAllowedBidModifierTypesByCampaignTypes();
        assertThat(bidModifiersByCampaignType.stream()
                .map(GdAllowedBidModifiersByCampaignType::getCampaignType)
                .distinct()
                .count())
                .isEqualTo(bidModifiersByCampaignType.size());
    }

    @Test
    public void getAllowedBidModifierTypesByCampaignTypes_ContentPromotionFeaturesOff_ContentPromotionCampaignNotReturned() {
        steps.featureSteps().addClientFeature(userInfo.getClientId(),
                FeatureName.CONTENT_PROMOTION_COLLECTIONS_ON_GRID, false);
        steps.featureSteps().addClientFeature(userInfo.getClientId(),
                FeatureName.CONTENT_PROMOTION_VIDEO_ON_GRID, false);

        List<GdAllowedBidModifiersByCampaignType> bidModifiersByCampaignType = clientConstantsDataService
                .getAllowedBidModifierTypesByCampaignTypes();
        assertThat(bidModifiersByCampaignType.stream()
                .map(GdAllowedBidModifiersByCampaignType::getCampaignType)
                .filter(gdCampaignType -> gdCampaignType == GdCampaignType.CONTENT_PROMOTION)
                .count())
                .isEqualTo(0);
    }

    @Test
    public void getAllowedBidModifierTypesByCampaignTypes_ContentPromotionCollectionFeatureOn_ContentPromotionCampaignReturned() {
        steps.featureSteps().addClientFeature(userInfo.getClientId(),
                FeatureName.CONTENT_PROMOTION_COLLECTIONS_ON_GRID, true);

        List<GdAllowedBidModifiersByCampaignType> bidModifiersByCampaignType = clientConstantsDataService
                .getAllowedBidModifierTypesByCampaignTypes();
        assertThat(bidModifiersByCampaignType.stream()
                .map(GdAllowedBidModifiersByCampaignType::getCampaignType)
                .filter(gdCampaignType -> gdCampaignType == GdCampaignType.CONTENT_PROMOTION)
                .count())
                .isEqualTo(1);
    }

    @Test
    public void getAllowedBidModifierTypesByCampaignTypes_ContentPromotionVideoFeatureOn_ContentPromotionCampaignReturned() {
        steps.featureSteps().addClientFeature(userInfo.getClientId(),
                FeatureName.CONTENT_PROMOTION_VIDEO_ON_GRID, true);

        List<GdAllowedBidModifiersByCampaignType> bidModifiersByCampaignType = clientConstantsDataService
                .getAllowedBidModifierTypesByCampaignTypes();
        assertThat(bidModifiersByCampaignType.stream()
                .map(GdAllowedBidModifiersByCampaignType::getCampaignType)
                .filter(gdCampaignType -> gdCampaignType == GdCampaignType.CONTENT_PROMOTION)
                .count())
                .isEqualTo(1);
    }

    @Test
    public void getAllowedBidModifierTypesByCampaignTypes_GeoWithoutFeature() {
        steps.featureSteps().addClientFeature(userInfo.getClientId(),
                FeatureName.GEO_RATE_CORRECTIONS_ALLOWED_FOR_DNA, false);
        List<GdAllowedBidModifiersByCampaignType> bidModifiersByCampaignType = clientConstantsDataService
                .getAllowedBidModifierTypesByCampaignTypes();
        Map<GdCampaignType, Set<GdBidModifierType>> campaignTypeSetMap = listToMap(bidModifiersByCampaignType,
                GdAllowedBidModifiersByCampaignType::getCampaignType,
                GdAllowedBidModifiersByCampaignType::getAllowedBidModifierTypes);
        Optional<Boolean> optionalHasAllowedGeoModifiers = EntryStream.of(campaignTypeSetMap)
                .values()
                .map(gdBidModifierTypes -> gdBidModifierTypes.contains(GEO_MULTIPLIER))
                .distinct()
                .filter(aBoolean -> aBoolean)
                .findFirst();
        assertThat(optionalHasAllowedGeoModifiers.isPresent()).isFalse();
    }

    @Test
    public void getAllowedBidModifierTypesByCampaignTypes_GeoWithFeature() {
        steps.featureSteps().addClientFeature(userInfo.getClientId(),
                FeatureName.GEO_RATE_CORRECTIONS_ALLOWED_FOR_DNA, true);
        List<GdAllowedBidModifiersByCampaignType> bidModifiersByCampaignType = clientConstantsDataService
                .getAllowedBidModifierTypesByCampaignTypes();
        Map<GdCampaignType, Set<GdBidModifierType>> campaignTypeSetMap = listToMap(bidModifiersByCampaignType,
                GdAllowedBidModifiersByCampaignType::getCampaignType,
                GdAllowedBidModifiersByCampaignType::getAllowedBidModifierTypes);
        Optional<Boolean> optionalHasAllowedGeoModifiers = EntryStream.of(campaignTypeSetMap)
                .values()
                .map(gdBidModifierTypes -> gdBidModifierTypes.contains(GEO_MULTIPLIER))
                .distinct()
                .filter(aBoolean -> aBoolean)
                .findFirst();
        assertThat(optionalHasAllowedGeoModifiers.isPresent()).isTrue();
    }

    @Test
    public void getAllowedBidModifierTypesByCampaignTypes_TrafaretPosition() {
        List<GdAllowedBidModifiersByCampaignType> bidModifiersByCampaignType = clientConstantsDataService
                .getAllowedBidModifierTypesByCampaignTypes();
        Map<GdCampaignType, Set<GdBidModifierType>> campaignTypeSetMap = listToMap(bidModifiersByCampaignType,
                GdAllowedBidModifiersByCampaignType::getCampaignType,
                GdAllowedBidModifiersByCampaignType::getAllowedBidModifierTypes);
        Optional<Boolean> optionalHasAllowedTrafaretPositionModifiers = EntryStream.of(campaignTypeSetMap)
                .values()
                .map(gdBidModifierTypes -> gdBidModifierTypes.contains(TRAFARET_POSITION_MULTIPLIER))
                .distinct()
                .filter(aBoolean -> aBoolean)
                .findFirst();
        assertThat(optionalHasAllowedTrafaretPositionModifiers.isPresent()).isTrue();
    }

    @Test
    public void getAllowedBidModifierTypesByCampaignTypes_IncomeGradeAllowed() {
        assertThat(isIncomeGradeEnabled(true)).isTrue();
    }

    @Test
    public void getAllowedBidModifierTypesByCampaignTypes_IncomeGradeNotAllowed() {
        assertThat(isIncomeGradeEnabled(false)).isFalse();
    }

    public void getAllowedBidModifierValues_IncomeGradeAllowed() {
        steps.featureSteps().addClientFeature(userInfo.getClientId(),
                FeatureName.INCOME_GRADE_BID_MODIFIER_ALLOWED, true);
        var allowedBidModifierValues = clientConstantsDataService.getAllowedBidModifierValues();
        var allowedBidModifierTypes =
                allowedBidModifierValues.stream().map(GdAllowedBidModifierValues::getType).collect(Collectors.toSet());

        assertThat(allowedBidModifierTypes.contains(PRISMA_INCOME_GRADE_MULTIPLIER)).isTrue();
    }

    private boolean isIncomeGradeEnabled(boolean incomeGradeAllowed) {
        steps.featureSteps().addClientFeature(userInfo.getClientId(),
                FeatureName.INCOME_GRADE_BID_MODIFIER_ALLOWED, incomeGradeAllowed);
        List<GdAllowedBidModifiersByCampaignType> bidModifiersByCampaignType = clientConstantsDataService
                .getAllowedBidModifierTypesByCampaignTypes();
        Map<GdCampaignType, Set<GdBidModifierType>> campaignTypeSetMap = listToMap(bidModifiersByCampaignType,
                GdAllowedBidModifiersByCampaignType::getCampaignType,
                GdAllowedBidModifiersByCampaignType::getAllowedBidModifierTypes);
        Optional<Boolean> optionalHasAllowedTrafaretPositionModifiers = EntryStream.of(campaignTypeSetMap)
                .values()
                .map(gdBidModifierTypes -> gdBidModifierTypes.contains(PRISMA_INCOME_GRADE_MULTIPLIER))
                .distinct()
                .filter(aBoolean -> aBoolean)
                .findFirst();
        return optionalHasAllowedTrafaretPositionModifiers.isPresent();
    }

}
