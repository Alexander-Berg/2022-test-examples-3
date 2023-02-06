package ru.yandex.direct.core.entity.client.repository;

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
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ClientBrandsRepositoryTest {
    private Integer shard;
    private ClientBrandInfo firstClientBrandInfo;
    private ClientBrandInfo secondClientBrandInfo;
    private LocalDateTime testDateTime;

    @Autowired
    private ClientSteps clientSteps;

    @Autowired
    private ClientBrandSteps clientBrandSteps;

    @Autowired
    private ClientBrandsRepository repository;

    @Before
    public void setUp() {
        testDateTime = LocalDateTime.now().truncatedTo(SECONDS);
        firstClientBrandInfo = clientBrandSteps.createClientBrand(testDateTime.minusHours(1));
        secondClientBrandInfo = clientBrandSteps.createClientBrand(testDateTime.plusHours(1));

        // Сейчас все клиенты создаются в одном шарде, мы полагаемся на это поведение
        assumeThat("оба клиента в одном шарде", firstClientBrandInfo.getShard(),
                equalTo(secondClientBrandInfo.getShard()));
        shard = firstClientBrandInfo.getShard();
    }

    @Test
    public void testSelectOlderThanDateTime() {
        List<ClientBrand> brandList = repository.getEntriesOlderThanDateTime(shard, testDateTime);
        assertSoftly(softly -> {
            softly.assertThat(brandList).as("список содержит старую запись")
                    .contains(firstClientBrandInfo.getClientBrand());
            softly.assertThat(brandList).as("список не содержит новую запись")
                    .doesNotContain(secondClientBrandInfo.getClientBrand());
        });
    }

    @Test
    public void testReplaceClientBrands() {
        ClientInfo thirdClient = clientSteps.createDefaultClient();
        ClientBrand newBrand = new ClientBrand().withClientId(firstClientBrandInfo.getClientId().asLong())
                .withBrandClientId(thirdClient.getClientId().asLong())
                .withLastSync(testDateTime.plusHours(1));

        repository.replaceClientBrands(shard, Collections.singletonList(newBrand));

        List<ClientBrand> brandList = repository.getEntriesOlderThanDateTime(shard, testDateTime.plusHours(2));
        assertSoftly(softly -> {
            softly.assertThat(brandList).as("список содержит новую запись первого клиента")
                    .contains(newBrand);
            softly.assertThat(brandList).as("список не содержит старую запись первого клиента")
                    .doesNotContain(firstClientBrandInfo.getClientBrand());
            softly.assertThat(brandList).as("список содержит старую запись второго клиента")
                    .contains(secondClientBrandInfo.getClientBrand());
        });
    }

    @After
    public void tearDown() {
        repository.deleteEntriesByClientId(shard,
                Arrays.asList(firstClientBrandInfo.getClientId(), secondClientBrandInfo.getClientId()));
    }
}
