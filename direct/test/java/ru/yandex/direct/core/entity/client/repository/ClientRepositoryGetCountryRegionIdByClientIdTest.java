package ru.yandex.direct.core.entity.client.repository;

import java.util.Optional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestClients.defaultClient;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ClientRepositoryGetCountryRegionIdByClientIdTest {
    private static final long EXPECTED_COUNTRY_REGION_ID = 999999;

    @Autowired
    private Steps steps;
    @Autowired
    private ClientRepository clientRepository;

    @Test
    public void test() {
        ClientInfo client = steps.clientSteps()
                .createClient(
                        defaultClient().withCountryRegionId(EXPECTED_COUNTRY_REGION_ID));

        Optional<Long> actualCountryRegionId = clientRepository.getCountryRegionIdByClientId(
                client.getShard(), client.getClientId());

        assertThat(actualCountryRegionId).isEqualTo(Optional.of(EXPECTED_COUNTRY_REGION_ID));
    }
}
