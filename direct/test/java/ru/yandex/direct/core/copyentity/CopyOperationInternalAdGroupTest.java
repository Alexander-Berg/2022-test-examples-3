package ru.yandex.direct.core.copyentity;

import java.util.List;
import java.util.Set;

import one.util.streamex.StreamEx;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.service.AdGroupService;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.AdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.service.AdGroupAdditionalTargetingService;
import ru.yandex.direct.core.entity.retargeting.model.ConditionType;
import ru.yandex.direct.core.entity.retargeting.model.RetargetingCondition;
import ru.yandex.direct.core.entity.retargeting.service.RetargetingConditionService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.RetConditionInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.result.MassResult;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestAdGroupAdditionalTargetings.allValidInternalAdAdditionalTargetings;
import static ru.yandex.direct.core.testing.data.TestRetargetingConditions.defaultRetCondition;
import static ru.yandex.direct.core.testing.data.TestRetargetings.defaultRetargeting;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CopyOperationInternalAdGroupTest {

    @Autowired
    private AdGroupService adgroupService;

    @Autowired
    private RetargetingConditionService retargetingConditionService;

    @Autowired
    private AdGroupAdditionalTargetingService additionalTargetingService;

    @Autowired
    private Steps steps;

    private Long uid;

    private ClientInfo clientInfoFrom;
    private ClientId clientIdFrom;

    private Long campaignIdFrom;
    private Long adGroupIdFrom;

    private List<AdGroupAdditionalTargeting> additionalTargetings;

    @Autowired
    private CopyOperationFactory factory;

    @Before
    public void setUp() {
        var superInfo = steps.clientSteps().createDefaultClientWithRole(RbacRole.SUPER);
        uid = superInfo.getUid();

        prepareData_SameClientSameCampaign();
    }

    @Test
    public void copyAdGroup() {
        CopyConfig copyConfig = CopyEntityTestUtils.adGroupCopyConfig(
                clientInfoFrom, adGroupIdFrom, campaignIdFrom, uid);
        var xerox = factory.build(copyConfig);
        var copyResult = xerox.copy();

        checkErrors(copyResult.getMassResult());

        @SuppressWarnings("unchecked")
        Set<Long> copiedAdGroupIds = StreamEx.of(copyResult.getEntityMapping(AdGroup.class).values())
                .select(Long.class)
                .toSet();
        var copiedAdGroups = adgroupService.get(clientIdFrom, uid, copiedAdGroupIds);
        assertThat(copiedAdGroups).hasSize(1);

        var copiedRetargetingConditions = retargetingConditionService
                .getRetargetingConditionsByAdGroupIds(clientIdFrom, copiedAdGroupIds);
        assertThat(copiedRetargetingConditions).hasSize(1);

        var copiedTargetings = additionalTargetingService.getTargetingsByAdGroupIds(clientIdFrom, copiedAdGroupIds);
        assertThat(copiedTargetings).hasSize(additionalTargetings.size());
    }

    private void checkErrors(MassResult massResult) {
        assertThat(massResult.getValidationResult().flattenErrors()).isEmpty();
    }

    private void prepareData_SameClientSameCampaign() {
        clientInfoFrom = steps.internalAdProductSteps().createDefaultInternalAdProduct();

        clientIdFrom = clientInfoFrom.getClientId();

        var campaignInfo = steps.campaignSteps().createActiveInternalFreeCampaign(clientInfoFrom);
        campaignIdFrom = campaignInfo.getCampaignId();

        var adGroupInfo = steps.adGroupSteps().createActiveInternalAdGroup(campaignInfo);
        adGroupIdFrom = adGroupInfo.getAdGroupId();
        RetargetingCondition retargetingCondition = defaultRetCondition(clientIdFrom);
        retargetingCondition.setType(ConditionType.interests);
        steps.retargetingSteps().createRetargeting(defaultRetargeting(campaignIdFrom, adGroupIdFrom,
                retargetingCondition.getId()), adGroupInfo,
                new RetConditionInfo().withClientInfo(clientInfoFrom).withRetCondition(retargetingCondition));
        additionalTargetings = allValidInternalAdAdditionalTargetings();
        steps.adGroupAdditionalTargetingSteps().addValidTargetingsToAdGroup(adGroupInfo, additionalTargetings);
    }
}
