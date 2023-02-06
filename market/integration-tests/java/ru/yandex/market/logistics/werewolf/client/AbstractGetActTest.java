package ru.yandex.market.logistics.werewolf.client;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import ru.yandex.market.logistics.werewolf.model.entity.DocOrder;
import ru.yandex.market.logistics.werewolf.model.entity.RtaOrdersData;
import ru.yandex.market.logistics.werewolf.model.enums.DocumentFormat;

abstract class AbstractGetActTest extends AbstractClientTest {

    @Test
    @DisplayName("Успешная генерация в html формате")
    void getReceptionTransferAct() {
        prepareMockRequest(
            MockRequestUtils.MockRequest.builder()
                .requestMethod(HttpMethod.PUT)
                .header("accept", List.of("application/json; q=0.9", MediaType.TEXT_HTML_VALUE))
                .path(getPath())
                .requestContentPath("request/app_html.json")
                .responseContentPath("response/app.html")
                .responseContentType(MediaType.TEXT_HTML)
                .build()
        );

        RtaOrdersData newOrdersData = createData(List.of(
            DocOrder.builder()
                .yandexId("a1")
                .partnerId("b1")
                .assessedCost(BigDecimal.TEN)
                .weight(BigDecimal.ONE)
                .placesCount(1)
                .build()
        ));

        softly.assertThat(executeQuery(newOrdersData, DocumentFormat.HTML))
            .isEqualTo(readFileIntoByteArray("response/app.html"));
    }

    @Test
    @DisplayName("Ошибка генерации: DocumentFormat не поддерживается")
    void getReceptionTransferActUnsupportedContent() {
        RtaOrdersData ordersData = createData(List.of());

        softly.assertThatThrownBy(() -> executeQuery(ordersData, DocumentFormat.UNSUPPORTED))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unsupported format: %s", DocumentFormat.UNSUPPORTED);
    }

    @Nonnull
    protected abstract byte[] executeQuery(RtaOrdersData data, DocumentFormat format);

    @Nonnull
    protected abstract String getPath();

    @Nonnull
    private RtaOrdersData createData(List<DocOrder> orders) {
        return RtaOrdersData.builder()
            .shipmentDate(LocalDate.parse("2020-02-02"))
            .shipmentId("testid")
            .senderId("1")
            .senderLegalName("legalname")
            .partnerLegalName("partnername")
            .orders(orders)
            .build();
    }
}
