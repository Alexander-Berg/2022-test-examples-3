package ru.yandex.market.deliverycalculator.indexerclient;

import com.google.common.collect.Sets;
import feign.FeignException;
import feign.RetryableException;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.matchers.Times;

import ru.yandex.market.deliverycalculator.indexerclient.model.UpdateShopRulesRequest;
import ru.yandex.market.deliverycalculator.indexerclient.modelv2.YaDeliverySenderSettingsDto;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.model.JsonBody.json;

/**
 * Тесты для {@link HttpDeliveryCalculatorIndexerClient}.
 */
class HttpDeliveryCalculatorIndexerClientTest {

    private static ClientAndServer mockServer;
    private static HttpDeliveryCalculatorIndexerClient tested;

    @BeforeAll
    static void startServer() {
        mockServer = startClientAndServer(0);
        tested = spy(new HttpDeliveryCalculatorIndexerClient(
                "http://localhost:" + mockServer.getLocalPort() + "/",
                HttpClientBuilder.create().build()
        ));
    }

    @AfterAll
    static void stopServer() {
        mockServer.stop();
    }

    /**
     * Тест для успешного вызова
     * {@link HttpDeliveryCalculatorIndexerClient#updateShopDeliveryRules(UpdateShopRulesRequest)}.
     */
    @Test
    void testUpdateShopSettings_successfulImport() {
        YaDeliverySenderSettingsDto shopSettings = new YaDeliverySenderSettingsDto.Builder()
                .withCarrierIds(Sets.newHashSet(1L, 2L))
                .withSenderId(1L)
                .withStartingRegionId(213)
                .build();

        mockServer.when(request()
                .withMethod("POST")
                .withPath("/updateYaDeliveryShopRules")
                .withBody(json(shopSettings)))
                .respond(response()
                        .withStatusCode(200));

        tested.updateShopSettings(shopSettings);
    }

    /**
     * Тест для реквеста, падающего с 400 (BAD_REQUEST) для
     * {@link HttpDeliveryCalculatorIndexerClient#updateShopDeliveryRules(UpdateShopRulesRequest)}.
     */
    @Test
    void testUpdateShopSettings_badRequest() {
        YaDeliverySenderSettingsDto shopSettings = new YaDeliverySenderSettingsDto.Builder()
                .withCarrierIds(Sets.newHashSet(1L, 2L))
                .build();

        mockServer.when(request()
                .withMethod("POST")
                .withPath("/updateYaDeliveryShopRules")
                .withBody(json(shopSettings)))
                .respond(response()
                        .withStatusCode(400)
                        .withBody(json("{\"message\": \"Shop not found. ShopId = 1\"}")));

        assertThrows(FeignException.BadRequest.class, () ->
                tested.updateShopSettings(shopSettings));
    }

    /**
     * Тест для реквеста, падающего с 500 (BAD_REQUEST) для
     * {@link HttpDeliveryCalculatorIndexerClient#updateShopDeliveryRules(UpdateShopRulesRequest)}.
     */
    @Test
    void testUpdateShopSettings_testRetries() {
        YaDeliverySenderSettingsDto shopSettings = new YaDeliverySenderSettingsDto.Builder()
                .withCarrierIds(Sets.newHashSet(1L, 2L))
                .build();

        mockServer.when(request()
                .withMethod("POST")
                .withPath("/updateYaDeliveryShopRules")
                .withBody(json(shopSettings)), Times.exactly(2))
                .respond(response()
                        .withStatusCode(500)
                        .withBody(json("{\"message\": \"Something bad happened\"}")));

        //1-ый запрос падает с 500. Идет ретрай. После ретрая, так как нет
        //спеификации мока - 404
        assertThrows(FeignException.NotFound.class, () ->
                tested.updateShopSettings(shopSettings));
    }

}
