package ru.yandex.direct.core.entity.adgroup.service.complex.cpm;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.adgroup.container.ComplexCpmAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.campaign.model.CpmPriceCampaign;
import ru.yandex.direct.core.entity.crypta.repository.CryptaSegmentRepository;
import ru.yandex.direct.core.entity.pricepackage.model.PricePackage;
import ru.yandex.direct.core.entity.pricepackage.model.PricePackageRetargetingSubCategory;
import ru.yandex.direct.core.entity.pricepackage.model.PriceRetargetingCondition;
import ru.yandex.direct.core.entity.pricepackage.model.TargetingsCustom;
import ru.yandex.direct.core.entity.retargeting.model.CryptaInterestType;
import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.entity.retargeting.model.GoalType;
import ru.yandex.direct.core.entity.retargeting.model.RetargetingCondition;
import ru.yandex.direct.core.entity.retargeting.model.Rule;
import ru.yandex.direct.core.entity.retargeting.model.RuleType;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.stub.MetrikaHelperStub;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.direct.core.entity.adgroup.service.AdGroupCpmPriceUtils.PRIORITY_DEFAULT;
import static ru.yandex.direct.core.entity.adgroup.service.AdGroupCpmPriceUtils.PRIORITY_SPECIFIC;
import static ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupDefects.notAllowedValue;
import static ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupDefects.priceSalesDisallowedAudienceSegments;
import static ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupDefects.priceSalesDisallowedMetrikaSegments;
import static ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupDefects.priceSalesTooFewTargetings;
import static ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupDefects.priceSalesTooManyTargetings;
import static ru.yandex.direct.core.entity.retargeting.model.GoalType.AB_SEGMENT;
import static ru.yandex.direct.core.entity.retargeting.model.GoalType.AUDIENCE;
import static ru.yandex.direct.core.entity.retargeting.model.GoalType.BEHAVIORS;
import static ru.yandex.direct.core.entity.retargeting.model.GoalType.CONTENT_CATEGORY;
import static ru.yandex.direct.core.entity.retargeting.model.GoalType.CONTENT_GENRE;
import static ru.yandex.direct.core.entity.retargeting.model.GoalType.ECOMMERCE;
import static ru.yandex.direct.core.entity.retargeting.model.GoalType.FAMILY;
import static ru.yandex.direct.core.entity.retargeting.model.GoalType.GOAL;
import static ru.yandex.direct.core.entity.retargeting.model.GoalType.INTERESTS;
import static ru.yandex.direct.core.entity.retargeting.model.GoalType.SEGMENT;
import static ru.yandex.direct.core.entity.retargeting.model.GoalType.SOCIAL_DEMO;
import static ru.yandex.direct.core.testing.data.TestGroups.activeCpmVideoAdGroup;
import static ru.yandex.direct.core.testing.data.TestPricePackages.defaultPricePackage;
import static ru.yandex.direct.core.testing.data.TestRetargetingConditions.defaultRetCondition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoErrors;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.utils.FunctionalUtils.filterList;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringRunner.class)
public class PricePackageAddComplexCpmAdGroupValidationServiceTest {
    @Autowired
    private AddComplexCpmAdGroupValidationService addValidationService;

    @Autowired
    private CryptaSegmentRepository cryptaSegmentRepository;

    @Autowired
    private Steps steps;

    @Autowired
    private MetrikaHelperStub metrikaHelperStub;

    private ClientId clientId;

    private PricePackage pricePackage;
    private CpmPriceCampaign cpmPriceCampaign;
    private ClientInfo defaultClient;
    private Long priority;
    private enum LocatGoalName {
        GENDER_MALE(2499000001L, 2499000021L, SOCIAL_DEMO),
        AGE_18_24(2499000004L, 2499000022L, SOCIAL_DEMO),
        AGE_25_34(2499000005L, 2499000022L, SOCIAL_DEMO),
        MARRIED(2499000101L, 2499000100L, FAMILY),
        LOW_TV(2499000201L, 2499000200L, BEHAVIORS),
        SMARTPHONE_15_25(2499000242L, 2499000240L, BEHAVIORS),
        TEA(2499001298L, 2499001184L, INTERESTS),
        CATS(2499001135L, 2499001255L, INTERESTS),
        FANTASY(4294970304L, 0L, CONTENT_GENRE),
        LOCAL_CINEMA(4294970299L, -42L, CONTENT_GENRE),
        RESTORANTS(4294968309L, 4294968308L, CONTENT_CATEGORY),
        FASTFOOD(4294968311L, 4294968308L, CONTENT_CATEGORY),
        AUTO(4294968296L, 0L, CONTENT_CATEGORY),
        LOCAL_TOURISM(4294968300L, 4294968298L, CONTENT_CATEGORY),
        AVIA_HOTEL(4294968301L, 4294968298L, CONTENT_CATEGORY),
        GEOGRAPHY(4294968298L, 0L, CONTENT_CATEGORY),
        METRIKA1(1_499_999_100L, 0L, SEGMENT),
        METRIKA2(234L, 0L, GOAL),
        METRIKA3(3_899_000_000L, 0L, ECOMMERCE),
        METRIKA4(2_599_000_000L, 0L, AB_SEGMENT),
        METRIKA_AUDIENCE(2498000000L, 0L, AUDIENCE);

        private final Long id;
        private final Long parentId;
        private final GoalType goalType;

        LocatGoalName(Long id, Long parentId, GoalType goalType) {
            this.id = id;
            this.parentId = parentId;
            this.goalType = goalType;
        }
    }
    private Goal makeGoalByLocalGoalName(LocatGoalName goalName) {
        Goal goal = new Goal();
        goal.setId(goalName.id);
        goal.setParentId(goalName.parentId);
        goal.setType(goalName.goalType);
        return goal;
    }
    @Before
    public void setUp() {
        defaultClient = steps.clientSteps().createDefaultClient();
        clientId = defaultClient.getClientId();
        priority = PRIORITY_DEFAULT;
    }

    private ValidationResult<List<AdGroup>, Defect> setUpAdGroups(List<Goal> packageGoals, List<Goal> adGroupGoals) {
        return setUpAdGroups(packageGoals, adGroupGoals, defaultPriceRetargetingCondition(), null);
    }

    private ValidationResult<List<AdGroup>, Defect> setUpAdGroups(
            List<Goal> packageGoals,
            List<Goal> adGroupGoals,
            Set<AdGroupType> allowedAdGroupTypes) {
        return setUpAdGroups(packageGoals, adGroupGoals, defaultPriceRetargetingCondition(), allowedAdGroupTypes);
    }

    private ValidationResult<List<AdGroup>, Defect> setUpAdGroups(
            List<Goal> packageGoals,
            List<Goal> adGroupGoals,
            PriceRetargetingCondition priceRetargetingCondition) {
        return setUpAdGroups(packageGoals, adGroupGoals, priceRetargetingCondition, null);
    }

    private ValidationResult<List<AdGroup>, Defect> setUpAdGroups(
            List<Goal> packageGoals,
            List<Goal> adGroupGoals,
            PriceRetargetingCondition priceRetargetingCondition,
            Set<AdGroupType> allowedAdGroupTypes) {
        if (priceRetargetingCondition != null ) {
            priceRetargetingCondition.withCryptaSegments(mapList(packageGoals, Goal::getId));
        }

        if (allowedAdGroupTypes == null) {
            allowedAdGroupTypes = Set.of(AdGroupType.CPM_YNDX_FRONTPAGE, AdGroupType.CPM_VIDEO);
        }
        PricePackage defaultPricePackage = defaultPricePackage()
                .withAvailableAdGroupTypes(allowedAdGroupTypes)
                .withIsFrontpage(false)
                .withTargetingsCustom(
                new TargetingsCustom().withRetargetingCondition(priceRetargetingCondition));
        pricePackage = steps.pricePackageSteps().createPricePackage(defaultPricePackage).getPricePackage();
        cpmPriceCampaign = steps.campaignSteps().createActiveCpmPriceCampaign(defaultClient, pricePackage);
        List<Rule> rules = List.of(new Rule().withType(RuleType.OR).withGoals(adGroupGoals));
        metrikaHelperStub.addGoalsFromRules(cpmPriceCampaign.getUid(), rules);

        List<Goal> contentCategoryGoals = filterList(adGroupGoals,
                        goal -> goal.getType().equals(CONTENT_CATEGORY) || goal.getType().equals(CONTENT_GENRE));

        List<Rule> contentCategoryRules = List.of(new Rule().withType(RuleType.OR).withGoals(
                filterList(adGroupGoals, contentCategoryGoals::contains)));

        List<Rule> retargetingRules = List.of(
                new Rule().withType(RuleType.OR).withGoals(
                filterList(adGroupGoals, goal -> goal.getType().isMetrika())),
                new Rule().withType(RuleType.OR)
                        .withGoals(filterList(adGroupGoals, goal -> goal.getType()==INTERESTS))
                        .withInterestType(CryptaInterestType.all),
                new Rule().withType(RuleType.OR).withGoals(
                filterList(adGroupGoals, goal -> goal.getType().isCrypta()
                        && !contentCategoryGoals.contains(goal)
                        && goal.getType()!=INTERESTS))
        );

        RetargetingCondition retargetingCondition = (RetargetingCondition) defaultRetCondition(clientId)
                .withRules(retargetingRules);
        AdGroup adGroup = activeCpmVideoAdGroup(cpmPriceCampaign.getId()).withPriority(priority);
        adGroup.setContentCategoriesRetargetingConditionRules(contentCategoryRules);
        ComplexCpmAdGroup complexCpmAdGroup = new ComplexCpmAdGroup()
                .withAdGroup(adGroup)
                .withRetargetingConditions(List.of(retargetingCondition));
        List<AdGroup> adGroups = singletonList(adGroup);
        return addValidationService.validateAdGroups(
                ValidationResult.success(adGroups), singletonList(complexCpmAdGroup), clientId);
    }

    private PriceRetargetingCondition defaultPriceRetargetingCondition() {
        return new PriceRetargetingCondition()
                .withAllowMetrikaSegments(true)
                .withAllowAudienceSegments(true)
                .withLowerCryptaTypesCount(2)
                .withUpperCryptaTypesCount(4);
    }

    @Test
    public void validateAdGroups_PricePackageRetargetings1_Success() {
        List<LocatGoalName> goalTypes = List.of(
                LocatGoalName.MARRIED,
                LocatGoalName.AGE_25_34,
                LocatGoalName.METRIKA2,
                LocatGoalName.METRIKA3);
        List<Goal> goals = mapList(goalTypes, this::makeGoalByLocalGoalName);

        var vr = setUpAdGroups(goals, goals);

        assertThat(vr, hasNoErrors());
    }
    @Test
    public void validateAdGroups_PricePackageRetargetings2_Success() {
        List<LocatGoalName> packageGoalTypes = List.of(
                LocatGoalName.MARRIED,
                LocatGoalName.AGE_25_34,
                LocatGoalName.METRIKA2,
                LocatGoalName.METRIKA3);
        List<Goal> packageGoals = mapList(packageGoalTypes, this::makeGoalByLocalGoalName);

        List<Goal> adGroupGoals = packageGoals.subList(0,2);
        var vr = setUpAdGroups(packageGoals, adGroupGoals);

        assertThat(vr, hasNoErrors());
    }

    @Test
    public void validateAdGroups_PricePackageRetargetings_NotValidGoals1_Error() {
        List<LocatGoalName> packageGoalTypes = List.of(
                LocatGoalName.MARRIED,
                LocatGoalName.AGE_25_34,
                LocatGoalName.METRIKA2,
                LocatGoalName.METRIKA3);
        List<Goal> packageGoals = mapList(packageGoalTypes, this::makeGoalByLocalGoalName);
        List<LocatGoalName> adGroupGoalTypes = List.of(
                LocatGoalName.METRIKA4,
                LocatGoalName.LOW_TV);
        List<Goal> adGroupGoals = mapList(adGroupGoalTypes, this::makeGoalByLocalGoalName);

        var vr = setUpAdGroups(packageGoals, adGroupGoals);

        assertThat(vr, hasDefectDefinitionWith(
                validationError(path(index(0)), notAllowedValue())));
    }

    @Test
    public void validateAdGroups_PricePackageRetargetings_NotValidGoals2_Error() {
        List<LocatGoalName> packageGoalTypes = List.of(
                LocatGoalName.MARRIED,
                LocatGoalName.AGE_25_34,
                LocatGoalName.METRIKA1);
        List<Goal> packageGoals = mapList(packageGoalTypes, this::makeGoalByLocalGoalName);

        List<Goal> adGroupGoals = new ArrayList<>(packageGoals);
        adGroupGoals.add(makeGoalByLocalGoalName(LocatGoalName.SMARTPHONE_15_25));
        var vr = setUpAdGroups(packageGoals, adGroupGoals);

        assertThat(vr, hasDefectDefinitionWith(
                validationError(path(index(0)), notAllowedValue())));
    }

    @Test
    public void validateAdGroups_PricePackageRetargetingsTooManyGoals_Error() {
        List<LocatGoalName> packageGoalTypes = List.of(
                LocatGoalName.MARRIED,
                LocatGoalName.GENDER_MALE,
                LocatGoalName.AGE_25_34,
                LocatGoalName.CATS,
                LocatGoalName.METRIKA1,
                LocatGoalName.METRIKA2,
                LocatGoalName.METRIKA3,
                LocatGoalName.METRIKA4);

        List<Goal> packageGoals = mapList(packageGoalTypes, this::makeGoalByLocalGoalName);

        List<Goal> adGroupGoals = List.copyOf(packageGoals);

        var priceRetargetingCondition = defaultPriceRetargetingCondition()
                .withUpperCryptaTypesCount(3);

        var vr = setUpAdGroups(packageGoals, adGroupGoals, priceRetargetingCondition);

        assertThat(vr, hasDefectDefinitionWith(
                validationError(path(index(0)),
                        priceSalesTooManyTargetings(4, 2, 3,
                                Set.of(PricePackageRetargetingSubCategory.FAMILY,
                                        PricePackageRetargetingSubCategory.SOCIAL_DEMO,
                                        PricePackageRetargetingSubCategory.INTERESTS,
                                        PricePackageRetargetingSubCategory.METRIKA)))));
    }

    @Test
    public void validateAdGroups_PricePackageRetargetings_TooFewGoals_Error() {
        List<LocatGoalName> packageGoalTypes = List.of(
                LocatGoalName.MARRIED,
                LocatGoalName.METRIKA1,
                LocatGoalName.METRIKA3,
                LocatGoalName.METRIKA4);

        List<Goal> packageGoals = mapList(packageGoalTypes, this::makeGoalByLocalGoalName);

        List<Goal> adGroupGoals = packageGoals.subList(0,1);

        var priceRetargetingCondition = defaultPriceRetargetingCondition()
                .withLowerCryptaTypesCount(2);

        var vr = setUpAdGroups(packageGoals, adGroupGoals, priceRetargetingCondition);

        assertThat(vr, hasDefectDefinitionWith(
                validationError(path(index(0)),
                        priceSalesTooFewTargetings(1, 2, 4,
                                Set.of(PricePackageRetargetingSubCategory.FAMILY)))));
    }

    @Test
    public void validateAdGroups_PricePackageRetargetings_CategoryAndGenreAsOneType_Success() {
        List<LocatGoalName> packageGoalTypes = List.of(
                LocatGoalName.GEOGRAPHY,
                LocatGoalName.LOCAL_CINEMA);

        List<Goal> packageGoals = mapList(packageGoalTypes, this::makeGoalByLocalGoalName);

        List<Goal> adGroupGoals = List.of(makeGoalByLocalGoalName(LocatGoalName.AVIA_HOTEL));

        var priceRetargetingCondition = defaultPriceRetargetingCondition()
                .withLowerCryptaTypesCount(1)
                .withUpperCryptaTypesCount(1);

        var vr = setUpAdGroups(packageGoals, adGroupGoals, priceRetargetingCondition);

        assertThat(vr, hasNoErrors());

    }

    @Test
    public void validateAdGroups_PricePackageRetargetings_CategoryAndGenreAsOneType_Error() {
        List<LocatGoalName> packageGoalTypes = List.of(
                LocatGoalName.GEOGRAPHY,
                LocatGoalName.LOCAL_CINEMA);

        List<Goal> packageGoals = mapList(packageGoalTypes, this::makeGoalByLocalGoalName);

        List<Goal> adGroupGoals = List.of(makeGoalByLocalGoalName(LocatGoalName.AVIA_HOTEL));

        var priceRetargetingCondition = defaultPriceRetargetingCondition()
                .withLowerCryptaTypesCount(2);

        var vr = setUpAdGroups(packageGoals, adGroupGoals, priceRetargetingCondition);

        assertThat(vr, hasDefectDefinitionWith(
                validationError(path(index(0)),
                        priceSalesTooFewTargetings(1, 2, 4,
                                Set.of(PricePackageRetargetingSubCategory.GENRE_CATEGORIES)))));
    }

    @Test
    public void validateAdGroups_PricePackageRetargetings_ZeroGoalsLowerLimit_Sucess() {
        List<LocatGoalName> packageGoalTypes = List.of(
                LocatGoalName.MARRIED,
                LocatGoalName.AGE_25_34,
                LocatGoalName.METRIKA1,
                LocatGoalName.METRIKA2,
                LocatGoalName.METRIKA3);

        List<Goal> packageGoals = mapList(packageGoalTypes, this::makeGoalByLocalGoalName);

        List<Goal> adGroupGoals = packageGoals.subList(0,1);

        var priceRetargetingCondition = defaultPriceRetargetingCondition()
                .withLowerCryptaTypesCount(0);

        var vr = setUpAdGroups(packageGoals, adGroupGoals, priceRetargetingCondition);

        assertThat(vr, hasNoErrors());
    }

    @Test
    public void validateAdGroups_PricePackageRetargetings_ZeroGoalsUpperLimit_Sucess() {
        List<LocatGoalName> packageGoalTypes = List.of(
                LocatGoalName.MARRIED,
                LocatGoalName.AGE_25_34,
                LocatGoalName.GENDER_MALE,
                LocatGoalName.METRIKA1,
                LocatGoalName.METRIKA2,
                LocatGoalName.METRIKA3,
                LocatGoalName.METRIKA4);

        List<Goal> packageGoals = mapList(packageGoalTypes, this::makeGoalByLocalGoalName);

        List<Goal> adGroupGoals = List.copyOf(packageGoals);

        var priceRetargetingCondition = defaultPriceRetargetingCondition()
                .withUpperCryptaTypesCount(0);

        var vr = setUpAdGroups(packageGoals, adGroupGoals, priceRetargetingCondition);

        assertThat(vr, hasNoErrors());
    }

    @Test
    public void validateAdGroups_PricePackageRetargetings_ZeroGoalsBothLimit_Sucess() {
        List<LocatGoalName> packageGoalTypes = List.of(
                LocatGoalName.MARRIED);

        List<Goal> packageGoals = mapList(packageGoalTypes, this::makeGoalByLocalGoalName);

        List<Goal> adGroupGoals = List.copyOf(packageGoals);


        var priceRetargetingCondition = defaultPriceRetargetingCondition()
                .withLowerCryptaTypesCount(0)
                .withUpperCryptaTypesCount(0);

        var vr = setUpAdGroups(packageGoals, adGroupGoals, priceRetargetingCondition);

        assertThat(vr, hasNoErrors());
    }

    @Test
    public void validateAdGroups_PricePackageRetargetings_DisallowMetrikaSegments_Error() {
        List<LocatGoalName> packageGoalTypes = List.of(
                LocatGoalName.MARRIED,
                LocatGoalName.AGE_25_34,
                LocatGoalName.GENDER_MALE,
                LocatGoalName.METRIKA1,
                LocatGoalName.METRIKA2,
                LocatGoalName.METRIKA3);

        List<Goal> packageGoals = mapList(packageGoalTypes, this::makeGoalByLocalGoalName);

        List<Goal> adGroupGoals = packageGoals.subList(2,4);

        var priceRetargetingCondition = defaultPriceRetargetingCondition()
                .withAllowMetrikaSegments(false);

        var vr = setUpAdGroups(packageGoals, adGroupGoals, priceRetargetingCondition);

        assertThat(vr, hasDefectDefinitionWith(
                validationError(path(index(0)),
                        priceSalesDisallowedMetrikaSegments())));

    }

    @Test
    public void validateAdGroups_PricePackageRetargetings_DisallowAudienceSegments_Error() {
        List<LocatGoalName> packageGoalTypes = List.of(
                LocatGoalName.MARRIED,
                LocatGoalName.AGE_25_34,
                LocatGoalName.GENDER_MALE,
                LocatGoalName.METRIKA1,
                LocatGoalName.METRIKA2,
                LocatGoalName.METRIKA3);

        List<Goal> packageGoals = mapList(packageGoalTypes, this::makeGoalByLocalGoalName);

        List<Goal> adGroupGoals = List.of(packageGoals.get(1), makeGoalByLocalGoalName(LocatGoalName.METRIKA_AUDIENCE));

        var priceRetargetingCondition = defaultPriceRetargetingCondition()
                .withAllowAudienceSegments(false);

        var vr = setUpAdGroups(packageGoals, adGroupGoals, priceRetargetingCondition);

        assertThat(vr, hasDefectDefinitionWith(
                validationError(path(index(0)),
                        priceSalesDisallowedAudienceSegments())));

    }

    @Test
    public void validateAdGroups_PricePackageRetargetings_NullRetargetingCondition_Success() {

        PriceRetargetingCondition priceRetargetingCondition = null;

        var vr = setUpAdGroups(emptyList(), emptyList(), priceRetargetingCondition);

        assertThat(vr, hasNoErrors());

    }

    @Test
    public void validateAdGroups_checkContentCategoriesByParent1_Success() {

        // Отечественный туризм - потомок Географии
        Goal localTourism = makeGoalByLocalGoalName(LocatGoalName.LOCAL_TOURISM);

        // Авиабилеты и гостиницы - потомок Географии
        Goal aviaHotel = makeGoalByLocalGoalName(LocatGoalName.AVIA_HOTEL);

        //Авто - без потомков и родителей
        Goal auto = makeGoalByLocalGoalName(LocatGoalName.AUTO);

        List<Goal> packageGoals = List.of(auto,
                // География
                makeGoalByLocalGoalName(LocatGoalName.GEOGRAPHY));

        List<Goal> adGroupGoals = List.of(localTourism, aviaHotel, auto);

        var vr = setUpAdGroups(
                packageGoals,
                adGroupGoals,
                defaultPriceRetargetingCondition().withLowerCryptaTypesCount(1));
        assertThat(vr, hasNoErrors());
    }

    @Test
    public void validateAdGroups_checkContentCategoriesByParent2_Success() {

        // Отечественный туризм - потомок Географии
        Goal localTourism = makeGoalByLocalGoalName(LocatGoalName.LOCAL_TOURISM);

        // Авиабилеты и гостиницы - потомок Географии
        Goal aviaHotel = makeGoalByLocalGoalName(LocatGoalName.AVIA_HOTEL);

        //Авто - без потомков и родителей
        Goal auto = makeGoalByLocalGoalName(LocatGoalName.AUTO);

        Goal cinemaGenre = makeGoalByLocalGoalName(LocatGoalName.LOCAL_CINEMA);

        List<Goal> packageGoals = List.of(auto,
                // География
                makeGoalByLocalGoalName(LocatGoalName.GEOGRAPHY),
                cinemaGenre);

        List<Goal> adGroupGoals = List.of(localTourism, aviaHotel, auto);

        var vr = setUpAdGroups(
                packageGoals,
                adGroupGoals,
                defaultPriceRetargetingCondition().withLowerCryptaTypesCount(1));
        assertThat(vr, hasNoErrors());
    }

    @Test
    public void validateAdGroups_PricePackagePrioritySpecific_Success() {
        priority = PRIORITY_SPECIFIC;
        List<LocatGoalName> goalTypes = List.of(
                LocatGoalName.MARRIED,
                LocatGoalName.AGE_25_34);

        List<Goal> goals = mapList(goalTypes, this::makeGoalByLocalGoalName);

        var vr = setUpAdGroups(goals, goals, Set.of(AdGroupType.CPM_VIDEO));

        assertThat(vr, hasNoErrors());
    }
}
