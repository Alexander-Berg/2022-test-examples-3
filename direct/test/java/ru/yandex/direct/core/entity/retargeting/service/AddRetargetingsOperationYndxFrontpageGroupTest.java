package ru.yandex.direct.core.entity.retargeting.service;

import java.math.BigDecimal;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.campaign.model.CpmPriceCampaign;
import ru.yandex.direct.core.entity.pricepackage.model.PricePackage;
import ru.yandex.direct.core.entity.retargeting.model.ConditionType;
import ru.yandex.direct.core.entity.retargeting.model.GoalType;
import ru.yandex.direct.core.entity.retargeting.model.Retargeting;
import ru.yandex.direct.core.entity.retargeting.model.RetargetingCondition;
import ru.yandex.direct.core.entity.retargeting.model.Rule;
import ru.yandex.direct.core.entity.retargeting.model.RuleType;
import ru.yandex.direct.core.entity.retargeting.model.TargetInterest;
import ru.yandex.direct.core.entity.retargeting.repository.RetargetingRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.RetConditionInfo;
import ru.yandex.direct.core.testing.repository.TestCpmYndxFrontpageRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.test.utils.differ.BigDecimalDiffer;
import ru.yandex.direct.validation.result.Path;

import static java.util.Collections.emptyList;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.direct.core.testing.data.TestClients.defaultClient;
import static ru.yandex.direct.core.testing.data.TestFullGoals.defaultGoalByType;
import static ru.yandex.direct.core.testing.data.TestPricePackages.DEFAULT_RETARGETING_CONDITION;
import static ru.yandex.direct.core.testing.data.TestPricePackages.allowedPricePackageClient;
import static ru.yandex.direct.core.testing.data.TestPricePackages.approvedPricePackage;
import static ru.yandex.direct.core.testing.data.TestPricePackages.emptyTargetingsCustom;
import static ru.yandex.direct.core.testing.data.TestRetargetingConditions.defaultRetCondition;
import static ru.yandex.direct.test.utils.matcher.LocalDateTimeMatcher.approximatelyNow;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isFullySuccessful;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.DefectIds.MUST_BE_NULL;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AddRetargetingsOperationYndxFrontpageGroupTest {

    private static final CompareStrategy RETARGETINGS_COMPARE_STRATEGY = DefaultCompareStrategies.allFields()
            .forFields(newPath("lastChangeTime")).useMatcher(approximatelyNow())
            .forFields(newPath("priceContext")).useDiffer(new BigDecimalDiffer());

    @Autowired
    private Steps steps;


    @Autowired
    private RetargetingService serviceUnderTest;

    @Autowired
    private RetargetingRepository retargetingRepository;

    @Autowired
    private TestCpmYndxFrontpageRepository testCpmYndxFrontpageRepository;

    private CpmPriceCampaign cpmPriceCampaign;
    private AdGroup adGroupForPriceCampaign;
    private RetConditionInfo retConditionForPriceCampaign;

    private ClientInfo clientInfo;
    private PricePackage pricePackage;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createClient(defaultClient().withWorkCurrency(CurrencyCode.RUB));
        testCpmYndxFrontpageRepository.fillMinBidsTestValues();

        var behaviorGoalForPriceCampaign = defaultGoalByType(GoalType.BEHAVIORS);
        pricePackage = approvedPricePackage()
                .withTargetingsCustom(emptyTargetingsCustom()
                        .withRetargetingCondition(
                                DEFAULT_RETARGETING_CONDITION
                                        .withCryptaSegments(List.of(behaviorGoalForPriceCampaign.getId()))
                        )
                )
                .withClients(List.of(allowedPricePackageClient(clientInfo)));
        steps.pricePackageSteps().createPricePackage(pricePackage);
        cpmPriceCampaign = steps.campaignSteps().createActiveCpmPriceCampaign(clientInfo, pricePackage);
        adGroupForPriceCampaign = steps.adGroupSteps().createDefaultAdGroupForPriceSales(cpmPriceCampaign, clientInfo);
        retConditionForPriceCampaign = steps.retConditionSteps()
                .createRetCondition((RetargetingCondition) defaultRetCondition(clientInfo.getClientId())
                .withType(ConditionType.interests)
                .withRules(List.of(new Rule()
                        .withType(RuleType.OR)
                        .withGoals(List.of(behaviorGoalForPriceCampaign)))), clientInfo);
    }

    @Test
    public void cpmPriceCampaign_Success() {
        TargetInterest targetInterest = new TargetInterest()
                .withAdGroupId(adGroupForPriceCampaign.getId())
                .withRetargetingConditionId(retConditionForPriceCampaign.getRetConditionId());

        MassResult<Long> result = addPartially(targetInterest);
        assertThat(result, isFullySuccessful());

        List<Retargeting> retargetings = retargetingRepository
                .getRetargetingsByAdGroups(clientInfo.getShard(), List.of(adGroupForPriceCampaign.getId()));
        assertThat(retargetings, hasSize(1));
        Retargeting expectedRetargeting = new Retargeting()
                .withId(result.get(0).getResult())
                .withCampaignId(cpmPriceCampaign.getId())
                .withAdGroupId(adGroupForPriceCampaign.getId())
                .withRetargetingConditionId(retConditionForPriceCampaign.getRetConditionId())
                .withIsSuspended(false)
                .withStatusBsSynced(StatusBsSynced.NO)
                .withPriceContext(pricePackage.getPrice())
                .withAutobudgetPriority(null);
        assertThat(retargetings.get(0), beanDiffer(expectedRetargeting).useCompareStrategy(RETARGETINGS_COMPARE_STRATEGY));
    }

    @Test
    public void cpmPriceCampaign_FailWhenRetargetingPriceProvided() {
        BigDecimal priceContext = pricePackage.getPrice().add(BigDecimal.TEN);
        TargetInterest targetInterest = new TargetInterest()
                .withAdGroupId(adGroupForPriceCampaign.getId())
                .withRetargetingConditionId(retConditionForPriceCampaign.getRetConditionId())
                .withPriceContext(priceContext);

        MassResult<Long> result = addPartially(targetInterest);

        Path errPath = path(index(0), field(Retargeting.PRICE_CONTEXT.name()));
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(errPath, MUST_BE_NULL)));
        assertThat(result.getValidationResult().flattenErrors(), hasSize(1));
    }


    @Test
    public void cpd_AdgroupPriceContext() {
        var pricePackage = approvedPricePackage()
                .withIsCpd(true)
                .withCurrency(CurrencyCode.RUB)
                .withOrderVolumeMin(25_000_000L)
                .withOrderVolumeMax(25_000_000L)
                .withPrice(BigDecimal.valueOf(50_000_000L))
                .withClients(List.of(allowedPricePackageClient(clientInfo)));
        steps.pricePackageSteps().createPricePackage(pricePackage);
        CpmPriceCampaign cpmPriceCampaign = steps.campaignSteps().createActiveCpmPriceCampaign(clientInfo, pricePackage);
        AdGroup adGroupForPriceCampaign = steps.adGroupSteps().createDefaultAdGroupForPriceSales(cpmPriceCampaign, clientInfo);
        RetConditionInfo retConditionForPriceCampaign = steps.retConditionSteps()
                .createRetCondition((RetargetingCondition) defaultRetCondition(clientInfo.getClientId())
                        .withType(ConditionType.interests)
                        .withRules(emptyList()), clientInfo);
        TargetInterest targetInterest = new TargetInterest()
                .withAdGroupId(adGroupForPriceCampaign.getId())
                .withRetargetingConditionId(retConditionForPriceCampaign.getRetConditionId());

        MassResult<Long> result = addPartially(targetInterest);
        assertThat(result, isFullySuccessful());

        List<Retargeting> retargetings = retargetingRepository
                .getRetargetingsByAdGroups(clientInfo.getShard(), List.of(adGroupForPriceCampaign.getId()));
        assertThat(retargetings, hasSize(1));
        assertThat(retargetings.get(0).getPriceContext(), comparesEqualTo(BigDecimal.valueOf(2000)));
    }

    private MassResult<Long> addPartially(TargetInterest targetInterests) {
        return serviceUnderTest
                .createAddOperation(Applicability.PARTIAL, List.of(targetInterests),
                        clientInfo.getUid(), clientInfo.getClientId(), clientInfo.getUid())
                .prepareAndApply();
    }
}
