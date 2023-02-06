package ru.yandex.market.checkout.util.sberbank;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.StreamSupport;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestComponent;

import ru.yandex.market.checkout.util.GenericMockHelper;
import ru.yandex.market.checkout.util.balance.OneElementBackIterator;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.apache.http.HttpStatus.SC_OK;
import static ru.yandex.market.checkout.checkouter.util.Utils.HUNDRED;

/**
 * @author : poluektov
 * date: 2019-04-15.
 */
//TODO: Подумать как их объеденить с конфигуратором трастовой моки.
@TestComponent
public class SberMockConfigurer {

    public static final String REGISTER_DO = "register.do";
    public static final String GET_ORDER_STATUS_EXTENDED_DO = "getOrderStatusExtended.do";
    public static final String REFUND_DO = "refund.do";

    public static final String SBERCREDIT_URL = "/sbercredit";
    public static final String PAYMENT_REST_URL = "/payment/rest";

    @Autowired
    private WireMockServer sberMock;

    private static String getStringBodyFromFile(String fileName) throws IOException {
        return getStringBodyFromFile(fileName, Collections.emptyMap());
    }

    private static String getStringBodyFromFile(String fileName, Map<String, Object> vars) throws
            IOException {
        final String[] template = {IOUtils.toString(SberMockConfigurer.class.getResourceAsStream(fileName))};
        vars.forEach((key, value) -> template[0] = template[0].replace(key, Objects.toString(value)));
        return template[0];
    }

    public static Iterator<ServeEvent> skipOrderStatusCalls(Iterable<ServeEvent> sberCalls) throws Exception {
        return StreamSupport.stream(sberCalls.spliterator(), false)
                .filter(serveEvent -> !GET_ORDER_STATUS_EXTENDED_DO.equals(serveEvent.getStubMapping()
                        .getName())).iterator();
    }

    private static ResponseDefinitionBuilder ok() {
        return aResponse()
                .withHeader("Content-Type", "application/json")
                .withStatus(SC_OK);
    }

    public void mockWholeSber() {
        try {
            mockRegisterDo();
            mockGetOrderStatus("orderStatusExtended.json");
            mockRefund();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void mockRefund() throws IOException {
        MappingBuilder builder = post(urlPathMatching(PAYMENT_REST_URL + "/" + REFUND_DO))
                .withName(REFUND_DO)
                .willReturn(ok().withBody(getStringBodyFromFile("refund.json")));
        sberMock.stubFor(builder);
    }

    public void mockRegisterDo() throws IOException {
        MappingBuilder builder = post(urlPathMatching(SBERCREDIT_URL + "/register.do"))
                .withName(REGISTER_DO)
                .willReturn(ok().withBody(getStringBodyFromFile("registerOrder.json")));
        sberMock.stubFor(builder);
    }

    public void mockGetOrderStatus(String filename) throws IOException {
        MappingBuilder builder = get(urlPathMatching(PAYMENT_REST_URL + "/" + GET_ORDER_STATUS_EXTENDED_DO))
                .withName(GET_ORDER_STATUS_EXTENDED_DO)
                .willReturn(ok().withBody(getStringBodyFromFile(filename)));
        sberMock.stubFor(builder);
    }

    public void mockGetOrderStatusFullRefund(BigDecimal paymentAmount) throws IOException {
        MappingBuilder builder = get(urlPathMatching(PAYMENT_REST_URL + "/" + GET_ORDER_STATUS_EXTENDED_DO))
                .withName(GET_ORDER_STATUS_EXTENDED_DO)
                .willReturn(ok().withBody(getStringBodyFromFile(
                        "orderStatusExtendedRefunded.json",
                        ImmutableMap.<String, Object>builder().put("!!PAYMENT_AMOUNT!!",
                                paymentAmount.multiply(HUNDRED).longValue()).build())));
        sberMock.stubFor(builder);
    }

    public void mockGetOrderStatusCompleted() {
        try {
            mockGetOrderStatus("orderStatusExtendedCompleted.json");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void mockGetOrderStatusRejected() {
        try {
            mockGetOrderStatus("orderStatusExtendedRejected.json");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void resetRequests() {
        sberMock.resetRequests();
    }

    public List<ServeEvent> servedEvents() {
        return GenericMockHelper.servedEvents(sberMock);
    }

    public OneElementBackIterator<ServeEvent> eventsIterator() {
        return new OneElementBackIterator<>(servedEvents().iterator());
    }
}
