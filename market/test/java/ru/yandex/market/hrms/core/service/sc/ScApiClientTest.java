package ru.yandex.market.hrms.core.service.sc;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.hrms.core.AbstractCoreTest;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;


public class ScApiClientTest extends AbstractCoreTest {

    @Autowired
    private ScApiConfigurer scApiConfigurer;

    @Autowired
    private ScApiClient scApiClient;

    @Test
    public void shouldCrateNewUser() {
        scApiConfigurer.mockCreateOrUpdate("""
                {
                    "id": 123
                }
                """);

        var request = new ScUserRequest(
                1L,
                "Гаджимурад Магомедов",
                "gjmrs@hrms.sc-ru",
                "STOCKMAN",
                "gjmrd");

        var response = scApiClient.createOrUpdateUser(request);

        assertThat(response.getDispatchPersonId(), is(123L));
    }
}
