package ru.yandex.market.logistics.nesu.admin;

import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.nesu.AbstractContextualTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@ParametersAreNonnullByDefault
@DatabaseSetup("/repository/shop-pickup-point-meta/before_detail.xml")
@DisplayName("Получение детальных данных о ПВЗ и тарифе")
class ShopPickupPointMetaDetailGetTest extends AbstractContextualTest {

    @Test
    @DisplayName("Существующие данные")
    public void allTabsExisting() throws Exception {
        mockMvc.perform(get("/admin/shop-pickup-point-metas/1"))
            .andExpect(status().isOk())
            .andExpect(jsonContent(
                "controller/admin/shop-pickup-point-meta/detail.json",
                "item.values.created",
                "item.values.updated"
            ));
    }

    @Test
    @DisplayName("Существующие данные без lmsId, shopId, ownerPartnerId")
    public void allTabsExistingWithoutSomeIds() throws Exception {
        mockMvc.perform(get("/admin/shop-pickup-point-metas/2"))
            .andExpect(status().isOk())
            .andExpect(jsonContent(
                "controller/admin/shop-pickup-point-meta/detail_some_null.json",
                "item.values.created",
                "item.values.updated"
            ));
    }

    @Test
    @DisplayName("Непустое расписание")
    public void nonBlankSchedule() throws Exception {
        mockMvc.perform(get("/admin/shop-pickup-point-metas/1/schedule?fromDate=2021-09-10"))
            .andExpect(status().isOk())
            .andExpect(jsonContent(
                "controller/admin/shop-pickup-point-meta/schedule_not_blank.json"
            ));
    }

    @Test
    @DisplayName("Пустое расписание")
    public void blankSchedule() throws Exception {
        mockMvc.perform(get("/admin/shop-pickup-point-metas/2/schedule?fromDate=2021-09-10"))
            .andExpect(status().isOk())
            .andExpect(jsonContent(
                "controller/admin/shop-pickup-point-meta/schedule_blank.json"
            ));
    }

    @Test
    @DisplayName("Несуществующие данные")
    public void allTabsNonExisting() throws Exception {
        mockMvc.perform(get("/admin/shop-pickup-point-metas/100"))
            .andExpect(status().isNotFound());
    }

}
