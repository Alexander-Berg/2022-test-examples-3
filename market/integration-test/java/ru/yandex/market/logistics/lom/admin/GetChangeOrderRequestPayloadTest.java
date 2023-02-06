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
import ru.yandex.market.logistics.lom.admin.enums.AdminChangeOrderRequestStatus;
import ru.yandex.market.logistics.lom.admin.filter.AdminChangeOrderRequestPayloadSearchFilter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;
import static ru.yandex.market.logistics.test.integration.utils.QueryParamUtils.toParams;

@DatabaseSetup("/controller/admin/change-request/before/setup.xml")
class GetChangeOrderRequestPayloadTest extends AbstractContextualTest {
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("searchArgument")
    @DisplayName("Поиск данных для заявок на изменение заказа")
    void search(
        String displayName,
        AdminChangeOrderRequestPayloadSearchFilter filter,
        String responsePath
    ) throws Exception {
        mockMvc.perform(get("/admin/change-order-request-payload").params(toParams(filter)))
            .andExpect(status().isOk())
            .andExpect(jsonContent(responsePath));
    }

    @Nonnull
    private static Stream<Arguments> searchArgument() {
        return Stream.of(
            Arguments.of(
                "Пустой фильтр",
                new AdminChangeOrderRequestPayloadSearchFilter(),
                "controller/admin/change-request/response/payload/all.json"
            ),
            Arguments.of(
                "По идентификатору заявки",
                new AdminChangeOrderRequestPayloadSearchFilter().setChangeOrderRequestId(1L),
                "controller/admin/change-request/response/payload/id_1_2.json"
            ),
            Arguments.of(
                "По статусу заявки",
                new AdminChangeOrderRequestPayloadSearchFilter()
                    .setChangeOrderRequestStatus(AdminChangeOrderRequestStatus.CREATED),
                "controller/admin/change-request/response/payload/id_1_3.json"
            ),
            Arguments.of(
                "По всем полям",
                new AdminChangeOrderRequestPayloadSearchFilter()
                    .setChangeOrderRequestId(1L)
                    .setChangeOrderRequestStatus(AdminChangeOrderRequestStatus.CREATED),
                "controller/admin/change-request/response/payload/id_1.json"
            )
        );
    }

    @Test
    @DisplayName("Получение деталей данных для заявки на изменение заказа")
    void getDetail() throws Exception {
        mockMvc.perform(get("/admin/change-order-request-payload/1"))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/admin/change-request/response/payload/detail_id_1.json"));
    }

    @Test
    @DisplayName("Данные для заявки на изменение заказа не найдены")
    void notFound() throws Exception {
        mockMvc.perform(get("/admin/change-order-request-payload/42"))
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [ORDER_CHANGE_REQUEST_PAYLOAD] with id [42]"));
    }
}
