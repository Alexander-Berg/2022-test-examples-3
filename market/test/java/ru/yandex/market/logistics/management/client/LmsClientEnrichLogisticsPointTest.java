package ru.yandex.market.logistics.management.client;

import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;

import ru.yandex.market.logistics.management.entity.request.point.LogisticsPointEnrichAddressRequest;

import static org.springframework.http.HttpStatus.OK;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

@DisplayName("Обогатить точки координатами")
public class LmsClientEnrichLogisticsPointTest extends AbstractClientTest {
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Обогатить точки координатами")
    void enrichPointsCoordinates() throws JsonProcessingException {
        mockServer.expect(requestTo(uri + "/externalApi/logisticsPoints/enrich-address"))
            .andExpect(method(HttpMethod.PUT))
            .andExpect(content()
                .string(objectMapper.writeValueAsString(new LogisticsPointEnrichAddressRequest(List.of(1L))))
            )
            .andRespond(withStatus(OK));

        client.enrichLogisticsPointAddress(Set.of(1L));
    }
}
