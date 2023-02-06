package ru.yandex.market.logistics.management.client;

import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import ru.yandex.market.logistics.management.entity.response.ListWrapper;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;

import static org.springframework.http.HttpStatus.OK;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

@DisplayName("Отключить дропофф в графе")
public class LmsClientDisableDropoffTest extends AbstractClientTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Проверка отключения дропофа в графе")
    void disableDropoff() throws JsonProcessingException {
        long dropoffId = 1L;
        PartnerResponse partnerResponse = PartnerResponse.newBuilder().name("test").build();
        mockServer.expect(requestTo(uri + "/externalApi/logisticsPoints/disable-dropoff/" + dropoffId))
            .andExpect(method(HttpMethod.PUT))
            .andRespond(
                withStatus(OK)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(objectMapper.writeValueAsString(ListWrapper.wrap(List.of(partnerResponse))))
            );

        List<PartnerResponse> result = client.disableDropoff(dropoffId);
        softly.assertThat(result).containsExactlyInAnyOrder(partnerResponse);
    }
}
