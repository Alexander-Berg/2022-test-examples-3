package ru.yandex.direct.core.entity.client.service;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang.math.RandomUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ClientServiceClientsOptionsTest {
    private final static List<Long> COUNTERS = List.of((long) RandomUtils.nextInt());

    @Autowired
    private ClientService clientService;
    @Autowired
    private Steps steps;

    private ClientInfo clientInfo;

    @Before
    public void initData() {
        clientInfo = steps.clientSteps().createDefaultClient();
    }

    @Test
    public void clientsCommonMetrikaCounters_EmptyList() {
        steps.clientSteps().addCommonMetrikaCounters(clientInfo, null);
        Map<Long, List<Long>> result = clientService.getClientsCommonMetrikaCounters(List.of(clientInfo.getClientId()));
        assertThat(result).isEmpty();
    }

    @Test
    public void clientsCommonMetrikaCounters_NotEmptyList() {
        steps.clientSteps().addCommonMetrikaCounters(clientInfo, COUNTERS);
        Map<Long, List<Long>> result = clientService.getClientsCommonMetrikaCounters(List.of(clientInfo.getClientId()));
        assertThat(result)
                .containsOnly(entry(clientInfo.getClientId().asLong(), COUNTERS));
    }

}
