package ru.yandex.direct.core.entity.dynamictextadtarget.service.validation;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.campaign.container.AffectedCampaignIdsContainer;
import ru.yandex.direct.core.entity.campaign.repository.CampaignAccessCheckRepository;
import ru.yandex.direct.core.entity.campaign.service.accesschecker.CampaignSubObjectAccessCheckerFactory;
import ru.yandex.direct.core.entity.campaign.service.accesschecker.RequestCampaignAccessibilityCheckerProvider;
import ru.yandex.direct.core.entity.dynamictextadtarget.repository.DynamicTextAdTargetRepository;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.DynamicTextAdTargetInfo;
import ru.yandex.direct.core.testing.steps.AdGroupSteps;
import ru.yandex.direct.core.testing.steps.DynamicTextAdTargetSteps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.rbac.RbacService;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class SuspendResumeDynamicTextAdTargetValidationServicePositiveTest {

    private SuspendResumeDynamicTextAdTargetValidationService validationService;

    @Autowired
    private ShardHelper shardHelper;

    @Autowired
    private RbacService rbacService;

    @Autowired
    private CampaignAccessCheckRepository campaignAccessCheckRepository;

    @Autowired
    private DynamicTextAdTargetRepository dynamicTextAdTargetRepository;

    @Autowired
    private AdGroupSteps adGroupSteps;

    @Autowired
    private RequestCampaignAccessibilityCheckerProvider requestAccessibleCampaignTypes;

    @Autowired
    private DynamicTextAdTargetSteps dynamicTextAdTargetSteps;

    @Autowired
    private FeatureService featureService;

    private long operatorUid;
    private ClientId clientId;
    private int shard;
    private DynamicTextAdTargetInfo dynamicTextAdTargetInfo;

    @Before
    public void before() {
        AdGroupInfo defaultAdGroup = adGroupSteps.createActiveDynamicTextAdGroup();

        operatorUid = defaultAdGroup.getUid();
        clientId = defaultAdGroup.getClientId();
        dynamicTextAdTargetInfo = dynamicTextAdTargetSteps.createDefaultDynamicTextAdTarget(defaultAdGroup);
        shard = defaultAdGroup.getShard();

        CampaignSubObjectAccessCheckerFactory campaignSubObjectAccessCheckerFactory = new CampaignSubObjectAccessCheckerFactory(
                shardHelper, spy(rbacService), campaignAccessCheckRepository, new AffectedCampaignIdsContainer(),
                requestAccessibleCampaignTypes, featureService);

        validationService =
                new SuspendResumeDynamicTextAdTargetValidationService(campaignSubObjectAccessCheckerFactory,
                        dynamicTextAdTargetRepository);
    }

    private ValidationResult<List<Long>, Defect> validate(List<Long> ids, boolean isResumeOperation) {
        return validationService.validate(shard, clientId, operatorUid, ids, isResumeOperation);
    }

    @Test
    public void positiveValidationResultWhenNoErrors() {
        Long dynamicConditionId = dynamicTextAdTargetInfo.getDynamicConditionId();
        ValidationResult<List<Long>, Defect> actual =
                validate(singletonList(dynamicConditionId), false);

        assertThat(actual).is(matchedBy(hasNoDefectsDefinitions()));
    }
}
