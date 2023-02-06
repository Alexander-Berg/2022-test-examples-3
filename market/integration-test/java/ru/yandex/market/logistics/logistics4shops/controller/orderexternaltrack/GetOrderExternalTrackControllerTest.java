package ru.yandex.market.logistics.logistics4shops.controller.orderexternaltrack;

import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import ru.yandex.market.logistics.logistics4shops.client.api.OrderExternalTrackApi;
import ru.yandex.market.logistics.logistics4shops.client.api.model.ApiError;
import ru.yandex.market.logistics.logistics4shops.client.api.model.OrderExternalTrackDto;
import ru.yandex.market.logistics.logistics4shops.factory.CheckouterFactory;
import ru.yandex.market.logistics.logistics4shops.logging.code.OrderExternalTrackCode;
import ru.yandex.market.logistics.logistics4shops.utils.logging.TskvLogRecord;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static ru.yandex.market.logistics.logistics4shops.client.ResponseSpecBuilders.shouldBeCode;
import static ru.yandex.market.logistics.logistics4shops.client.ResponseSpecBuilders.validatedWith;
import static ru.yandex.market.logistics.logistics4shops.utils.logging.BackLogAssertions.logEqualsTo;
import static ru.yandex.market.logistics.logistics4shops.utils.logging.BackLogAssertions.logHasCode;

@DisplayName("Внешний трекинг DBS: получение")
@ParametersAreNonnullByDefault
class GetOrderExternalTrackControllerTest extends AbstractOrderExternalTrackControllerTest {

    @Test
    @DisplayName("Успех, получаем трек из чекаутера, сохраняем в БД")
    @ExpectedDatabase(
        value = "/controller/orderexternaltrack/get/after/saved.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void successFromCheckouter() throws Exception {
        try (var mockGet = mockCheckouter()) {
            OrderExternalTrackDto orderExternalTrackDto = apiOperation(
                String.valueOf(CheckouterFactory.CHECKOUTER_ORDER_ID),
                CheckouterFactory.CHECKOUTER_SHOP_ID
            )
                .executeAs(validatedWith(shouldBeCode(SC_OK)));

            softly.assertThat(orderExternalTrackDto).isEqualTo(buildDefaultResponse());
            assertLogs().noneMatch(logHasCode(OrderExternalTrackCode.OVERWRITE_EXISTING));
        }
    }

    @Test
    @DisplayName("Есть трек в БД")
    @DatabaseSetup("/controller/orderexternaltrack/get/before/setup.xml")
    @ExpectedDatabase(
        value = "/controller/orderexternaltrack/get/before/setup.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void successWithSavedTrack() throws Exception {
        try (var mockGet = mockCheckouter()) {
            OrderExternalTrackDto orderExternalTrackDto = apiOperation(
                String.valueOf(CheckouterFactory.CHECKOUTER_ORDER_ID),
                CheckouterFactory.CHECKOUTER_SHOP_ID
            )
                .executeAs(validatedWith(shouldBeCode(SC_OK)));

            softly.assertThat(orderExternalTrackDto).isEqualTo(buildDefaultResponse());
            assertLogs().noneMatch(logHasCode(OrderExternalTrackCode.OVERWRITE_EXISTING));
        }
    }

    @Test
    @DisplayName("Есть трек, но в чекаутере другая служба")
    @DatabaseSetup("/controller/orderexternaltrack/get/before/setup_different_ds.xml")
    @ExpectedDatabase(
        value = "/controller/orderexternaltrack/get/after/saved.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void overwriteDifferentDS() throws Exception {
        try (var mockGet = mockCheckouter()) {
            OrderExternalTrackDto orderExternalTrackDto = apiOperation(
                String.valueOf(CheckouterFactory.CHECKOUTER_ORDER_ID),
                CheckouterFactory.CHECKOUTER_SHOP_ID
            )
                .executeAs(validatedWith(shouldBeCode(SC_OK)));

            softly.assertThat(orderExternalTrackDto).isEqualTo(buildDefaultResponse());
            assertLogs().anyMatch(logEqualsTo(
                TskvLogRecord.info(
                        "Existing track OrderExternalTrack(id=1, deliveryServiceId=321, trackNumber=external-track-ds) "
                        + "were overwritten with other from checkouter "
                        + "OrderExternalTrack(id=null, deliveryServiceId=123, trackNumber=external-track-ds)"
                    )
                    .setLoggingCode(OrderExternalTrackCode.OVERWRITE_EXISTING)
                    .setEntities(Map.of("order", List.of("123456")))
            ));
        }
    }

    @Test
    @DisplayName("Есть трек, но в чекаутере другой код трекинга")
    @DatabaseSetup("/controller/orderexternaltrack/get/before/setup_different_track.xml")
    @ExpectedDatabase(
        value = "/controller/orderexternaltrack/get/after/saved.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void overwriteDifferentTrackingCode() throws Exception {
        try (var mockGet = mockCheckouter()) {
            OrderExternalTrackDto orderExternalTrackDto = apiOperation(
                String.valueOf(CheckouterFactory.CHECKOUTER_ORDER_ID),
                CheckouterFactory.CHECKOUTER_SHOP_ID
            )
                .executeAs(validatedWith(shouldBeCode(SC_OK)));

            softly.assertThat(orderExternalTrackDto).isEqualTo(buildDefaultResponse());
            assertLogs().anyMatch(logEqualsTo(
                TskvLogRecord.info(
                        "Existing track OrderExternalTrack(id=1, deliveryServiceId=123, trackNumber=another-track-ds) "
                        + "were overwritten with other from checkouter "
                        + "OrderExternalTrack(id=null, deliveryServiceId=123, trackNumber=external-track-ds)"
                    )
                    .setLoggingCode(OrderExternalTrackCode.OVERWRITE_EXISTING)
                    .setEntities(Map.of("order", List.of("123456")))
            ));
        }
    }

    @Test
    @DisplayName("Не найдено треков, с походом в чекаутер")
    @ExpectedDatabase(
        value = "/controller/orderexternaltrack/get/after/not_saved.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void trackNotFoundCheckouter() throws Exception {
        try (var mockGet = mockCheckouterWithoutTracks()) {
            OrderExternalTrackDto orderExternalTrackDto = apiOperation(
                String.valueOf(CheckouterFactory.CHECKOUTER_ORDER_ID),
                CheckouterFactory.CHECKOUTER_SHOP_ID
            )
                .executeAs(validatedWith(shouldBeCode(SC_OK)));

            softly.assertThat(orderExternalTrackDto).isEqualTo(new OrderExternalTrackDto());
            assertLogs().noneMatch(logHasCode(OrderExternalTrackCode.OVERWRITE_EXISTING));
        }
    }

    @Test
    @DisplayName("Трек есть в БД, но удален в чекаутере")
    @DatabaseSetup("/controller/orderexternaltrack/get/before/setup.xml")
    @ExpectedDatabase(
        value = "/controller/orderexternaltrack/get/after/not_found_track.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void deletedInCheckouterAfterPush() throws Exception {
        try (var mockGet = mockCheckouterWithoutTracks()) {
            OrderExternalTrackDto orderExternalTrackDto = apiOperation(
                String.valueOf(CheckouterFactory.CHECKOUTER_ORDER_ID),
                CheckouterFactory.CHECKOUTER_SHOP_ID
            )
                .executeAs(validatedWith(shouldBeCode(SC_OK)));

            softly.assertThat(orderExternalTrackDto).isEqualTo(new OrderExternalTrackDto());
            assertLogs().anyMatch(logEqualsTo(
                TskvLogRecord.info(
                        "Existing track OrderExternalTrack(id=1, deliveryServiceId=123, trackNumber=external-track-ds) "
                        + "were overwritten with other from checkouter "
                        + "OrderExternalTrack(id=null, deliveryServiceId=null, trackNumber=null)"
                    )
                    .setLoggingCode(OrderExternalTrackCode.OVERWRITE_EXISTING)
                    .setEntities(Map.of("order", List.of("123456")))
            ));
        }
    }

    @Test
    @DisplayName("Неправильный формат идентификатора заказа при походе в чекаутер")
    @ExpectedDatabase(
        value = "/controller/orderexternaltrack/get/after/not_saved.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void incorrectOrderId() {
        ApiError apiError = apiOperation(
            "incorrect-id",
            CheckouterFactory.CHECKOUTER_SHOP_ID
        )
            .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
            .as(ApiError.class);

        softly.assertThat(apiError).isEqualTo(
            new ApiError().message("Invalid format of id incorrect-id: must be Long")
        );
    }

    @Test
    @DisplayName("Есть трек в БД, логика финальная")
    @DatabaseSetup("/controller/orderexternaltrack/get/before/setup.xml")
    @ExpectedDatabase(
        value = "/controller/orderexternaltrack/get/before/setup.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void successFinalLogic() {
        setupLogic(true);
        OrderExternalTrackDto orderExternalTrackDto = apiOperation(
            String.valueOf(CheckouterFactory.CHECKOUTER_ORDER_ID),
            CheckouterFactory.CHECKOUTER_SHOP_ID
        )
            .executeAs(validatedWith(shouldBeCode(SC_OK)));

        softly.assertThat(orderExternalTrackDto).isEqualTo(buildDefaultResponse());
        assertLogs().noneMatch(logHasCode(OrderExternalTrackCode.OVERWRITE_EXISTING));
    }

    @Test
    @DisplayName("Трек чужого магазина, логика финальная")
    @DatabaseSetup("/controller/orderexternaltrack/get/before/setup.xml")
    @ExpectedDatabase(
        value = "/controller/orderexternaltrack/get/before/setup.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void wrongShopFinalLogic() {
        setupLogic(true);
        ApiError apiError = apiOperation(
            String.valueOf(CheckouterFactory.CHECKOUTER_ORDER_ID),
            CheckouterFactory.CHECKOUTER_SHOP_ID + 1
        )
            .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)))
            .as(ApiError.class);

        softly.assertThat(apiError).isEqualTo(
            new ApiError().message("Failed to find [ORDER] with id [123456]")
        );
    }

    @Test
    @DisplayName("Трек не найден, логика финальная")
    void successEmptyFinalLogic() {
        setupLogic(true);
        OrderExternalTrackDto orderExternalTrackDto = apiOperation(
            String.valueOf(CheckouterFactory.CHECKOUTER_ORDER_ID),
            CheckouterFactory.CHECKOUTER_SHOP_ID
        )
            .executeAs(validatedWith(shouldBeCode(SC_OK)));

        softly.assertThat(orderExternalTrackDto).isEqualTo(new OrderExternalTrackDto());
        assertLogs().noneMatch(logHasCode(OrderExternalTrackCode.OVERWRITE_EXISTING));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("Заказ другого магазина, проверка в БД")
    @DatabaseSetup("/controller/orderexternaltrack/get/before/setup.xml")
    @ExpectedDatabase(
        value = "/controller/orderexternaltrack/get/before/setup.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void wrongShopNotSynced(boolean useFinalLogic) {
        setupLogic(useFinalLogic);
        ApiError apiError = apiOperation(
            String.valueOf(CheckouterFactory.CHECKOUTER_ORDER_ID),
            CheckouterFactory.CHECKOUTER_SHOP_ID + 1
        )
            .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)))
            .as(ApiError.class);

        softly.assertThat(apiError).isEqualTo(
            new ApiError().message("Failed to find [ORDER] with id [123456]")
        );
    }

    @Test
    @DisplayName("Заказ другого магазина, проверка чекаутером")
    void wrongShopSynced() throws Exception {
        try (var mockGet = mockCheckouterNotFound()) {
            ApiError apiError = apiOperation(
                String.valueOf(CheckouterFactory.CHECKOUTER_ORDER_ID),
                CheckouterFactory.CHECKOUTER_SHOP_ID
            )
                .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)))
                .as(ApiError.class);

            softly.assertThat(apiError).isEqualTo(
                new ApiError().message("Failed to find [ORDER] with id [123456]")
            );
        }
    }

    @Nonnull
    private OrderExternalTrackApi.GetOrderExternalTrackOper apiOperation(String orderId, long shopId) {
        return apiClient.orderExternalTrack()
            .getOrderExternalTrack()
            .mbiPartnerIdQuery(shopId)
            .orderIdPath(orderId);
    }
}
