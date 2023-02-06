package ru.yandex.direct.core.entity.client.service.checker;

import java.util.Collections;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.creative.model.Creative;
import ru.yandex.direct.core.entity.creative.repository.CreativeRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.sharding.ShardHelper;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;


@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ClientAccessCheckerCreativeTypeSupportTest {

    private ClientInfo clientInfo1;
    private ClientInfo clientInfo2;
    private Creative creative1;
    private Creative creative2;
    private Creative anotherClientCreative;

    @Autowired
    private Steps steps;

    @Autowired
    private CreativeRepository creativeRepository;

    @Autowired
    private ShardHelper shardHelper;

    private ClientAccessCheckerCreativeTypeSupport clientAccessCheckerCreativeTypeSupport;

    @Before
    public void before() {
        clientInfo1 = steps.clientSteps().createDefaultClient();
        clientInfo2 = steps.clientSteps().createDefaultClient();
        creative1 = steps.creativeSteps().createCreative(clientInfo1).getCreative();
        creative2 = steps.creativeSteps().createCreative(clientInfo1).getCreative();
        anotherClientCreative = steps.creativeSteps().createCreative(clientInfo2).getCreative();
        clientAccessCheckerCreativeTypeSupport = new ClientAccessCheckerCreativeTypeSupport(creativeRepository,
                shardHelper);
    }

    @Test
    public void oneCreativeFromThisClientTest() {
        Set<Long> result =
                clientAccessCheckerCreativeTypeSupport.getAccessibleObjectIds(Collections.singletonList(creative1.getId()),
                        clientInfo1.getClientId());
        assertThat(result).isEqualTo(Set.of(creative1.getId()));
    }

    @Test
    public void oneCreativeFromAnotherClientTest() {
        Set<Long> result =
                clientAccessCheckerCreativeTypeSupport.getAccessibleObjectIds(Collections.singletonList(creative1.getId()),
                        clientInfo2.getClientId());
        assertThat(result).isEqualTo(Set.of());
    }

    @Test
    public void twoCreativesFromThisClientTest() {
        Set<Long> result = clientAccessCheckerCreativeTypeSupport.getAccessibleObjectIds(
                ImmutableList.of(creative1.getId(), creative2.getId()), clientInfo1.getClientId());
        assertThat(result).isEqualTo(Set.of(creative1.getId(), creative2.getId()));
    }

    @Test
    public void twoCreativesFromBothClientsTest() {
        Set<Long> result = clientAccessCheckerCreativeTypeSupport.getAccessibleObjectIds(
                ImmutableList.of(creative1.getId(), anotherClientCreative.getId()), clientInfo1.getClientId());
        assertThat(result).isEqualTo(Set.of(creative1.getId()));
    }
}
