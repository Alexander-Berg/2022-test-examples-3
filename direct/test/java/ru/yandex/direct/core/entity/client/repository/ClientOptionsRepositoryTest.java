package ru.yandex.direct.core.entity.client.repository;


import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.client.model.ClientFlags;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ClientOptionsRepositoryTest {
    private ClientOptionsRepository clientOptionsRepository;

    @Autowired
    private Steps steps;

    @Autowired
    private DslContextProvider dslContextProvider;

    private ClientInfo clientInfo;
    private int shard;
    private ClientId clientId;
    private ClientFlags flag = ClientFlags.AS_SOON_AS_POSSIBLE;
    private ClientFlags anotherFlag = ClientFlags.CAN_PAY_BEFORE_MODERATION;

    @Before
    public void before() {
        clientOptionsRepository = new ClientOptionsRepository(dslContextProvider);
        clientInfo = steps.clientSteps().createDefaultClient();
        clientId = clientInfo.getClientId();
        shard = clientInfo.getShard();
        steps.clientOptionsSteps().addEmptyClientOptions(shard, clientId);
    }

    @Test
    public void setClientFlag_null_flags() {
        clientOptionsRepository.setClientFlag(shard, clientId, flag);
        Set<String> clientFlags =
                clientOptionsRepository.getClientsOptions(shard, singletonList(clientId)).get(0).getClientFlags();
        Set<String> expectedFlags = Set.of(flag.getTypedValue());

        assertThat(clientFlags).isEqualTo(expectedFlags);

    }

    @Test
    public void setClientFlag_empty_flags() {
        steps.clientOptionsSteps().setClientFlags(shard, clientId, "");

        clientOptionsRepository.setClientFlag(shard, clientId, flag);
        Set<String> clientFlags =
                clientOptionsRepository.getClientsOptions(shard, singletonList(clientId)).get(0).getClientFlags();
        Set<String> expectedFlags = Set.of(flag.getTypedValue());

        assertThat(clientFlags).isEqualTo(expectedFlags);
    }

    @Test
    public void setClientFlag_has_flags() {
        steps.clientOptionsSteps().setClientFlags(shard, clientId, anotherFlag.getTypedValue());

        clientOptionsRepository.setClientFlag(shard, clientId, flag);
        Set<String> clientFlags =
                clientOptionsRepository.getClientsOptions(shard, singletonList(clientId)).get(0).getClientFlags();
        Set<String> expectedFlags = Set.of(flag.getTypedValue(), anotherFlag.getTypedValue());

        assertThat(clientFlags).isEqualTo(expectedFlags);
    }
}
