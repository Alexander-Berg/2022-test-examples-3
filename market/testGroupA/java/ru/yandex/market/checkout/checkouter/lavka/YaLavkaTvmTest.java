package ru.yandex.market.checkout.checkouter.lavka;

import java.util.Optional;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.http.HttpHeaders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.RestTemplate;

import ru.yandex.common.util.collections.CollectionUtils;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.service.yalavka.client.YaLavkaDeliveryServiceClientImpl;
import ru.yandex.market.checkout.util.yalavka.YaLavkaDeliveryServiceConfigurer;
import ru.yandex.market.common.taxi.TaxiDeliveryServiceClient;
import ru.yandex.market.common.taxi.model.OrderCreateRequestBody;
import ru.yandex.market.common.taxi.model.OrderReserveRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;

class YaLavkaTvmTest extends AbstractWebTestBase {

    private static final String TVM_TICKET = "taxi-dispatcher-service-ticket";

    @Autowired
    private WireMockServer yaLavkaDeliveryServiceMock;
    @Autowired
    private YaLavkaDeliveryServiceConfigurer yaLavkaDSConfigurer;
    @Autowired
    private RestTemplate yaLavkaRestTemplate;
    @Autowired
    private RestTemplate yaLavkaCheckRestTemplate;

    @BeforeEach
    void setup() {
        yaLavkaDSConfigurer.reset();
    }

    @Test
    void test() {
        TaxiDeliveryServiceClient client = new YaLavkaDeliveryServiceClientImpl(
                "http://localhost",
                yaLavkaDeliveryServiceMock.port(),
                "",
                () -> Optional.of(TVM_TICKET),
                yaLavkaRestTemplate,
                yaLavkaCheckRestTemplate,
                checkouterProperties);

        OrderReserveRequest request = new OrderReserveRequest(new OrderCreateRequestBody(null, null, null, -1, null,
                null));
        try {
            client.reserveDeliveryByLavka(request);
        } catch (Exception ex) {
            // Запрос заведомо неуспешный
        }
        HttpHeaders headers = yaLavkaDSConfigurer.getAllInteractions()
                .get(0)
                .getRequest()
                .getHeaders();
        assertEquals(TVM_TICKET, CollectionUtils.expectedSingleResult(
                headers.getHeader("X-Ya-Service-Ticket").values()
        ));
    }
}
