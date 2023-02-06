package ru.yandex.direct.core.entity.client.service.checker;

import java.util.Collections;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.retargeting.model.RetargetingCondition;
import ru.yandex.direct.core.entity.retargeting.repository.RetargetingConditionRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.sharding.ShardHelper;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ClientAccessCheckerRetargetingListTypeSupportTest {

    private ClientInfo clientInfo1;
    private ClientInfo clientInfo2;
    private RetargetingCondition retargetingCondition1;
    private RetargetingCondition retargetingCondition2;
    private RetargetingCondition anotherClientRetargetingCondition;

    @Autowired
    private Steps steps;

    @Autowired
    private RetargetingConditionRepository retargetingConditionRepository;

    @Autowired
    private ShardHelper shardHelper;

    private ClientAccessCheckerRetargetingListTypeSupport clientAccessCheckerRetargetingListTypeSupport;

    @Before
    public void before() {
        clientInfo1 = steps.clientSteps().createDefaultClient();
        clientInfo2 = steps.clientSteps().createDefaultClient();
        retargetingCondition1 = steps.retConditionSteps().createDefaultRetCondition(clientInfo1).getRetCondition();
        retargetingCondition2 = steps.retConditionSteps().createDefaultRetCondition(clientInfo1).getRetCondition();
        anotherClientRetargetingCondition =
                steps.retConditionSteps().createDefaultRetCondition(clientInfo2).getRetCondition();
        clientAccessCheckerRetargetingListTypeSupport =
                new ClientAccessCheckerRetargetingListTypeSupport(retargetingConditionRepository, shardHelper);
    }

    @Test
    public void oneRetargetingConditionFromThisClientTest() {
        Set<Long> result =
                clientAccessCheckerRetargetingListTypeSupport.getAccessibleObjectIds(
                        Collections.singletonList(retargetingCondition1.getId()), clientInfo1.getClientId());
        assertThat(result).isEqualTo(Set.of(retargetingCondition1.getId()));
    }

    @Test
    public void oneRetargetingConditionFromAnotherClientTest() {
        Set<Long> result =
                clientAccessCheckerRetargetingListTypeSupport.getAccessibleObjectIds(
                        Collections.singletonList(retargetingCondition1.getId()), clientInfo2.getClientId());
        assertThat(result).isEqualTo(Set.of());
    }

    @Test
    public void twoRetargetingConditionsFromThisClientTest() {
        Set<Long> result = clientAccessCheckerRetargetingListTypeSupport.getAccessibleObjectIds(
                ImmutableList.of(retargetingCondition1.getId(), retargetingCondition2.getId()),
                clientInfo1.getClientId());
        assertThat(result).isEqualTo(Set.of(retargetingCondition1.getId(), retargetingCondition2.getId()));
    }

    @Test
    public void twoRetargetingConditionsFromBothClientsTest() {
        Set<Long> result = clientAccessCheckerRetargetingListTypeSupport.getAccessibleObjectIds(
                ImmutableList.of(retargetingCondition1.getId(), anotherClientRetargetingCondition.getId()),
                clientInfo1.getClientId());
        assertThat(result).isEqualTo(Set.of(retargetingCondition1.getId()));
    }

    @Test
    public void twoRetargetingConditionsFromThisClientOneIsBrandSafety() {
        RetargetingCondition brandSafetyCondition = steps.retConditionSteps()
                .createDefaultBrandSafetyRetCondition(clientInfo1).getRetCondition();
        Set<Long> result = clientAccessCheckerRetargetingListTypeSupport.getAccessibleObjectIds(
                ImmutableList.of(retargetingCondition1.getId(), brandSafetyCondition.getId()),
                clientInfo1.getClientId());
        assertThat(result).isEqualTo(Set.of(retargetingCondition1.getId()));
    }
}
