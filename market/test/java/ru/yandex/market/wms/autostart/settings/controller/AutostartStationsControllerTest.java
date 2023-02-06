package ru.yandex.market.wms.autostart.settings.controller;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DatabaseSetups;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.wms.autostart.AutostartIntegrationTest;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

class AutostartStationsControllerTest extends AutostartIntegrationTest {

    @Test
    @DatabaseSetups({
            @DatabaseSetup(value = "/fixtures/autostart/2/sorting_stations.xml")
    })
    @ExpectedDatabase(value = "/fixtures/autostart/settings/stations/after.xml", assertionMode = NON_STRICT_UNORDERED)
    public void getThenUpdateThenGet() throws Exception {
        String url = "/settings/stations";

        String initial = json("[\n  "
                + "{station:'S01',mode:'ORDERS'},\n"
                + "  {station:'S02',mode:'ORDERS'},\n"
                + "  {station:'S03',mode:'ORDERS'},\n"
                + "  {station:'S04',mode:'ORDERS'},\n"
                + "  {station:'S05',mode:'ORDERS'},\n"
                + "  {station:'S06',mode:'OFF'}\n"
                + "]");
        mockMvc.perform(get(url))
                .andExpect(status().isOk())
                .andExpect(content().json(initial, STRICT));

        String update = json("[\n"
                + "  {station:'S01',mode:'OFF'},\n"
                + "  {station:'S02',mode:'WITHDRAWALS'},\n"
                + "  {station:'S03',mode:'ORDERS'},\n"
                + "  {station:'S04',mode:'ORDERS'},\n"
                + "  {station:'S05',mode:'ORDERS'},\n"
                + "  {station:'S06',mode:'OFF'}\n"
                + "]");
        mockMvc.perform(
                        post(url)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(update))
                .andExpect(status().isOk());

        mockMvc.perform(get(url))
                .andExpect(status().isOk())
                .andExpect(content().json(update, STRICT));
    }

    @Test
    @DatabaseSetup("/settings/controller/station/db/immutable-state.xml")
    public void getSortingStationsReturnsStationWithCode() throws Exception {
        ResultActions result = mockMvc.perform(get("/settings/stations")
                .param("waveType", "WITHDRAWAL")
                .contentType(MediaType.APPLICATION_JSON));

        result.andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "settings/controller/station/response/only-type2response.json")));
    }

    @Test
    @DatabaseSetup("/settings/controller/station/db/immutable-state.xml")
    public void getSortingStationsReturnsAllStations() throws Exception {
        ResultActions result = mockMvc.perform(get("/settings/stations")
                .contentType(MediaType.APPLICATION_JSON));

        result.andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "settings/controller/station/response/all-stations-response.json")));
    }

    @Test
    @DatabaseSetup("/settings/controller/station/db/immutable-state.xml")
    public void getHobbitSortingStations() throws Exception {
        ResultActions result = mockMvc.perform(get("/settings/stations")
                .param("waveType", "HOBBIT")
                .contentType(MediaType.APPLICATION_JSON));

        result.andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "settings/controller/station/response/hobbit-stations-response.json")));
    }

    @Test
    @DatabaseSetup("/settings/controller/station/db/immutable-state.xml")
    public void getDefaultSortingStations() throws Exception {
        ResultActions result = mockMvc.perform(get("/settings/stations")
                .param("waveType", "ALL")
                .contentType(MediaType.APPLICATION_JSON));

        result.andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "settings/controller/station/response/default-stations-response.json")));
    }

    @Test
    @DatabaseSetup("/fixtures/autostart/2/sorting_stations.xml")
    @DatabaseSetup("/settings/controller/station/db/hold-enabled.xml")
    @ExpectedDatabase(value = "/fixtures/autostart/settings/stations/after.xml", assertionMode = NON_STRICT_UNORDERED)
    public void updateDefaultSortingStations() throws Exception {
        String updatedStations = json("[\n"
                + "  {station:'S01',mode:'OFF'},\n"
                + "  {station:'S02',mode:'WITHDRAWALS'},\n"
                + "  {station:'S03',mode:'ORDERS'},\n"
                + "  {station:'S04',mode:'ORDERS'},\n"
                + "  {station:'S05',mode:'ORDERS'},\n"
                + "  {station:'S06',mode:'OFF'}\n"
                + "]");

        ResultActions result = mockMvc.perform(post("/settings/stations")
                .content(updatedStations)
                .contentType(MediaType.APPLICATION_JSON));

        result.andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup("/fixtures/autostart/2/sorting_stations.xml")
    @DatabaseSetup("/settings/controller/station/db/hold-enabled.xml")
    @DatabaseSetup("/settings/controller/station/db/waves.xml")
    @ExpectedDatabase(value = "/fixtures/autostart/settings/stations/after.xml", assertionMode = NON_STRICT_UNORDERED)
    public void updateDefaultSortingStationsWithActiveWavesValidationOk() throws Exception {
        String updatedStations = json("[\n"
                + "  {station:'S01',mode:'OFF'},\n"
                + "  {station:'S02',mode:'WITHDRAWALS'},\n"
                + "  {station:'S03',mode:'ORDERS'},\n"
                + "  {station:'S04',mode:'ORDERS'},\n"
                + "  {station:'S05',mode:'ORDERS'},\n"
                + "  {station:'S06',mode:'OFF'}\n"
                + "]");

        ResultActions result = mockMvc.perform(post("/settings/stations")
                .content(updatedStations)
                .contentType(MediaType.APPLICATION_JSON));

        result.andExpect(status().isOk());
    }

    @Test
    @Disabled("TBD")
    @DatabaseSetup("/fixtures/autostart/2/sorting_stations_2.xml")
    @DatabaseSetup("/settings/controller/station/db/hold-enabled.xml")
    @DatabaseSetup("/settings/controller/station/db/waves.xml")
    @ExpectedDatabase(value = "/fixtures/autostart/2/sorting_stations_2.xml", assertionMode = NON_STRICT_UNORDERED)
    public void updateDefaultSortingStationsWithActiveWavesValidationNotOk() throws Exception {
        String updatedStations = json("[\n"
                + "  {station:'S01',mode:'OFF'},\n"
                + "  {station:'S02',mode:'WITHDRAWALS'},\n"
                + "  {station:'S03',mode:'ORDERS'},\n"
                + "  {station:'S04',mode:'OFF'},\n"
                + "  {station:'S05',mode:'OFF'},\n"
                + "  {station:'S06',mode:'OFF'}\n"
                + "]");

        ResultActions result = mockMvc.perform(post("/settings/stations")
                .content(updatedStations)
                .contentType(MediaType.APPLICATION_JSON));

        result.andExpect(status().is4xxClientError())
              .andExpect(content().json(getFileContent(
                      "settings/controller/station/response/update-stations-response.json")));


    }
}
