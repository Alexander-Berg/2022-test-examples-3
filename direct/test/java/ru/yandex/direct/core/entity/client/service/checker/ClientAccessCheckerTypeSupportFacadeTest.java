package ru.yandex.direct.core.entity.client.service.checker;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.addition.callout.model.Callout;
import ru.yandex.direct.core.entity.addition.callout.repository.CalloutRepository;
import ru.yandex.direct.core.entity.creative.model.Creative;
import ru.yandex.direct.core.entity.creative.repository.CreativeRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.model.ModelWithId;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ClientAccessCheckerTypeSupportFacadeTest {
    @Autowired
    private Steps steps;

    @Autowired
    private CreativeRepository creativeRepository;

    @Autowired
    private CalloutRepository calloutRepository;

    @Autowired
    private ShardHelper shardHelper;

    private ClientAccessCheckerTypeSupportFacade clientAccessCheckerCreativeTypeSupportFacade;
    private ClientAccessCheckerCreativeTypeSupport clientAccessCheckerCreativeTypeSupport;
    private ClientAccessCheckerAdExtensionTypeSupport clientAccessCheckerAdExtensionTypeSupport;
    private Callout adExtension1;
    private Callout anotherClientAdExtension;
    private Creative creative1;
    private Creative creative2;
    private Creative anotherClientCreative;
    private ClientInfo clientInfo1;
    private ClientInfo clientInfo2;

    @Before
    public void before() {
        clientAccessCheckerCreativeTypeSupport = new ClientAccessCheckerCreativeTypeSupport(creativeRepository,
                shardHelper);
        clientAccessCheckerAdExtensionTypeSupport = new ClientAccessCheckerAdExtensionTypeSupport(calloutRepository,
                shardHelper);
        clientInfo1 = steps.clientSteps().createDefaultClient();
        clientInfo2 = steps.clientSteps().createDefaultClient();
        adExtension1 = steps.calloutSteps().createDefaultCallout(clientInfo1);
        anotherClientAdExtension = steps.calloutSteps().createDefaultCallout(clientInfo2);
        creative1 = steps.creativeSteps().createCreative(clientInfo1).getCreative();
        creative2 = steps.creativeSteps().createCreative(clientInfo1).getCreative();
        anotherClientCreative = steps.creativeSteps().createCreative(clientInfo2).getCreative();
        clientAccessCheckerCreativeTypeSupportFacade =
                new ClientAccessCheckerTypeSupportFacade(ImmutableList.of(clientAccessCheckerAdExtensionTypeSupport,
                        clientAccessCheckerCreativeTypeSupport));
    }

    @Test
    public void oneCreativeOneCalloutFromThisClientTest() {
        Map<Class<? extends ModelWithId>, Collection<Long>> objects = Map.of(
                Creative.class, Set.of(creative1.getId()),
                Callout.class, Set.of(adExtension1.getId()));
        var result = clientAccessCheckerCreativeTypeSupportFacade.sendToCheck(objects, clientInfo1.getClientId());
        assertThat(result.get(Creative.class)).isEqualTo(Set.of(creative1.getId()));
        assertThat(result.get(Callout.class)).isEqualTo(Set.of(adExtension1.getId()));
    }

    @Test
    public void oneCreativeOneCalloutFromBothClientsTest() {
        Map<Class<? extends ModelWithId>, Collection<Long>> objects = Map.of(
                Creative.class, Set.of(creative1.getId()),
                Callout.class, Set.of(anotherClientAdExtension.getId()));
        var result = clientAccessCheckerCreativeTypeSupportFacade.sendToCheck(objects, clientInfo1.getClientId());
        assertThat(result.get(Creative.class)).isEqualTo(Set.of(creative1.getId()));
        assertThat(result.get(Callout.class)).isEqualTo(Set.of());
    }

    @Test
    public void threeCreativesFromBothClientsTest() {
        Map<Class<? extends ModelWithId>, Collection<Long>> objects = Map.of(
                Creative.class, Set.of(creative1.getId(), creative2.getId(), anotherClientCreative.getId()));
        var result = clientAccessCheckerCreativeTypeSupportFacade.sendToCheck(objects, clientInfo1.getClientId());
        assertThat(result.get(Creative.class)).isEqualTo(Set.of(creative1.getId(), creative2.getId()));
    }
}
