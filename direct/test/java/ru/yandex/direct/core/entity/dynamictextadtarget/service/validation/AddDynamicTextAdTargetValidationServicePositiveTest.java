package ru.yandex.direct.core.entity.dynamictextadtarget.service.validation;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.campaign.container.AffectedCampaignIdsContainer;
import ru.yandex.direct.core.entity.campaign.repository.CampaignAccessCheckRepository;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.campaign.service.accesschecker.CampaignSubObjectAccessCheckerFactory;
import ru.yandex.direct.core.entity.campaign.service.accesschecker.RequestCampaignAccessibilityCheckerProvider;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.entity.dynamictextadtarget.model.DynamicTextAdTarget;
import ru.yandex.direct.core.entity.dynamictextadtarget.model.WebpageRule;
import ru.yandex.direct.core.entity.dynamictextadtarget.model.WebpageRuleKind;
import ru.yandex.direct.core.entity.dynamictextadtarget.model.WebpageRuleType;
import ru.yandex.direct.core.entity.dynamictextadtarget.repository.DynamicTextAdTargetRepository;
import ru.yandex.direct.core.entity.dynamictextadtarget.service.DynamicTextAdTargetService;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.steps.AdGroupSteps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.rbac.RbacService;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;
import static ru.yandex.direct.core.entity.dynamictextadtarget.service.validation.DynamicTextAdTargetConstants.MAX_DYNAMIC_TEXT_AD_TARGETS_IN_GROUP;
import static ru.yandex.direct.core.testing.data.TestDynamicTextAdTargets.defaultDynamicTextAdTarget;
import static ru.yandex.direct.core.testing.data.TestDynamicTextAdTargets.defaultDynamicTextAdTargetWithRandomRules;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AddDynamicTextAdTargetValidationServicePositiveTest {

    private AddDynamicTextAdTargetValidationService addValidationService;

    @Autowired
    private ShardHelper shardHelper;

    @Autowired
    private RbacService rbacService;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private CampaignAccessCheckRepository campaignAccessCheckRepository;

    @Autowired
    private AdGroupRepository adGroupRepository;

    @Autowired
    private ClientService clientService;

    @Autowired
    private DynamicTextAdTargetRepository dynamicTextAdTargetRepository;

    @Autowired
    private DynamicTextAdTargetService dynamicTextAdTargetService;

    @Autowired
    private AdGroupSteps adGroupSteps;

    @Autowired
    private RequestCampaignAccessibilityCheckerProvider requestAccessibleCampaignTypes;

    @Autowired
    private FeatureService featureService;

    private long operatorUid;
    private ClientId clientId;
    private int shard;
    private DynamicTextAdTarget dynamicTextAdTarget;
    private AdGroupInfo defaultAdGroup;

    @Before
    public void before() {
        defaultAdGroup = adGroupSteps.createActiveDynamicTextAdGroup();

        operatorUid = defaultAdGroup.getUid();
        clientId = defaultAdGroup.getClientId();
        dynamicTextAdTarget = defaultDynamicTextAdTarget(defaultAdGroup);
        shard = defaultAdGroup.getShard();

        CampaignSubObjectAccessCheckerFactory campaignSubObjectAccessCheckerFactory =
                new CampaignSubObjectAccessCheckerFactory(
                shardHelper, spy(rbacService), campaignAccessCheckRepository, new AffectedCampaignIdsContainer(),
                requestAccessibleCampaignTypes, featureService);

        addValidationService = new AddDynamicTextAdTargetValidationService(
                campaignSubObjectAccessCheckerFactory, clientService, campaignRepository, adGroupRepository,
                dynamicTextAdTargetRepository);
    }

    private ValidationResult<List<DynamicTextAdTarget>, Defect> validate(List<DynamicTextAdTarget> models) {
        return addValidationService.validateAdd(shard, operatorUid, clientId, models);
    }

    @Test
    public void positiveValidationResultWhenNoErrors() {
        ValidationResult<List<DynamicTextAdTarget>, Defect> actual = validate(singletonList(dynamicTextAdTarget));

        assertThat(actual).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void validate_addMaxCountInEmptyGroup() {
        ArrayList<DynamicTextAdTarget> dynamicTextAdTargetsToAdd = new ArrayList<>();
        for (int i = 0; i < MAX_DYNAMIC_TEXT_AD_TARGETS_IN_GROUP; i++) {
            dynamicTextAdTargetsToAdd.add(defaultDynamicTextAdTargetWithRandomRules(defaultAdGroup));
        }

        ValidationResult<List<DynamicTextAdTarget>, Defect> actual = validate(dynamicTextAdTargetsToAdd);

        assertThat(actual).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void validate_addMaxCountInNotEmptyGroup() {
        dynamicTextAdTargetService.addDynamicTextAdTargets(clientId, operatorUid,
                singletonList(defaultDynamicTextAdTargetWithRandomRules(defaultAdGroup)));

        ArrayList<DynamicTextAdTarget> dynamicTextAdTargetsToAdd = new ArrayList<>();
        for (int i = 0; i < MAX_DYNAMIC_TEXT_AD_TARGETS_IN_GROUP - 1; i++) {
            dynamicTextAdTargetsToAdd.add(defaultDynamicTextAdTargetWithRandomRules(defaultAdGroup));
        }

        ValidationResult<List<DynamicTextAdTarget>, Defect> actual = validate(dynamicTextAdTargetsToAdd);

        assertThat(actual).is(matchedBy(hasNoDefectsDefinitions()));
    }

    /**
     * https://a.yandex-team.ru/arc/trunk/arcadia/direct/perl/perl/settings/Settings.pm?rev=r7674288#L598
     */
    @Test
    public void validate_ConditionArgumentAllowedChars() {
        ValidationResult<List<DynamicTextAdTarget>, Defect> actual =
                validate(singletonList(
                        dynamicTextAdTarget.withAutobudgetPriority(null).withCondition(
                                singletonList(new WebpageRule()
                                        .withKind(WebpageRuleKind.EQUALS)
                                        .withType(WebpageRuleType.URL_PRODLIST)
                                        .withValue(singletonList("https://yandex.ru/?param1=value$(123#&page=1"))
                                )
                        )));
        assertThat(actual).is(matchedBy(hasNoDefectsDefinitions()));
    }
}
