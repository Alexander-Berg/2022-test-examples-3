package ru.yandex.market.logistics.iris.client;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import org.junit.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.ResponseCreator;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import ru.yandex.market.logistics.iris.client.api.PushApiClient;
import ru.yandex.market.logistics.iris.client.api.PushApiClientImpl;
import ru.yandex.market.logistics.iris.client.http.IrisHttpMethod;
import ru.yandex.market.logistics.iris.client.model.entity.Korobyte;
import ru.yandex.market.logistics.iris.client.model.entity.MeasurementDimensions;
import ru.yandex.market.logistics.iris.client.model.entity.ReferenceItem;
import ru.yandex.market.logistics.iris.client.model.entity.UnitId;
import ru.yandex.market.logistics.iris.client.model.request.PushMeasurementDimensionsRequest;
import ru.yandex.market.logistics.iris.client.model.request.PushMeasurementShelfLifesRequest;
import ru.yandex.market.logistics.iris.client.model.request.PushReferenceItemsRequest;
import ru.yandex.market.logistics.iris.client.utils.TestHttpTemplateImpl;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

public class PushApiClientTest extends AbstractClientTest {

    private final PushApiClient client =
            new PushApiClientImpl(new TestHttpTemplateImpl(uri, restTemplate));

    @Test
    public void onSuccessPush() {
        ResponseCreator responseCreator = withStatus(OK);

        UriComponents requestUri = UriComponentsBuilder.fromHttpUrl(uri)
                .path(IrisHttpMethod.PUSH_API_PREFIX)
                .pathSegment(IrisHttpMethod.REFERENCE_ITEMS)
                .build();

        mockServer.expect(requestTo(requestUri.toUriString()))
                .andExpect(
                        content().json(extractFileContent("fixtures/push_api/reference-item/success_request.json"),
                                false))
                .andExpect(method(HttpMethod.POST))
                .andRespond(responseCreator);

        ReferenceItem referenceItem = ReferenceItem.builder()
                .setUnitId(new UnitId(null, 13412L, "sku_1"))
                .setName("tovar1")
                .setLifetime(47)
                .setUpdatedDateTime(OffsetDateTime.parse("2020-02-14T09:01:55.47+03:00"))
                .build();

        PushReferenceItemsRequest request = new PushReferenceItemsRequest(
                List.of(referenceItem),
                172
        );

        client.pushReferenceItems(request);

        mockServer.verify();
    }


    @Test(expected = HttpClientErrorException.class)
    public void onBadRequest() {
        ResponseCreator responseCreator = withStatus(BAD_REQUEST).body("Invalid request");

        UriComponents requestUri = UriComponentsBuilder.fromHttpUrl(uri)
                .path(IrisHttpMethod.PUSH_API_PREFIX)
                .pathSegment(IrisHttpMethod.REFERENCE_ITEMS)
                .build();

        mockServer.expect(requestTo(requestUri.toUriString()))
                .andExpect(
                        content().json(extractFileContent(
                                "fixtures/push_api/reference-item/request_with_duplicates.json"), false))
                .andExpect(method(HttpMethod.POST))
                .andRespond(responseCreator);

        ReferenceItem referenceItem1 = ReferenceItem.builder()
                .setUnitId(new UnitId(null, 13412L, "sku_1"))
                .setName("tovar1")
                .setLifetime(47)
                .setUpdatedDateTime(OffsetDateTime.parse("2020-02-14T09:01:55.47+03:00"))
                .build();

        ReferenceItem referenceItem2 = ReferenceItem.builder()
                .setUnitId(new UnitId(null, 13412L, "sku_1"))
                .setName("tovar47")
                .setLifetime(555)
                .setUpdatedDateTime(OffsetDateTime.parse("2020-02-14T09:01:55.47+03:00"))
                .build();

        PushReferenceItemsRequest request = new PushReferenceItemsRequest(
                List.of(referenceItem1, referenceItem2),
                172
        );

        client.pushReferenceItems(request);

        mockServer.verify();
    }

    @Test
    public void onSuccessPushMeasureDimensions() {
        ResponseCreator responseCreator = withStatus(OK);

        UriComponents requestUri = UriComponentsBuilder.fromHttpUrl(uri)
                .path(IrisHttpMethod.PUSH_API_PREFIX)
                .pathSegment(IrisHttpMethod.MEASUREMENT_DIMENSIONS)
                .build();

        mockServer.expect(requestTo(requestUri.toUriString()))
                .andExpect(
                        content().json(extractFileContent(
                                "fixtures/push_api/measurement_dimensions/success_measurement_request.json"),
                                false))
                .andExpect(method(HttpMethod.POST))
                .andRespond(responseCreator);

        MeasurementDimensions measurementDimensions = MeasurementDimensions.builder()
                .setUnitId(new UnitId(null, 1L, "sku_1"))
                .setKorobyte(new Korobyte.KorobyteBuilder()
                        .setWidth(BigDecimal.valueOf(10))
                        .setHeight(BigDecimal.valueOf(20))
                        .setLength(BigDecimal.valueOf(30))
                        .setWeightGross(BigDecimal.valueOf(1220))
                        .build())
                .build();

        PushMeasurementDimensionsRequest request = new PushMeasurementDimensionsRequest(
                List.of(measurementDimensions),
                172
        );

        client.pushMeasurementDimensions(request);

        mockServer.verify();
    }

    @Test
    public void onSuccessPushMeasureShelfLifes() {
        ResponseCreator responseCreator = withStatus(OK);

        UriComponents requestUri = UriComponentsBuilder.fromHttpUrl(uri)
                .path(IrisHttpMethod.PUSH_API_PREFIX)
                .pathSegment(IrisHttpMethod.MEASUREMENT_SHELF_LIFES)
                .build();

        mockServer.expect(requestTo(requestUri.toUriString()))
                .andExpect(
                        content().json(extractFileContent(
                                "fixtures/push_api/measurement_shelf_lifes/success_measurement_request.json"),
                                false))
                .andExpect(method(HttpMethod.POST))
                .andRespond(responseCreator);

        PushMeasurementShelfLifesRequest.MeasurementShelfLifes shelfLifes =
                PushMeasurementShelfLifesRequest.MeasurementShelfLifes.builder()
                        .setUnitId(new UnitId(null, 1L, "sku_1"))
                        .setOperatorId("john_doe_666")
                        .setShelfLife(14)
                        .build();

        PushMeasurementShelfLifesRequest request = new PushMeasurementShelfLifesRequest(
                List.of(shelfLifes),
                172
        );

        client.pushMeasurementShelfLifes(request);

        mockServer.verify();
    }
}
