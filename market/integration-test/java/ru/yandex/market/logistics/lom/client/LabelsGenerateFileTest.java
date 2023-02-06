package ru.yandex.market.logistics.lom.client;

import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ResponseActions;

import ru.yandex.market.logistics.lom.model.dto.OrderLabelRequestDto;
import ru.yandex.market.logistics.lom.model.enums.PageSize;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class LabelsGenerateFileTest extends AbstractClientTest {
    private static final String LABELS_FILE_CONTENT = "label file content";

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LomClient lomClient;

    @Value("${lom.api.url}")
    protected String uri;

    @Test
    @DisplayName("Получение ярлыка заказа")
    void generateLabelsFile() throws Exception {
        OrderLabelRequestDto orderLabelRequestDto = OrderLabelRequestDto.builder()
            .ordersIds(Set.of(1L))
            .pageSize(PageSize.A4)
            .build();

        mockGenerateLabelsFile(orderLabelRequestDto);

        byte[] actual = lomClient.generateLabelsFile(orderLabelRequestDto);
        softly.assertThat(actual).isEqualTo(LABELS_FILE_CONTENT.getBytes());
    }

    private void mockGenerateLabelsFile(OrderLabelRequestDto orderLabelRequestDto) throws Exception {
        ResponseActions expect = mock.expect(requestTo(uri + "/orders/labels/generate"))
            .andExpect(method(HttpMethod.PUT))
            .andExpect(header("Accept", "application/json; q=0.9", "application/pdf"))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(content().json(objectMapper.writeValueAsString(orderLabelRequestDto)));
        expect.andRespond(
            withStatus(HttpStatus.OK)
                .contentType(MediaType.APPLICATION_PDF)
                .body(LABELS_FILE_CONTENT.getBytes())
        );
    }
}
