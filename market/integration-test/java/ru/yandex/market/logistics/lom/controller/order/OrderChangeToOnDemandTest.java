package ru.yandex.market.logistics.lom.controller.order;

import java.time.Instant;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.market.common.util.DateTimeUtils;
import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.model.dto.ChangeOrderToOnDemandRequestDto;
import ru.yandex.market.logistics.lom.model.enums.ChangeOrderRequestReason;

import static ru.yandex.market.logistics.lom.utils.TestUtils.NOT_NULL_ERROR_MESSAGE;
import static ru.yandex.market.logistics.lom.utils.TestUtils.fieldValidationErrorMatcher;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Изменение типа заказа на OnDemand")
@ParametersAreNonnullByDefault
@DatabaseSetup("/controller/order/change_to_ondemand/before/setup.xml")
class OrderChangeToOnDemandTest extends AbstractContextualTest {
    private static final Instant FIXED_TIME = Instant.parse("2021-03-10T10:00:00Z");

    @BeforeEach
    void setup() {
        clock.setFixed(FIXED_TIME, DateTimeUtils.MOSCOW_ZONE);
    }

    @Test
    @DisplayName("Успешное создание заявки на изменение")
    @ExpectedDatabase(
        value = "/controller/order/change_to_ondemand/after/change_request_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void successRequest() throws Exception {
        performRequest(validRequestBuilder().build())
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Повторный запрос")
    @DatabaseSetup(
        value = "/controller/order/change_to_ondemand/after/change_request_created.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/controller/order/change_to_ondemand/after/change_request_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void duplicateRequest() throws Exception {
        performRequest(validRequestBuilder().build())
            .andExpect(status().isConflict())
            .andExpect(errorMessage(
                "Active Change Request with type = CHANGE_TO_ON_DEMAND is already exists for order 1001"
            ));
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("validateRequestArguments")
    @DisplayName("Валидация запроса")
    @DatabaseSetup("/controller/order/change_to_ondemand/before/setup.xml")
    void validateRequest(
        String displayName,
        ChangeOrderToOnDemandRequestDto request,
        ResultMatcher resultMatcher
    ) throws Exception {
        performRequest(request)
            .andExpect(status().isBadRequest())
            .andExpect(resultMatcher);
        assertOrderHistoryNeverChanged(1001L);
    }

    @Nonnull
    private static Stream<Arguments> validateRequestArguments() {
        return Stream.of(
            Arguments.of(
                "barcode заказа не указан",
                validRequestBuilder().barcode(null).build(),
                fieldValidationErrorMatcher("barcode", NOT_NULL_ERROR_MESSAGE)
            ),
            Arguments.of(
                "reason не указан",
                validRequestBuilder().reason(null).build(),
                fieldValidationErrorMatcher("reason", NOT_NULL_ERROR_MESSAGE)
            )
        );
    }

    @Nonnull
    private static ChangeOrderToOnDemandRequestDto.ChangeOrderToOnDemandRequestDtoBuilder validRequestBuilder() {
        return ChangeOrderToOnDemandRequestDto.builder()
            .barcode("1001")
            .reason(ChangeOrderRequestReason.SHIPPING_DELAYED)
            .segmentId(2L);
    }

    @Nonnull
    private ResultActions performRequest(ChangeOrderToOnDemandRequestDto request) throws Exception {
        return mockMvc.perform(request(HttpMethod.PUT, "/orders/changeToOnDemand", request));
    }
}
