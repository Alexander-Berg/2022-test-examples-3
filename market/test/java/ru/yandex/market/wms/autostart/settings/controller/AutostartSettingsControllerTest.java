package ru.yandex.market.wms.autostart.settings.controller;


import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DatabaseSetups;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.annotation.ExpectedDatabases;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import ru.yandex.market.wms.autostart.AutostartIntegrationTest;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.autostart.autostartlogic.util.FileContentUtils.getFileContent;

@DatabaseSetups({
        @DatabaseSetup(value = "/fixtures/autostart/1/settings-before.xml", connection = "wmwhseConnection")
})
class AutostartSettingsControllerTest extends AutostartIntegrationTest {

    @Test
    public void getAOSSettings() throws Exception {
        //language=JSON5
        String expected = "{\n"
                + "  activeBatchesPerPutwall:2,\n"
                + "  maxItemsPerPutwall:1000000,\n"
                + "  minOrdersIntoPutWall:5,\n"
                + "  ordersIntoPutWall:30,\n"
                + "  putWallsIntoBatch:1,\n"
                + "  itemsIntoPickingOrder:30,\n"
                + "  uniformPickingOrdersEnabled:null,\n"
                + "  maxWeightPerPickingOrder:null,\n"
                + "  maxVolumePerPickingOrder:null,\n"
                + "  itemsIntoWave:30,\n"
                + "  ordersIntoWave:30,\n"
                + "  combineDs:false,\n"
                + "  combinePdo:false,\n"
                + "  timeForNearestCTInHours:24,\n"
                + "  period:15,\n"
                + "  freeOrdersForPutWall:1,\n"
                + "  batchingByPopularZonesEnabled:false,\n"
                + "  maxNumberOfPopularZonesForScanning:12\n"
                + "}";
        mockMvc.perform(get("/settings"))
                .andExpect(status().isOk())
                .andExpect(content().json(expected, STRICT));
    }

    @Test
    @ExpectedDatabases({
            @ExpectedDatabase(
                    value = "/fixtures/autostart/1/settings-before.xml",
                    connection = "wmwhseConnection",
                    assertionMode = NON_STRICT_UNORDERED
            )
    })
    public void tryUpdateBySomeFieldIsEmpty() throws Exception {
        mockMvc.perform(
                post("/settings/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("requests/request/aos_settings_without_one_field.json"))
        ).andExpect(status().isBadRequest());
    }

    @Test
    @ExpectedDatabases({
            @ExpectedDatabase(
                    value = "/fixtures/autostart/1/settings-after.xml",
                    connection = "wmwhseConnection",
                    assertionMode = NON_STRICT_UNORDERED
            )
    })
    public void tryUpdateHappyPath() throws Exception {
        mockMvc.perform(
                post("/settings/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("requests/request/aos_settings_correct.json"))
        ).andExpect(status().is2xxSuccessful());
    }

    @Test
    public void tryUpdateAnotherHappyPath() throws Exception {
        mockMvc.perform(
                post("/settings/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("requests/request/aos_settings_without_wgh.json"))
        ).andExpect(status().is2xxSuccessful());

        //language=JSON5
        String expected = "{\n"
                + "  activeBatchesPerPutwall:2,\n"
                + "  maxItemsPerPutwall:1000000,\n"
                + "  minOrdersIntoPutWall:3,\n"
                + "  ordersIntoPutWall:10,\n"
                + "  putWallsIntoBatch:11,\n"
                + "  itemsIntoPickingOrder:12,\n"
                + "  uniformPickingOrdersEnabled:null,\n"
                + "  maxWeightPerPickingOrder:null,\n"
                + "  maxVolumePerPickingOrder:null,\n"
                + "  itemsIntoWave:13,\n"
                + "  ordersIntoWave:14,\n"
                + "  combineDs:true,\n"
                + "  combinePdo:true,\n"
                + "  timeForNearestCTInHours:25,\n"
                + "  period:18,\n"
                + "  freeOrdersForPutWall:30,\n"
                + "  batchingByPopularZonesEnabled:false,\n"
                + "  maxNumberOfPopularZonesForScanning:12\n"
                + "}";

        mockMvc.perform(get("/settings"))
                .andExpect(status().isOk())
                .andExpect(content().json(expected, STRICT));
    }

    @Test
    @DatabaseSetups({
            @DatabaseSetup(value = "/fixtures/autostart/3/settings-before.xml", connection = "wmwhseConnection"),
            @DatabaseSetup(value = "/fixtures/autostart/3/history-before.xml", connection = "wmwhseConnection")
    })
    @ExpectedDatabases({
            @ExpectedDatabase(
                    value = "/fixtures/autostart/3/settings-after.xml",
                    connection = "wmwhseConnection",
                    assertionMode = NON_STRICT_UNORDERED
            ),
            @ExpectedDatabase(
                    value = "/fixtures/autostart/3/history-after.xml",
                    connection = "wmwhseConnection",
                    assertionMode = NON_STRICT_UNORDERED
            )
    })
    public void updateHistoryShouldBeWritten() throws Exception {
        mockMvc.perform(
                post("/settings/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("requests/request/aos_settings_correct.json"))
        ).andExpect(status().is2xxSuccessful());
    }

    @Test
    @DatabaseSetups({
            @DatabaseSetup(value = "/fixtures/autostart/4/history.xml", connection = "wmwhseConnection")
    })
    public void settingsHistoryShouldBeReturned() throws Exception {
        mockMvc.perform(
                get("/settings/history")
        )
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent("requests/response/history.json"), STRICT));
    }

    @Test
    @DatabaseSetups({
            @DatabaseSetup(value = "/fixtures/autostart/5/history.xml", connection = "wmwhseConnection")
    })
    public void emptySettingsHistoryShouldBeReturned() throws Exception {
        mockMvc.perform(
                get("/settings/history")
        )
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent("requests/response/empty-history.json"), STRICT));
    }
}
