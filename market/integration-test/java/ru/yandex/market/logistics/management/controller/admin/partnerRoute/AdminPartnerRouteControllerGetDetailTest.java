package ru.yandex.market.logistics.management.controller.admin.partnerRoute;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.util.TestUtil;
import ru.yandex.market.logistics.management.util.WithBlackBoxUser;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.management.controller.admin.partnerRoute.Helper.READ_ONLY;
import static ru.yandex.market.logistics.management.controller.admin.partnerRoute.Helper.READ_WRITE;
import static ru.yandex.market.logistics.management.controller.admin.partnerRoute.Helper.getDetail;

@DisplayName("Получение детальной карточки магистрали партнера")
@DatabaseSetup("/data/controller/admin/partnerRoute/before/partner_routes.xml")
class AdminPartnerRouteControllerGetDetailTest extends AbstractContextualTest {

    private static final int PARTNER_ROUTE_ID = 5000;

    @Test
    @DisplayName("Сущность не найдена")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {READ_ONLY})
    void getDetailNotFound() throws Exception {
        mockMvc.perform(getDetail(4005)).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("ReadOnly mode - Успешно")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {READ_ONLY})
    void getDetailSuccessReadOnly() throws Exception {
        mockMvc.perform(getDetail(PARTNER_ROUTE_ID))
            .andExpect(status().isOk())
            .andExpect(TestUtil.jsonContent("data/controller/admin/partnerRoute/response/get_detail.json"));
    }

    @Test
    @DisplayName("ReadWrite mode - Успешно")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {READ_ONLY, READ_WRITE})
    void getDetailSuccessReadWrite() throws Exception {
        mockMvc.perform(getDetail(PARTNER_ROUTE_ID))
            .andExpect(status().isOk())
            .andExpect(TestUtil.jsonContent("data/controller/admin/partnerRoute/response/get_detail_edit.json"));
    }

    @Test
    @DisplayName("Магистраль партнёра без расписания закладки в ПВЗ")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {READ_ONLY})
    void getDetailWithoutSchedules() throws Exception {
        mockMvc.perform(getDetail(6000))
            .andExpect(status().isOk())
            .andExpect(TestUtil.jsonContent(
                "data/controller/admin/partnerRoute/response/get_detail_without_pickup_inbound_schedule.json"
            ));
    }
}
