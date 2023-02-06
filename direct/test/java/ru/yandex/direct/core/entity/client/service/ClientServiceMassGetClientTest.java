package ru.yandex.direct.core.entity.client.service;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.client.model.ClientWithUsers;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ClientServiceMassGetClientTest {
    @Autowired
    private ClientService clientService;
    @Autowired
    private Steps steps;

    @Test
    public void massGetClientWithUsers_Empty() {
        Collection<ClientWithUsers> result = clientService.massGetClientWithUsers(emptyList());
        assertThat("ответ должен быть пустой", result, empty());
    }

    @Test
    public void massGetClientWithUsers_DifferentShards() {
        ClientInfo clientInfo1 = steps.clientSteps().createClient(new ClientInfo().withShard(1));
        ClientInfo clientInfo2 = steps.clientSteps().createClient(new ClientInfo().withShard(2));

        List<Long> requestIds = asList(clientInfo1.getClientId().asLong(), clientInfo2.getClientId().asLong());
        Collection<ClientWithUsers> result = clientService.massGetClientWithUsers(requestIds);

        assumeThat("ответ должен содержать двух клиентов", result, hasSize(requestIds.size()));

        Collection<Long> clientIds = mapList(result, ClientWithUsers::getId);
        assertThat("в ответе должны содержаться запрашиваемые id", clientIds, containsInAnyOrder(requestIds.toArray()));
    }

    @Test
    public void massGetClientsByClientIds_DifferentShards() {
        ClientInfo clientInfo1 = steps.clientSteps().createClient(new ClientInfo().withShard(1));
        ClientInfo clientInfo2 = steps.clientSteps().createClient(new ClientInfo().withShard(2));
        ClientInfo clientInfo3 = steps.clientSteps().createClient(new ClientInfo().withShard(2));
        var request = List.of(clientInfo1.getClientId(), clientInfo2.getClientId(), clientInfo3.getClientId());

        var result = clientService.massGetClientsByClientIds(request);

        assumeThat("ответ должен содержать трёх клиентов", result.size(), equalTo(3));

        assertThat("в ключах должны содержаться запрашиваемые id", result.keySet(),
                containsInAnyOrder(request.toArray()));
        assertThat("в значениях должны содержаться запрашиваемые id",
                result.values().stream().map(c -> ClientId.fromLong(c.getId())).collect(Collectors.toList()),
                containsInAnyOrder(request.toArray()));
    }
}
