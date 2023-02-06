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
import ru.yandex.direct.core.entity.dynamictextadtarget.service.DynamicTextAdTargetSuspendResumeService;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.DynamicTextAdTargetInfo;
import ru.yandex.direct.core.testing.repository.TestCampaignRepository;
import ru.yandex.direct.core.testing.steps.AdGroupSteps;
import ru.yandex.direct.core.testing.steps.DynamicTextAdTargetSteps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.rbac.RbacService;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignDefects.archivedCampaignModification;
import static ru.yandex.direct.core.entity.dynamictextadtarget.service.validation.DynamicTextAdTargetDefects.alreadySuspended;
import static ru.yandex.direct.core.entity.dynamictextadtarget.service.validation.DynamicTextAdTargetDefects.isNotSuspended;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasWarningWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CollectionDefects.duplicatedObject;
import static ru.yandex.direct.validation.defect.CommonDefects.objectNotFound;
import static ru.yandex.direct.validation.defect.CommonDefects.validId;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class SuspendResumeDynamicTextAdTargetValidationServiceNegativeTest {

    private SuspendResumeDynamicTextAdTargetValidationService validationService;

    @Autowired
    private ShardHelper shardHelper;

    @Autowired
    private RbacService rbacService;

    @Autowired
    private CampaignAccessCheckRepository campaignAccessCheckRepository;

    @Autowired
    private TestCampaignRepository testCampaignRepository;

    @Autowired
    private DynamicTextAdTargetRepository dynamicTextAdTargetRepository;

    @Autowired
    private DynamicTextAdTargetSuspendResumeService dynamicTextAdTargetSuspendResumeService;

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

    @Test
    public void validate_InvalidId() {
        ValidationResult<List<Long>, Defect> vr = validateSuspend(singletonList(-1L));

        assertThat(vr).is(matchedBy(hasDefectWithDefinition(validationError(path(index(0)), validId()))));
    }

    @Test
    public void validate_NotUnique() {
        Long dynamicConditionId = dynamicTextAdTargetInfo.getDynamicConditionId();

        ValidationResult<List<Long>, Defect> vr = validateSuspend(asList(dynamicConditionId, dynamicConditionId));

        assertThat(vr).is(matchedBy(hasWarningWithDefinition(validationError(path(index(0)), duplicatedObject()))));
        assertThat(vr).is(matchedBy(hasWarningWithDefinition(validationError(path(index(1)), duplicatedObject()))));
    }

    @Test
    public void validate_ArchivedCampaign() {
        Long campaignId = dynamicTextAdTargetInfo.getAdGroupInfo().getCampaignId();
        Long dynamicConditionId = dynamicTextAdTargetInfo.getDynamicConditionId();

        testCampaignRepository.archiveCampaign(shard, campaignId);

        ValidationResult<List<Long>, Defect> vr = validateSuspend(singletonList(dynamicConditionId));
        assertThat(vr).is(matchedBy(
                hasDefectWithDefinition(validationError(path(index(0)), archivedCampaignModification()))));
    }

    @Test
    public void validate_SuspendAlreadySuspended() {
        Long dynamicConditionId = dynamicTextAdTargetInfo.getDynamicConditionId();

        dynamicTextAdTargetSuspendResumeService
                .suspendDynamicTextAdTargets(clientId, operatorUid, singletonList(dynamicConditionId));

        ValidationResult<List<Long>, Defect> vr = validateSuspend(singletonList(dynamicConditionId));
        assertThat(vr).is(matchedBy(hasWarningWithDefinition(validationError(path(index(0)), alreadySuspended()))));
    }

    @Test
    public void validate_ResumeActiveDynamicTextAdTarget() {
        Long dynamicConditionId = dynamicTextAdTargetInfo.getDynamicConditionId();

        ValidationResult<List<Long>, Defect> vr = validateResume(singletonList(dynamicConditionId));
        assertThat(vr).is(matchedBy(hasWarningWithDefinition(validationError(path(index(0)), isNotSuspended()))));
    }

    @Test
    public void validate_DynamicTextAdTargetNotFound() {
        ValidationResult<List<Long>, Defect> vr = validateSuspend(singletonList(123L));

        assertThat(vr).is(matchedBy(hasDefectWithDefinition(validationError(path(index(0)), objectNotFound()))));
    }

    @Test
    public void validate_DynamicTextAdTargetAnotherClient() {
        AdGroupInfo anotherClientAdGroup = adGroupSteps.createActiveDynamicTextAdGroup();

        DynamicTextAdTargetInfo anotherClientDynamicTextAdTarget =
                dynamicTextAdTargetSteps.createDefaultDynamicTextAdTarget(anotherClientAdGroup);

        ValidationResult<List<Long>, Defect> vr =
                validateSuspend(singletonList(anotherClientDynamicTextAdTarget.getDynamicConditionId()));

        assertThat(vr).is(matchedBy(hasDefectWithDefinition(validationError(path(index(0)), objectNotFound()))));
    }

    private ValidationResult<List<Long>, Defect> validateSuspend(List<Long> ids) {
        return validationService.validate(shard, clientId, operatorUid, ids, false);
    }

    private ValidationResult<List<Long>, Defect> validateResume(List<Long> ids) {
        return validationService.validate(shard, clientId, operatorUid, ids, true);
    }
}
