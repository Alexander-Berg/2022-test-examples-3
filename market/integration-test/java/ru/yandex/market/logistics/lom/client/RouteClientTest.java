package ru.yandex.market.logistics.lom.client;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import ru.yandex.market.logistics.lom.model.dto.CombinatorRoute;
import ru.yandex.market.logistics.lom.model.enums.DeliveryType;
import ru.yandex.market.logistics.lom.model.enums.PointType;
import ru.yandex.market.logistics.lom.model.enums.RoutePointServiceType;
import ru.yandex.market.logistics.lom.model.enums.ServiceCodeName;
import ru.yandex.market.logistics.util.client.exception.HttpTemplateException;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;

@DisplayName("Получение комбинированного маршрута")
class RouteClientTest extends AbstractClientTest {
    @Autowired
    private LomClient lomClient;

    @Test
    @DisplayName("Получить маршрут по штрихкоду заказа")
    void getRouteByOrderBarcode() {
        mock.expect(method(HttpMethod.PUT))
            .andExpect(requestTo(uri + "/routes/findOne"))
            .andExpect(content().json("{\"barcode\":\"abc\"}"))
            .andRespond(
                withSuccess(
                    extractFileContent("response/route/get_route.json"),
                    MediaType.APPLICATION_JSON
                )
            );

        CombinatorRoute actual = lomClient.getRouteByOrderBarcode("abc").orElseThrow(IllegalStateException::new);
        softly.assertThat(actual).usingRecursiveComparison().isEqualTo(expectedRoute());
    }

    @Test
    @DisplayName("Получить маршрут по его идентификатору")
    void getRouteByRouteUuid() {
        UUID routeUuid = UUID.randomUUID();

        mock.expect(method(HttpMethod.GET))
            .andExpect(requestTo(uri + "/routes/by-uuid/" + routeUuid))
            .andRespond(
                withSuccess(
                    extractFileContent("response/route/get_route.json"),
                    MediaType.APPLICATION_JSON
                )
            );

        CombinatorRoute actual = lomClient.getRouteByUuid(routeUuid.toString()).orElseThrow(IllegalStateException::new);
        softly.assertThat(actual).usingRecursiveComparison().isEqualTo(expectedRoute());
    }

    @Test
    @DisplayName("Не существует маршрута с заданным идентификатором")
    void getRouteByUuidMissing() {
        UUID routeUuid = UUID.randomUUID();

        mock.expect(method(HttpMethod.GET))
            .andExpect(requestTo(uri + "/routes/by-uuid/" + routeUuid))
            .andRespond(
                withStatus(HttpStatus.NOT_FOUND).contentType(MediaType.APPLICATION_JSON)
            );

        softly.assertThat(lomClient.getRouteByUuid(routeUuid.toString())).isEmpty();
    }

    @Test
    @DisplayName("Получение маршрута по некорректному идентификатору")
    void getRouteByInvalidUuid() {
        String invalidUuid = "invalidUuid";
        mock.expect(method(HttpMethod.GET))
            .andExpect(requestTo(uri + "/routes/by-uuid/" + invalidUuid))
            .andRespond(
                withStatus(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON)
            );

        softly.assertThatThrownBy(() -> lomClient.getRouteByUuid(invalidUuid))
            .isInstanceOf(HttpTemplateException.class)
            .hasMessage("Http request exception: status <400>, response body <>.");
    }

    @Test
    @DisplayName("Получить маршруты по его идентификаторам")
    void getRoutesByRouteUuids() {
        String routeUuid1 = "b1239cf0-68ab-11ec-8b84-7ba9ffe9841b";
        String routeUuid2 = "bbcfd2cc-68ab-11ec-b62e-bb7a9da8ef97";
        String routeUuid3 = "c154de7c-68ab-11ec-afeb-236e2ea364d5";

        mock.expect(method(HttpMethod.PUT))
            .andExpect(requestTo(uri + "/routes/by-uuids"))
            .andExpect(content().json(extractFileContent("request/route/get_routes.json")))
            .andRespond(
                withSuccess(
                    extractFileContent("response/route/get_routes.json"),
                    MediaType.APPLICATION_JSON
                )
            );

        Map<String, CombinatorRoute> actual = lomClient.getRoutesByUuids(List.of(routeUuid1, routeUuid2, routeUuid3));
        softly.assertThat(actual).usingRecursiveComparison().isEqualTo(Map.of(
            routeUuid1, expectedRoute()
        ));
    }


    @Test
    @DisplayName("Получение маршрутов: есть некорректный идентификатор")
    void getRoutesByInvalidUuids() {
        String routeUuid1 = "b1239cf0-68ab-11ec-8b84-7ba9ffe9841b";
        String invalidUuid = "invalidUuid";

        mock.expect(method(HttpMethod.PUT))
            .andExpect(requestTo(uri + "/routes/by-uuids"))
            .andExpect(content().json(extractFileContent("request/route/get_routes_invalid_uuid.json")))
            .andRespond(
                withStatus(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON)
            );

        softly.assertThatThrownBy(() -> lomClient.getRoutesByUuids(List.of(routeUuid1, invalidUuid)))
            .isInstanceOf(HttpTemplateException.class)
            .hasMessage("Http request exception: status <400>, response body <>.");
    }

    @Test
    @DisplayName("Не существует маршрут по штрихкоду заказа")
    void getRouteByOrderBarcodeMissing() {
        mock.expect(method(HttpMethod.PUT))
            .andExpect(requestTo(uri + "/routes/findOne"))
            .andExpect(content().json("{\"barcode\":\"abc\"}"))
            .andRespond(
                withStatus(HttpStatus.NOT_FOUND).contentType(MediaType.APPLICATION_JSON)
            );

        softly.assertThat(lomClient.getRouteByOrderBarcode("abc")).isEmpty();
    }

    @Nonnull
    private CombinatorRoute expectedRoute() {
        return new CombinatorRoute().setRoute(
            new CombinatorRoute.DeliveryRoute()
                .setCost(new BigDecimal(249))
                .setCostForShop(new BigDecimal(220))
                .setPaths(List.of(
                    new CombinatorRoute.Path().setPointFrom(0).setPointTo(1),
                    new CombinatorRoute.Path().setPointFrom(1).setPointTo(2),
                    new CombinatorRoute.Path().setPointFrom(2).setPointTo(3)
                ))
                .setPoints(List.of(
                    new CombinatorRoute.Point()
                        .setSegmentType(PointType.WAREHOUSE)
                        .setIds(
                            new CombinatorRoute.PointIds()
                                .setRegionId(213)
                                .setLogisticPointId(10001472360L)
                                .setPartnerId(48018L)
                        )
                        .setServices(List.of(
                            new CombinatorRoute.DeliveryService()
                                .setId(2319799L)
                                .setCost(BigDecimal.ZERO)
                                .setCode(ServiceCodeName.CUTOFF)
                                .setType(RoutePointServiceType.INTERNAL)
                                .setItems(List.of(new CombinatorRoute.ProcessedItem().setItemIndex(0).setQuantity(1)))
                                .setDuration(new CombinatorRoute.Timestamp().setSeconds(0).setNanos(0))
                                .setStartTime(new CombinatorRoute.Timestamp().setSeconds(1606856400L).setNanos(0))
                                .setDurationDelta(42)
                                .setDisabledDates(List.of(100, 5)),
                            new CombinatorRoute.DeliveryService()
                                .setId(1984329)
                                .setCost(BigDecimal.ZERO)
                                .setCode(ServiceCodeName.PROCESSING)
                                .setType(RoutePointServiceType.INTERNAL)
                                .setItems(List.of(new CombinatorRoute.ProcessedItem().setItemIndex(0).setQuantity(1)))
                                .setDuration(new CombinatorRoute.Timestamp().setSeconds(86400L).setNanos(0))
                                .setStartTime(new CombinatorRoute.Timestamp().setSeconds(1606892400L).setNanos(0))
                                .setDisabledDates(List.of(1, 5)),
                            new CombinatorRoute.DeliveryService()
                                .setId(1984330L)
                                .setCost(BigDecimal.ZERO)
                                .setCode(ServiceCodeName.SHIPMENT)
                                .setType(RoutePointServiceType.OUTBOUND)
                                .setItems(List.of(new CombinatorRoute.ProcessedItem().setItemIndex(0).setQuantity(1)))
                                .setDuration(new CombinatorRoute.Timestamp().setSeconds(0).setNanos(0))
                                .setStartTime(new CombinatorRoute.Timestamp().setSeconds(1606978800L).setNanos(0))
                        ))
                        .setSegmentId(411960L),
                    new CombinatorRoute.Point()
                        .setSegmentType(PointType.MOVEMENT)
                        .setIds(
                            new CombinatorRoute.PointIds()
                                .setRegionId(0)
                                .setLogisticPointId(0)
                                .setPartnerId(1003937L)
                        )
                        .setServices(List.of(
                            new CombinatorRoute.DeliveryService()
                                .setId(1991956L)
                                .setCost(BigDecimal.ZERO)
                                .setCode(ServiceCodeName.INBOUND)
                                .setType(RoutePointServiceType.INBOUND)
                                .setItems(List.of(new CombinatorRoute.ProcessedItem().setItemIndex(0).setQuantity(1)))
                                .setDuration(new CombinatorRoute.Timestamp().setSeconds(0).setNanos(0))
                                .setStartTime(new CombinatorRoute.Timestamp().setSeconds(1606978800L).setNanos(0)),
                            new CombinatorRoute.DeliveryService()
                                .setId(1991957L)
                                .setCost(BigDecimal.ZERO)
                                .setCode(ServiceCodeName.MOVEMENT)
                                .setType(RoutePointServiceType.INTERNAL)
                                .setItems(List.of(new CombinatorRoute.ProcessedItem().setItemIndex(0).setQuantity(1)))
                                .setDuration(new CombinatorRoute.Timestamp().setSeconds(3600L).setNanos(0))
                                .setStartTime(new CombinatorRoute.Timestamp().setSeconds(1606996800L).setNanos(0))
                        ))
                        .setSegmentId(414118L),
                    new CombinatorRoute.Point()
                        .setSegmentType(PointType.LINEHAUL)
                        .setIds(
                            new CombinatorRoute.PointIds()
                                .setRegionId(225)
                                .setLogisticPointId(0)
                                .setPartnerId(1003937L)
                        )
                        .setServices(List.of(
                            new CombinatorRoute.DeliveryService()
                                .setId(2529617L)
                                .setCost(BigDecimal.ZERO)
                                .setCode(ServiceCodeName.DELIVERY)
                                .setType(RoutePointServiceType.INTERNAL)
                                .setItems(List.of(new CombinatorRoute.ProcessedItem().setItemIndex(0).setQuantity(1)))
                                .setDuration(new CombinatorRoute.Timestamp().setSeconds(86400L).setNanos(0))
                                .setStartTime(new CombinatorRoute.Timestamp().setSeconds(1607000400L).setNanos(0)),
                            new CombinatorRoute.DeliveryService()
                                .setId(2529618L)
                                .setCost(BigDecimal.ZERO)
                                .setCode(ServiceCodeName.LAST_MILE)
                                .setType(RoutePointServiceType.INTERNAL)
                                .setItems(List.of(new CombinatorRoute.ProcessedItem().setItemIndex(0).setQuantity(1)))
                                .setDuration(new CombinatorRoute.Timestamp().setSeconds(0).setNanos(0))
                                .setStartTime(new CombinatorRoute.Timestamp().setSeconds(1607086800L).setNanos(0))
                        ))
                        .setSegmentId(629319L),
                    new CombinatorRoute.Point()
                        .setSegmentType(PointType.HANDING)
                        .setIds(
                            new CombinatorRoute.PointIds()
                                .setRegionId(101461)
                                .setLogisticPointId(0)
                                .setPartnerId(1003937)
                        )
                        .setServices(List.of(
                            new CombinatorRoute.DeliveryService()
                                .setId(2443844L)
                                .setCost(BigDecimal.ZERO)
                                .setCode(ServiceCodeName.HANDING)
                                .setType(RoutePointServiceType.OUTBOUND)
                                .setDeliveryIntervals(List.of(
                                    new CombinatorRoute.DeliveryInterval()
                                        .setFrom(new CombinatorRoute.Time().setHour(9).setMinute(0))
                                        .setTo(new CombinatorRoute.Time().setHour(18).setMinute(0))
                                ))
                                .setItems(List.of(new CombinatorRoute.ProcessedItem().setItemIndex(0).setQuantity(1)))
                                .setDuration(new CombinatorRoute.Timestamp().setSeconds(0).setNanos(0))
                                .setStartTime(new CombinatorRoute.Timestamp().setSeconds(1607407200L).setNanos(0))
                        ))
                        .setSegmentId(596846L)
                ))
                .setTariffId(100217)
                .setDeliveryType(DeliveryType.COURIER)
                .setDateFrom(new CombinatorRoute.Date().setYear(2020).setMonth(12).setDay(8))
                .setDateTo(new CombinatorRoute.Date().setYear(2020).setMonth(12).setDay(8))
        )
            .setOffers(
                List.of(
                    new CombinatorRoute.Offer()
                        .setShopId(583243)
                        .setPartnerId(725559)
                        .setPartnerId(48018)
                        .setAvailableCount(1)
                        .setShopSku("725559")
                )
            )
            .setVirtualBox(
                new CombinatorRoute.Box()
                    .setWeight(300)
                    .setDimensions(List.of(4, 11, 11))
            );
    }

}
