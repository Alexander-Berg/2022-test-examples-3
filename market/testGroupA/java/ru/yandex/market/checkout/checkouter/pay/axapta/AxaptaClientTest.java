package ru.yandex.market.checkout.checkouter.pay.axapta;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.pay.RefundReason;
import ru.yandex.market.checkout.checkouter.pay.RefundStatus;
import ru.yandex.market.checkout.checkouter.pay.axapta.model.AxaptaRefund;
import ru.yandex.market.checkout.util.axapta.AxaptaApiMockConfigurer;

import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

public class AxaptaClientTest extends AbstractWebTestBase {
    @Autowired
    private AxaptaClient axaptaClient;
    @Autowired
    private WireMockServer axaptaMock;
    @Autowired
    private AxaptaApiMockConfigurer axaptaApiMockConfigurer;

    @Test
    public void testAcceptRefundContract() {
        AxaptaRefund refund = new AxaptaRefund();
        refund.setId(100);
        refund.setOrderId(101);
        refund.setRefundStatus(RefundStatus.ACCEPTED);
        refund.setCurrency(Currency.RUR);
        refund.setAmount(BigDecimal.valueOf(99898, 2));
        refund.setOrderRemainder(BigDecimal.valueOf(9999, 2));
        refund.setReason(RefundReason.ORDER_CANCELLED);
        refund.setComment("Comment for comment");
        refund.setUpdatedAt(Date.from(Instant.parse("2000-01-20T12:30:50.123Z")));

        AxaptaRefund.Item item = new AxaptaRefund.Item();
        item.setItemId(200L);
        item.setCount(2);
        item.setPrice(BigDecimal.valueOf(49949, 2));
        refund.setItems(List.of(item));

        axaptaApiMockConfigurer.mockAcceptRefund();
        axaptaClient.acceptRefund(refund);

        // MARKETCHECKOUT-26056
        String expectedJson = "" +
                "{\n" +
                "  \"id\": 100,\n" +
                "  \"orderId\": 101,\n" +
                "  \"refundStatus\": \"ACCEPTED\",\n" +
                "  \"currency\": \"RUR\",\n" +
                "  \"amount\": 998.98,\n" +
                "  \"orderRemainder\": 99.99,\n" +
                "  \"reason\": 9,\n" +
                "  \"comment\": \"Comment for comment\",\n" +
                "  \"updatedAt\": \"2000-01-20 12:30:50.123\",\n" +
                "  \"items\": [\n" +
                "    {\n" +
                "      \"itemId\": 200,\n" +
                "      \"count\": 2,\n" +
                "      \"price\": 499.49\n" +
                "    }\n" +
                "  ]\n" +
                "}";
        axaptaMock.verify(1,
                postRequestedFor(urlPathEqualTo(AxaptaApiMockConfigurer.REFUND_ACCEPT_URL))
                        .withRequestBody(equalToJson(expectedJson, false, false)));
    }
}
