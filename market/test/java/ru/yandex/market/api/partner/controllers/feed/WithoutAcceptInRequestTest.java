package ru.yandex.market.api.partner.controllers.feed;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

import javax.annotation.Nonnull;

import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import ru.yandex.market.api.partner.context.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.util.MbiAsserts;
import ru.yandex.market.mbi.util.io.MbiFiles;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * Проверяем сериализацию
 */
public class WithoutAcceptInRequestTest extends FunctionalTest {

    @Test
    @DbUnitDataSet(before = "outletInfo.before.csv")
    public void doRequestAndCheckResponseTest() throws IOException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpUriRequest httpRequest = outletRequest(10774, 123L, "");

            // Проверяем ответ в XML формате.
            checkXml(httpClient, httpRequest);

            // Проверяем ответ в XML формате .xml.
            httpRequest = outletRequest(10774, 124L, ".xml");
            checkXml(httpClient, httpRequest);

            httpRequest = outletRequest(10774, 125L, ".json");
            // Проверяем ответ в JSON формате c .json .
            checkJson(httpClient, httpRequest);


            //с Accept заголовком
            httpRequest = outletRequest(10774, 126L, "");
            httpRequest.setHeader("Accept", "application/xml");
            checkXml(httpClient, httpRequest);

            httpRequest = outletRequest(10774, 127L, "");
            httpRequest.setHeader("Accept", "application/json");
            checkJson(httpClient, httpRequest);

            httpRequest = outletRequest(10774, 128L, ".xml");
            httpRequest.setHeader("Accept", "application/xml");
            checkXml(httpClient, httpRequest);

            httpRequest = outletRequest(10774, 129L, ".json");
            httpRequest.setHeader("Accept", "application/json");
            checkJson(httpClient, httpRequest);

            httpRequest = outletRequest(10774, 130L, ".json");
            httpRequest.setHeader("Accept", "application/xml");
            checkXml(httpClient, httpRequest);

            httpRequest = outletRequest(10774, 131L, ".xml");
            httpRequest.setHeader("Accept", "application/json");
            checkJson(httpClient, httpRequest);

        }
    }

    @Test
    public void doRoutesRequestAndCheckResponseTest() throws IOException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpUriRequest httpRequest = routesRequest("", null);
            checkOk(httpClient, httpRequest);

            // Проверяем ответ в XML формате .xml.
            httpRequest = routesRequest(".xml", null);
            checkOk(httpClient, httpRequest);

            // Проверяем ответ в JSON формате c .json .
            httpRequest = routesRequest(".json", null);
            checkOk(httpClient, httpRequest);

            //с Accept заголовком
            httpRequest = routesRequest("", new BasicHeader("Accept", "application/xml"));
            checkOk(httpClient, httpRequest);

            httpRequest = routesRequest("", new BasicHeader("Accept", "application/json"));
            checkOk(httpClient, httpRequest);

            httpRequest = routesRequest(".xml", new BasicHeader("Accept", "application/xml"));
            checkOk(httpClient, httpRequest);

            httpRequest = routesRequest(".json", new BasicHeader("Accept", "application/json"));
            checkOk(httpClient, httpRequest);

            httpRequest = routesRequest(".json", new BasicHeader("Accept", "application/xml"));
            checkOk(httpClient, httpRequest);


            httpRequest = routesRequest(".xml", new BasicHeader("Accept", "application/json"));
            checkOk(httpClient, httpRequest);

        }
    }

    private void checkXml(CloseableHttpClient httpClient, HttpUriRequest httpRequest) throws IOException {
        try (CloseableHttpResponse response = httpClient.execute(httpRequest)) {
            String body = MbiFiles.readText(response.getEntity()::getContent, StandardCharsets.UTF_8);
            String expected = "<response><status>OK</status></response>";
            MbiAsserts.assertXmlEquals(expected, body);
            ContentType contentType = ContentType.parse(response.getEntity().getContentType().getValue());
            assertEquals(ContentType.APPLICATION_XML.getMimeType(), contentType.getMimeType());
        }
    }

    private void checkJson(CloseableHttpClient httpClient, HttpUriRequest httpRequest) throws IOException {
        try (CloseableHttpResponse response = httpClient.execute(httpRequest)) {
            String body = MbiFiles.readText(response.getEntity()::getContent, StandardCharsets.UTF_8);
            String expected = "{\"status\":\"OK\"}";
            MbiAsserts.assertJsonEquals(expected, body);
            ContentType contentType = ContentType.parse(response.getEntity().getContentType().getValue());
            assertEquals(ContentType.APPLICATION_JSON.getMimeType(), contentType.getMimeType());
        }
    }

    private void checkOk(CloseableHttpClient httpClient, HttpUriRequest httpRequest) throws IOException {
        try (CloseableHttpResponse response = httpClient.execute(httpRequest)) {
            assertThat(response.getStatusLine().getStatusCode(), equalTo(HttpStatus.OK.value()));
        }
    }

    @Nonnull
    HttpUriRequest outletRequest(long campaignId, long outletId, String format) {
        try {
            URIBuilder uriBuilder = new URIBuilder(url(campaignId, outletId, format));
            HttpUriRequest httpRequest = new HttpDelete(uriBuilder.build());
            httpRequest.setHeader("X-AuthorizationService", "Mock");
            httpRequest.setHeader("Cookie", String.format("yandexuid = %d;", 67282295));
            return httpRequest;
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Expecting to be valid URI", e);
        }
    }

    private String url(long campaignId, long outletId, String str) {
        return String.format("%s/v2/campaigns/%d/outlets/%d%s",
                urlBasePrefix, campaignId, outletId, str);
    }


    @Nonnull
    HttpUriRequest routesRequest(String format, Header acceptHeader) {
        try {
            URIBuilder uriBuilder = new URIBuilder(urlRoutes(format));
            HttpUriRequest httpRequest = new HttpGet(uriBuilder.build());
            httpRequest.setHeader("X-AuthorizationService", "Mock");
            httpRequest.setHeader("Cookie", String.format("yandexuid = %d;", 67282295));
            if (acceptHeader != null) {
                httpRequest.setHeader(acceptHeader);
            }
            return httpRequest;
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Expecting to be valid URI", e);
        }
    }

    private String urlRoutes(String format) {
        return String.format("%s/routes%s?dd3d5ec121c279043f6353549ceb82ca", urlBasePrefix, format);
    }


}
