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
import ru.yandex.market.logistics.lom.admin.filter.AdminChangeOrderSegmentRequestSearchFilter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;
import static ru.yandex.market.logistics.test.integration.utils.QueryParamUtils.toParams;

@DatabaseSetup("/controller/admin/change-request/before/setup.xml")
class GetChangeOrderSegmentRequestTest extends AbstractContextualTest {
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("searchArgument")
    @DisplayName("Поиск заявок на изменение заказа на сегменте путевого листа")
    void search(
        String displayName,
        AdminChangeOrderSegmentRequestSearchFilter filter,
        String responsePath
    ) throws Exception {
        mockMvc.perform(get("/admin/change-order-segment-requests").params(toParams(filter)))
            .andExpect(status().isOk())
            .andExpect(jsonContent(responsePath));
    }

    @Nonnull
    private static Stream<Arguments> searchArgument() {
        return Stream.of(
            Arguments.of(
                "Пустой фильтр",
                new AdminChangeOrderSegmentRequestSearchFilter(),
                "controller/admin/change-request/response/segment/all.json"
            ),
            Arguments.of(
                "По идентификатору заявки на изменение заказа",
                new AdminChangeOrderSegmentRequestSearchFilter().setChangeOrderRequestId(1L),
                "controller/admin/change-request/response/segment/id_1_2.json"
            )
        );
    }

    @Test
    @DisplayName("Получение деталей заявки на изменение заказа на сегменте путевого листа")
    void getDetail() throws Exception {
        mockMvc.perform(get("/admin/change-order-segment-requests/1"))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/admin/change-request/response/segment/detail_id_1.json"));
    }

    @Test
    @DisplayName("Заявка на изменение заказа на сегменте путевого листа не найдена")
    void notFound() throws Exception {
        mockMvc.perform(get("/admin/change-order-segment-requests/42"))
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [ORDER_CHANGE_SEGMENT_REQUEST] with id [42]"));
    }
}
