package ru.yandex.market.logistics.iris.client;

import java.math.BigDecimal;
import java.util.List;

import org.assertj.core.util.Lists;
import org.junit.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.ResponseCreator;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import ru.yandex.market.logistics.iris.client.api.MeasurementApiClient;
import ru.yandex.market.logistics.iris.client.api.MeasurementApiClientImpl;
import ru.yandex.market.logistics.iris.client.http.IrisHttpMethod;
import ru.yandex.market.logistics.iris.client.model.request.ProbabilityInfoRequest;
import ru.yandex.market.logistics.iris.client.model.request.PutPositiveVerdictRequest;
import ru.yandex.market.logistics.iris.client.model.response.ProbabilityInfoResponse;
import ru.yandex.market.logistics.iris.client.model.response.ProbabilityItemInfo;
import ru.yandex.market.logistics.iris.client.utils.TestHttpTemplateImpl;
import ru.yandex.market.logistics.iris.core.domain.item.ItemIdentifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

public class MeasurementApiClientTest extends AbstractClientTest {

    private final MeasurementApiClient client =
            new MeasurementApiClientImpl(new TestHttpTemplateImpl(uri, restTemplate));

    @Test
    public void onSuccessPut() {
        ResponseCreator responseCreator = withStatus(OK);

        UriComponents requestUri = UriComponentsBuilder.fromHttpUrl(uri)
                .path(IrisHttpMethod.MEASUREMENT_API_PREFIX)
                .pathSegment(IrisHttpMethod.PUT_POSITIVE_VERDICT)
                .build();

        mockServer.expect(requestTo(requestUri.toUriString()))
                .andExpect(
                        content().json(extractFileContent("fixtures/measurement_api/put_positive_verdict/success_request.json"),
                                false))
                .andExpect(method(HttpMethod.POST))
                .andRespond(responseCreator);


        PutPositiveVerdictRequest request = new PutPositiveVerdictRequest(List.of(
                ItemIdentifier.of("1", "sku1"),
                ItemIdentifier.of("1", "sku3")
        ));

        client.putPositiveVerdictOfMeasurement(request);

        mockServer.verify();
    }

    @Test(expected = HttpClientErrorException.class)
    public void onBadRequest() {
        ResponseCreator responseCreator = withStatus(BAD_REQUEST).body("List of identifiers is empty");

        UriComponents requestUri = UriComponentsBuilder.fromHttpUrl(uri)
                .path(IrisHttpMethod.MEASUREMENT_API_PREFIX)
                .pathSegment(IrisHttpMethod.PUT_POSITIVE_VERDICT)
                .build();

        mockServer.expect(requestTo(requestUri.toUriString()))
                .andExpect(
                        content().json(extractFileContent("fixtures/measurement_api/put_positive_verdict/empty_request.json"),
                                false))
                .andExpect(method(HttpMethod.POST))
                .andRespond(responseCreator);


        PutPositiveVerdictRequest request = new PutPositiveVerdictRequest(List.of());

        client.putPositiveVerdictOfMeasurement(request);

        mockServer.verify();
    }

    @Test
    public void getProbabilityMeasurementInfo() {

        UriComponents probabilityRequestUri = UriComponentsBuilder.fromHttpUrl(uri)
                .path(IrisHttpMethod.MEASUREMENT_API_PREFIX)
                .pathSegment(IrisHttpMethod.PROBABILITY_INFO)
                .build();

        ResponseCreator responseCreator = withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(extractFileContent(
                        "fixtures/measurement_api/probability_measurement_info/probability_measurement_info_response.json"));

        mockServer.expect(requestTo(probabilityRequestUri.toUriString()))
                .andExpect(
                        content().json(extractFileContent(
                                "fixtures/measurement_api/probability_measurement_info/probability_measurement_info_request.json"), false))
                .andExpect(method(HttpMethod.POST))
                .andRespond(responseCreator);

        ProbabilityInfoRequest request = new ProbabilityInfoRequest(
                Lists.newArrayList(ItemIdentifier.of("1", "sku1"), ItemIdentifier.of("2", "sku2"))
        );

        ProbabilityInfoResponse response = client.getProbabilityInfo(request);

        mockServer.verify();
        assertNotNull(response);
        assertNotNull(response.getResult());
        assertEquals(2, response.getResult().size());

        ProbabilityItemInfo first = getBytPartnerIdAndSkuOrThrow("1", "sku1", response);
        ProbabilityItemInfo second = getBytPartnerIdAndSkuOrThrow("2", "sku2", response);

        assertEquals(BigDecimal.valueOf(25.5), first.getProbability());
        assertEquals(BigDecimal.valueOf(100), second.getProbability());
    }

    private ProbabilityItemInfo getBytPartnerIdAndSkuOrThrow(String id, String sku, ProbabilityInfoResponse response) {
        return response.getResult().stream()
                .filter(it -> it.getPartnerSku().equals(sku) && it.getPartnerId().equals(id))
                .findFirst()
                .orElseThrow(() -> new RuntimeException(String.format(
                        "Not found item with partner id %s and sku %s", id, sku)));
    }
}
