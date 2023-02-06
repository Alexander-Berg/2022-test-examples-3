package ru.yandex.market.logistics.management.controller.admin.locationzone;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.service.plugin.LMSPlugin;
import ru.yandex.market.logistics.management.util.WithBlackBoxUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.management.util.TestUtil.jsonContent;

@DatabaseSetup("/data/controller/admin/locationZone/before/prepare_data.xml")
class LocationZoneDetailsTest extends AbstractContextualTest {
    private static final Long NON_EXIST_LOCATION_ZONE_ID = 100L;

    @Test
    @DisplayName("Получить информацию о зоне локации будучи неавторизованным")
    void getLocationZoneDetailInfoIsUnauthorized() throws Exception {
        performGetDetailInfo(1L).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Получить информацию о зоне локации не имея прав")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {})
    void getLocationZoneDetailInfoIsForbidden() throws Exception {
        performGetDetailInfo(1L).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Получить информацию о зоне локации")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_LOCATION_ZONE})
    void getLocationZoneDetailInfo() throws Exception {
        performGetDetailInfo(1L)
            .andExpect(jsonContent("data/controller/admin/locationZone/response/detail_id_1.json"));
    }

    @Test
    @DisplayName("Получить информацию о несуществующей зоне локации")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_LOCATION_ZONE})
    void getLocationZoneDetailInfoNotFound() throws Exception {
        performGetDetailInfo(NON_EXIST_LOCATION_ZONE_ID)
            .andExpect(status().isNotFound())
            .andExpect(status().reason("Can't find location zone with id=100"));
    }

    @Nonnull
    private ResultActions performGetDetailInfo(long locationZoneId) throws Exception {
        return mockMvc.perform(get("/admin/lms/location-zone/" + locationZoneId));
    }
}
