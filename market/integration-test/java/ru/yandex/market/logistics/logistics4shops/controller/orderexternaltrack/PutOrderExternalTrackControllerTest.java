package ru.yandex.market.logistics.logistics4shops.controller.orderexternaltrack;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.checkout.checkouter.delivery.DeliveryServiceType;
import ru.yandex.market.checkout.checkouter.delivery.tracking.Track;
import ru.yandex.market.logistics.logistics4shops.client.api.OrderExternalTrackApi;
import ru.yandex.market.logistics.logistics4shops.client.api.model.ApiError;
import ru.yandex.market.logistics.logistics4shops.client.api.model.OrderExternalTrackDto;
import ru.yandex.market.logistics.logistics4shops.client.api.model.OrderExternalTrackRequestDto;
import ru.yandex.market.logistics.logistics4shops.client.api.model.ValidationError;
import ru.yandex.market.logistics.logistics4shops.client.api.model.ValidationViolation;
import ru.yandex.market.logistics.logistics4shops.factory.CheckouterFactory;
import ru.yandex.market.logistics.logistics4shops.logging.code.OrderExternalTrackCode;
import ru.yandex.market.logistics.logistics4shops.utils.logging.TskvLogRecord;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static ru.yandex.market.logistics.logistics4shops.client.ResponseSpecBuilders.shouldBeCode;
import static ru.yandex.market.logistics.logistics4shops.client.ResponseSpecBuilders.validatedWith;
import static ru.yandex.market.logistics.logistics4shops.factory.CheckouterFactory.CHECKOUTER_ORDER_ID;
import static ru.yandex.market.logistics.logistics4shops.factory.CheckouterFactory.CHECKOUTER_SHOP_ID;
import static ru.yandex.market.logistics.logistics4shops.utils.logging.BackLogAssertions.logEqualsTo;
import static ru.yandex.market.logistics.logistics4shops.utils.logging.BackLogAssertions.logHasCode;

@DisplayName("Внешний трекинг DBS: сохранение")
@ParametersAreNonnullByDefault
@DatabaseSetup("/controller/orderexternaltrack/put/before/empty_track.xml")
class PutOrderExternalTrackControllerTest extends AbstractOrderExternalTrackControllerTest {

    @Test
    @DisplayName("Успех")
    @ExpectedDatabase(
        value = "/controller/orderexternaltrack/put/after/saved.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void successWithEmptyTrack() throws Exception {
        try (
            var mockGetTrack = mockCheckouterWithoutTracks(CHECKOUTER_ORDER_ID);
            var mockGetOrder = checkouterFactory.mockGetOrder(
                checkouterFactory.shopUserInfo(CHECKOUTER_SHOP_ID),
                CHECKOUTER_ORDER_ID,
                CheckouterFactory.buildOrder(CHECKOUTER_ORDER_ID, CHECKOUTER_SHOP_ID)
            );
            var mockUpdateTrack = mockUpdateTracks(CHECKOUTER_ORDER_ID)
        ) {
            OrderExternalTrackDto orderExternalTrackDto = apiOperation(
                CHECKOUTER_SHOP_ID,
                String.valueOf(CHECKOUTER_ORDER_ID),
                buildRequest()
            )
                .executeAs(validatedWith(shouldBeCode(SC_OK)));

            softly.assertThat(orderExternalTrackDto).isEqualTo(buildDefaultResponse());
        }

        softly.assertThat(tracksCaptor.getValue())
            .containsExactly(expectedDefaultTrack());
    }

    @Test
    @DisplayName("В чекаутере трек отличается от БД")
    @ExpectedDatabase(
        value = "/controller/orderexternaltrack/put/after/saved.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void successWithOverwrite() throws Exception {
        try (var mockGetTrack = mockCheckouter(CHECKOUTER_ORDER_ID)) {
            OrderExternalTrackDto orderExternalTrackDto = apiOperation(
                CHECKOUTER_SHOP_ID,
                String.valueOf(CHECKOUTER_ORDER_ID),
                buildRequest()
            )
                .executeAs(validatedWith(shouldBeCode(SC_OK)));

            softly.assertThat(orderExternalTrackDto).isEqualTo(buildDefaultResponse());
        }

        assertLogs().anyMatch(logEqualsTo(
            TskvLogRecord.info(
                    "Discrepancy between db and checkouter found when setting"
                    + " new order track with deliveryId=123, trackCode=external-track-ds;"
                    + " db: OrderExternalTrack(id=1, deliveryServiceId=null, trackNumber=null),"
                    + " checkouter: OrderExternalTrack(id=null, deliveryServiceId=123, trackNumber=external-track-ds)"
                )
                .setLoggingCode(OrderExternalTrackCode.CHANGING_WITH_DISCREPANCY)
                .setEntities(Map.of("order", List.of("123456")))
        ));
    }

    @Test
    @DisplayName("При сохранении вылетело 4xx из чекаутера")
    @ExpectedDatabase(
        value = "/controller/orderexternaltrack/put/after/not_saved.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void badRequestCheckouter() throws Exception {
        try (
            var mockGetTrack = mockCheckouterWithoutTracks(CHECKOUTER_ORDER_ID);
            var mockGetOrder = checkouterFactory.mockGetOrder(
                checkouterFactory.shopUserInfo(CHECKOUTER_SHOP_ID),
                CHECKOUTER_ORDER_ID,
                CheckouterFactory.buildOrder(CHECKOUTER_ORDER_ID, CHECKOUTER_SHOP_ID)
            );
            var mockUpdateTrack = mockUpdateWithBadRequest()
        ) {
            ApiError apiError = apiOperation(
                CHECKOUTER_SHOP_ID,
                String.valueOf(CHECKOUTER_ORDER_ID),
                buildRequest()
            )
                .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
                .as(ApiError.class);

            softly.assertThat(apiError).isEqualTo(
                new ApiError().message("Cannot update tracks")
            );
        }
    }

    @Test
    @DisplayName("Заказ другого магазина, трек существует")
    @ExpectedDatabase(
        value = "/controller/orderexternaltrack/put/after/not_saved.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void wrongShopWithTrack() {
        ApiError apiError = apiOperation(
            CHECKOUTER_SHOP_ID + 1,
            String.valueOf(CHECKOUTER_ORDER_ID),
            buildRequest()
        )
            .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)))
            .as(ApiError.class);

        softly.assertThat(apiError).isEqualTo(
            new ApiError().message("Failed to find [ORDER] with id [123456]")
        );
    }

    @Test
    @DisplayName("Заказ другого магазина, трек существует только в чекаутере")
    @ExpectedDatabase(
        value = "/controller/orderexternaltrack/put/after/not_saved.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void wrongShopWithCheckouterTrack() throws Exception {
        long shopId = CHECKOUTER_SHOP_ID + 1;
        long orderId = CHECKOUTER_ORDER_ID + 1;
        try (var mockGetTrack = mockCheckouterNotFound(orderId, shopId)) {
            ApiError apiError = apiOperation(shopId, String.valueOf(orderId), buildRequest())
                .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)))
                .as(ApiError.class);

            softly.assertThat(apiError).isEqualTo(
                new ApiError().message("Failed to find [ORDER] with id [123457]")
            );
        }
    }

    @Test
    @DisplayName("Меняем трек на тот же, везде такой же")
    @DatabaseSetup("/controller/orderexternaltrack/put/before/has_track.xml")
    @ExpectedDatabase(
        value = "/controller/orderexternaltrack/put/before/has_track.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void changingToSameNotSynced() throws Exception {
        try (var mockGetTrack = mockCheckouter(CHECKOUTER_ORDER_ID)) {
            OrderExternalTrackDto orderExternalTrackDto = apiOperation(
                CHECKOUTER_SHOP_ID,
                String.valueOf(CHECKOUTER_ORDER_ID),
                buildRequest()
            )
                .executeAs(validatedWith(shouldBeCode(SC_OK)));

            softly.assertThat(orderExternalTrackDto).isEqualTo(buildDefaultResponse());
        }

        assertLogs().noneMatch(logHasCode(OrderExternalTrackCode.CHANGING_WITH_DISCREPANCY));
    }

    @Test
    @DisplayName("Трек не найден в БД, заказ есть в чекаутере без трека")
    @ExpectedDatabase(
        value = "/controller/orderexternaltrack/put/after/saved_other.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void notFoundOrder() throws Exception {
        long orderId = CHECKOUTER_ORDER_ID + 1;
        try (
            var mockGetTrack = mockCheckouterWithoutTracks(orderId);
            var mockGetOrder = checkouterFactory.mockGetOrder(
                checkouterFactory.shopUserInfo(CHECKOUTER_SHOP_ID),
                orderId,
                CheckouterFactory.buildOrder(orderId, CHECKOUTER_SHOP_ID)
            );
            var mockUpdateTrack = mockUpdateTracks(orderId)
        ) {
            OrderExternalTrackDto orderExternalTrackDto = apiOperation(
                CHECKOUTER_SHOP_ID,
                String.valueOf(orderId),
                buildRequest()
            )
                .executeAs(validatedWith(shouldBeCode(SC_OK)));

            softly.assertThat(orderExternalTrackDto).isEqualTo(buildDefaultResponse());
        }

        softly.assertThat(tracksCaptor.getValue())
            .containsExactly(expectedDefaultTrack());

        assertLogs().noneMatch(logHasCode(OrderExternalTrackCode.CHANGING_WITH_DISCREPANCY));
    }

    @Test
    @DisplayName("Трека нет в БД, есть в чекаутере, меняем на такой же")
    @ExpectedDatabase(
        value = "/controller/orderexternaltrack/put/after/saved_other.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void notFoundChangingToSame() throws Exception {
        long orderId = CHECKOUTER_ORDER_ID + 1;
        try (var mockGetTrack = mockCheckouter(orderId)) {
            OrderExternalTrackDto orderExternalTrackDto = apiOperation(
                CHECKOUTER_SHOP_ID,
                String.valueOf(orderId),
                buildRequest()
            )
                .executeAs(validatedWith(shouldBeCode(SC_OK)));

            softly.assertThat(orderExternalTrackDto).isEqualTo(buildDefaultResponse());
        }

        assertLogs().noneMatch(logHasCode(OrderExternalTrackCode.CHANGING_WITH_DISCREPANCY));
    }

    @Test
    @DisplayName("Заказ не найден в чекаутере")
    @ExpectedDatabase(
        value = "/controller/orderexternaltrack/put/after/not_saved.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void notFoundAtAll() throws Exception {
        long orderId = CHECKOUTER_ORDER_ID + 1;
        try (var mockGet = mockCheckouterNotFound(orderId, CHECKOUTER_SHOP_ID)) {
            ApiError apiError = apiOperation(
                CHECKOUTER_SHOP_ID,
                String.valueOf(orderId),
                buildRequest()
            )
                .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)))
                .as(ApiError.class);

            softly.assertThat(apiError).isEqualTo(
                new ApiError().message("Failed to find [ORDER] with id [123457]")
            );
        }
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @DisplayName("Невалидный запрос")
    @ExpectedDatabase(
        value = "/controller/orderexternaltrack/put/after/not_saved.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void invalidRequest(
        @SuppressWarnings("unused") String name,
        String field,
        String fieldError,
        OrderExternalTrackRequestDto request
    ) {
        ValidationError error = apiOperation(
            CHECKOUTER_SHOP_ID,
            String.valueOf(CHECKOUTER_ORDER_ID),
            request
        )
            .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
            .as(ValidationError.class);

        softly.assertThat(error.getErrors()).containsExactly(
            new ValidationViolation()
                .field(field)
                .message(fieldError)
        );
    }

    @Nonnull
    private static Stream<Arguments> invalidRequest() {
        return Stream.of(
            Arguments.of(
                "Отрицательный идентификатор СД",
                "deliveryServiceId",
                "must be positive",
                buildRequest().deliveryServiceId(-1L)
            ),
            Arguments.of(
                "Идентификатор СД = 0",
                "deliveryServiceId",
                "must be positive",
                buildRequest().deliveryServiceId(0L)
            ),
            Arguments.of(
                "Идентификатор СД не передан",
                "deliveryServiceId",
                "must be positive",
                buildRequest().deliveryServiceId(null)
            ),
            Arguments.of(
                "Пустой трек код",
                "trackingNumber",
                "must not be blank",
                buildRequest().trackingNumber(" ")
            ),
            Arguments.of(
                "Трек код не передан",
                "trackingNumber",
                "must not be blank",
                buildRequest().trackingNumber(null)
            )
        );
    }

    @Test
    @DisplayName("Неправильный формат идентификатора заказа")
    @ExpectedDatabase(
        value = "/controller/orderexternaltrack/put/after/not_saved.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void incorrectOrderId() {
        ApiError apiError = apiOperation(
            CHECKOUTER_SHOP_ID,
            "incorrect-id",
            buildRequest()
        )
            .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
            .as(ApiError.class);

        softly.assertThat(apiError).isEqualTo(
            new ApiError().message("Invalid format of id incorrect-id: must be Long")
        );
    }

    @Nonnull
    private OrderExternalTrackApi.PutOrderExternalTrackOper apiOperation(
        Long mbiPartnerId,
        String orderId,
        OrderExternalTrackRequestDto requestDto
    ) {
        return apiClient.orderExternalTrack()
            .putOrderExternalTrack()
            .mbiPartnerIdQuery(mbiPartnerId)
            .orderIdPath(orderId)
            .body(requestDto);
    }

    @Nonnull
    private static Track expectedDefaultTrack() {
        Track track = new Track(CheckouterFactory.DS_TRACK_CODE, CheckouterFactory.DS_ID);
        track.setDeliveryServiceType(DeliveryServiceType.CARRIER);
        return track;
    }

    @Nonnull
    private static OrderExternalTrackRequestDto buildRequest() {
        return new OrderExternalTrackRequestDto()
            .deliveryServiceId(CheckouterFactory.DS_ID)
            .trackingNumber(CheckouterFactory.DS_TRACK_CODE);
    }
}
