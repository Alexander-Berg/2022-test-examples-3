package ru.yandex.market.logistics.lom.controller.document.label;

import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.lom.AbstractContextualTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Получение ярлыка заказа")
@DatabaseSetup("/controller/document/before/labels.xml")
class LabelGetTest extends AbstractContextualTest {

    @ParameterizedTest
    @DisplayName("Получение ярлыка для заказа")
    @MethodSource("successSource")
    void getLabelSuccess(Integer orderId, String responsePath) throws Exception {

        getLabel(orderId)
            .andExpect(status().isOk())
            .andExpect(jsonContent(responsePath));

        getOrder(orderId)
            .andExpect(status().isOk())
            .andExpect(jsonPath("hasLabels").value(true));
    }

    @Nonnull
    private static Stream<Arguments> successSource() {
        return Stream.of(
            Pair.of(2, "controller/document/response/for_order_2_ww.json"),
            Pair.of(4, "controller/document/response/for_order_4_ww.json")
        ).map(p -> Arguments.of(p.getLeft(), p.getRight()));
    }

    @Test
    @DisplayName("Ошибка при получении ярлыка заказа, заказ не найден")
    void getLabelNoOrder() throws Exception {
        getLabel(10)
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [LABEL_FOR_ORDER] with id [10]"));

        getOrder(10)
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [ORDER] with id [10]"));
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @DisplayName("У заказа не найдены ярлыки")
    @MethodSource("labelNotFoundSource")
    void labelNotFound(@SuppressWarnings("unused") String displayName, int orderId) throws Exception {
        getLabel(orderId)
            .andExpect(status().isNotFound())
            .andExpect(errorMessage(String.format("Failed to find [LABEL_FOR_ORDER] with id [%d]", orderId)));

        getOrder(orderId)
            .andExpect(status().isOk())
            .andExpect(jsonPath("hasLabels").value(false));
    }

    @Nonnull
    private static Stream<Arguments> labelNotFoundSource() {
        return Stream.of(
            Arguments.of("Заказ без ярлыка", 1),
            Arguments.of("Заказ с ярлыком партнера", 3)
        );
    }

    @Nonnull
    private ResultActions getLabel(long orderId) throws Exception {
        return mockMvc.perform(get("/orders/" + orderId + "/label"));
    }

    @Nonnull
    private ResultActions getOrder(long orderId) throws Exception {
        return mockMvc.perform(get("/orders/" + orderId));
    }
}
