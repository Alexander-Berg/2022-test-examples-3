package ru.yandex.market.logistics.management.controller.admin.locationzone;

import java.util.Map;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.util.MultiValueMap;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.service.plugin.LMSPlugin;
import ru.yandex.market.logistics.management.util.WithBlackBoxUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.management.util.TestUtil.jsonContent;
import static ru.yandex.market.logistics.management.util.TestUtil.toMultiValueMap;

@DatabaseSetup("/data/controller/admin/locationZone/before/prepare_data.xml")
@ParametersAreNonnullByDefault
class LocationZoneSearchTest extends AbstractContextualTest {

    @Test
    @DisplayName("Получить зоны локаций будучи неавторизованным")
    void getLocationGridIsUnauthorized() throws Exception {
        mockMvc.perform(get("/admin/lms/location-zone")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Получить зоны локаций не имея прав")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {})
    void getLocationGridIsForbidden() throws Exception {
        mockMvc.perform(get("/admin/lms/location-zone")).andExpect(status().isForbidden());
    }

    @DisplayName("Поиск зон локации")
    @MethodSource("filterArguments")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_LOCATION_ZONE})
    @ParameterizedTest(name = "[{index}] {0}")
    void filter(String displayName, MultiValueMap<String, String> filterParams, String responsePath) throws Exception {
        mockMvc.perform(get("/admin/lms/location-zone").params(filterParams))
            .andExpect(status().isOk())
            .andExpect(jsonContent(responsePath));
    }

    @Nonnull
    private static Stream<Arguments> filterArguments() {
        return Stream.of(
            Arguments.of(
                "Пустой фильтр",
                toMultiValueMap(Map.of()),
                "data/controller/admin/locationZone/response/all.json"
            ),
            Arguments.of(
                "Фильтр по идентификатору",
                toMultiValueMap(Map.of("locationZoneId", "1")),
                "data/controller/admin/locationZone/response/id_1.json"
            ),
            Arguments.of(
                "Фильтр по идентификатору локации",
                toMultiValueMap(Map.of("locationId", "1")),
                "data/controller/admin/locationZone/response/id_3_4.json"
            ),
            Arguments.of(
                "Фильтр по всем параметрам",
                toMultiValueMap(Map.of(
                    "locationZoneId", "2",
                    "locationId", "213"
                )),
                "data/controller/admin/locationZone/response/id_2.json"
            )
        );
    }
}
