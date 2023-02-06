package ru.yandex.market.logistics.management.controller.admin;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import ru.yandex.market.logistics.management.AbstractContextualAspectValidationTest;
import ru.yandex.market.logistics.management.domain.dto.front.partner.PartnerNewDto;
import ru.yandex.market.logistics.management.domain.entity.Partner;
import ru.yandex.market.logistics.management.domain.entity.type.PartnerStatus;
import ru.yandex.market.logistics.management.service.plugin.LMSPlugin;
import ru.yandex.market.logistics.management.util.WithBlackBoxUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.logging.jdbc.json.JsonJdbcExceptionLogger.OBJECT_MAPPER;

@DisplayName("Контроллер для редактирования партнеров")
@DatabaseSetup("/data/controller/admin/partner/prepare_data.xml")
class AdminPartnerControllerUpdateTest extends AbstractContextualAspectValidationTest {

    private static final String PARTNER_URL = "/admin/lms/partner";

    private final PartnerNewDto partnerNewDto = new PartnerNewDto()
        .setPartnerId(1L);

    @Test
    @DisplayName("Обновить партнера - Неавторизованный пользователь")
    void testUpdatePartner_unauthorized() throws Exception {
        mockMvc.perform(put(PARTNER_URL + "/" + partnerNewDto.getPartnerId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsBytes(partnerNewDto)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Обновить партнера - Недостаточно прав")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_PARTNER})
    void testUpdatePartner_forbidden() throws Exception {
        mockMvc.perform(put(PARTNER_URL + "/" + partnerNewDto.getPartnerId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsBytes(partnerNewDto)))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Обновить партнера - Не найден партнер с таким ID")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_PARTNER_EDIT})
    void testUpdatePartner_pathNotFound() throws Exception {
        mockMvc.perform(put(PARTNER_URL + "/128")
                .contentType(MediaType.APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsBytes(partnerNewDto)))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_PARTNER_EDIT})
    @ExpectedDatabase(
        value = "/data/controller/admin/partner/after/update.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    @DisplayName("Обновить партнера - Успешно")
    void testUpdatePartner_success() throws Exception {

        Partner partner = new Partner();

        mockMvc.perform(put(PARTNER_URL + "/" + partnerNewDto.getPartnerId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsBytes(partner
                    .setId(partnerNewDto.getPartnerId())
                    .setLocationId(10)
                    .setReadableName("Merch")
                    .setStockSyncEnabled(false)
                    .setStatus(PartnerStatus.INACTIVE))))
            .andExpect(status().isOk());
    }
}
