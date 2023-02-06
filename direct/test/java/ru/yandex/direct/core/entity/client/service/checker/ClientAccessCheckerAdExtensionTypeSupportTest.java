package ru.yandex.direct.core.entity.client.service.checker;

import java.util.Collections;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.addition.callout.model.Callout;
import ru.yandex.direct.core.entity.addition.callout.repository.CalloutRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.sharding.ShardHelper;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ClientAccessCheckerAdExtensionTypeSupportTest {

    private ClientInfo clientInfo1;
    private ClientInfo clientInfo2;
    private Callout adExtension1;
    private Callout adExtension2;
    private Callout anotherClientAdExtension;

    @Autowired
    private Steps steps;

    @Autowired
    private CalloutRepository calloutRepository;

    @Autowired
    private ShardHelper shardHelper;

    private ClientAccessCheckerAdExtensionTypeSupport clientAccessCheckerAdExtensionTypeSupport;

    @Before
    public void before() {
        clientInfo1 = steps.clientSteps().createDefaultClient();
        clientInfo2 = steps.clientSteps().createDefaultClient();
        adExtension1 = steps.calloutSteps().createDefaultCallout(clientInfo1);
        adExtension2 = steps.calloutSteps().createDefaultCallout(clientInfo1);
        anotherClientAdExtension = steps.calloutSteps().createDefaultCallout(clientInfo2);
        clientAccessCheckerAdExtensionTypeSupport = new ClientAccessCheckerAdExtensionTypeSupport(calloutRepository,
                shardHelper);
    }

    @Test
    public void oneAdExtensionFromThisClientTest() {
        Set<Long> result =
                clientAccessCheckerAdExtensionTypeSupport.getAccessibleObjectIds(Collections.singletonList(adExtension1.getId()),
                        clientInfo1.getClientId());
        assertThat(result).isEqualTo(Set.of(adExtension1.getId()));
    }

    @Test
    public void oneAdExtensionFromAnotherClientTest() {
        Set<Long> result =
                clientAccessCheckerAdExtensionTypeSupport.getAccessibleObjectIds(Collections.singletonList(adExtension1.getId()),
                        clientInfo2.getClientId());
        assertThat(result).isEqualTo(Set.of());
    }

    @Test
    public void twoAdExtensionsFromThisClientTest() {
        Set<Long> result = clientAccessCheckerAdExtensionTypeSupport.getAccessibleObjectIds(
                ImmutableList.of(adExtension1.getId(), adExtension2.getId()), clientInfo1.getClientId());
        assertThat(result).isEqualTo(Set.of(adExtension1.getId(), adExtension2.getId()));

    }

    @Test
    public void twoAdExtensionsFromBothClientsTest() {
        Set<Long> result = clientAccessCheckerAdExtensionTypeSupport.getAccessibleObjectIds(
                ImmutableList.of(adExtension1.getId(), anotherClientAdExtension.getId()), clientInfo1.getClientId());
        assertThat(result).isEqualTo(Set.of(adExtension1.getId()));
    }
}
