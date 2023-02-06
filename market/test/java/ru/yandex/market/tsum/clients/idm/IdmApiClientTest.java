package ru.yandex.market.tsum.clients.idm;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.gson.Gson;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.request.netty.WrongStatusCodeException;
import ru.yandex.misc.io.http.HttpStatus;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

public class IdmApiClientTest {
    Gson gson = new Gson();

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(WireMockConfiguration.wireMockConfig().dynamicPort());

    @Test
    public void badRequestTest() throws InterruptedException {
        IdmRequestRoleRequest badRequest = new IdmRequestRoleRequest("wrongName", "rolePath", "system");
        String url = "https://idm-api.yandex-team.ru/api/v1/rolerequests/";
        String payload = gson.toJson(badRequest);
        String response = String.format("{\"error_code\": \"BAD_REQUEST\", \"errors\": {\"user\": [\"Пользователь " +
            "%s не найден\"]}, \"message\": \"Invalid data sent\"} ", badRequest.getUser());
        String message = String.format("url :%s\npayload %s\nresponse: %s", url, payload, response);
        WrongStatusCodeException responseException = new WrongStatusCodeException(
            HttpStatus.SC_400_BAD_REQUEST, message, "https://idm-api.yandex-team.ru/api/v1/rolerequests/", payload
        );
        IdmApiClient idmApiClient = Mockito.mock(IdmApiClient.class);
        Mockito.doThrow(responseException)
            .when(idmApiClient)
            .requestRole(badRequest);
        try {
            idmApiClient.requestRole(badRequest);
            Assert.fail();
        } catch (WrongStatusCodeException wrongStatusCodeException) {
            Assert.assertEquals(wrongStatusCodeException, responseException);
        }
    }

    @Test
    public void goodRequestTest() throws InterruptedException {
        IdmApiClient idmApiClient = new IdmApiClient(null, "token", "http://localhost:" + wireMockRule.port());
        IdmRequestRoleRequest goodRequest = new IdmRequestRoleRequest("name", "rolePath", "system");
        wireMockRule.stubFor(post(urlEqualTo("/rolerequests/"))
            .withHeader("Authorization", containing("OAuth token"))
            .withHeader("Content-Type", containing("application/json"))
            .withRequestBody(equalToJson(gson.toJson(goodRequest)))
            .willReturn(aResponse()
                .withStatus(201)));
        idmApiClient.requestRole(goodRequest);
    }
}
