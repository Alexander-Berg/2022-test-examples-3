package ru.yandex.market.wms.transportation.controller;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.util.LinkedMultiValueMap;

import ru.yandex.market.wms.common.spring.IntegrationTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

public class LocationControllerTest extends IntegrationTest {
    @Test
    @DatabaseSetup("/controller/location/multiple-transporters.xml")
    void getSourceZones() throws Exception {
        var params = new LinkedMultiValueMap<String, String>();
        params.addAll("zones", List.of("IN_CONV_A"));
        mockMvc.perform(get("/location/source-zones").queryParams(params))
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent("controller/location/get-source-zones.json")));
    }

    @Test
    @DatabaseSetup("/controller/location/multiple-transporters.xml")
    void getSourceZonesByMultipleDestinations() throws Exception {
        var params = new LinkedMultiValueMap<String, String>();
        params.addAll("zones", List.of("IN_CONV_A", "IN_CONV_B", "IN_C", "IN_D"));
        mockMvc.perform(get("/location/source-zones").queryParams(params))
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent("controller/location/get-all-source-zones.json")));
    }

    @Test
    @DatabaseSetup("/controller/location/multiple-transporters.xml")
    void getSourceZonesByEmptyParams() throws Exception {
        var params = new LinkedMultiValueMap<String, String>();
        params.addAll("zones", List.of(""));
        mockMvc.perform(get("/location/source-zones").queryParams(params))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DatabaseSetup("/controller/location/multiple-transporters.xml")
    void getDestinationZone() throws Exception {
        var params = new LinkedMultiValueMap<String, String>();
        params.add("zone", "IN_BUF_C");
        mockMvc.perform(get("/location/destination-zones").queryParams(params))
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent("controller/location/get-destination-zone.json")));
    }

    @Test
    @DatabaseSetup("/controller/location/multiple-transporters.xml")
    void getTransporters() throws Exception {
        var params = new LinkedMultiValueMap<String, String>();
        params.addAll("sourceZones", List.of("IN_BUF_A", "IN_BUF_C"));
        mockMvc.perform(get("/location/transporters").queryParams(params))
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent("controller/location/get-all-transporters.json")));
    }
}
