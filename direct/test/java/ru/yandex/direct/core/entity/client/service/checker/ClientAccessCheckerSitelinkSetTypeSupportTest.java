package ru.yandex.direct.core.entity.client.service.checker;

import java.util.Collections;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.sitelink.model.SitelinkSet;
import ru.yandex.direct.core.entity.sitelink.repository.SitelinkSetRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.sharding.ShardHelper;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ClientAccessCheckerSitelinkSetTypeSupportTest {

    private ClientInfo clientInfo1;
    private ClientInfo clientInfo2;
    private SitelinkSet sitelinkSet1;
    private SitelinkSet sitelinkSet2;
    private SitelinkSet anotherClientSitelinkSet;

    @Autowired
    private Steps steps;

    @Autowired
    private SitelinkSetRepository sitelinkSetRepository;

    @Autowired
    private ShardHelper shardHelper;

    private ClientAccessCheckerSitelinkSetTypeSupport clientAccessCheckerSitelinkSetTypeSupport;

    @Before
    public void before() {
        clientInfo1 = steps.clientSteps().createDefaultClient();
        clientInfo2 = steps.clientSteps().createDefaultClient();
        sitelinkSet1 = steps.sitelinkSetSteps().createDefaultSitelinkSet(clientInfo1).getSitelinkSet();
        sitelinkSet2 = steps.sitelinkSetSteps().createDefaultSitelinkSet(clientInfo1).getSitelinkSet();
        anotherClientSitelinkSet = steps.sitelinkSetSteps().createDefaultSitelinkSet(clientInfo2).getSitelinkSet();
        clientAccessCheckerSitelinkSetTypeSupport = new ClientAccessCheckerSitelinkSetTypeSupport(sitelinkSetRepository,
                shardHelper);
    }

    @Test
    public void oneSitelinkSetFromThisClientTest() {
        Set<Long> result =
                clientAccessCheckerSitelinkSetTypeSupport.getAccessibleObjectIds(Collections.singletonList(sitelinkSet1.getId()),
                        clientInfo1.getClientId());
        assertThat(result).isEqualTo(Set.of(sitelinkSet1.getId()));
    }

    @Test
    public void oneSitelinkSetFromAnotherClientTest() {
        Set<Long> result =
                clientAccessCheckerSitelinkSetTypeSupport.getAccessibleObjectIds(Collections.singletonList(sitelinkSet1.getId()),
                        clientInfo2.getClientId());
        assertThat(result).isEqualTo(Set.of());
    }

    @Test
    public void twoSitelinkSetsFromThisClientTest() {
        Set<Long> result = clientAccessCheckerSitelinkSetTypeSupport.getAccessibleObjectIds(
                ImmutableList.of(sitelinkSet1.getId(), sitelinkSet2.getId()), clientInfo1.getClientId());
        assertThat(result).isEqualTo(Set.of(sitelinkSet1.getId(), sitelinkSet2.getId()));
    }

    @Test
    public void twoSitelinkSetsFromBothClientsTest() {
        Set<Long> result = clientAccessCheckerSitelinkSetTypeSupport.getAccessibleObjectIds(
                ImmutableList.of(sitelinkSet1.getId(), anotherClientSitelinkSet.getId()), clientInfo1.getClientId());
        assertThat(result).isEqualTo(Set.of(sitelinkSet1.getId()));
    }
}
