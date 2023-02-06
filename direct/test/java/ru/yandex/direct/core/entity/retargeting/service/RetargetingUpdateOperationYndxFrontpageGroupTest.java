package ru.yandex.direct.core.entity.retargeting.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.retargeting.model.Retargeting;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.RetargetingInfo;
import ru.yandex.direct.core.testing.repository.TestCpmYndxFrontpageRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.rbac.RbacService;
import ru.yandex.direct.result.MassResult;

import static java.util.Collections.singletonList;
import static ru.yandex.direct.core.entity.currency.model.cpmyndxfrontpage.FrontpageCampaignShowType.FRONTPAGE;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isFullySuccessful;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class RetargetingUpdateOperationYndxFrontpageGroupTest {

    @Autowired
    private RbacService rbacService;
    @Autowired
    private RetargetingService retargetingService;
    @Autowired
    private TestCpmYndxFrontpageRepository testCpmYndxFrontpageRepository;
    @Autowired
    private Steps steps;

    private ClientInfo clientInfo;
    private RetargetingInfo defaultRetargetingInfo;

    @Before
    public void before() {
        testCpmYndxFrontpageRepository.fillMinBidsTestValues();
        clientInfo = steps.clientSteps().createDefaultClient();
        int shard = clientInfo.getShard();

        AdGroupInfo cpmYndxFrontpageAdGroup = steps.adGroupSteps().createActiveCpmYndxFrontpageAdGroup(clientInfo);
        testCpmYndxFrontpageRepository.setCpmYndxFrontpageCampaignsAllowedFrontpageTypes(
                shard, cpmYndxFrontpageAdGroup.getCampaignId(), Set.of(FRONTPAGE));
        defaultRetargetingInfo = steps.retargetingSteps().createDefaultRetargeting(cpmYndxFrontpageAdGroup);
    }

    @Test
    public void prepareAndApply_changePrice_success() {
        ModelChanges<Retargeting> mc = ModelChanges.build(defaultRetargetingInfo.getRetargetingId(),
                Retargeting.class, Retargeting.PRICE_CONTEXT, BigDecimal.TEN);

        MassResult<Long> result = createUpdateOperation(singletonList(mc), clientInfo).prepareAndApply();
        assumeThat(result, isFullySuccessful());
    }

    private RetargetingUpdateOperation createUpdateOperation(List<ModelChanges<Retargeting>> modelChanges,
                                                             ClientInfo clientInfo) {
        long clientUid = rbacService.getChiefByClientId(clientInfo.getClientId());
        return retargetingService.createUpdateOperation(Applicability.FULL, modelChanges,
                clientInfo.getUid(), clientInfo.getClientId(), clientUid);
    }
}
