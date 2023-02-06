package ru.yandex.market.wms.autostart.settings.controller;


import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DatabaseSetups;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import ru.yandex.market.wms.autostart.AutostartIntegrationTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.autostart.util.PutAwayZoneTestData.S01_ZONE;
import static ru.yandex.market.wms.autostart.util.PutAwayZoneTestData.zonesJson;

@DatabaseSetups({
        @DatabaseSetup(value = "/fixtures/autostart/2/zones.xml", connection = "wmwhseConnection")
})
class AutostartZoneSettingsControllerTest extends AutostartIntegrationTest {

    public static final boolean STRICT = true;

    @Test
    public void zones() throws Exception {
        mockMvc.perform(get("/settings/zone"))
                .andExpect(status().isOk())
                .andExpect(content().json(zonesJson(), STRICT));
    }

    @Test
    public void get_update_get() throws Exception {
        get_update_get(
                json("{itemsIntoPickingOrder:88,maxVolumePerPickingOrder:0.66,maxWeightPerPickingOrder:100.2}"),
                json("{itemsIntoPickingOrder:88,maxVolumePerPickingOrder:0.66,maxWeightPerPickingOrder:100.2}")
        );
    }

    @Test
    public void get_updateNoOptionals_get() throws Exception {
        get_update_get(
                json("{itemsIntoPickingOrder:88}"),
                json("{itemsIntoPickingOrder:88,maxVolumePerPickingOrder:null,maxWeightPerPickingOrder:null}")
        );
    }

    @Test
    public void get_updateOptionalsNull_get() throws Exception {
        get_update_get(
                json("{itemsIntoPickingOrder:88,maxVolumePerPickingOrder:null,maxWeightPerPickingOrder:null}"),
                json("{itemsIntoPickingOrder:88,maxVolumePerPickingOrder:null,maxWeightPerPickingOrder:null}")
        );
    }


    void get_update_get(String update, String updated) throws Exception {
        String url = "/settings/zone/" + S01_ZONE;

        String existing = json("{itemsIntoPickingOrder:80,maxVolumePerPickingOrder:0.6,maxWeightPerPickingOrder:100" +
                ".1}");
        mockMvc.perform(get(url))
                .andExpect(status().isOk())
                .andExpect(content().json(existing, STRICT));

        mockMvc.perform(
                put(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(update))
                .andExpect(status().isOk());

        mockMvc.perform(get(url))
                .andExpect(status().isOk())
                .andExpect(content().json(updated, STRICT));
    }
}
