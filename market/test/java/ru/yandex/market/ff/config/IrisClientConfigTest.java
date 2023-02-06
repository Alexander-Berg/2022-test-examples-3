package ru.yandex.market.ff.config;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import javax.annotation.Nonnull;

import org.apache.commons.io.IOUtils;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseCreator;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import ru.yandex.market.ff.mvc.handler.RestTemplateResponseErrorHandler;
import ru.yandex.market.logistics.iris.client.api.TrustworthyInfoClient;
import ru.yandex.market.logistics.iris.client.model.entity.Dimensions;
import ru.yandex.market.logistics.iris.client.model.entity.TrustworthyItem;
import ru.yandex.market.logistics.iris.client.model.request.TrustworthyInfoRequest;
import ru.yandex.market.logistics.iris.client.model.response.TrustworthyInfoResponse;
import ru.yandex.market.logistics.iris.client.model.response.TrustworthyItemInfo;
import ru.yandex.market.logistics.iris.core.domain.item.ItemIdentifier;

import static java.lang.ClassLoader.getSystemResourceAsStream;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

public class IrisClientConfigTest extends IrisClientConfig {

    private static final String MOCK_IRIS_HOST = "http://localhost:80";
    private MockRestServiceServer mockServer;
    private TrustworthyInfoClient client;


    @BeforeEach
    public void prepare() {
        errorHandler = new RestTemplateResponseErrorHandler();
        RestTemplate restTemplate = restTemplate();
        mockServer = MockRestServiceServer.createServer(restTemplate);
        client = trustworthyInfoClient(MOCK_IRIS_HOST, restTemplate);
    }

    @Test
    public void trustWorthyClientIsSane() {

        ResponseCreator responseCreator = withStatus(OK)
            .contentType(APPLICATION_JSON)
            .body(extractFileContent("config/iris/trustworthy_info_response.json"));

        mockServer.expect(
            requestTo(UriComponentsBuilder.fromHttpUrl(MOCK_IRIS_HOST).path("/trustworthy_values").toUriString()))
            .andExpect(
                content().json(extractFileContent("config/iris/trustworthy_info_request.json"), false))
            .andExpect(method(HttpMethod.POST))
            .andRespond(responseCreator);

        TrustworthyInfoRequest request = new TrustworthyInfoRequest(
            Lists.newArrayList(ItemIdentifier.of("1", "sku1"), ItemIdentifier.of("1", "sku3")),
            Lists.newArrayList("dimensions")
        );

        TrustworthyInfoResponse response = client.getTrustworthyInfo(request);

        mockServer.verify();
        assertNotNull(response);
        assertNotNull(response.getResult());
        assertEquals(2, response.getResult().size());
        TrustworthyItemInfo first = getBytPartnerIdAndSkuOrThrow("1", "sku1", response);
        TrustworthyItemInfo second = getBytPartnerIdAndSkuOrThrow("1", "sku3", response);
        TrustworthyItem firstTrustworthyItem = first.getTrustworthyItem();
        Dimensions expected = new Dimensions(of(10.20), of(12.23), of(13.37));
        assertEquals(expected, firstTrustworthyItem.getDimensions());
        TrustworthyItem secondTrustworthyItem = second.getTrustworthyItem();
        assertNull(secondTrustworthyItem.getDimensions());
    }

    private TrustworthyItemInfo getBytPartnerIdAndSkuOrThrow(String id, String sku, TrustworthyInfoResponse response) {
        return response.getResult().stream()
            .filter(it -> it.getPartnerSku().equals(sku) && it.getPartnerId().equals(id))
            .findFirst()
            .orElseThrow(() -> new RuntimeException(String.format(
                "Not found item with partner id %s and sku %s", id, sku)));
    }

    public static String extractFileContent(@Nonnull String relativePath) {
        try {
            return IOUtils.toString(
                getSystemResourceAsStream(relativePath),
                StandardCharsets.UTF_8
            );
        } catch (IOException e) {
            throw new RuntimeException("Error during reading from file " + relativePath, e);
        }
    }

    private static BigDecimal of(double value) {
        return BigDecimal.valueOf(value);
    }
}
