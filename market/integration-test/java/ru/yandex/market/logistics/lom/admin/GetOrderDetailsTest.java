package ru.yandex.market.logistics.lom.admin;

import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.lom.AbstractContextualTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Получение детальной карточки заказа")
class GetOrderDetailsTest extends AbstractContextualTest {

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("getInfoArgument")
    @DisplayName("Получение информации о заказе")
    @DatabaseSetup("/controller/admin/order/detail/before/orders.xml")
    void getOrderInfo(long orderId, String responsePath, String displayName) throws Exception {
        mockMvc.perform(get("/admin/orders/" + orderId))
            .andExpect(status().isOk())
            .andExpect(jsonContent(responsePath));
    }

    @Nonnull
    private static Stream<Arguments> getInfoArgument() {
        return Stream.of(
            Triple.of(1L, "controller/admin/order/detail/response/info_1.json", "Заказ со всеми полями"),
            Triple.of(2L, "controller/admin/order/detail/response/info_2.json", "Заказ только с обязательными полями"),
            Triple.of(3L, "controller/admin/order/detail/response/info_3.json", "Заказ не ЯДО")
        )
            .map(triple -> Arguments.of(triple.getLeft(), triple.getMiddle(), triple.getRight()));
    }
}
