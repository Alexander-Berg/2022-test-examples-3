package ru.yandex.market.logistics.management.controller.admin.locationzone;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.domain.dto.front.locationZone.LocationZoneUpdateDto;
import ru.yandex.market.logistics.management.service.plugin.LMSPlugin;
import ru.yandex.market.logistics.management.util.WithBlackBoxUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.management.util.TestUtil.pojoToString;

@DatabaseSetup("/data/controller/admin/locationZone/before/prepare_data.xml")
@ParametersAreNonnullByDefault
class LocationZoneUpdateTest extends AbstractContextualTest {

    @Test
    @DisplayName("Обновить информацию о зоне локации, будучи неавторизованным")
    void updateLocationZoneIsUnauthorized() throws Exception {
        performUpdateLocationZone(defaultUpdateDto()).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Обновить информацию о зоне локации, не имея прав")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {})
    void updateLocationZoneIsForbidden() throws Exception {
        performUpdateLocationZone(defaultUpdateDto()).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Обновить информацию о зоне локации, имея права только на чтение")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_LOCATION_ZONE})
    void updateLocationZoneReadOnly() throws Exception {
        performUpdateLocationZone(defaultUpdateDto()).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Обновить информацию о зоне локации — без изменений")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_LOCATION_ZONE_EDIT})
    @ExpectedDatabase(
        value = "/data/controller/admin/locationZone/before/prepare_data.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateLocationZoneNoChanges() throws Exception {
        performUpdateLocationZone(defaultUpdateDto()).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Обновить информацию о зоне локации — изменение названия")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_LOCATION_ZONE_EDIT})
    @ExpectedDatabase(
        value = "/data/controller/admin/locationZone/after/update_name.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateLocationZoneChangeName() throws Exception {
        performUpdateLocationZone(defaultUpdateDto().setName("Новое название")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Обновить информацию о зоне локации — изменение описания")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_LOCATION_ZONE_EDIT})
    @ExpectedDatabase(
        value = "/data/controller/admin/locationZone/after/update_description.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateLocationZoneChangeDescription() throws Exception {
        performUpdateLocationZone(defaultUpdateDto().setDescription("Новое описание")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Обновить информацию о зоне локации — изменение всех возможных полей")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_LOCATION_ZONE_EDIT})
    @ExpectedDatabase(
        value = "/data/controller/admin/locationZone/after/update_all_possible_fields.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateLocationZoneChangeAllPossibleFields() throws Exception {
        performUpdateLocationZone(defaultUpdateDto().setName("Новое название").setDescription("Новое описание"))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Обновить информацию о зоне локации — изменение названия")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_LOCATION_ZONE_EDIT})
    void updateLocationZoneNameAlreadyExists() throws Exception {
        performUpdateLocationZone(defaultUpdateDto().setName("10 кМ от МКАДа"))
            .andExpect(status().isBadRequest())
            .andExpect(status().reason("Location 213 already has zone with name: 10 км от МКАДа"));
    }

    @Nonnull
    private ResultActions performUpdateLocationZone(LocationZoneUpdateDto updateDto) throws Exception {
        return mockMvc.perform(put("/admin/lms/location-zone/1")
            .content(pojoToString(updateDto))
            .contentType(MediaType.APPLICATION_JSON)
        );
    }

    @Nonnull
    private LocationZoneUpdateDto defaultUpdateDto() {
        return new LocationZoneUpdateDto()
            .setName("Центр")
            .setDescription("Центр");
    }
}
