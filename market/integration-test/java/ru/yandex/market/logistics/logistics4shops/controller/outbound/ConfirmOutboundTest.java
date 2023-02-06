package ru.yandex.market.logistics.logistics4shops.controller.outbound;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.common.util.DateTimeUtils;
import ru.yandex.market.logistics.logistics4shops.AbstractIntegrationTest;
import ru.yandex.market.logistics.logistics4shops.client.api.OutboundApi;
import ru.yandex.market.logistics.logistics4shops.client.api.model.ApiError;
import ru.yandex.market.logistics.logistics4shops.client.api.model.OutboundConfirmRequest;
import ru.yandex.market.logistics.logistics4shops.client.api.model.ValidationError;
import ru.yandex.market.logistics.logistics4shops.client.api.model.ValidationViolation;
import ru.yandex.market.logistics.logistics4shops.logging.code.OutboundLoggingCode;
import ru.yandex.market.logistics.logistics4shops.utils.logging.TskvLogRecord;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static ru.yandex.market.logistics.logistics4shops.client.ResponseSpecBuilders.shouldBeCode;
import static ru.yandex.market.logistics.logistics4shops.client.ResponseSpecBuilders.validatedWith;
import static ru.yandex.market.logistics.logistics4shops.utils.logging.BackLogAssertions.logEqualsTo;

@DisplayName("Подтверждение отправки")
@ParametersAreNonnullByDefault
@DatabaseSetup("/controller/outbound/confirm/before/prepare.xml")
class ConfirmOutboundTest extends AbstractIntegrationTest {
    private static final Instant NOW = Instant.parse("2022-02-21T14:30:00.00Z");

    @BeforeEach
    void setUp() {
        clock.setFixed(NOW, DateTimeUtils.MOSCOW_ZONE);
    }

    @Test
    @DisplayName("Успешное подтверждение")
    @ExpectedDatabase(
        value = "/controller/outbound/confirm/after/success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void confirmOutbound() {
        apiOperation(buildRequest("1002", "cool-1", Set.of("100102", "100103")))
            .execute(validatedWith(shouldBeCode(SC_OK)));
    }

    @Test
    @DisplayName("Успешное подтверждение, заказ не привязан к отгрузке базе")
    @ExpectedDatabase(
        value = "/controller/outbound/confirm/after/success_with_bind.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void orderNotPresent() {
        apiOperation(buildRequest("1002", "cool-1", Set.of("100102", "100103", "100105")))
            .execute(validatedWith(shouldBeCode(SC_OK)));
        assertLogs().anyMatch(logEqualsTo(backlogNotFoundMessage(List.of("100105"))));
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @DisplayName("Вернуть BAD_REQUEST, если некорректный запрос")
    void exceptionOnIncorrectRequest(
        @SuppressWarnings("unused") String name,
        OutboundConfirmRequest outboundConfirmRequest,
        String field,
        String message
    ) {
        ValidationError error = apiOperation(outboundConfirmRequest)
            .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
            .as(ValidationError.class);

        softly.assertThat(error.getErrors())
            .containsExactly(new ValidationViolation().field(field).message(message));
    }

    @Nonnull
    static Stream<Arguments> exceptionOnIncorrectRequest() {
        return Stream.of(
            Arguments.of(
                "Пустой идентификатор отправки",
                buildRequest(null, "cool-1", Set.of("100102", "100103")),
                "yandexId",
                "must not be null"
            ),
            Arguments.of(
                "Пустое идентификатор отправки в системе партнера",
                buildRequest("1002", null, Set.of("100102", "100103")),
                "externalId",
                "must not be null"
            ),
            Arguments.of(
                "Пустое множество заказов",
                buildRequest("1002", "cool-1", Set.of()),
                "orderIds",
                "size must be between 1 and 2147483647"
            ),
            Arguments.of(
                "Множество заказов не указано",
                buildRequest("1002", "cool-1", null),
                "orderIds",
                "must not be null"
            )
        );
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @DisplayName("Ошибки при обработке запроса")
    void exceptionOnError(
        @SuppressWarnings("unused") String name,
        OutboundConfirmRequest outboundConfirmRequest,
        Integer status,
        String errorMessage,
        @Nullable TskvLogRecord<?> backlogMessage
    ) {
        var error = apiOperation(outboundConfirmRequest)
            .execute(validatedWith(shouldBeCode(status)))
            .as(ApiError.class);
        softly.assertThat(error).isEqualTo(
            new ApiError().message(errorMessage)
        );
        if (backlogMessage != null) {
            assertLogs().anyMatch(logEqualsTo(backlogMessage));
        }
    }

    @Nonnull
    static Stream<Arguments> exceptionOnError() {
        return Stream.of(
            Arguments.of(
                "Отправка не найдена",
                buildRequest("1020", "cool-1", Set.of("100101")),
                SC_NOT_FOUND,
                "Failed to find outbound with yandexId 1020",
                null
            ),
            Arguments.of(
                "Отправка уже подтверждена",
                buildRequest("1001", "cool-1", Set.of("100101")),
                SC_BAD_REQUEST,
                "Outbound 1001 is already confirmed",
                null
            ),
            Arguments.of(
                "null в orderIds",
                buildRequest("1002", "cool-1", Collections.singleton(null)),
                SC_BAD_REQUEST,
                "Invalid outbound request. Violations: [Field 'orderIds' is invalid: must not contain nulls]",
                null
            ),
            Arguments.of(
                "Не все заказы для потверждения присутствуют",
                buildRequest("1002", "cool-1", Set.of("100102", "100103", "100105", "100106")),
                SC_BAD_REQUEST,
                "Can't confirm outbound 1002. Orders [100106] not present",
                backlogNotFoundMessage(List.of("100105", "100106"))
            )
        );
    }

    @Nonnull
    private static TskvLogRecord<String> backlogNotFoundMessage(List<String> orderIds) {
        return TskvLogRecord.info("Orders %s not bound to outbound 1002".formatted(orderIds))
            .setLoggingCode(OutboundLoggingCode.CONFIRM_ORDER_NOT_BOUND_TO_OUTBOUND)
            .setEntities(Map.of(
                "outbound", List.of("1002"),
                "order", orderIds
            ));
    }

    @Nonnull
    private OutboundApi.ConfirmOutboundOper apiOperation(OutboundConfirmRequest request) {
        return apiClient.outbound().confirmOutbound().body(request);
    }

    @Nonnull
    private static OutboundConfirmRequest buildRequest(
        @Nullable String yandexId,
        @Nullable String externalId,
        @Nullable Set<String> orderIds
    ) {
        return new OutboundConfirmRequest()
            .yandexId(yandexId)
            .externalId(externalId)
            .orderIds(orderIds);
    }
}
