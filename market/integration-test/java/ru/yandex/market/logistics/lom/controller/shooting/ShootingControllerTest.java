package ru.yandex.market.logistics.lom.controller.shooting;

import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.market.logistics.lom.AbstractContextualTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static ru.yandex.market.logistics.lom.utils.TestUtils.validationErrorsJsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

public class ShootingControllerTest extends AbstractContextualTest {

    @Nonnull
    private static Stream<Arguments> getOrdersProcessingStatusInvalidTestArgument() {
        return Stream.of(
            Arguments.of(
                "Получение статуса стрельбы без начальной даты создания заказа",
                "controller/shooting/request/order-processing-status-wo-created-from.json",
                validationErrorsJsonContent("orderCreatedFrom", "must not be null")
            ),
            Arguments.of(
                "Получение статуса стрельбы c невалидной начальной датой создания заказа",
                "controller/shooting/request/order-processing-status-with-invalid-created-from.json",
                errorMessage("Text 'invalid-date' could not be parsed at index 0")
            ),
            Arguments.of(
                "Получение статуса стрельбы без конечной даты создания заказа",
                "controller/shooting/request/order-processing-status-wo-created-to.json",
                validationErrorsJsonContent("orderCreatedTo", "must not be null")
            ),
            Arguments.of(
                "Получение статуса стрельбы c невалидной конечной датой создания заказа",
                "controller/shooting/request/order-processing-status-with-invalid-created-to.json",
                errorMessage("Text 'invalid-date' could not be parsed at index 0")
            ),
            Arguments.of(
                "Получение статуса стрельбы без uid, uidRangeLowerBound",
                "controller/shooting/request/order-processing-status-wo-uid-and-lower-uid.json",
                errorMessage("uid or uidRangeLowerBound and uidRangeUpperBound must not be null")
            ),
            Arguments.of(
                "Получение статуса стрельбы без uid, uidRangeUpperBound",
                "controller/shooting/request/order-processing-status-wo-uid-and-upper-uid.json",
                errorMessage("uid or uidRangeLowerBound and uidRangeUpperBound must not be null")
            )
        );
    }

    @Nonnull
    private static Stream<Arguments> getOrdersForCancellingPageArguments() {
        return Stream.of(
            Arguments.of(
                "0",
                "2",
                "controller/shooting/response/orders-ready-to-cancel-first-page.json"
            ),
            Arguments.of(
                "1",
                "2",
                "controller/shooting/response/orders-ready-to-cancel-second-page.json"
            ),
            Arguments.of(
                "0",
                "4",
                "controller/shooting/response/orders-ready-to-cancel-full-page.json"
            )
        );
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("getOrdersProcessingStatusInvalidTestArgument")
    @DisplayName("Ошибка валидации при запросе статуса обработки заказов")
    @DatabaseSetup("/controller/shooting/init/setup-order-processing-status-invalid.xml")
    void getOrdersProcessingStatusInvalidTest(
        String displayName,
        String request,
        ResultMatcher errorMatcher
    ) throws Exception {
        mockMvc.perform(put("/shooting/orders-processing-status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent(request))
            )
            .andExpect(status().isBadRequest())
            .andExpect(errorMatcher);
    }

    @Test
    @DisplayName("Успешный запрос статуса обработки заказов")
    @DatabaseSetup("/controller/shooting/init/setup-order-processing-status.xml")
    void getOrdersProcessingStatusTest() throws Exception {
        mockMvc.perform(put("/shooting/orders-processing-status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent("controller/shooting/request/order-processing-status.json")))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/shooting/response/order-processing-status.json"));
    }

    @Test
    @DisplayName("Получаем orderId, которые можно отменять (фильтруем по дате)")
    @DatabaseSetup("/controller/shooting/init/setup-order-ready-to-cancel-filter-by-date.xml")
    void getOrderIdsForCancellingFilterByDate() throws Exception {
        mockMvc.perform(post("/shooting/orders-ready-to-cancel")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent(
                    "controller/shooting/request/order-ready-to-cancel-filter-by-date.json"
                ))
            )
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/shooting/response/orders-ready-to-cancel-filter-by-date.json"));
    }

    @Test
    @DisplayName("Получаем orderId, которые можно отменять (фильтруем по статусу)")
    @DatabaseSetup("/controller/shooting/init/setup-order-ready-to-cancel-filter-by-status.xml")
    void getOrderIdsForCancellingFilterByStatus() throws Exception {
        mockMvc.perform(post("/shooting/orders-ready-to-cancel")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent(
                    "controller/shooting/request/order-ready-to-cancel-filter-by-status.json"
                ))
            )
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/shooting/response/orders-ready-to-cancel-filter-by-status.json"));
    }

    @Test
    @DisplayName("Получаем orderId, которые можно отменять (фильтруем по uid)")
    @DatabaseSetup("/controller/shooting/init/setup-order-ready-to-cancel-filter-by-uid.xml")
    void getOrderIdsForCancellingFilterByUid() throws Exception {
        mockMvc.perform(post("/shooting/orders-ready-to-cancel")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent(
                    "controller/shooting/request/order-ready-to-cancel-filter-by-uid.json"
                ))
            )
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/shooting/response/orders-ready-to-cancel-filter-by-uid.json"));
    }

    @ParameterizedTest(name = "Получаем orderId, которые можно отменять (#{0} страница по {1} элементов)")
    @MethodSource("getOrdersForCancellingPageArguments")
    @DatabaseSetup("/controller/shooting/init/setup-order-ready-to-cancel-page.xml")
    void getOrderIdsForCancellingPage(
        String page,
        String size,
        String response
    ) throws Exception {
        mockMvc.perform(post("/shooting/orders-ready-to-cancel")
                .param("page", page)
                .param("size", size)
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent(
                    "controller/shooting/request/order-ready-to-cancel-page.json"
                ))
            )
            .andExpect(status().isOk())
            .andExpect(jsonContent(response));
    }
}
