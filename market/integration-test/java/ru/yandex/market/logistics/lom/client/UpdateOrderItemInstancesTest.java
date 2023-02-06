package ru.yandex.market.logistics.lom.client;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import ru.yandex.market.logistics.lom.model.dto.ChangeOrderRequestDto;
import ru.yandex.market.logistics.lom.model.dto.ChangeOrderRequestPayloadDto;
import ru.yandex.market.logistics.lom.model.dto.ItemInstancesDto;
import ru.yandex.market.logistics.lom.model.dto.UpdateItemInstancesRequestDto;
import ru.yandex.market.logistics.lom.model.enums.ChangeOrderRequestStatus;
import ru.yandex.market.logistics.lom.model.enums.ChangeOrderRequestType;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;

@DisplayName("Обновление маркировок товаров заказа")
class UpdateOrderItemInstancesTest extends AbstractClientTest {
    @Autowired
    private LomClient lomClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Успешное обновление маркировок")
    void updateSuccess() {
        mock.expect(method(HttpMethod.PUT))
            .andExpect(requestTo(uri + "/orders/updateItemInstances"))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(content().json(extractFileContent("request/order/update/item_instances/request.json"), true))
            .andRespond(withSuccess(
                extractFileContent("response/order/update_item_instances.json"),
                MediaType.APPLICATION_JSON
            ));

        ChangeOrderRequestDto result = lomClient.updateOrderItemInstances(createRequest());
        softly.assertThat(result)
            .usingRecursiveComparison()
            .isEqualTo(createExpectedResponse());
    }

    @Nonnull
    private UpdateItemInstancesRequestDto createRequest() {
        return UpdateItemInstancesRequestDto.builder()
            .orderId(1L)
            .itemInstances(
                List.of(
                    ItemInstancesDto.builder()
                        .vendorId(1001L)
                        .article("article-1")
                        .instances(List.of(Map.of("CIS", "cis-1"), Map.of("UIT", "uit-1")))
                        .build(),
                    ItemInstancesDto.builder()
                        .vendorId(1002L)
                        .article("article-2")
                        .instances(List.of(Map.of("CIS", "cis-2"), Map.of("UIT", "uit-2")))
                        .build()
                )
            )
            .build();
    }

    @Nonnull
    private ChangeOrderRequestDto createExpectedResponse() {
        return ChangeOrderRequestDto.builder()
            .id(1L)
            .requestType(ChangeOrderRequestType.UPDATE_ITEMS_INSTANCES)
            .status(ChangeOrderRequestStatus.PROCESSING)
            .payloads(Set.of(createPayload()))
            .build();
    }

    @Nonnull
    private ChangeOrderRequestPayloadDto createPayload() {
        return ChangeOrderRequestPayloadDto.builder()
            .id(1L)
            .status(ChangeOrderRequestStatus.PROCESSING)
            .payload(objectMapper.convertValue(createRequest(), JsonNode.class))
            .build();
    }
}
