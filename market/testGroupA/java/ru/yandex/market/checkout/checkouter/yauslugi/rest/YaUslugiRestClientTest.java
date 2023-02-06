package ru.yandex.market.checkout.checkouter.yauslugi.rest;

import java.io.IOException;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import ru.yandex.common.util.IOUtils;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.yauslugi.model.ServiceDtoProvider;
import ru.yandex.market.checkout.checkouter.yauslugi.model.YaServiceDto;
import ru.yandex.market.checkout.common.web.CheckoutHttpParameters;

import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * @author zagidullinri
 * @date 20.09.2021
 */
public class YaUslugiRestClientTest extends AbstractWebTestBase {

    @Autowired
    private WireMockServer yaUslugiMock;
    @Autowired
    private YaUslugiClient yaUslugiClient;

    @AfterEach
    public void resetMocks() {
        yaUslugiMock.resetAll();
    }

    @Test
    public void createServiceRequestShouldBeProperlyCreated() {
        yaUslugiMock.resetAll();
        yaUslugiMock.stubFor(
                post(urlPathEqualTo("/ydo/api/market_partner_orders/services"))
                        .willReturn(okJson("{}")));

        yaUslugiMock.addMockServiceRequestListener((request, response) -> {
            String bodyAsString = request.getBodyAsString();
            String expectedJson = readResourceFile("/json/yaUslugiDefaultServiceDto.json");
            JSONAssert.assertEquals(expectedJson, bodyAsString, JSONCompareMode.NON_EXTENSIBLE);

            var serviceTicketHeader = request.getHeaders()
                    .getHeader(CheckoutHttpParameters.SERVICE_TICKET_HEADER);
            assertThat(serviceTicketHeader.isPresent(), equalTo(true));
            var contentTypeHeader = request.getHeaders()
                    .getHeader(HttpHeaders.CONTENT_TYPE);
            assertThat(contentTypeHeader.isPresent(), equalTo(true));
            assertThat(contentTypeHeader.containsValue(MediaType.APPLICATION_JSON_UTF8_VALUE), equalTo(true));
        });

        YaServiceDto yaServiceDto = ServiceDtoProvider.defaultServiceDto();
        yaUslugiClient.create(yaServiceDto);
    }

    private String readResourceFile(String filePath) {
        try {
            return IOUtils.readInputStream(getClass().getResourceAsStream(filePath));
        } catch (IOException e) {
            throw new RuntimeException("Reading resource " + filePath + "failed");
        }
    }
}
