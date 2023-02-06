package ru.yandex.market.logistics.lom.admin;

import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.entity.enums.CancellationOrderStatus;
import ru.yandex.market.logistics.lom.filter.CancelOrderSearchFilter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;
import static ru.yandex.market.logistics.test.integration.utils.QueryParamUtils.toParams;

@DatabaseSetup("/controller/admin/cancel/prepare.xml")
public class CancelOrderRequestTest extends AbstractContextualTest {

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("searchArgument")
    @DisplayName("Поиск заявок на отмену заказа")
    void search(String displayName, CancelOrderSearchFilter filter, String responsePath) throws Exception {
        mockMvc.perform(get("/admin/cancel-order").params(toParams(filter)))
            .andExpect(status().isOk())
            .andExpect(jsonContent(responsePath));
    }

    @Nonnull
    private static Stream<Arguments> searchArgument() {
        return Stream.of(
            Arguments.of(
                "Пустой фильтр",
                new CancelOrderSearchFilter(),
                "controller/admin/cancel/all.json"
            ),
            Arguments.of(
                "По штрихкоду",
                new CancelOrderSearchFilter().setBarcode("LO-1"),
                "controller/admin/cancel/id_1.json"
            ),
            Arguments.of(
                "По идентификатору заказа",
                new CancelOrderSearchFilter().setOrderId(2L),
                "controller/admin/cancel/id_2.json"
            ),
            Arguments.of(
                "По статусу",
                new CancelOrderSearchFilter().setStatus(CancellationOrderStatus.CREATED),
                "controller/admin/cancel/id_2_by_status.json"
            )
        );
    }

    @Test
    @DisplayName("Получение деталей заявки на отмену")
    void getOne() throws Exception {
        mockMvc.perform(get("/admin/cancel-order/2"))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/admin/cancel/detail_2.json"));
    }

    @Test
    @DisplayName("Получение деталей заявки на отмену с причиной")
    void getOneWithReason() throws Exception {
        mockMvc.perform(get("/admin/cancel-order/3"))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/admin/cancel/detail_3.json"));
    }

    @Test
    @DisplayName("Заявка на отмену не найдена")
    void notFound() throws Exception {
        mockMvc.perform(get("/admin/cancel-order/42"))
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [CANCELLATION_ORDER_REQUEST] with id [42]"));
    }
}
