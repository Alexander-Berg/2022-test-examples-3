package ru.yandex.market.logistics.management.client;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

@DisplayName("Замены логистических точек")
class LmsClientGetLogisticsPointChangesTest extends AbstractClientTest {
    @DisplayName("Получение замен логистических точек")
    @Test
    void getLogisticsPointRegistry() {
        mockServer.expect(requestTo(uri + "/externalApi/logisticsPoints/changes"))
            .andExpect(method(HttpMethod.PUT))
            .andRespond(
                withStatus(OK)
                    .contentType(APPLICATION_JSON)
                    .body("{\"1\": 3, \"2\": 3}")
            );

        softly.assertThat(client.getLogisticsPointChanges(Set.of(1L, 2L)))
            .containsAllEntriesOf(Map.of(1L, 3L, 2L, 3L));
    }
}
