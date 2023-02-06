package ru.yandex.market.logistics.lom.client;

import java.util.Optional;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import ru.yandex.market.logistics.lom.model.dto.MdsFileDto;
import ru.yandex.market.logistics.lom.model.dto.OrderLabelDto;
import ru.yandex.market.logistics.lom.model.enums.FileType;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;

class LabelGetTest extends AbstractClientTest {
    @Autowired
    private LomClient lomClient;

    @Value("${lom.api.url}")
    protected String uri;

    @Test
    @DisplayName("Получение ярлыка заказа")
    void getLabel() {
        long orderId = 1;

        prepareMockRequest(HttpMethod.GET, "/orders/" + orderId + "/label", null, "response/document/get_label.json");

        Optional<OrderLabelDto> actual = lomClient.getLabel(orderId);
        softly.assertThat(actual).isPresent();

        OrderLabelDto expected = OrderLabelDto.builder()
            .orderId(orderId)
            .partnerId(1L)
            .labelFile(createMdsFileDto(orderId))
            .build();
        softly.assertThat(actual.get()).isEqualToComparingFieldByFieldRecursively(expected);
    }

    @Test
    @DisplayName("Получение ярлыка заказа, ярлык не найден")
    void getLabelNull() {
        mock.expect(method(HttpMethod.GET))
            .andExpect(requestTo(uri + "/orders/1/label"))
            .andRespond(
                withStatus(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(extractFileContent("response/document/label_for_order_1_not_found.json"))
            );

        Optional<OrderLabelDto> actual = lomClient.getLabel(1);
        softly.assertThat(actual).isNotPresent();
    }

    @Nonnull
    private MdsFileDto createMdsFileDto(long id) {
        return MdsFileDto.builder()
            .id(id)
            .mimeType("application/pdf")
            .fileType(FileType.ORDER_LABEL)
            .fileName(String.format("label-%d.pdf", id))
            .build();
    }
}
