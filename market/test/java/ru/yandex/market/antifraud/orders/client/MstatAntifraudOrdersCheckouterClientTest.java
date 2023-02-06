package ru.yandex.market.antifraud.orders.client;

import java.util.Collections;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.antifraud.orders.client.config.MstatAntifraudOrdersClientConfig;
import ru.yandex.market.antifraud.orders.client.config.TestConfig;
import ru.yandex.market.antifraud.orders.entity.AntifraudAction;
import ru.yandex.market.antifraud.orders.entity.AntifraudCheckResult;
import ru.yandex.market.antifraud.orders.entity.OrderVerdict;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.OrderItemResponseDto;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.OrderRequestDto;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.OrderResponseDto;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * @author Alexander Novikov <a href="mailto:hronos@yandex-team.ru"></a>
 * @date 22.08.2019
 */
@RunWith(SpringRunner.class)
@RestClientTest(MstatAntifraudOrdersClientConfig.class)
@ContextConfiguration(classes = {TestConfig.class})
public class MstatAntifraudOrdersCheckouterClientTest {

    @Autowired
    private MstatAntifraudOrdersCheckouterClient mstatAntifraudOrdersCheckouterClient;

    @Autowired
    private RestTemplate mstatAntifraudOltpClientRestTemplate;

    private MockRestServiceServer mockServer;

    @Before
    public void init() {
        mockServer = MockRestServiceServer.createServer(mstatAntifraudOltpClientRestTemplate);
        mockServer.reset();
    }

    @Test
    public void detectFraudQuickly() {
        OrderResponseDto orderResponseDto =
                new OrderResponseDto(ImmutableList.of(OrderItemResponseDto.builder().build()));
        OrderVerdict expectedOrderVerdict = OrderVerdict.builder()
                .checkResults(Collections.singleton(new AntifraudCheckResult(AntifraudAction.CANCEL_ORDER,
                        "TEST_CANCEL"
                        , "BECAUSE")))
                .fixedOrder(orderResponseDto)
                .isDegradation(false)
                .build();

        //language=JSON
        String str =
                "{\n" +
                        "  \"degradation\": false,\n" +
                        "  \"fixed_order\": {" +
                        "\n" +
                        "    \"items\": [\n" +
                        "      {\n" +
                        "        \"id\": null,\n" +
                        "        \"feedId\": null,\n" +
                        "        \"offerId\": null,\n" +
                        "        \"count\": null,\n" +
                        "        \"changes\": null\n" +
                        "      }\n" +
                        "    ]\n" +
                        "  },\n" +
                        "  \"check_results\": [\n" +
                        "    {\n" +
                        "      \"action\": \"CANCEL_ORDER\",\n" +
                        "      \"answer_text\": \"TEST_CANCEL\",\n" +
                        "      \"reason\": \"BECAUSE\"\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}";

        mockServer.expect(requestTo("/antifraud/detect"))
                .andRespond(withSuccess(str, MediaType.APPLICATION_JSON));

        OrderVerdict orderVerdict =
                mstatAntifraudOrdersCheckouterClient.detectFraudQuickly(OrderRequestDto.builder().build());

        assertEquals(orderVerdict, expectedOrderVerdict);
        mockServer.verify();
    }
}
