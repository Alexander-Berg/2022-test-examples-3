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
import ru.yandex.market.logistics.lom.admin.filter.AdminChangeOrderRequestSearchFilter;
import ru.yandex.market.logistics.lom.entity.enums.ChangeOrderRequestStatus;
import ru.yandex.market.logistics.lom.entity.enums.ChangeOrderRequestType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;
import static ru.yandex.market.logistics.test.integration.utils.QueryParamUtils.toParams;

@DatabaseSetup("/controller/admin/change-request/before/setup.xml")
class GetChangeOrderRequestTest extends AbstractContextualTest {
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("searchArgument")
    @DisplayName("Поиск заявок на изменение заказа")
    void search(String displayName, AdminChangeOrderRequestSearchFilter filter, String responsePath) throws Exception {
        mockMvc.perform(get("/admin/change-order-requests").params(toParams(filter)))
            .andExpect(status().isOk())
            .andExpect(jsonContent(responsePath));
    }

    @Nonnull
    private static Stream<Arguments> searchArgument() {
        return Stream.of(
            Arguments.of(
                "Пустой фильтр",
                new AdminChangeOrderRequestSearchFilter(),
                "controller/admin/change-request/response/order/all.json"
            ),
            Arguments.of(
                "По идентификатору заказа",
                new AdminChangeOrderRequestSearchFilter().setOrderId(1L),
                "controller/admin/change-request/response/order/id_1.json"
            ),
            Arguments.of(
                "По штрихкоду заказа",
                new AdminChangeOrderRequestSearchFilter().setBarcode("barcode-2"),
                "controller/admin/change-request/response/order/id_2_3.json"
            ),
            Arguments.of(
                "По типу заявки",
                new AdminChangeOrderRequestSearchFilter()
                    .setRequestType(ChangeOrderRequestType.ORDER_ITEM_IS_NOT_SUPPLIED),
                "controller/admin/change-request/response/order/id_1_2.json"
            ),
            Arguments.of(
                "По статусу заявки",
                new AdminChangeOrderRequestSearchFilter().setStatus(ChangeOrderRequestStatus.PROCESSING),
                "controller/admin/change-request/response/order/id_1.json"
            ),
            Arguments.of(
                "По идентификатору партнёра",
                new AdminChangeOrderRequestSearchFilter().setPartnerId(1L),
                "controller/admin/change-request/response/order/id_1.json"
            ),
            Arguments.of(
                "По всем полям",
                new AdminChangeOrderRequestSearchFilter()
                    .setOrderId(1L)
                    .setBarcode("barcode-1")
                    .setStatus(ChangeOrderRequestStatus.PROCESSING)
                    .setRequestType(ChangeOrderRequestType.ORDER_ITEM_IS_NOT_SUPPLIED)
                    .setPartnerId(1L),
                "controller/admin/change-request/response/order/id_1.json"
            )
        );
    }

    @Test
    @DisplayName("Получение деталей заявки на изменение заказа")
    void getDetail() throws Exception {
        mockMvc.perform(get("/admin/change-order-requests/1"))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/admin/change-request/response/order/detail_id_1.json"));
    }

    @Test
    @DisplayName("Заявка на изменение заказа не найдена")
    void notFound() throws Exception {
        mockMvc.perform(get("/admin/change-order-requests/42"))
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [ORDER_CHANGE_REQUEST] with id [42]"));
    }
}
