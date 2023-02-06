package ru.yandex.market.logistics.management.controller.admin.partnerRoute;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.util.TestUtil;
import ru.yandex.market.logistics.management.util.WithBlackBoxUser;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.management.controller.admin.partnerRoute.Helper.READ_ONLY;
import static ru.yandex.market.logistics.management.controller.admin.partnerRoute.Helper.READ_WRITE;
import static ru.yandex.market.logistics.management.controller.admin.partnerRoute.Helper.getNew;

@DisplayName("Получение dto для создания новой сущности")
class AdminPartnerRouteControllerGetNewTest extends AbstractContextualTest {
    @Test
    @DisplayName("Успешно")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {READ_ONLY, READ_WRITE})
    void getNewSuccess() throws Exception {
        mockMvc.perform(getNew())
            .andExpect(status().isOk())
            .andExpect(TestUtil.jsonContent("data/controller/admin/partnerRoute/response/get_new.json"));
    }
}
