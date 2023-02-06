package ru.yandex.market.logistics.management.controller.admin.partnerTransport;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.util.TestUtil;
import ru.yandex.market.logistics.management.util.WithBlackBoxUser;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.management.controller.admin.partnerTransport.Helper.READ_ONLY;
import static ru.yandex.market.logistics.management.controller.admin.partnerTransport.Helper.READ_WRITE;
import static ru.yandex.market.logistics.management.controller.admin.partnerTransport.Helper.getDetail;

@DatabaseSetup("/data/controller/admin/partnerTransport/before/setup.xml")
public class AdminPartnerTransportControllerDetailsTest extends AbstractContextualTest {

    @Test
    @DisplayName("Сущность не найдена")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {READ_ONLY})
    void getDetailNotFound() throws Exception {
        mockMvc.perform(getDetail(2)).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("ReadOnly mode - Успешно")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {READ_ONLY})
    void getDetailSuccessReadOnly() throws Exception {
        mockMvc.perform(getDetail(1))
            .andExpect(status().isOk())
            .andExpect(TestUtil.jsonContent("data/controller/admin/partnerTransport/after/get_detail.json"));
    }

    @Test
    @DisplayName("ReadWrite mode - Успешно")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {READ_ONLY, READ_WRITE})
    void getDetailSuccessReadWrite() throws Exception {
        mockMvc.perform(getDetail(1))
            .andExpect(status().isOk())
            .andExpect(TestUtil.jsonContent("data/controller/admin/partnerTransport/after/get_detail_edit.json"));
    }
}
