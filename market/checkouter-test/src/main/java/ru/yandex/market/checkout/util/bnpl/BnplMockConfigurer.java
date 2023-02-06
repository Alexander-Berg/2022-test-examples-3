package ru.yandex.market.checkout.util.bnpl;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Resource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestComponent;

import ru.yandex.market.checkout.checkouter.pay.bnpl.model.BnplOrder;
import ru.yandex.market.checkout.checkouter.pay.bnpl.model.BnplRefundStatus;
import ru.yandex.market.checkout.util.GenericMockHelper;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_SERVICE_UNAVAILABLE;
import static ru.yandex.market.checkout.checkouter.pay.bnpl.model.BnplUserId.YANDEX_UID_HEADER;

/**
 * @author : poluektov
 * date: 2021-06-23.
 */
@TestComponent
public class BnplMockConfigurer {

    private static final String BASE_BNPL_URL = "/yandex";
    public static final String POST_ORDER_CREATE = BASE_BNPL_URL + "/order/create";
    public static final String GET_ORDER_INFO = BASE_BNPL_URL + "/order/info";
    public static final String POST_PLAN_CHECK = BASE_BNPL_URL + "/v1/checkouter/plan/check";
    public static final String POST_ORDER_DELIVER = BASE_BNPL_URL + "/order/deliver";
    public static final String POST_ORDER_REFUND = BASE_BNPL_URL + "/order/refund";
    public static final String POST_ORDER_REFUND_START = BASE_BNPL_URL + "/order/refund/start";
    public static final String GET_ORDER_REFUND_INFO = BASE_BNPL_URL + "/order/refund/info";
    @Autowired
    @SuppressWarnings("checkstyle:VisibilityModifier")
    public WireMockServer bnplMock;
    @Resource(name = "checkouterAnnotationObjectMapper")
    private ObjectMapper checkouterAnnotationObjectMapper;

    private static String getStringBodyFromFile(String fileName) throws IOException {
        return getStringBodyFromFile(fileName, Collections.emptyMap());
    }

    private static String getStringBodyFromFile(String fileName, Map<String, Object> vars) throws
            IOException {
        final String[] template = {IOUtils.toString(
                BnplMockConfigurer.class.getResourceAsStream(fileName),
                Charset.defaultCharset())};
        vars.forEach((key, value) -> template[0] = template[0].replace(key, Objects.toString(value)));
        return template[0];
    }

    private static ResponseDefinitionBuilder ok() {
        return aResponse()
                .withHeader("Content-Type", "application/json")
                .withStatus(SC_OK);
    }

    private static ResponseDefinitionBuilder neOk() {
        return aResponse()
                .withHeader("Content-Type", "application/json")
                .withStatus(SC_BAD_REQUEST);
    }

    private static ResponseDefinitionBuilder unavailable() {
        return aResponse()
                .withHeader("Content-Type", "application/json")
                .withStatus(SC_SERVICE_UNAVAILABLE);
    }

    public void mockWholeBnpl() {
        try {
            mockOrderCreate(YANDEX_UID_HEADER);
            mockGetBnplOrder();
            mockPlanCheck(YANDEX_UID_HEADER);
            mockOrderDeliver();
            mockOrderRefund();
            mockOrderRefundStart();
            mockRefundInfo(BnplRefundStatus.APPROVED);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void mockWholeNoAuthBnpl(String headerKey) {
        try {
            mockOrderCreate(headerKey);
            mockGetBnplOrder("orderInfoNoAuthResponse.json");
            mockPlanCheck(headerKey);
            mockOrderDeliver();
            mockOrderRefund();
            mockOrderRefundStart();
            mockRefundInfo(BnplRefundStatus.APPROVED);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void mockOrderCreate(String headerKey) throws IOException {
        MappingBuilder builder = post(urlPathMatching(POST_ORDER_CREATE))
                .withName(POST_ORDER_CREATE)
                .withHeader(headerKey, WireMock.matching("^\\d+$"))
                .willReturn(ok().withBody(getStringBodyFromFile("orderCreateResponse.json")));
        bnplMock.stubFor(builder);
    }

    public void mockGetBnplOrder() throws IOException {
        MappingBuilder builder = get(urlPathMatching(GET_ORDER_INFO))
                .withName(GET_ORDER_INFO)
                .withHeader(YANDEX_UID_HEADER, WireMock.matching("^\\d+$"))
                .willReturn(ok().withBody(getStringBodyFromFile("orderInfoResponse.json")));
        bnplMock.stubFor(builder);
    }

    public void mockGetBnplOrder(String filename) throws IOException {
        MappingBuilder builder = get(urlPathMatching(GET_ORDER_INFO))
                .withName(GET_ORDER_INFO)
                .willReturn(ok().withBody(getStringBodyFromFile(filename)));
        bnplMock.stubFor(builder);
    }

    public void mockNotOkGetBnplOrder() {
        MappingBuilder builder = get(urlPathMatching(GET_ORDER_INFO))
                .withName(GET_ORDER_INFO)
                .willReturn(unavailable());
        bnplMock.stubFor(builder);
    }

    public void mockGetBnplOrder(BnplOrder order) throws JsonProcessingException {
        MappingBuilder builder = get(urlPathMatching(GET_ORDER_INFO))
                .withName(GET_ORDER_INFO)
                .willReturn(ok().withBody(checkouterAnnotationObjectMapper.writeValueAsString(order)));
        bnplMock.stubFor(builder);
    }

    public BnplOrder getDefaultBnplOrderResponse() throws IOException {
        return checkouterAnnotationObjectMapper.readValue(
                getStringBodyFromFile("orderInfoResponse.json"), BnplOrder.class);
    }

    public void mockPlanCheck(String headerKey) throws IOException {
        mockPlanCheck(headerKey, 0, "planCheckResponse.json");
    }

    public void mockPlanCheck(String headerKey, int fixedDelayMs) throws IOException {
        mockPlanCheck(headerKey, fixedDelayMs, "planCheckResponse.json");
    }

    public void mockEmptyPlanCheck(String headerKey) throws IOException {
        mockPlanCheck(headerKey, 0, "planCheckEmptyResponse.json");
    }

    public void mockPlanCheck(String headerKey, int fixedDelayMs, String jsonResponseFileName) throws IOException {
        MappingBuilder builder = post(urlPathMatching(POST_PLAN_CHECK))
                .withName(POST_PLAN_CHECK)
                .withHeader(headerKey, WireMock.matching("^\\d+$"))
                .willReturn(ok()
                        .withBody(getStringBodyFromFile(jsonResponseFileName))
                        .withFixedDelay(fixedDelayMs)
                );
        bnplMock.stubFor(builder);
    }

    public void mockOrderDeliver() {
        MappingBuilder builder = post(urlPathMatching(POST_ORDER_DELIVER))
                .withName(POST_ORDER_DELIVER)
                .withHeader(YANDEX_UID_HEADER, WireMock.matching("^\\d+$"))
                .willReturn(ok());
        bnplMock.stubFor(builder);
    }

    public void mockOrderDeliverBadRequest() {
        MappingBuilder builder = post(urlPathMatching(POST_ORDER_DELIVER))
                .withName(POST_ORDER_DELIVER)
                .withHeader(YANDEX_UID_HEADER, WireMock.matching("^\\d+$"))
                .willReturn(neOk());
        bnplMock.stubFor(builder);
    }

    private void mockOrderRefund() throws IOException {
        MappingBuilder builder = post(urlPathMatching(POST_ORDER_REFUND))
                .withName(POST_ORDER_REFUND)
                .withHeader(YANDEX_UID_HEADER, WireMock.matching("^\\d+$"))
                .willReturn(ok().withBody(getStringBodyFromFile("orderRefundResponse.json")));
        bnplMock.stubFor(builder);
    }

    private void mockOrderRefundStart() {
        MappingBuilder builder = post(urlPathMatching(POST_ORDER_REFUND_START))
                .withName(POST_ORDER_REFUND_START)
                .withHeader(YANDEX_UID_HEADER, WireMock.matching("^\\d+$"))
                .willReturn(ok());
        bnplMock.stubFor(builder);
    }

    public void mockRefundInfo(BnplRefundStatus refundStatus) {
        MappingBuilder builder = get(urlPathMatching(GET_ORDER_REFUND_INFO))
                .withName(GET_ORDER_REFUND_INFO)
                .withHeader(YANDEX_UID_HEADER, WireMock.matching("^\\d+$"))
                .withQueryParam("refund_id", WireMock.matching("^\\S+$"))
                .willReturn(ok().withBody(
                        "{\n" +
                                "\"refund_id\": \"de8abb8e-95a3-11e2-7f45-4bc4ac3061fc\"," +
                                "\"status\": \"" + refundStatus.getValue() + "\"" +
                                "}"));
        bnplMock.stubFor(builder);
    }

    public void resetRequests() {
        bnplMock.resetRequests();
    }

    public List<ServeEvent> servedEvents() {
        return GenericMockHelper.servedEvents(bnplMock);
    }

    public List<ServeEvent> findEventsByStubName(String stubName) {
        return createEventStreamFilterByStubName(stubName)
                .collect(Collectors.toList());
    }

    public Stream<ServeEvent> createEventStreamFilterByStubName(
            String stubName
    ) {
        return GenericMockHelper.servedEvents(bnplMock).stream()
                .filter(event -> event.getStubMapping().getName().equals(stubName));
    }

    public String getBnplRequestBody(String stubName) {
        return this
                .createEventStreamFilterByStubName(stubName)
                .map(ServeEvent::getRequest)
                .max(Comparator.comparing(LoggedRequest::getLoggedDate))
                .map(LoggedRequest::getBodyAsString)
                .orElseThrow(() -> new IllegalStateException("No create bnpl order request body found"));
    }

    public JsonSchema createCheckPlanValidator() throws ProcessingException {
        return JsonSchemaFactory.byDefault()
                .getJsonSchema(BnplMockConfigurer.class.getResource("schema/planCheckSchema.json").toString());
    }
}
