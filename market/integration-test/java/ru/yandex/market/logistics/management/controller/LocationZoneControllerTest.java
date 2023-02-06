package ru.yandex.market.logistics.management.controller;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.util.CleanDatabase;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.management.util.TestUtil.testJson;

@CleanDatabase
@Sql("/data/controller/locationZone/prepare_data.sql")
@ParametersAreNonnullByDefault
class LocationZoneControllerTest extends AbstractContextualTest {
    private static final String URI = "/externalApi/location-zone/location/";
    private static final Long MOSCOW_LOCATION_ID  = 213L;
    private static final Long NOVOSIBIRSK_LOCATION_ID = 65L;
    private static final Long UNKNOWN_LOCATION_ID = -1L;

    @Test
    void getMultipleLocationZones() throws Exception {
        getLocationZones(MOSCOW_LOCATION_ID)
            .andExpect(status().isOk())
            .andExpect(testJson("data/controller/locationZone/multiple_location_zones.json"));
    }

    @Test
    void getSingleLocationZone() throws Exception {
        getLocationZones(NOVOSIBIRSK_LOCATION_ID)
            .andExpect(status().isOk())
            .andExpect(testJson("data/controller/locationZone/single_location_zone.json"));
    }

    @Test
    void getNoLocationZones() throws Exception {
        getLocationZones(UNKNOWN_LOCATION_ID)
            .andExpect(status().isOk())
            .andExpect(testJson("data/controller/empty_entities.json"));
    }

    @Nonnull
    private ResultActions getLocationZones(Long locationId) throws Exception {
        return mockMvc.perform(get(URI + locationId));
    }
}
