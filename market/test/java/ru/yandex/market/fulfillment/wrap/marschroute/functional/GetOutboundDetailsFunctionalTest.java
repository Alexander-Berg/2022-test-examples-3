package ru.yandex.market.fulfillment.wrap.marschroute.functional;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import ru.yandex.market.fulfillment.wrap.core.scenario.builder.FulfillmentInteraction;
import ru.yandex.market.fulfillment.wrap.core.scenario.builder.FunctionalTestScenarioBuilder;
import ru.yandex.market.fulfillment.wrap.marschroute.functional.configuration.RepositoryTest;
import ru.yandex.market.logistic.api.model.common.ErrorCode;
import ru.yandex.market.logistic.api.model.fulfillment.response.GetOutboundDetailsResponse;

import java.util.Arrays;

import static ru.yandex.market.fulfillment.wrap.core.scenario.builder.FulfillmentUrl.fulfillmentUrl;


class GetOutboundDetailsFunctionalTest extends RepositoryTest {

    @Test
    @DatabaseSetup(value = "classpath:repository/get_outbound_details_setup.xml")
    void getQuarantineOutboundDetails() throws Exception {
        String wrapRequest = "functional/get_outbound_details/not_fit/wrap_request.xml";
        String marschrouteResponse = "functional/get_outbound_details/not_fit/marschroute_response.json";
        String wrapResponse = "functional/get_outbound_details/not_fit/wrap_response.xml";

        testNotFitStock(wrapRequest, marschrouteResponse, wrapResponse);
    }

    @Test
    @DatabaseSetup(value = "classpath:repository/get_outbound_details_setup.xml")
    void getQuarantineOutboundDetailsInvalidStatus() throws Exception {
        String wrapRequest = "functional/get_outbound_details/not_fit/wrap_request.xml";
        String marschrouteResponse = "functional/get_outbound_details/not_fit_invalid_status/marschroute_response.json";
        String wrapResponse = "functional/get_outbound_details/not_fit_invalid_status/wrap_response.xml";

        testNotFitStock(wrapRequest, marschrouteResponse, wrapResponse);
    }

    @Test
    @DatabaseSetup(value = "classpath:repository/get_fit_outbound_details_setup.xml")
    void getFitOutboundDetails() throws Exception {
        String wrapRequest = "functional/get_outbound_details/fit/wrap_request.xml";
        String marschrouteResponse = "functional/get_outbound_details/fit/marschroute_response.json";
        String wrapResponse = "functional/get_outbound_details/fit/wrap_response.xml";

        testFitStock(wrapRequest, marschrouteResponse, wrapResponse);
    }

    @Test
    @DatabaseSetup(value = "classpath:repository/get_fit_outbound_details_setup.xml")
    void getFitOutboundDetailsInvalidStatus() throws Exception {
        String wrapRequest = "functional/get_outbound_details/fit/wrap_request.xml";
        String marschrouteResponse = "functional/get_outbound_details/fit_invalid_status/marschroute_response.json";
        String wrapResponse = "functional/get_outbound_details/fit_invalid_status/wrap_response.xml";

        testFitStock(wrapRequest, marschrouteResponse, wrapResponse);
    }

    /**
     * ??????????????????, ?????? ???????? ?? ???????? ?????? ???????????? ?? ?????????????? outbound_dispatch_info ?????? ?????????????? dispatch_doc_id,
     * ???? ?? ???????????????? ?????????? 2 ?????????????? (???? doc_id ?? dispatch_doc_id) ?? ?????????? ???????????? ?????????? ??????????????
     */
    @Test
    @DatabaseSetup("classpath:functional/get_outbound_details/not_fit_dispatch/unregistered/db_setup.xml")
    @ExpectedDatabase(value = "classpath:functional/get_outbound_details/not_fit_dispatch/unregistered/db_expected.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void getDefectOutboundDetailsWhenWaybillInfoHasUnregisteredDispatchDocId() throws Exception {
        String basePath = "functional/get_outbound_details/not_fit_dispatch/unregistered/";
        testStock(
            basePath,
            newGetWaybillInteraction("partner-101", basePath + "waybill-101.json"),
            newGetWaybillInteraction("partner-101-d", basePath + "waybill-101-d.json")
        );
    }

    /**
     * ??????????????????, ?????? ???????? ?? ???????? ???????? ???????????? ?? ?????????????? outbound_dispatch_info ?????? ?????????????? dispatch_doc_id,
     * ???? ?? ???????????????? ?????????? ???????????? 1 ???????????? ???? dispatch_doc_id
     */
    @Test
    @DatabaseSetup("classpath:functional/get_outbound_details/not_fit_dispatch/registered/db_setup.xml")
    @ExpectedDatabase(value = "classpath:functional/get_outbound_details/not_fit_dispatch/registered/db_expected.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void getDefectOutboundDetailsWhenWaybillInfoHasRegisteredDispatchDocId() throws Exception {
        String basePath = "functional/get_outbound_details/not_fit_dispatch/registered/";
        testStock(
            basePath,
            newGetWaybillInteraction("partner-102-d", basePath + "waybill-102-d.json")
        );
    }

    /**
     * ??????????????????, ?????? ?? ???????????? ???????????????????? yandex_id ?????? ?????????????????? ?????????????? ?????????????? -
     * ???????????? ???? ?????????????? ??????????????????.
     */
    @Test
    void outboundIdValidationOnMissingYandexId() throws Exception {
        testOutboundIdValidation("functional/get_outbound_details/negative/missing_yandex_id.xml");
    }

    /**
     * ??????????????????, ?????? ?? ???????????? ???????????????????? partner_id ?????? ?????????????????? ?????????????? ?????????????? -
     * ???????????? ???? ?????????????? ??????????????????.
     */
    @Test
    void outboundIdValidationOnMissingPartnerId() throws Exception {
        testOutboundIdValidation("functional/get_outbound_details/negative/missing_partner_id.xml");
    }

    /**
     * ??????????????????, ?????? ?? ???????????? ???????????????????? yandex_id ?? partner_id ?????? ?????????????????? ?????????????? ?????????????? -
     * ???????????? ???? ?????????????? ??????????????????.
     */
    @Test
    void outboundIdValidationOnBothIdsMissing() throws Exception {
        testOutboundIdValidation("functional/get_outbound_details/negative/missing_both.xml");
    }

    private FulfillmentInteraction newGetWaybillInteraction(String waybillId, String responsePath) {
        return FulfillmentInteraction.createMarschrouteInteraction()
            .setFulfillmentUrl(fulfillmentUrl(Arrays.asList("waybill", waybillId), HttpMethod.GET))
            .setResponsePath(responsePath);
    }

    private FulfillmentInteraction newGetOrderInteraction(String orderId, String responsePath) {
        return FulfillmentInteraction.createMarschrouteInteraction()
            .setFulfillmentUrl(fulfillmentUrl(Arrays.asList("order", orderId), HttpMethod.GET))
            .setResponsePath(responsePath);
    }

    private void testStock(String wrapRequest, String wrapResponse, FulfillmentInteraction... interactions)
        throws Exception {
        FunctionalTestScenarioBuilder.start(GetOutboundDetailsResponse.class)
            .sendRequestToWrapQueryGateway(wrapRequest)
            .thenMockFulfillmentRequests(interactions)
            .andExpectWrapAnswerToBeEqualTo(wrapResponse)
            .build(mockMvc, restTemplate, fulfillmentMapper, apiUrl, apiKey)
            .start();
    }

    private void testStock(String testDataPath, FulfillmentInteraction... interactions) throws Exception {
        testStock(testDataPath + "wrap_request.xml", testDataPath + "wrap_response.xml", interactions);
    }

    private void testFitStock(String wrapRequest, String marschrouteResponse, String wrapResponse) throws Exception {
        testStock(
            wrapRequest,
            wrapResponse,
            newGetOrderInteraction("EXT69667301", marschrouteResponse)
        );
    }

    private void testNotFitStock(String wrapRequest, String marschrouteResponse, String wrapResponse) throws Exception {
        testStock(
            wrapRequest,
            wrapResponse,
            newGetWaybillInteraction("123", marschrouteResponse)
        );
    }

    private void testOutboundIdValidation(String requestPath) throws Exception {
        FunctionalTestScenarioBuilder.start(GetOutboundDetailsResponse.class)
            .sendRequestToWrapQueryGateway(requestPath)
            .andExpectWrapAnswerToContainErrors(ImmutableMap.of(ErrorCode.BAD_REQUEST, 1))
            .build(mockMvc, restTemplate, fulfillmentMapper, apiUrl, apiKey)
            .start();
    }
}
