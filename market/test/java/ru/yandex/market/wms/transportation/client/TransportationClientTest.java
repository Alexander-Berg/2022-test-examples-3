package ru.yandex.market.wms.transportation.client;

import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.EqualToJsonPattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.wms.common.model.enums.EmptyToteAction;
import ru.yandex.market.wms.common.spring.dao.SortOrder;
import ru.yandex.market.wms.shared.libs.querygenerator.rsql.RsqlOperator;
import ru.yandex.market.wms.transportation.core.domain.TransportOrderSortType;
import ru.yandex.market.wms.transportation.core.findquerygenerator.ApiField;
import ru.yandex.market.wms.transportation.core.findquerygenerator.Expression;
import ru.yandex.market.wms.transportation.core.findquerygenerator.Filter;
import ru.yandex.market.wms.transportation.core.findquerygenerator.Term;
import ru.yandex.market.wms.transportation.core.model.request.PushConsolidationUnitTrackingRequest;
import ru.yandex.market.wms.transportation.core.model.request.TransportOrderCreateRequestBody;
import ru.yandex.market.wms.transportation.core.model.request.TransportOrderUpdateRequestBody;
import ru.yandex.market.wms.transportation.core.model.response.DeleteBatchTransportOrdersResult;
import ru.yandex.market.wms.transportation.core.model.response.GetTransportOrdersResponseWithCursor;
import ru.yandex.market.wms.transportation.core.model.response.Resource;
import ru.yandex.market.wms.transportation.core.model.response.TransportOrderResourceContent;

import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;

class TransportationClientTest extends AbstractClientTest {

    @Autowired
    private TransportationClient transportationClient;

    @Test
    public void successPushTransportUnitTracking() {
        final String testUrl = "/transportation/transport-unit-tracking";

        server.stubFor(WireMock.post(urlEqualTo(testUrl))
                .withRequestBody(
                        new EqualToJsonPattern(
                                extractFileContent("transport-unit-tracking/successful-push-unit-tacking/request.json"),
                                true,
                                true
                        ))
                .willReturn(WireMock.aResponse().withStatus(HttpStatus.OK.value())));

        PushConsolidationUnitTrackingRequest requestBody = readValue(
                "transport-unit-tracking/successful-push-unit-tacking/request.json",
                new TypeReference<PushConsolidationUnitTrackingRequest>() {
                });

        transportationClient.pushConsolidationUnitTracking(requestBody);

        server.verify(postRequestedFor(urlEqualTo(testUrl)));
    }

    @Test
    public void successCreateTransportOrder() {
        final String testUrl = "/transportation/transport-orders";

        server.stubFor(WireMock.post(urlEqualTo(testUrl))
                .withRequestBody(new EqualToJsonPattern(extractFileContent("order/successful-create-transport-order" +
                        "/request.json"), true, true))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody(extractFileContent("order/successful-create-transport-order/response.json"))));

        Resource<TransportOrderResourceContent> expectedResponse = readValue(
                "order/successful-create-transport-order/response.json",
                new TypeReference<Resource<TransportOrderResourceContent>>() {
                }
        );

        TransportOrderCreateRequestBody requestBody = readValue(
                "order/successful-create-transport-order/request.json",
                new TypeReference<TransportOrderCreateRequestBody>() {
                }
        );

        ResponseEntity<Resource<TransportOrderResourceContent>> response =
                transportationClient.createTransportOrder(requestBody);

        assertSoftly(assertions -> {
            assertions.assertThat(response.getBody()).isNotNull();
            assertionsContent(response.getBody().getContent(), expectedResponse.getContent());
        });
        server.verify(postRequestedFor(urlEqualTo(testUrl)));
    }

    @Test
    public void successGetTransportOrder() {
        final String testUrl = "/transportation/transport-orders/6d809e60-d707-11ea-9550-a9553a7b0571";

        server.stubFor(WireMock.get(urlEqualTo(testUrl))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody(extractFileContent("order/successful-get-transport-order/response.json"))));

        Resource<TransportOrderResourceContent> expectedResponse = readValue(
                "order/successful-get-transport-order/response.json",
                new TypeReference<Resource<TransportOrderResourceContent>>() {
                }
        );

        ResponseEntity<Resource<TransportOrderResourceContent>> response = transportationClient.getTransportOrder(
                "6d809e60-d707-11ea-9550-a9553a7b0571");

        assertSoftly(assertions -> {
            assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertions.assertThat(response.getBody()).isNotNull();
            assertionsContent(response.getBody().getContent(), expectedResponse.getContent());
        });
        server.verify(getRequestedFor(urlEqualTo(testUrl)));
    }

    @Test
    public void successUpdateTransportOrder() {
        final String testUrl = "/transportation/transport-orders/6d809e60-d707-11ea-9550-a9553a7b0571";

        server.stubFor(WireMock.put(urlEqualTo(testUrl))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody(extractFileContent("order/successful-update-transport-order/request.json"))));

        TransportOrderUpdateRequestBody requestBody = readValue(
                "order/successful-update-transport-order/request.json",
                new TypeReference<TransportOrderUpdateRequestBody>() {
                }
        );

        transportationClient.updateTransportOrder("6d809e60-d707-11ea-9550-a9553a7b0571", requestBody);

        server.verify(putRequestedFor(urlEqualTo(testUrl)));
    }

    @Test
    public void successGetTransportOrders() {
        final String testUrl = "/transportation/transport-orders?limit=2&cursor=PMRHI4TBNZZXA&sort=STATUS&order=ASC";

        server.stubFor(WireMock.get(urlEqualTo(testUrl))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody(extractFileContent("order/successful-get-transport-orders/response.json"))));

        GetTransportOrdersResponseWithCursor expectedResponse = readValue(
                "order/successful-get-transport-orders/response.json",
                new TypeReference<GetTransportOrdersResponseWithCursor>() {
                }
        );

        GetTransportOrdersResponseWithCursor response = transportationClient.getTransportOrders(
                2,
                "PMRHI4TBNZZXA",
                Filter.emptyFilter(),
                Optional.of(TransportOrderSortType.STATUS),
                Optional.of(SortOrder.ASC)
        );

        assertSoftly(assertions -> {
            assertionsContent(response.getContent(), expectedResponse.getContent());
            assertions.assertThat(response.getCursor().getValue()).isNotNull();
            assertions.assertThat(response.getCursor().getValue()).isEqualTo("PMRHI4TBNZZXA");
        });
        server.verify(getRequestedFor(urlEqualTo(testUrl)));
    }

    @Test
    public void successGetTransportOrdersIfCursorEmpty() {
        final String testUrl = "/transportation/transport-orders?limit=2&cursor=&sort=STATUS&order=ASC";

        server.stubFor(WireMock.get(urlEqualTo(testUrl))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody(extractFileContent("order/successful-get-transport-orders-if-cursor-empty/response" +
                                ".json"))));

        GetTransportOrdersResponseWithCursor expectedResponse = readValue(
                "order/successful-get-transport-orders-if-cursor-empty/response.json",
                new TypeReference<GetTransportOrdersResponseWithCursor>() {
                }
        );

        GetTransportOrdersResponseWithCursor response = transportationClient.getTransportOrders(
                2,
                "",
                Filter.emptyFilter(),
                Optional.of(TransportOrderSortType.STATUS),
                Optional.of(SortOrder.ASC)
        );

        assertSoftly(assertions -> {
            assertionsContent(response.getContent(), expectedResponse.getContent());
            assertions.assertThat(response.getCursor().getValue()).isNotNull();
            assertions.assertThat(response.getCursor().getValue()).isEqualTo("");
        });
        server.verify(getRequestedFor(urlEqualTo(testUrl)));
    }

    @Test
    public void successGetTransportOrdersWithFilter() {
        final String testUrl = "/transportation/transport-orders?limit=2&cursor=PMRHI4TBNZZXA&filter=currentLoc%3D" +
                "%3DCONS01&sort=STATUS&order=ASC";

        server.stubFor(WireMock.get(urlEqualTo(testUrl))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody(extractFileContent("order/successful-get-transport-orders-with-filter/response" +
                                ".json"))));

        GetTransportOrdersResponseWithCursor expectedResponse = readValue(
                "order/successful-get-transport-orders-with-filter/response.json",
                new TypeReference<GetTransportOrdersResponseWithCursor>() {
                }
        );

        GetTransportOrdersResponseWithCursor response = transportationClient.getTransportOrders(
                2,
                "PMRHI4TBNZZXA",
                Filter.of(Expression.of(Term.of(ApiField.ACTUALCELL, RsqlOperator.EQUALS, "CONS01"))),
                Optional.of(TransportOrderSortType.STATUS),
                Optional.of(SortOrder.ASC)
        );

        assertSoftly(assertions -> {
            assertionsContent(response.getContent(), expectedResponse.getContent());
            assertions.assertThat(response.getCursor().getValue()).isNotNull();
            assertions.assertThat(response.getCursor().getValue()).isEqualTo("PMRHI4TBNZZXA");
        });
        server.verify(getRequestedFor(urlEqualTo(testUrl)));
    }

    @Test
    public void successDeleteTransportOrder() {
        final String testUrl = "/transportation/transport-orders/active/6d809e60-d707-11ea-9550-a9553a7b0571";

        server.stubFor(WireMock.delete(urlEqualTo(testUrl))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.OK.value())));

        transportationClient.deleteTransportOrder("6d809e60-d707-11ea-9550-a9553a7b0571");

        server.verify(deleteRequestedFor(urlEqualTo(testUrl)));
    }

    @Test
    public void makeToteEmptyWithLoc() {
        final String testUrl = "/transportation/emptytotes/TM001/makeEmpty";

        server.stubFor(WireMock.put(urlEqualTo(testUrl))
                .withRequestBody(new EqualToJsonPattern(extractFileContent(
                        "emptytotes/make-tote-empty-with-loc/request.json"),
                        true, true))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody(extractFileContent(
                                "emptytotes/make-tote-empty-with-loc/response.json"))));

        Resource<EmptyToteAction> expectedResponse = readValue(
                "emptytotes/make-tote-empty-with-loc/response.json",
                new TypeReference<Resource<EmptyToteAction>>() {
                }
        );

        EmptyToteAction response = transportationClient.makeToteEmpty("TM001", "LOC1");

        assertSoftly(assertions -> {
            assertions.assertThat(response).isNotNull();
            assertions.assertThat(expectedResponse.getContent()).isNotNull();
            assertions.assertThat(response).isEqualTo(expectedResponse.getContent());
        });

        server.verify(putRequestedFor(urlEqualTo(testUrl)));
    }

    @Test
    public void makeToteEmptyWithoutLoc() {
        final String testUrl = "/transportation/emptytotes/TM001/makeEmpty";

        server.stubFor(WireMock.put(urlEqualTo(testUrl))
                .withRequestBody(new EqualToJsonPattern(extractFileContent(
                        "emptytotes/make-tote-empty-without-loc/request.json"),
                        true, true))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody(extractFileContent(
                                "emptytotes/make-tote-empty-without-loc/response.json"))));

        Resource<EmptyToteAction> expectedResponse = readValue(
                "emptytotes/make-tote-empty-without-loc/response.json",
                new TypeReference<Resource<EmptyToteAction>>() {
                }
        );

        EmptyToteAction response = transportationClient.makeToteEmpty("TM001");

        assertSoftly(assertions -> {
            assertions.assertThat(response).isNotNull();
            assertions.assertThat(expectedResponse.getContent()).isNotNull();
            assertions.assertThat(response).isEqualTo(expectedResponse.getContent());
        });

        server.verify(putRequestedFor(urlEqualTo(testUrl)));
    }

    @Test
    public void deleteTransportOrdersBatch() {
        final String testUrl = "/transportation/transport-orders/active";

        server.stubFor(WireMock.delete(urlEqualTo(testUrl))
                .withRequestBody(new EqualToJsonPattern(extractFileContent(
                        "delete-transport-orders-batch/request.json"),
                        true, true))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody(extractFileContent(
                                "delete-transport-orders-batch/response.json"))));

        DeleteBatchTransportOrdersResult expectedResponse = readValue(
                "delete-transport-orders-batch/response.json",
                new TypeReference<DeleteBatchTransportOrdersResult>() {
                }
        );

        DeleteBatchTransportOrdersResult response =
                transportationClient.deleteTransportOrdersBatch(List.of("1", "2", "3"));

        assertSoftly(assertions -> {
            assertions.assertThat(response).isNotNull();
            assertions.assertThat(expectedResponse).isNotNull();
            assertions.assertThat(response).isEqualTo(expectedResponse);
            assertions.assertThat(response.getCancelRowsCount()).isEqualTo(expectedResponse.getCancelRowsCount());
        });

        server.verify(deleteRequestedFor(urlEqualTo(testUrl)));
    }

    private void assertionsContent(
            List<TransportOrderResourceContent> actualContent,
            List<TransportOrderResourceContent> expectedContent
    ) {
        for (int i = 0; i < actualContent.size(); i++) {
            TransportOrderResourceContent actual = actualContent.get(i);
            TransportOrderResourceContent expected = expectedContent.get(i);

            assertSoftly(assertions -> {
                assertions.assertThat(actual).isNotNull();
                assertions.assertThat(expected).isNotNull();
                assertionsContent(actual, expected);
            });
        }
    }

    private void assertionsContent(
            TransportOrderResourceContent actual,
            TransportOrderResourceContent expected
    ) {
        assertSoftly(assertions -> {
            assertions.assertThat(actual).isNotNull();
            assertions.assertThat(expected).isNotNull();

            assertions.assertThat(actual.getId()).isEqualTo(expected.getId());
            assertions.assertThat(actual.getAssignee()).isEqualTo(expected.getAssignee());
            assertions.assertThat(actual.getMovement().getFrom()).isEqualTo(expected.getMovement().getFrom());
            assertions.assertThat(actual.getMovement().getActual()).isEqualTo(expected.getMovement().getActual());
            assertions.assertThat(actual.getPriority()).isEqualTo(expected.getPriority());
            assertions.assertThat(actual.getStatus()).isEqualTo(expected.getStatus());
            assertions.assertThat(actual.getType()).isEqualTo(expected.getType());
            assertions.assertThat(actual.getUnit()).isEqualTo(expected.getUnit());
        });
    }
}
