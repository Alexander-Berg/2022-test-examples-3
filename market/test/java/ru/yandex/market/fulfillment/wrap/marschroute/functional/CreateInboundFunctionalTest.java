package ru.yandex.market.fulfillment.wrap.marschroute.functional;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;

import ru.yandex.market.fulfillment.wrap.core.scenario.builder.FulfillmentInteraction;
import ru.yandex.market.fulfillment.wrap.core.scenario.builder.FulfillmentUrl;
import ru.yandex.market.fulfillment.wrap.core.scenario.builder.FunctionalTestScenarioBuilder;
import ru.yandex.market.fulfillment.wrap.core.scenario.builder.JsonMatcher;
import ru.yandex.market.fulfillment.wrap.marschroute.api.ProductsClient;
import ru.yandex.market.fulfillment.wrap.marschroute.entity.InboundInfo;
import ru.yandex.market.fulfillment.wrap.marschroute.entity.UpdateProductJob;
import ru.yandex.market.fulfillment.wrap.marschroute.functional.configuration.IntegrationTest;
import ru.yandex.market.fulfillment.wrap.marschroute.repository.UpdateProductJobRepository;
import ru.yandex.market.fulfillment.wrap.marschroute.scheduled.UpdateProductJobExecutor;
import ru.yandex.market.fulfillment.wrap.marschroute.service.common.SystemPropertyKey;
import ru.yandex.market.logistic.api.model.fulfillment.response.CreateInboundResponse;

import static java.lang.ClassLoader.getSystemResourceAsStream;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@SuppressWarnings("Duplicates")
class CreateInboundFunctionalTest extends IntegrationTest {

    private static final String YANDEX_ID = "443444";
    private static final String PARTNER_ID = "990027";
    private static final InboundInfo INFO = new InboundInfo(YANDEX_ID, PARTNER_ID);

    private static final String CREATE_INBOUND_WRAP_REQUEST_PATH =
        "functional/create_inbound_and_update_products/create_inbound_wrapper_request.xml";
    private static final String CREATE_INBOUND_EXPECTED_MARSCHROUTE_REQUEST_PATH =
        "functional/create_inbound_and_update_products/create_inbound_expected_marschroute_request.json";
    private static final String CREATE_INBOUND_EXPECTED_MARSCHROUTE_REQUEST_WITH_SUPPLIER_ID_PATH =
        "functional/create_inbound_and_update_products/create_inbound_expected_marschroute_request_with_supplier.json";
    private static final String CREATE_INBOUND_MARSCHROUTE_RESPONSE_PATH =
        "functional/create_inbound_and_update_products/create_inbound_marschroute_response.json";
    private static final String CREATE_INBOUND_WRAP_RESPONSE_PATH =
        "functional/create_inbound_and_update_products/create_inbound_expected_wrapper_response.xml";
    private static final String UPDATE_PRODUCTS_EXPECTED_MARSCHROUTE_REQUEST_PATH =
        "functional/create_inbound_and_update_products/update_products_expected_marschroute_request.json";
    private static final String UPDATE_PRODUCTS_MARSCHROUTE_RESPONSE_PATH =
        "functional/create_inbound_and_update_products/update_products_marschroute_response.json";
    private static final String CREATE_RETURN_INBOUND_WRAP_REQUEST_PATH =
        "functional/create_inbound_and_update_products/return/create_inbound_wrapper_request.xml";
    private static final String CREATE_RETURN_INBOUND_EXPECTED_MARSCHROUTE_REQUEST_PATH =
        "functional/create_inbound_and_update_products/return/create_inbound_expected_marschroute_request.json";
    private static final String UPDATE_RETURN_PRODUCTS_EXPECTED_MARSCHROUTE_REQUEST_PATH =
        "functional/create_inbound_and_update_products/return/update_products_expected_marschroute_request.json";
    private static final String FAIL_EXPECTED_WRAP_RESPONSE_PATH =
        "functional/create_inbound_and_update_products/fail_expected_wrapper_response.xml";
    private static final String FAIL_MARSCHROUTE_RESPONSE_PATH =
        "functional/create_inbound_and_update_products/fail_marschroute_response.json";
    private static final String CREATE_INBOUND_FOR_CROSSDOCK_WRAP_REQUEST_PATH =
        "functional/create_inbound_and_update_products/crossdock/create_inbound_wrapper_request.xml";
    private static final String CREATE_INBOUND_FOR_CROSSDOCK_EXPECTED_WRAP_RESPONSE_PATH =
        "functional/create_inbound_and_update_products/crossdock/create_inbound_expected_wrapper_response.xml";

    @Autowired
    private UpdateProductJobRepository updateProductJobRepository;

    @Autowired
    private ProductsClient productsClient;

    /**
     * Проверяет сценарий, когда успешно создаётся поставка и с первого раза успешно обновляются товары из неё.
     */
    @Test
    void createInboundAndUpdateProductsPositiveScenario() throws Exception {
        given(inboundInfoRepository.findByYandexId(YANDEX_ID)).willReturn(Optional.empty());
        given(inboundInfoRepository.save(INFO)).willReturn(INFO);

        FulfillmentInteraction createInboundInteraction = FulfillmentInteraction.createMarschrouteInteraction()
            .setFulfillmentUrl(FulfillmentUrl.fulfillmentUrl(Arrays.asList("waybill"), HttpMethod.PUT))
            .setExpectedRequestPath(CREATE_INBOUND_EXPECTED_MARSCHROUTE_REQUEST_PATH)
            .setResponsePath(CREATE_INBOUND_MARSCHROUTE_RESPONSE_PATH);

        FulfillmentInteraction updateProductsInteraction = FulfillmentInteraction.createMarschrouteInteraction()
            .setFulfillmentUrl(FulfillmentUrl.fulfillmentUrl(Arrays.asList("products"), HttpMethod.POST))
            .setExpectedRequestPath(UPDATE_PRODUCTS_EXPECTED_MARSCHROUTE_REQUEST_PATH)
            .setResponsePath(UPDATE_PRODUCTS_MARSCHROUTE_RESPONSE_PATH);

        FunctionalTestScenarioBuilder
            .start(CreateInboundResponse.class)
            .sendRequestToWrapQueryGateway(CREATE_INBOUND_WRAP_REQUEST_PATH)
            .thenMockFulfillmentRequest(createInboundInteraction)
            .thenMockFulfillmentRequest(updateProductsInteraction)
            .andExpectWrapAnswerToBeEqualTo(CREATE_INBOUND_WRAP_RESPONSE_PATH)
            .build(mockMvc, restTemplate, fulfillmentMapper, apiUrl, apiKey)
            .start();
        verify(inboundInfoRepository).save(INFO);
    }

    /**
     *  Успешно создаётся ВОЗВРАТНАЯ поставка и с первого раза успешно обновляются товары из неё.
     *  При этом не обновляются параметры многоместности и признак контроля срока годности.
     */
    @Test
    void createReturnInboundAndUpdateProducts() throws Exception {
        given(inboundInfoRepository.findByYandexId(YANDEX_ID)).willReturn(Optional.empty());
        given(inboundInfoRepository.save(INFO)).willReturn(INFO);

        FulfillmentInteraction createInboundInteraction = FulfillmentInteraction.createMarschrouteInteraction()
            .setFulfillmentUrl(FulfillmentUrl.fulfillmentUrl(Arrays.asList("waybill"), HttpMethod.PUT))
            .setExpectedRequestPath(CREATE_RETURN_INBOUND_EXPECTED_MARSCHROUTE_REQUEST_PATH)
            .setResponsePath(CREATE_INBOUND_MARSCHROUTE_RESPONSE_PATH);

        FulfillmentInteraction updateProductsInteraction = FulfillmentInteraction.createMarschrouteInteraction()
            .setFulfillmentUrl(FulfillmentUrl.fulfillmentUrl(Arrays.asList("products"), HttpMethod.POST))
            .setExpectedRequestPath(UPDATE_RETURN_PRODUCTS_EXPECTED_MARSCHROUTE_REQUEST_PATH)
            .setResponsePath(UPDATE_PRODUCTS_MARSCHROUTE_RESPONSE_PATH);

        FunctionalTestScenarioBuilder
            .start(CreateInboundResponse.class)
            .sendRequestToWrapQueryGateway(CREATE_RETURN_INBOUND_WRAP_REQUEST_PATH)
            .thenMockFulfillmentRequests(createInboundInteraction, updateProductsInteraction)
            .andExpectWrapAnswerToBeEqualTo(CREATE_INBOUND_WRAP_RESPONSE_PATH)
            .build(mockMvc, restTemplate, fulfillmentMapper, apiUrl, apiKey)
            .start();
        verify(inboundInfoRepository).save(INFO);
    }

    /**
     * Проверяет сценарий, когда успешно создаётся поставка, но товары из неё не удаётся обновить обе попытки.
     */
    @Test
    void createInboundAndFailUpdateProductsTwiceNegativeScenario() throws Exception {
        given(inboundInfoRepository.findByYandexId(YANDEX_ID)).willReturn(Optional.empty());
        given(inboundInfoRepository.save(INFO)).willReturn(INFO);

        FulfillmentInteraction createInboundInteraction = FulfillmentInteraction.createMarschrouteInteraction()
            .setFulfillmentUrl(FulfillmentUrl.fulfillmentUrl(Arrays.asList("waybill"), HttpMethod.PUT))
            .setExpectedRequestPath(CREATE_INBOUND_EXPECTED_MARSCHROUTE_REQUEST_PATH)
            .setResponsePath(CREATE_INBOUND_MARSCHROUTE_RESPONSE_PATH);

        FulfillmentInteraction updateProductsFailFirstInteraction = FulfillmentInteraction.createMarschrouteInteraction()
            .setFulfillmentUrl(FulfillmentUrl.fulfillmentUrl(Arrays.asList("products"), HttpMethod.POST))
            .setExpectedRequestPath(UPDATE_PRODUCTS_EXPECTED_MARSCHROUTE_REQUEST_PATH)
            .setResponsePath(FAIL_MARSCHROUTE_RESPONSE_PATH);

        FunctionalTestScenarioBuilder
            .start(CreateInboundResponse.class)
            .sendRequestToWrapQueryGateway(CREATE_INBOUND_WRAP_REQUEST_PATH)
            .thenMockFulfillmentRequest(createInboundInteraction)
            .thenMockFulfillmentRequest(updateProductsFailFirstInteraction)
            .andExpectWrapAnswerToBeEqualTo(CREATE_INBOUND_WRAP_RESPONSE_PATH)
            .build(mockMvc, restTemplate, fulfillmentMapper, apiUrl, apiKey)
            .start();

        verify(inboundInfoRepository).save(INFO);
    }

    /**
     * Проверяет сценарий, когда поставку не удаётся создать.
     */
    @Test
    void failCreateInboundNegativeScenario() throws Exception {
        given(inboundInfoRepository.findByYandexId(YANDEX_ID)).willReturn(Optional.empty());

        FulfillmentInteraction createInboundInteraction = FulfillmentInteraction.createMarschrouteInteraction()
            .setFulfillmentUrl(FulfillmentUrl.fulfillmentUrl(Arrays.asList("waybill"), HttpMethod.PUT))
            .setExpectedRequestPath(CREATE_INBOUND_EXPECTED_MARSCHROUTE_REQUEST_PATH)
            .setResponsePath(FAIL_MARSCHROUTE_RESPONSE_PATH);

        FunctionalTestScenarioBuilder
            .start(CreateInboundResponse.class)
            .sendRequestToWrapQueryGateway(CREATE_INBOUND_WRAP_REQUEST_PATH)
            .thenMockFulfillmentRequest(createInboundInteraction)
            .andExpectWrapAnswerToBeEqualTo(FAIL_EXPECTED_WRAP_RESPONSE_PATH)
            .build(mockMvc, restTemplate, fulfillmentMapper, apiUrl, apiKey)
            .start();

        verify(inboundInfoRepository, never()).save(INFO);
    }

    @Test
    void testCreatingUpdateProductRequestsWithLifetime() throws Exception {
        List<UpdateProductJob> job1 = Collections.singletonList(
            new UpdateProductJob()
                .setName("Edited name")
                .setLifetime(true)
                .setVendorId(100500L)
                .setShopSku("Yeahitsjoba1")
        );

        String itemId = job1.get(0).getShopSku() + "." + job1.get(0).getVendorId();

        String expectedContent = IOUtils.toString(getSystemResourceAsStream(
            "functional/create_inbound_and_update_products/created_update_product_lifetime_true_request.json"
            ), "UTF-8"
        );

        MockRestServiceServer mockServer = MockRestServiceServer.createServer(restTemplate);

        testUpdateProductRequest(itemId, expectedContent, job1, mockServer);
    }

    @Test
    void testCreatingUpdateProductRequestsWithoutLifetime() throws IOException {
        List<UpdateProductJob> job = Collections.singletonList(
            new UpdateProductJob()
                .setName("Edited name")
                .setLifetime(false)
                .setVendorId(100500L)
                .setShopSku("Yeahitsjoba2")
        );

        String itemId = job.get(0).getShopSku() + "." + job.get(0).getVendorId();

        String expectedContent = IOUtils.toString(getSystemResourceAsStream(
            "functional/create_inbound_and_update_products/created_update_product_lifetime_false_request.json"
            ), "UTF-8"
        );

        MockRestServiceServer mockServer = MockRestServiceServer.createServer(restTemplate);

        testUpdateProductRequest(itemId, expectedContent, job, mockServer);
    }

    /**
     * Проверяет сценарий, когда поставка уже была создана ранее и мы не должны создавать еще одну
     */
    @Test
    void skipCreatingInbound() throws Exception {
        given(inboundInfoRepository.findByYandexId(YANDEX_ID)).willReturn(Optional.of(INFO));

        FunctionalTestScenarioBuilder
            .start(CreateInboundResponse.class)
            .sendRequestToWrapQueryGateway(CREATE_INBOUND_WRAP_REQUEST_PATH)
            .andExpectWrapAnswerToBeEqualTo(CREATE_INBOUND_WRAP_RESPONSE_PATH)
            .build(mockMvc, restTemplate, fulfillmentMapper, apiUrl, apiKey)
            .start();

        verify(inboundInfoRepository, never()).save(INFO);
    }

    /**
     * Проверяет сценарий, когда создается кроссдок поставка. Данный тип поставок не поддерживается в прослойке, а
     * значит должна быть возвращена ошибка.
     */
    @Test
    void createInboundForCrossdock() throws Exception {
        FunctionalTestScenarioBuilder
            .start(CreateInboundResponse.class)
            .sendRequestToWrapQueryGateway(CREATE_INBOUND_FOR_CROSSDOCK_WRAP_REQUEST_PATH)
            .andExpectWrapAnswerToBeEqualTo(CREATE_INBOUND_FOR_CROSSDOCK_EXPECTED_WRAP_RESPONSE_PATH)
            .build(mockMvc, restTemplate, fulfillmentMapper, apiUrl, apiKey)
            .start();
    }

    /**
     * Проверяем, что supplierId будет отправляться при включенной проперти в БД
     */
    @Test
    void createInboundWithShouldSendSupplierIdProperty() throws Exception {
        given(inboundInfoRepository.findByYandexId(YANDEX_ID)).willReturn(Optional.empty());
        given(inboundInfoRepository.save(INFO)).willReturn(INFO);
        given(systemPropertyRepository.getBooleanProperty(
                SystemPropertyKey.SHOULD_SEND_SUPPLIER_ID_ON_CREATE_INBOUND.name())).willReturn(true);

        FulfillmentInteraction createInboundInteraction = FulfillmentInteraction.createMarschrouteInteraction()
                .setFulfillmentUrl(FulfillmentUrl.fulfillmentUrl(Arrays.asList("waybill"), HttpMethod.PUT))
                .setExpectedRequestPath(CREATE_INBOUND_EXPECTED_MARSCHROUTE_REQUEST_WITH_SUPPLIER_ID_PATH)
                .setResponsePath(CREATE_INBOUND_MARSCHROUTE_RESPONSE_PATH);

        FulfillmentInteraction updateProductsInteraction = FulfillmentInteraction.createMarschrouteInteraction()
                .setFulfillmentUrl(FulfillmentUrl.fulfillmentUrl(Arrays.asList("products"), HttpMethod.POST))
                .setExpectedRequestPath(UPDATE_PRODUCTS_EXPECTED_MARSCHROUTE_REQUEST_PATH)
                .setResponsePath(UPDATE_PRODUCTS_MARSCHROUTE_RESPONSE_PATH);

        FunctionalTestScenarioBuilder
                .start(CreateInboundResponse.class)
                .sendRequestToWrapQueryGateway(CREATE_INBOUND_WRAP_REQUEST_PATH)
                .thenMockFulfillmentRequest(createInboundInteraction)
                .thenMockFulfillmentRequest(updateProductsInteraction)
                .andExpectWrapAnswerToBeEqualTo(CREATE_INBOUND_WRAP_RESPONSE_PATH)
                .build(mockMvc, restTemplate, fulfillmentMapper, apiUrl, apiKey)
                .start();
        verify(inboundInfoRepository).save(INFO);
    }

    private void testUpdateProductRequest(String itemId, String expectedContent, List<UpdateProductJob> job,
                                          MockRestServiceServer mockServer) {
        mockServer
            .expect(MockRestRequestMatchers.requestTo("http://api.url/api-key/product/" + itemId))
            .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
            .andExpect(MockRestRequestMatchers.content().string(new JsonMatcher(expectedContent)));

        new UpdateProductJobExecutor(updateProductJobRepository, productsClient).executeJobs(job);
    }
}
