package ru.yandex.direct.core.entity.retargeting.service;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.retargeting.model.TargetInterest;
import ru.yandex.direct.core.entity.retargeting.model.TargetingCategory;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.RetConditionInfo;
import ru.yandex.direct.core.testing.repository.TestTargetingCategoriesRepository;
import ru.yandex.direct.core.testing.steps.AdGroupSteps;
import ru.yandex.direct.core.testing.steps.RetConditionSteps;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.rbac.RbacService;
import ru.yandex.direct.result.MassResult;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.retargeting.service.validation2.RetargetingDefects.retargetingConditionIsInvalidForRetargeting;
import static ru.yandex.direct.core.testing.data.TestRetargetings.defaultTargetInterest;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessful;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AddRetargetingsOperationIndoorGroupTest {
    private static final TargetingCategory TARGETING_CATEGORY =
            new TargetingCategory(54L, null, "", "", BigInteger.valueOf(10000L), true);

    @Autowired
    public AdGroupSteps adGroupSteps;

    @Autowired
    public RetConditionSteps retConditionSteps;

    @Autowired
    private RetargetingService serviceUnderTest;


    @Autowired
    private Steps steps;

    @Autowired
    private RbacService rbacService;

    @Autowired
    private TestTargetingCategoriesRepository testTargetingCategoriesRepository;

    private AdGroupInfo adGroupInfo;
    private RetConditionInfo retConditionInfoIndoor;
    private RetConditionInfo retConditionInfoCpm;
    private long operatorUid;
    private ClientId clientId;
    private long clientUid;

    @Before
    public void before() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        CampaignInfo cpmBannerCampaign = steps.campaignSteps().createActiveCpmBannerCampaign(clientInfo);
        adGroupInfo = adGroupSteps.createActiveCpmIndoorAdGroup(cpmBannerCampaign);
        retConditionInfoIndoor = retConditionSteps.createIndoorRetCondition(adGroupInfo.getClientInfo());
        retConditionInfoCpm = retConditionSteps.createCpmRetCondition(adGroupInfo.getClientInfo());
        operatorUid = adGroupInfo.getUid();
        clientId = adGroupInfo.getClientId();
        clientUid = rbacService.getChiefByClientId(clientId);
        testTargetingCategoriesRepository.addTargetingCategory(TARGETING_CATEGORY);
    }

    @Test
    public void addRetargeting_OneItemIsValid_ReturnsFullySuccessfulResult() {
        addPartiallyAndCheckResult(singletonList(targetInterestIndoor()), true);
    }

    @Test
    public void addRetargeting_BindBadRetargeting_NotAllowed() {
        MassResult<Long> result = addPartially(singletonList(targetInterestCpm()));
        assertThat(result, isSuccessful(false));
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(path(index(0), field("retargetingConditionId")),
                        retargetingConditionIsInvalidForRetargeting())));
    }

    private void addPartiallyAndCheckResult(List<TargetInterest> retargetings, Boolean... expectedResults) {
        MassResult<Long> result = addPartially(retargetings);
        assertThat(result, isSuccessful(expectedResults));
    }

    private MassResult<Long> addPartially(List<TargetInterest> targetInterests) {
        return serviceUnderTest
                .createAddOperation(Applicability.PARTIAL, targetInterests, operatorUid, clientId, clientUid)
                .prepareAndApply();
    }

    private TargetInterest targetInterestIndoor() {
        return defaultTargetInterest(adGroupInfo.getCampaignId(),
                adGroupInfo.getAdGroupId(), retConditionInfoIndoor.getRetConditionId());
    }

    private TargetInterest targetInterestCpm() {
        return defaultTargetInterest(adGroupInfo.getCampaignId(),
                adGroupInfo.getAdGroupId(), retConditionInfoCpm.getRetConditionId())
                .withPriceContext(BigDecimal.valueOf(47.3))
                .withAutobudgetPriority(1)
                .withIsSuspended(true);
    }
}
