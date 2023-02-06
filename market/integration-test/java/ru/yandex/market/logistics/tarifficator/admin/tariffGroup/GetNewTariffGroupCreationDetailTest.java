package ru.yandex.market.logistics.tarifficator.admin.tariffGroup;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.tarifficator.AbstractContextualTest;
import ru.yandex.market.logistics.tarifficator.admin.controller.AdminTariffGroupController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Получение через админку формы для создания группы тарифов")
class GetNewTariffGroupCreationDetailTest extends AbstractContextualTest {

    @Test
    @DisplayName("Получение через админку формы для создания группы тарифов")
    void shouldReturnCreationDetailDto() throws Exception {
        mockMvc
            .perform(
                get(AdminTariffGroupController.PATH_ADMIN_TARIFF_GROUPS + "/new")
            )
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/admin/tariffGroup/response/new_tariff_group_creation_detail.json"));
    }
}
