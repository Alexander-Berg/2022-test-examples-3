package ru.yandex.market.logistics.tarifficator.admin.tariffGroup;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.tarifficator.AbstractContextualTest;
import ru.yandex.market.logistics.tarifficator.admin.controller.AdminTariffGroupController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static ru.yandex.market.logistics.tarifficator.util.ValidationUtil.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Получение через админку деталей группы тарифов")
@DatabaseSetup("/controller/admin/tariffGroup/before/prepare_groups.xml")
class GetTariffGroupDetailsTest extends AbstractContextualTest {

    @Test
    @DisplayName("Получение через админку деталей группы тарифов")
    void shouldReturnOk() throws Exception {
        performGetTariffGroupDetails(18)
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/admin/tariffGroup/response/tariff_group_18_detail.json"));
    }

    @Test
    @DisplayName("Не найдена несуществующая группа тарифов")
    void shouldNotFound() throws Exception {
        performGetTariffGroupDetails(999)
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [TARIFF_GROUP] with ids [[999]]"));
    }

    @SneakyThrows
    @Nonnull
    private ResultActions performGetTariffGroupDetails(long tariffGroupId) {
        return mockMvc.perform(
            get(AdminTariffGroupController.PATH_ADMIN_TARIFF_GROUPS + "/" + tariffGroupId)
        );
    }
}
