package ru.yandex.market.logistics.lom.admin;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.market.logistics.lom.admin.filter.ActionDto;
import ru.yandex.market.logistics.lom.utils.TestUtils;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static ru.yandex.market.logistics.lom.controller.order.TvmClientApiTestUtil.SERVICE_HEADERS;
import static ru.yandex.market.logistics.lom.controller.order.TvmClientApiTestUtil.USER_HEADERS;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.noContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Множественное создание заявок на изменение опций доставки заказов")
@DatabaseSetup("/controller/admin/change-request/before/create.xml")
public class CreateChangeOrderDeliveryOptionRequestsTest extends AbstractCreateChangeOrderRequestTest {

    @DisplayName("Валидация тела запроса")
    @ParameterizedTest(name = "[{index}] {0} {1}")
    @MethodSource("validateRequestArguments")
    void validateRequestDto(String path, String message, Set<Long> ids) throws Exception {
        createChangeOrderDeliveryOptionRequest(ids)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("errors[0].field").value(path))
            .andExpect(jsonPath("errors[0].defaultMessage").value(message));
    }

    @Nonnull
    private static Stream<Arguments> validateRequestArguments() {
        return Stream.of(
            Arguments.of("ids", "must not be empty", null),
            Arguments.of("ids", "must not be empty", Set.of()),
            Arguments.of("ids[]", "must not be null", Collections.singleton(null))
        );
    }

    @Nonnull
    @Override
    ResultActions createChangeOrderRequest(long orderId) throws Exception {
        Set<Long> orderIds = Stream.of(orderId, REQUEST_CAN_BE_CREATED_FOR_ORDER_ID).collect(Collectors.toSet());
        return createChangeOrderDeliveryOptionRequest(orderIds);
    }

    @Nonnull
    @Override
    ResultMatcher changeRequestCreatedResponseBodyMatcher() {
        return noContent();
    }

    @Nonnull
    private ResultActions createChangeOrderDeliveryOptionRequest(Set<Long> orderIds) throws Exception {
        return mockMvc.perform(
            request(
                HttpMethod.POST,
                "/admin/orders/create-change-delivery-option-requests",
                new ActionDto().setIds(orderIds)
            )
                .headers(TestUtils.toHttpHeaders(USER_HEADERS))
                .headers(TestUtils.toHttpHeaders(SERVICE_HEADERS))
        );
    }
}
