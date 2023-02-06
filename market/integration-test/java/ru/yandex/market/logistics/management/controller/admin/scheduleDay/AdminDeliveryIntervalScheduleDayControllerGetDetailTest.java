package ru.yandex.market.logistics.management.controller.admin.scheduleDay;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.util.TestUtil;
import ru.yandex.market.logistics.management.util.WithBlackBoxUser;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.management.controller.admin.scheduleDay.Helper.READ_ONLY;
import static ru.yandex.market.logistics.management.controller.admin.scheduleDay.Helper.getDetail;

@DisplayName("Получение детальной карточки интервала доставки в конечных точках")
@DatabaseSetup("/data/controller/admin/scheduleDay/before/prepare_data.xml")
public class AdminDeliveryIntervalScheduleDayControllerGetDetailTest extends AbstractContextualTest {
    @Test
    @DisplayName("Сущность не найдена")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {READ_ONLY})
    void getDetailNotFound() throws Exception {
        mockMvc.perform(getDetail(1000)).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("ReadOnly mode - Успешно")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {READ_ONLY})
    void getDetailSuccessReadOnly() throws Exception {
        mockMvc.perform(getDetail(1))
            .andExpect(status().isOk())
            .andExpect(TestUtil.jsonContent("data/controller/admin/scheduleDay/response/get_detail.json"));
    }
}
