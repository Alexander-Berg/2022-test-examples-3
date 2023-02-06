package ru.yandex.market.sc.internal.client;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nullable;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.MediaType;
import org.mockserver.model.Parameter;

import ru.yandex.market.request.HttpMethod;
import ru.yandex.market.sc.internal.model.CourierDto;
import ru.yandex.market.sc.internal.model.CreateClientReturnDto;
import ru.yandex.market.sc.internal.model.CreateDemoOrderDto;
import ru.yandex.market.sc.internal.model.InternalSortingCenterDto;
import ru.yandex.market.sc.internal.model.OrderIdDto;
import ru.yandex.market.sc.internal.model.ScIntModelConfiguration;
import ru.yandex.market.tpl.common.util.JacksonUtil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

/**
 * @author valter
 */
class ScIntClientTest extends ClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void createSortingCenterNoParams() {
        var dto = new InternalSortingCenterDto(
                0L, "rest-test", null, null, "rest-test", "rest-test", null, null, false
        );
        mockServer(dto);
        assertThat(scIntClient.createSortingCenter()).isEqualTo(dto);
    }

    @Test
    void createSortingCenterAllParams() {
        var dto = new InternalSortingCenterDto(
                1L, "2", "3", "4", "5", "6", "7", "8", true
        );
        mockServer(dto);
        assertThat(scIntClient.createSortingCenter(dto)).isEqualTo(dto);
    }

    @SneakyThrows
    private void mockServer(InternalSortingCenterDto dto) {
        String json = "{" +
                "\"id\":" + dto.getId() + "" +
                (dto.getAddress() == null ? "" : ",\"address\":\"" + dto.getAddress() + "\"") +
                (dto.getPartnerId() == null ? "" : ",\"partnerId\":\"" + dto.getPartnerId() + "\"") +
                (dto.getLogisticPointId() == null ? "" : ",\"logisticPointId\":\"" + dto.getLogisticPointId() + "\"") +
                (dto.getPartnerName() == null ? "" : ",\"partnerName\":\"" + dto.getPartnerName() + "\"") +
                (dto.getScName() == null ? "" : ",\"scName\":\"" + dto.getScName() + "\"") +
                (dto.getRegionTagSuffix() == null ? "" : ",\"regionTagSuffix\":\"" + dto.getRegionTagSuffix() + "\"") +
                (dto.getToken() == null ? "" : ",\"token\":\"" + dto.getToken() + "\"") +
                ",\"thirdParty\":" + dto.isThirdParty() +
                "}";
        mockServer
                .when(
                        request()
                                .withMethod("POST")
                                .withPath("/manual/sortingCenters")
                                .withContentType(MediaType.APPLICATION_JSON)
                                .withBody(json)
                )
                .respond(
                        response()
                                .withStatusCode(200)
                                .withContentType(MediaType.APPLICATION_JSON)
                                .withBody(json)
                );
    }

    @Test
    void createClientReturn() {
        var dto = new CreateClientReturnDto(
                null,
                "barcode",
                null,
                123L,
                "token",
                null,
                null,
                null
        );
        var response = new OrderIdDto(1, "extId");
        mockServer(dto, response);
        assertThat(scIntClient.createClientReturn(dto)).isEqualTo(response);
    }

    @SneakyThrows
    private void mockServer(CreateClientReturnDto dto, OrderIdDto response) {
        String requestJson = JacksonUtil.toString(dto);
        String responseJson = JacksonUtil.toString(response);
        mockServer
            .when(
                request()
                    .withMethod("POST")
                    .withPath("/internal/courier/clientReturn")
                    .withContentType(MediaType.APPLICATION_JSON)
                    .withBody(requestJson)
            )
            .respond(
                response()
                    .withStatusCode(200)
                    .withContentType(MediaType.APPLICATION_JSON)
                    .withBody(responseJson)
            );
    }

    @Test
    void createDemoOrderNoParams() {
        var dto = new CreateDemoOrderDto(1L);
        String externalOrderId = mockServer(dto);
        assertThat(scIntClient.createDemoOrder(dto)).isEqualTo(externalOrderId);
    }

    @Test
    void testCreateDemoOrderAllParams() {
        var dto = new CreateDemoOrderDto(1L, true, 2, 3L, LocalDate.MIN, "4", "5", "6",
                new CourierDto(7L, "8", "9", "10", "11", "12", null, false), false);
        String externalOrderId = mockServer(dto);
        assertThat(scIntClient.createDemoOrder(dto)).isEqualTo(externalOrderId);
    }

    @SneakyThrows
    private String mockServer(CreateDemoOrderDto dto) {
        String externalOrderId = "mocked-externalOrderId";
        var courierJson = ScIntModelConfiguration.OBJECT_MAPPER.writeValueAsString(dto.getCourierDto());
        var request = request()
                .withMethod("POST")
                .withPath("/manual/orders/createDemo")
                .withContentType(MediaType.APPLICATION_JSON);
        if (dto.getCourierDto() != null) {
            request = request.withBody(courierJson);
        }
        request = putNotRequiredParam(request, "textExternalId", dto.getTextExternalId());
        request = putNotRequiredParam(request, "placesCnt", dto.getPlacesCnt());
        request = putNotRequiredParam(request, "courierId", dto.getCourierId());
        request = putNotRequiredParam(request, "shipmentDate", dto.getShipmentDate());
        request = putNotRequiredParam(request, "warehouseYandexId", dto.getWarehouseYandexId());
        request = putNotRequiredParam(request, "warehousePartnerId", dto.getWarehousePartnerId());
        request = putNotRequiredParam(request, "deliveryServiceYandexId", dto.getDeliveryServiceYandexId());
        request = putNotRequiredParam(request, "isMiddleMile", dto.getIsMiddleMile());

        mockServer
                .when(request)
                .respond(
                        response()
                                .withStatusCode(200)
                                .withContentType(MediaType.APPLICATION_JSON)
                                .withBody("\"" + externalOrderId + "\"")
                );
        return externalOrderId;
    }

    private HttpRequest putNotRequiredParam(HttpRequest request, String name, @Nullable Object value) {
        if (value != null) {
            request = request.withQueryStringParameter(name, Objects.toString(value));
        }
        return request;
    }

    @Test
    void cancelOrder() {
        mockServer("/manual/orders/cancel", HttpMethod.POST,
                List.of(
                        new Parameter("externalOrderId", "1"),
                        new Parameter("scId", "2")
                ));
        assertThatCode(() -> scIntClient.cancelOrder("1", 2)).doesNotThrowAnyException();
    }

    @Test
    void updateShipmentDateForOrder() {
        mockServer("/manual/orders/updateShipmentDate", HttpMethod.POST,
                List.of(
                        new Parameter("externalOrderId", "1"),
                        new Parameter("shipmentDate", "2021-01-02"),
                        new Parameter("scId", "3")
                ));
        assertThatCode(() -> scIntClient.updateShipmentDateForOrder(
                "1", LocalDate.of(2021, 1, 2), 3))
                .doesNotThrowAnyException();
    }

    @Test
    void updateCourierForOrder() {
        mockServer("/manual/orders/updateCourier", HttpMethod.POST,
                List.of(
                        new Parameter("externalOrderId", "1"),
                        new Parameter("scId", "2")
                ),
                "{\"id\":3}");
        assertThatCode(() -> scIntClient.updateCourier("1", new CourierDto(3L), 2))
                .doesNotThrowAnyException();
    }

    @Test
    void acceptOrder() {
        mockServer("/manual/orders/accept", HttpMethod.POST,
                List.of(
                        new Parameter("externalOrderId", "1"),
                        new Parameter("scId", "2")
                ));
        assertThatCode(() -> scIntClient.acceptOrder("1", null, 2))
                .doesNotThrowAnyException();
    }

    @Test
    void acceptOrderWithPlace() {
        mockServer("/manual/orders/accept", HttpMethod.POST,
                List.of(
                        new Parameter("externalOrderId", "1"),
                        new Parameter("externalPlaceId", "2"),
                        new Parameter("scId", "3")
                ));
        assertThatCode(() -> scIntClient.acceptOrder("1", "2", 3))
                .doesNotThrowAnyException();
    }

    @Test
    void sortOrder() {
        mockServer("/manual/orders/sort", HttpMethod.POST,
                List.of(
                        new Parameter("externalOrderId", "1"),
                        new Parameter("cellId", "2"),
                        new Parameter("scId", "3")
                ));
        assertThatCode(() -> scIntClient.sortOrder("1", null, 2, 3))
                .doesNotThrowAnyException();
    }

    @Test
    void sortOrderWithPlace() {
        mockServer("/manual/orders/sort", HttpMethod.POST,
                List.of(
                        new Parameter("externalOrderId", "1"),
                        new Parameter("externalPlaceId", "2"),
                        new Parameter("cellId", "3"),
                        new Parameter("scId", "4")
                ));
        assertThatCode(() -> scIntClient.sortOrder("1", "2", 3, 4))
                .doesNotThrowAnyException();
    }

    @Test
    void prepareToShipOrder() {
        mockServer("/manual/orders/preship", HttpMethod.POST,
                List.of(
                        new Parameter("externalOrderId", "1"),
                        new Parameter("cellId", "2"),
                        new Parameter("routeId", "3"),
                        new Parameter("scId", "4")
                ));
        assertThatCode(() -> scIntClient.prepareToShipOrder(
                "1", null, 2, 3, 4))
                .doesNotThrowAnyException();
    }

    @Test
    void prepareToShipOrderWithPlace() {
        mockServer("/manual/orders/preship", HttpMethod.POST,
                List.of(
                        new Parameter("externalOrderId", "1"),
                        new Parameter("externalPlaceId", "2"),
                        new Parameter("cellId", "3"),
                        new Parameter("routeId", "4"),
                        new Parameter("scId", "5")
                ));
        assertThatCode(() -> scIntClient.prepareToShipOrder(
                "1", "2", 3, 4, 5))
                .doesNotThrowAnyException();
    }

    @Test
    void keepOrder() {
        mockServer("/manual/orders/keep", HttpMethod.POST,
                List.of(
                        new Parameter("externalOrderId", "1"),
                        new Parameter("cellId", "2"),
                        new Parameter("ignoreTodayRoute", "true"),
                        new Parameter("scId", "4")
                ));
        assertThatCode(() -> scIntClient.keepOrder(
                "1", null, 2, true, 4))
                .doesNotThrowAnyException();
    }

    @Test
    void keepOrderWithPlace() {
        mockServer("/manual/orders/keep", HttpMethod.POST,
                List.of(
                        new Parameter("externalOrderId", "1"),
                        new Parameter("externalPlaceId", "2"),
                        new Parameter("cellId", "3"),
                        new Parameter("ignoreTodayRoute", "false"),
                        new Parameter("scId", "4")
                ));
        assertThatCode(() -> scIntClient.keepOrder(
                "1", "2", 3, false, 4))
                .doesNotThrowAnyException();
    }

    @Test
    void shipOrderToCourier() {
        mockServer("/manual/orders/ship", HttpMethod.POST,
                List.of(
                        new Parameter("externalOrderId", "1"),
                        new Parameter("courierId", "2"),
                        new Parameter("scId", "3")
                ));
        assertThatCode(() -> scIntClient.shipOrder(
                "1", null, 2L, null, 3))
                .doesNotThrowAnyException();
    }

    @Test
    void shipOrderToWarehouse() {
        mockServer("/manual/orders/ship", HttpMethod.POST,
                List.of(
                        new Parameter("externalOrderId", "1"),
                        new Parameter("externalPlaceId", "2"),
                        new Parameter("warehouseId", "3"),
                        new Parameter("scId", "4")
                ));
        assertThatCode(() -> scIntClient.shipOrder(
                "1", "2", null, 3L, 4))
                .doesNotThrowAnyException();
    }

    @Test
    void markAsDamagedOrder() {
        mockServer("/manual/orders/markOrderAsDamaged", HttpMethod.POST,
                List.of(
                        new Parameter("externalOrderId", "1"),
                        new Parameter("scId", "2")
                ));
        assertThatCode(() -> scIntClient.markAsDamagedOrder("1", 2)).doesNotThrowAnyException();
    }

    @SuppressWarnings("SameParameterValue")
    private void mockServer(String path, HttpMethod method, List<Parameter> parameters) {
        mockServer(path, method, parameters, null);
    }

    @SuppressWarnings("SameParameterValue")
    private void mockServer(String path, HttpMethod method, List<Parameter> parameters, @Nullable String requestBody) {
        var request = request()
                .withMethod(method.name())
                .withPath(path)
                .withQueryStringParameters(parameters);
        if (requestBody != null) {
            request = request
                    .withBody(requestBody)
                    .withHeader("Content-Type", "application/json");
        }
        mockServer
                .when(request)
                .respond(
                        response()
                                .withStatusCode(200)
                );
    }

}
