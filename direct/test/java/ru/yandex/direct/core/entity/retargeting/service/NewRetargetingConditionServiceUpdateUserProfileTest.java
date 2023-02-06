package ru.yandex.direct.core.entity.retargeting.service;

import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.common.net.NetAcl;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.bs.resync.queue.repository.BsResyncQueueRepository;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.metrika.repository.LalSegmentRepository;
import ru.yandex.direct.core.entity.metrika.service.MobileGoalsService;
import ru.yandex.direct.core.entity.multipliers.repository.MultipliersRepository;
import ru.yandex.direct.core.entity.placements.repository.PlacementsRepository;
import ru.yandex.direct.core.entity.retargeting.model.CryptaInterestType;
import ru.yandex.direct.core.entity.retargeting.model.GoalType;
import ru.yandex.direct.core.entity.retargeting.model.Retargeting;
import ru.yandex.direct.core.entity.retargeting.model.RetargetingCondition;
import ru.yandex.direct.core.entity.retargeting.model.Rule;
import ru.yandex.direct.core.entity.retargeting.repository.RetargetingConditionRepository;
import ru.yandex.direct.core.entity.retargeting.repository.RetargetingGoalsRepository;
import ru.yandex.direct.core.entity.retargeting.repository.TargetingCategoriesCache;
import ru.yandex.direct.core.entity.retargeting.service.common.GoalUtilsService;
import ru.yandex.direct.core.entity.retargeting.service.helper.RetargetingConditionBannerWithPixelsValidationHelper;
import ru.yandex.direct.core.entity.retargeting.service.helper.RetargetingConditionWithLalSegmentHelper;
import ru.yandex.direct.core.entity.retargeting.service.validation2.AddRetargetingConditionValidationService2;
import ru.yandex.direct.core.entity.retargeting.service.validation2.DeleteRetargetingConditionValidationService2;
import ru.yandex.direct.core.entity.retargeting.service.validation2.ReplaceGoalsInRetargetingValidationService2;
import ru.yandex.direct.core.entity.retargeting.service.validation2.RetargetingConditionCryptaSegmentsProvider;
import ru.yandex.direct.core.entity.retargeting.service.validation2.RetargetingConditionsWithAdsValidator;
import ru.yandex.direct.core.entity.retargeting.service.validation2.UpdateRetargetingConditionValidationService2;
import ru.yandex.direct.core.entity.retargeting.service.validation2.cpmprice.RetargetingConditionsCpmPriceValidationDataFactory;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.RetConditionInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.rbac.RbacService;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.DefectInfo;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.entity.retargeting.service.validation2.RetargetingDefects.invalidRetargetingConditionInUserProfileInTgo;
import static ru.yandex.direct.core.testing.data.TestFullGoals.defaultGoalByType;
import static ru.yandex.direct.core.testing.data.TestRetargetingConditions.defaultCpmRetCondition;
import static ru.yandex.direct.core.testing.data.TestRetargetingConditions.defaultRule;
import static ru.yandex.direct.core.testing.data.TestRetargetings.defaultRetargeting;
import static ru.yandex.direct.feature.FeatureName.SKIP_GOAL_EXISTENCE_FOR_AGENCY;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class NewRetargetingConditionServiceUpdateUserProfileTest extends BaseRetargetingConditionServiceTest {

    @Autowired
    private Steps steps;

    @Autowired
    private PlacementsRepository placementsRepository;

    @Autowired
    private RetargetingConditionRepository retConditionRepository;

    @Autowired
    private RetargetingGoalsRepository retGoalsRepository;

    @Autowired
    private AdGroupRepository adGroupRepository;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private RetargetingConditionsCpmPriceValidationDataFactory cpmPriceValidationDataFactory;

    @Autowired
    private MultipliersRepository multipliersRepository;

    @Autowired
    private BsResyncQueueRepository bsResyncQueueRepository;

    @Autowired
    private RbacService rbacService;

    @Autowired
    private ShardHelper shardHelper;

    @Autowired
    private GoalUtilsService goalUtilsService;

    @Autowired
    private AddRetargetingConditionValidationService2 addValidationService;

    @Autowired
    private DeleteRetargetingConditionValidationService2 deleteValidationService;

    @Autowired
    private ReplaceGoalsInRetargetingValidationService2 replaceGoalsInRetargetingValidationService;

    @Autowired
    private RetargetingConditionBannerWithPixelsValidationHelper cpmBannerHelper;

    @Autowired
    private RetargetingConditionsWithAdsValidator retargetingConditionsWithAdsValidator;

    @Autowired
    private FindOrCreateRetargetingConditionService findOrCreateRetargetingConditionService;

    @Autowired
    private FeatureService featureService;

    @Mock
    private RetargetingConditionCryptaSegmentsProvider retargetingConditionCryptaSegmentsProvider;

    @Autowired
    private RetargetingConditionWithLalSegmentHelper retargetingConditionWithLalSegmentHelper;

    @Autowired
    private NetAcl netAcl;

    @Before
    public void before() {
        super.before();
        MockitoAnnotations.initMocks(this);
        var lalSegmentRepository = mock(LalSegmentRepository.class);
        FeatureService featureService = mock(FeatureService.class);
        when(featureService.isEnabledForClientId(any(ClientId.class), any(FeatureName.class)))
                .thenReturn(true);
        when(featureService.getEnabledForClientId(any(ClientId.class)))
                .thenReturn(Set.of(SKIP_GOAL_EXISTENCE_FOR_AGENCY.getName()));

        MobileGoalsService mobileGoalsService = mock(MobileGoalsService.class);
        when(mobileGoalsService.getAllAvailableInAppMobileGoals(any())).thenReturn(List.of());

        var targetingCategoriesCache = mock(TargetingCategoriesCache.class);
        retargetingConditionOperationFactory = new RetargetingConditionOperationFactory(retConditionRepository,
                lalSegmentRepository,
                retGoalsRepository,
                adGroupRepository,
                multipliersRepository,
                bsResyncQueueRepository,
                shardHelper,
                goalUtilsService,
                addValidationService,
                new UpdateRetargetingConditionValidationService2(retConditionRepository,
                        cpmBannerHelper, retargetingConditionCryptaSegmentsProvider, lalSegmentRepository, goalUtilsService,
                        retargetingConditionsWithAdsValidator, featureService, adGroupRepository, campaignRepository,
                        targetingCategoriesCache,
                        cpmPriceValidationDataFactory, rbacService, mobileGoalsService, netAcl),
                deleteValidationService,
                replaceGoalsInRetargetingValidationService,
                featureService, findOrCreateRetargetingConditionService, retargetingConditionWithLalSegmentHelper);
    }

    @Test
    public void validate_Retargetings_interestsCanOnlyBeShortTermInUserProfileForTextAdGroups_Errors() {
        // создаем текстовую группу с условием на короткие интересы
        AdGroupInfo activeTextAdGroup = steps.adGroupSteps().createActiveTextAdGroup();

        RetargetingCondition retargetingConditionForTgo = (RetargetingCondition) defaultCpmRetCondition()
                .withRules(singletonList(defaultRule(singletonList(defaultGoalByType(GoalType.INTERESTS)),
                        CryptaInterestType.short_term)));

        RetConditionInfo retConditionInfo =
                steps.retConditionSteps().createRetCondition(retargetingConditionForTgo, clientInfo);

        Retargeting retargeting = defaultRetargeting(activeTextAdGroup.getCampaignId(),
                activeTextAdGroup.getAdGroupId(), retConditionInfo.getRetConditionId());

        steps.retargetingSteps()
                .createRetargeting(retargeting, new CampaignInfo().withClientInfo(clientInfo), retConditionInfo);

        // готовим изменения - меняем условие на длинные интересы
        retargetingConditionForTgo.getRules().get(0).setInterestType(CryptaInterestType.long_term);
        List<ModelChanges<RetargetingCondition>> modelChangesList = singletonList(
                changeCondition(retargetingConditionForTgo, retargetingConditionForTgo.getId(), null,
                        null, retargetingConditionForTgo.getRules()));

        MassResult<Long> result =
                retargetingConditionOperationFactory.updateRetargetingConditions(clientId, modelChangesList);
        List<DefectInfo<Defect>> errors = result.getValidationResult().flattenErrors();
        Assert.assertThat(errors, hasSize(1));
        Assert.assertThat(errors,
                contains(validationError(path(index(0)), invalidRetargetingConditionInUserProfileInTgo())));

    }
    private ModelChanges<RetargetingCondition> changeCondition(
            RetargetingCondition condition, long id, String name,
            String description, List<Rule> rules) {
        ModelChanges<RetargetingCondition> modelChanges = retargetingModelChanges(id);

        condition.setId(id);
        if (name != null) {
            condition.setName(name);
            modelChanges.process(condition.getName(), RetargetingCondition.NAME);
        }
        if (description != null) {
            condition.setDescription(description);
            modelChanges.process(condition.getDescription(), RetargetingCondition.DESCRIPTION);
        }
        if (rules != null) {
            condition.setRules(rules);
            modelChanges.process(condition.getRules(), RetargetingCondition.RULES);
        }
        return modelChanges;
    }

    private static ModelChanges<RetargetingCondition> retargetingModelChanges(long id) {
        return new ModelChanges<>(id, RetargetingCondition.class);
    }
}
