package ru.yandex.market.logistics.management.controller.admin.locationzone;

import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.domain.dto.front.locationZone.LocationZoneCreateDto;
import ru.yandex.market.logistics.management.service.plugin.LMSPlugin;
import ru.yandex.market.logistics.management.util.WithBlackBoxUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.management.util.TestUtil.pojoToString;

@DatabaseSetup("/data/controller/admin/locationZone/before/prepare_data.xml")
@ParametersAreNonnullByDefault
class LocationZoneCreateTest extends AbstractContextualTest {
    private static final Long MOSCOW_LOCATION_ID = 213L;

    @Test
    @DisplayName("Создать зону локации, будучи неавторизованным")
    void createLocationZoneIsUnauthorized() throws Exception {
        createLocationZone(defaultCreateDto()).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Создать зону локации, не имея прав")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {})
    void createLocationZoneIsForbidden() throws Exception {
        createLocationZone(defaultCreateDto()).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Создать зону локации, имея права только на чтение")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_LOCATION_ZONE})
    void createLocationZoneReadOnly() throws Exception {
        createLocationZone(defaultCreateDto()).andExpect(status().isForbidden());
    }

    @ParameterizedTest(name = "[{index}] {1}")
    @MethodSource("invalidRequestProvider")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_LOCATION_ZONE_EDIT})
    void validateDto(LocationZoneCreateDto createDto, String message) throws Exception {
        createLocationZone(createDto)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("errors[0].codes[0]").value(message));
    }

    static Stream<Arguments> invalidRequestProvider() {
        return Stream.of(
            Pair.of(defaultCreateDto().setName(""), "NotBlank.locationZoneCreateDto.name"),
            Pair.of(defaultCreateDto().setLocationId(null), "NotNull.locationZoneCreateDto.locationId")
        )
            .map(pair -> Arguments.of(pair.getFirst(), pair.getSecond()));
    }

    @Test
    @DisplayName("Создать зону локации")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_LOCATION_ZONE_EDIT})
    @ExpectedDatabase(
        value = "/data/controller/admin/locationZone/after/create_location_zone.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void createLocationZoneSuccess() throws Exception {
        createLocationZone(defaultCreateDto())
            .andExpect(status().isCreated())
            .andExpect(header().string("location", "http://localhost/admin/lms/location-zone/5"));
    }

    @Test
    @DisplayName("Создать зону локации - в локации уже есть зона с таким названием")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_LOCATION_ZONE_EDIT})
    void createLocationZoneAlreadyExists() throws Exception {
        createLocationZone(defaultCreateDto().setName("ЦеНтР"))
            .andExpect(status().isBadRequest())
            .andExpect(status().reason("Location 213 already has zone with name: Центр"));
    }

    @Nonnull
    private ResultActions createLocationZone(LocationZoneCreateDto createDto) throws Exception {
        return mockMvc.perform(post("/admin/lms/location-zone")
            .contentType(MediaType.APPLICATION_JSON)
            .content(pojoToString(createDto))
        );
    }

    @Nonnull
    private static LocationZoneCreateDto defaultCreateDto() {
        return new LocationZoneCreateDto()
            .setName("Название зоны")
            .setDescription("Описание зоны")
            .setLocationId(MOSCOW_LOCATION_ID);
    }

}
