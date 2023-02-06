package ru.yandex.market.delivery.mdbclient;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import ru.yandex.market.delivery.mdbclient.model.dbs.AddDbsDeliveryServiceRequest;
import ru.yandex.market.delivery.mdbclient.model.dbs.DbsDeliveryServiceResponse;
import ru.yandex.market.delivery.mdbclient.model.delivery.ItemPlace;
import ru.yandex.market.delivery.mdbclient.model.delivery.Korobyte;
import ru.yandex.market.delivery.mdbclient.model.delivery.OrderDeliveryDate;
import ru.yandex.market.delivery.mdbclient.model.delivery.Place;
import ru.yandex.market.delivery.mdbclient.model.delivery.ResourceId;
import ru.yandex.market.delivery.mdbclient.model.delivery.UnitId;
import ru.yandex.market.delivery.mdbclient.model.dto.DeliveryType;
import ru.yandex.market.delivery.mdbclient.model.dto.OrderToShipDto;
import ru.yandex.market.delivery.mdbclient.model.last_mile.LastMileMappingDto;
import ru.yandex.market.delivery.mdbclient.model.request.CancelStatus;
import ru.yandex.market.delivery.mdbclient.model.request.GetOrderError;
import ru.yandex.market.delivery.mdbclient.model.request.GetOrderResult;
import ru.yandex.market.delivery.mdbclient.model.request.GetOrdersDeliveryDateError;
import ru.yandex.market.delivery.mdbclient.model.request.GetOrdersDeliveryDateResult;
import ru.yandex.market.delivery.mdbclient.model.response.CreateOrderResponse;

class MdbApiRestClientTest extends AbstractRestTest {

    private static final long ORDER_ID = 123L;

    private static final long PARCEL_ID = 126L;

    private static final String STR_PARCEL_ID = "ABC123";

    private static final Long PLATFORM_CLIENT_ID = 1L;

    private static final Long PARTNER_ID = 147L;

    private static final Long UPDATE_REQUEST_ID = 113L;

    private static final String ERROR_MESSAGE = "error message";

    @Test
    void setDsOrderSuccess() {
        executePositiveVoidScenario(
            "orders/" + ORDER_ID + "/lgwSuccess",
            "request/set_lgw_ds_order_success.json",
            () -> mdbClient.setDsOrderSuccess(ORDER_ID, "456", "ABC789")
        );
    }

    @Test
    void setFfOrderSuccess() throws IOException {
        executePositiveScenario(
            "orders/fulfillment/" + ORDER_ID + "/lgwSuccess",
            "request/set_lgw_ff_order_success.json",
            "response/set_lgw_order_success.json",
            CreateOrderResponse.class,
            () -> mdbClient.setFfOrderSuccess(ORDER_ID, "456"),
            (expectedResponse, actualResult) ->
                assertions.assertThat(actualResult)
                    .as("Asserting the response is correct")
                    .usingRecursiveComparison()
                    .isEqualTo(expectedResponse)
        );
    }

    @Test
    void setDsOrderLabel() {
        executePositiveVoidScenario(
            "orders/" + ORDER_ID + "/label",
            "request/set_lgw_order_label.json",
            () -> mdbClient.setDsOrderLabel(ORDER_ID, "456", "http://label.pdf")
        );
    }

    @Test
    void setDsUpdateOrderSuccess() {
        executePositiveVoidScenario(
            "orders/" + ORDER_ID + "/lgwUpdateSuccess",
            "request/set_lgw_update_order_success.json",
            () -> mdbClient.setDsUpdateOrderSuccess(ORDER_ID, "ABC789", Collections.singletonList(createDsPlace()))
        );
    }

    @Test
    void setCreateOrderError() {
        executePositiveVoidBodilessScenario(
            "orders/" + ORDER_ID + "/error",
            () -> mdbClient.setCreateOrderError(ORDER_ID)
        );
    }

    @Test
    void setCreateLgwOrderError() {
        executePositiveVoidScenario(
            "orders/" + ORDER_ID + "/lgwError",
            "request/set_lgw_order_error.json",
            () -> mdbClient.setCreateOrderError(ORDER_ID, PARCEL_ID)
        );
    }

    @Test
    void setFulfilmentOrderCancelSuccess() {
        executePositiveVoidBodilessScenario(
            "orders/" + ORDER_ID + "/cancelFulfillment",
            () -> mdbClient.setFulfillmentOrderCancelResult(ORDER_ID, CancelStatus.SUCCESS)
        );
    }

    @Test
    void setFulfilmentOrderCancelFail() {
        executePositiveVoidBodilessScenario(
            "orders/" + ORDER_ID + "/cancelFulfillment",
            () -> mdbClient.setFulfillmentOrderCancelResult(ORDER_ID, CancelStatus.FAIL)
        );
    }

    @Test
    void setParcelCancelSuccess() {
        executePositiveVoidBodilessScenario(
            "orders/" + ORDER_ID + "/parcels/" + PARCEL_ID + "/cancel",
            () -> mdbClient.setParcelCancelResult(ORDER_ID, PARCEL_ID, CancelStatus.SUCCESS)
        );
    }

    @Test
    void setParcelCancelFail() {
        executePositiveVoidBodilessScenario(
            "orders/" + ORDER_ID + "/parcels/" + PARCEL_ID + "/cancel",
            () -> mdbClient.setParcelCancelResult(ORDER_ID, PARCEL_ID, CancelStatus.FAIL)
        );
    }

    @Test
    void createOrderToShip() throws IOException {
        executePositiveScenario(
            "orderToShip",
            "request/order_to_ship.json",
            "response/order_to_ship.json",
            OrderToShipDto.class,
            () -> mdbClient.createOrderToShip(createOrderToShipDto()),
            (expectedResponse, actualResult) ->
                assertions.assertThat(actualResult)
                    .as("Asserting the response is correct")
                    .usingRecursiveComparison()
                    .isEqualTo(expectedResponse)
        );
    }

    @Test
    void deleteOrderToShip() {
        executePositiveVoidBodilessScenario(
            "orderToShip?id=" + STR_PARCEL_ID + "&platformClientId=" + PLATFORM_CLIENT_ID + "&partnerId=" + PARTNER_ID,
            () -> mdbClient.deleteOrderToShip(STR_PARCEL_ID, PLATFORM_CLIENT_ID, PARTNER_ID)
        );
    }

    @Test
    void setUpdateDeliveryDateSuccess() {
        executePositiveVoidScenario(
            "orders/" + ORDER_ID + "/lgwUpdateDeliveryDateSuccess",
            "request/set_lgw_update_delivery_date_success.json",
            () -> mdbClient.setUpdateDeliveryDateSuccess(ORDER_ID, UPDATE_REQUEST_ID)
        );
    }

    @Test
    void setUpdateDeliveryDateError() {
        executePositiveVoidScenario(
            "orders/" + ORDER_ID + "/lgwUpdateDeliveryDateError",
            "request/set_lgw_update_delivery_date_error.json",
            () -> mdbClient.setUpdateDeliveryDateError(ORDER_ID, UPDATE_REQUEST_ID, ERROR_MESSAGE)
        );
    }

    @Test
    void setGetOrderSuccess() {
        executePositiveVoidScenario(
            "orders/" + ORDER_ID,
            "request/set_lgw_get_order_success.json",
            () -> mdbClient.setGetOrderSuccess(createFfGetOrderResult())
        );
    }

    @Test
    void setGetOrderSuccessWithNullPlace() {
        executePositiveVoidScenario(
            "orders/" + ORDER_ID,
            "request/set_lgw_get_order_success_without_place.json",
            () -> mdbClient.setGetOrderSuccess(createFfGetOrderResultWithoutPlaces())
        );
    }

    @Test
    void setGetOrderError() {
        executePositiveVoidScenario(
            "orders/" + ORDER_ID + "/get/error",
            "request/set_lgw_get_order_error.json",
            () -> mdbClient.setGetOrderError(createFfGetOrderError())
        );
    }

    @Test
    void setGetOrdersDeliveryDateSuccess() {
        executePositiveVoidScenario(
            "orders/getOrdersDeliveryDateSuccess",
            "request/set_get_orders_delivery_date_success.json",
            () -> mdbClient.setGetOrdersDeliveryDateSuccess(createGetOrdersDeliveryDateResult())
        );
    }

    @Test
    void setGetOrdersDeliveryDateError() {
        executePositiveVoidScenario(
            "orders/getOrdersDeliveryDateError",
            "request/set_get_orders_delivery_date_error.json",
            () -> mdbClient.setGetOrdersDeliveryDateError(createGetOrdersDeliveryDateError())
        );
    }

    @Test
    void saveLastMileSettings() throws IOException {
        executePositiveScenario(
            "last-mile",
            "request/add_last_mile.json",
            "request/add_last_mile.json",
            LastMileMappingDto.class,
            () -> mdbClient.addLastMileMapping(new LastMileMappingDto(2L, 3L, false)),
            (expectedResponse, actualResult) -> assertions.assertThat(actualResult).isEqualTo(expectedResponse)
        );
    }

    @Test
    void addDbsDeliveryService() throws IOException {
        executePositiveScenario(
            "dbs",
            "request/add_dbs_delivery_service.json",
            "request/add_dbs_delivery_service.json",
            DbsDeliveryServiceResponse.class,
            () -> mdbClient.addDbsDeliveryService(new AddDbsDeliveryServiceRequest(100500L)),
            (expectedResponse, actualResult) -> assertions.assertThat(actualResult).isEqualTo(expectedResponse)
        );
    }

    private GetOrdersDeliveryDateResult createGetOrdersDeliveryDateResult() {
        ResourceId orderId = new ResourceId("456", "ABC789");
        ZoneOffset offset = ZoneOffset.ofHours(4);
        OffsetDateTime deliveryDate = OffsetDateTime.of(2020, 7, 14, 0, 0, 0, 0, offset);
        OffsetTime deliveryFromTime = OffsetTime.of(8, 0, 0, 0, offset);
        OffsetTime deliveryToTime = OffsetTime.of(12, 0, 0, 0, offset);
        String message = "text";

        return new GetOrdersDeliveryDateResult(
            "process-1",
            1234L,
            Collections.singletonList(
                new OrderDeliveryDate(
                    orderId,
                    deliveryDate,
                    deliveryFromTime,
                    deliveryToTime,
                    message
                )
            )
        );
    }

    private GetOrdersDeliveryDateError createGetOrdersDeliveryDateError() {
        return new GetOrdersDeliveryDateError(
            "process-1",
            1234L,
            Collections.singletonList(new ResourceId("456", "ABC789")),
            "error message"
        );
    }

    private Place createDsPlace() {
        ResourceId placeId = new ResourceId("456", "ABC789");
        Korobyte korobyte = new Korobyte(10, 10, 10, BigDecimal.valueOf(0.25), null, null);
        UnitId itemPlaceId = new UnitId(null, 101112L, "AB-12345");
        ItemPlace itemPlace = new ItemPlace(itemPlaceId, 2);

        return new Place(placeId, korobyte, Collections.emptyList(), Collections.singletonList(itemPlace));
    }

    private GetOrderResult createFfGetOrderResult() {
        return new GetOrderResult("123", 147L, "ABCDEF123456", Collections.singletonList(createFfPlace()));
    }

    private GetOrderResult createFfGetOrderResultWithoutPlaces() {
        return new GetOrderResult("123", 147L, "ABCDEF123456", null);
    }

    private GetOrderError createFfGetOrderError() {
        return new GetOrderError("123", "You have the used parcel, please change to the new");
    }

    private ru.yandex.market.delivery.mdbclient.model.fulfillment.Place createFfPlace() {
        ru.yandex.market.delivery.mdbclient.model.fulfillment.ResourceId placeId =
            new ru.yandex.market.delivery.mdbclient.model.fulfillment.ResourceId("456789", "DEFGHI456789");
        ru.yandex.market.delivery.mdbclient.model.fulfillment.Korobyte korobyte =
            new ru.yandex.market.delivery.mdbclient.model.fulfillment.Korobyte(
                2, 4, 6, BigDecimal.valueOf(123.456789), null, null);
        ru.yandex.market.delivery.mdbclient.model.fulfillment.UnitId unitId =
            new ru.yandex.market.delivery.mdbclient.model.fulfillment.UnitId(
                "place1",
                112L,
                "place1-article-dontchangethisarticleoryourparcelwillbechangedforever" +
                    "-notthatparcelyouthinkabout"
            );
        ru.yandex.market.delivery.mdbclient.model.fulfillment.ItemPlace itemPlace =
            new ru.yandex.market.delivery.mdbclient.model.fulfillment.ItemPlace(unitId, 3);

        return new ru.yandex.market.delivery.mdbclient.model.fulfillment.Place(
            placeId,
            korobyte,
            Collections.singletonList(itemPlace)
        );
    }

    private OrderToShipDto createOrderToShipDto() {
        return new OrderToShipDto("ABC123", 1L, 147L, 1L, 225L,
            DeliveryType.POST, null, LocalDate.of(2019, 6, 30)
        );
    }
}
