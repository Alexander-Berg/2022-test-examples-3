package ru.yandex.direct.core.entity.client.service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.client.model.ClientBrand;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientBrandInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.ClientBrandSteps;
import ru.yandex.direct.core.testing.steps.ClientSteps;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ClientBrandsServiceTest {
    private ClientBrandInfo firstClientBrandInfo;
    private ClientBrandInfo secondClientBrandInfo;
    private LocalDateTime testDateTime;

    @Autowired
    private ClientSteps clientSteps;

    @Autowired
    private ClientBrandSteps clientBrandSteps;

    @Autowired
    private ClientBrandsService service;

    @Before
    public void setUp() {
        testDateTime = LocalDateTime.now().truncatedTo(SECONDS);
        firstClientBrandInfo = clientBrandSteps.createClientBrand(testDateTime.minusHours(1));
        secondClientBrandInfo = clientBrandSteps.createClientBrand(testDateTime.plusHours(1));
    }

    @Test
    public void testDeleteOlderThanDateTime() {
        service.deleteEntriesOlderThanDateTime(testDateTime);
        List<ClientBrand> brandList = service.getEntriesOlderThanDateTime(testDateTime.plusHours(2));

        assertSoftly(softly -> {
            softly.assertThat(brandList).as("список содержит новую запись")
                    .contains(secondClientBrandInfo.getClientBrand());
            softly.assertThat(brandList).as("список содержит не содержит старую запись")
                    .doesNotContain(firstClientBrandInfo.getClientBrand());
        });
    }

    @Test
    public void testReplaceClientBrands() {
        ClientInfo thirdClient = clientSteps.createDefaultClient();
        ClientBrand newBrand = new ClientBrand()
                .withClientId(firstClientBrandInfo.getClientId().asLong())
                .withBrandClientId(thirdClient.getClientId().asLong())
                .withLastSync(testDateTime.plusHours(1));

        service.replaceClientBrands(Collections.singletonList(newBrand));

        List<ClientBrand> brandList = service.getEntriesOlderThanDateTime(testDateTime.plusHours(2));
        assertSoftly(softly -> {
            softly.assertThat(brandList).as("список не содержит старую запись первого клиента")
                    .doesNotContain(firstClientBrandInfo.getClientBrand());
            softly.assertThat(brandList).as("список содержит новую запись первого клиента")
                    .contains(newBrand);
            softly.assertThat(brandList).as("список содержит старую запись второго клиента")
                    .contains(secondClientBrandInfo.getClientBrand());
        });
    }

    @After
    public void tearDown() {
        service.deleteEntriesByClientId(
                Arrays.asList(firstClientBrandInfo.getClientId(), secondClientBrandInfo.getClientId()));
    }
}
