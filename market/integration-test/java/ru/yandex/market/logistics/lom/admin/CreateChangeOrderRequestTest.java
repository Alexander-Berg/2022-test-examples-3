package ru.yandex.market.logistics.lom.admin;

import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.market.logistics.lom.admin.dto.ChangeOrderRequestCreateDto;
import ru.yandex.market.logistics.lom.entity.enums.ChangeOrderRequestType;
import ru.yandex.market.logistics.lom.utils.TestUtils;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static ru.yandex.market.logistics.lom.controller.order.TvmClientApiTestUtil.SERVICE_HEADERS;
import static ru.yandex.market.logistics.lom.controller.order.TvmClientApiTestUtil.USER_HEADERS;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Создание заявки на изменение опций доставки")
@DatabaseSetup("/controller/admin/change-request/before/create.xml")
public class CreateChangeOrderRequestTest extends AbstractCreateChangeOrderRequestTest {

    @Test
    @DisplayName("Получить объект для создания заявки")
    void getNewDto() throws Exception {
        mockMvc.perform(get("/admin/change-order-requests/new"))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/admin/change-request/response/order/get_new_dto.json"));
    }

    @DisplayName("Валидация тела запроса")
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("validateRequestArguments")
    void validateRequestDto(
        String path,
        UnaryOperator<ChangeOrderRequestCreateDto.ChangeOrderRequestCreateDtoBuilder> requestBuilderModifier
    ) throws Exception {
        createChangeOrderRequest(
            requestBuilderModifier.apply(
                ChangeOrderRequestCreateDto.builder()
                    .orderId(1L)
                    .requestType(ChangeOrderRequestType.DELIVERY_OPTION)
            )
                .build()
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("errors[0].field").value(path))
            .andExpect(jsonPath("errors[0].defaultMessage").value("must not be null"));
    }

    @Nonnull
    private static Stream<Arguments> validateRequestArguments() {
        return Stream.<Pair<String, UnaryOperator<ChangeOrderRequestCreateDto.ChangeOrderRequestCreateDtoBuilder>>>of(
            Pair.of("requestType", b -> b.requestType(null)),
            Pair.of("orderId", b -> b.orderId(null))
        )
            .map(pair -> Arguments.of(pair.getLeft(), pair.getRight()));
    }

    @DisplayName("Создание заявки этого типа не поддержано")
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @EnumSource(value = ChangeOrderRequestType.class, names = "DELIVERY_OPTION", mode = EnumSource.Mode.EXCLUDE)
    @ExpectedDatabase(
        value = "/controller/admin/change-request/before/create.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void unsupportedChangeRequestType(ChangeOrderRequestType requestType) throws Exception {
        createChangeOrderRequest(0, requestType)
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage(String.format(
                "Создание заявок типов [%s] не поддержано. " +
                    "Можно создавать только заявки следующих типов: [DELIVERY_OPTION]",
                requestType
            )));
    }

    @Nonnull
    @Override
    ResultActions createChangeOrderRequest(long orderId) throws Exception {
        return createChangeOrderRequest(orderId, ChangeOrderRequestType.DELIVERY_OPTION);
    }

    @Nonnull
    @Override
    ResultMatcher changeRequestCreatedResponseBodyMatcher() {
        return content().string("3");
    }

    @Nonnull
    private ResultActions createChangeOrderRequest(long orderId, ChangeOrderRequestType requestType) throws Exception {
        return createChangeOrderRequest(
            ChangeOrderRequestCreateDto.builder().orderId(orderId).requestType(requestType).build()
        );
    }

    @Nonnull
    private ResultActions createChangeOrderRequest(ChangeOrderRequestCreateDto requestBody) throws Exception {
        return mockMvc.perform(
            request(HttpMethod.POST, "/admin/change-order-requests", requestBody)
                .headers(TestUtils.toHttpHeaders(USER_HEADERS))
                .headers(TestUtils.toHttpHeaders(SERVICE_HEADERS))
        );
    }
}
