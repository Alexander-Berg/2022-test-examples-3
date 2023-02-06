package ru.yandex.market.cashier.mocks.sber;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.yandex.market.cashier.mocks.trust.ResponseVariable;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.apache.http.HttpStatus.SC_OK;

/**
 * @author : poluektov
 * date: 2019-04-15.
 */
//TODO: Подумать как их объеденить с конфигуратором трастовой моки.

@Component
public class SberMockConfigurer {
    public static final String REGISTER_DO = "registerOrder.do";
    public static final String GET_ORDER_STATUS_EXTENDED_DO = "getOrderStatusExtended.do";

    private static final String SBERCREDIT_URL = "/sbercredit";
    private static final String PAYMENT_REST_URL = "/payment/rest";

    @Autowired
    public WireMockServer sberMock;

    public void mockWholeSber() {
        try {
            mockRegisterDo();
            mockGetOrderStatus();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void mockRegisterDo() throws IOException {
        MappingBuilder builder = get(urlPathMatching(SBERCREDIT_URL + "/registerOrder.do"))
                .withName(REGISTER_DO)
                .willReturn(ok().withBody(getStringBodyFromFile("registerOrder.json")));
        sberMock.stubFor(builder);
    }

    public void mockGetOrderStatus() throws IOException {
        MappingBuilder builder = get(urlPathMatching(PAYMENT_REST_URL + "/" + GET_ORDER_STATUS_EXTENDED_DO))
                .withName(GET_ORDER_STATUS_EXTENDED_DO)
                .willReturn(ok().withBody(getStringBodyFromFile("orderStatusExtended.json")));
        sberMock.stubFor(builder);
    }

    public void resetRequests() {
        sberMock.resetRequests();
    }

    public List<ServeEvent> getLastRequestEvents() {
        return sberMock.getServeEvents().getRequests();
    }

    private static String getStringBodyFromFile(String fileName) throws
            IOException {
        return getStringBodyFromFile(fileName, null);
    }

    private static String getStringBodyFromFile(String fileName, Map<ResponseVariable, Object> vars) throws
            IOException {
        final String[] template = {IOUtils.toString(SberMockConfigurer.class.getResourceAsStream(fileName))};
        return template[0];
    }

    private static ResponseDefinitionBuilder ok() {
        return aResponse()
                .withHeader("Content-Type", "application/json")
                .withStatus(SC_OK);
    }
}
