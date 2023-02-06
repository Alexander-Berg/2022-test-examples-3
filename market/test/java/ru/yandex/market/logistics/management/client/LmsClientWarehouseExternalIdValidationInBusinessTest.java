package ru.yandex.market.logistics.management.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import ru.yandex.market.logistics.management.entity.request.businessWarehouse.BusinessWarehouseValidationRequest;
import ru.yandex.market.logistics.management.entity.response.businessWarehouse.BusinessWarehouseValidationStatus;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static ru.yandex.market.logistics.management.client.util.TestUtil.jsonResource;

@DisplayName("Проверка уникальности externalId")
class LmsClientWarehouseExternalIdValidationInBusinessTest extends AbstractClientTest {
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @SneakyThrows
    @DisplayName("Запрос на уникальность externalId в разрезе бизнеса")
    void validateExternalId() {
        mockServer.expect(requestTo(uri + "/externalApi/business-warehouse/validate"))
            .andExpect(method(HttpMethod.PUT))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(content().json(
                jsonResource("data/controller/businessWarehouse/validate_external_id_request.json"))
            )
            .andRespond(withStatus(HttpStatus.OK)
                .contentType(MediaType.APPLICATION_JSON)
                .body(objectMapper.writeValueAsString(BusinessWarehouseValidationStatus.OK)));

        BusinessWarehouseValidationRequest validationRequest = BusinessWarehouseValidationRequest.builder()
            .externalId("ext-id")
            .partnerId(1L)
            .businessId(123L)
            .build();

        client.validateExternalIdInBusiness(validationRequest);
    }
}
