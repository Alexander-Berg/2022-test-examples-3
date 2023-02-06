package ru.yandex.market.pvz.internal.controller.pi.dict;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.market.pvz.core.test.factory.TestBrandRegionFactory;
import ru.yandex.market.pvz.internal.BaseShallowTest;
import ru.yandex.market.pvz.internal.WebLayerTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.pvz.core.TestUtils.getFileContent;

@WebLayerTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class DictionaryControllerTest extends BaseShallowTest {

    private final TestBrandRegionFactory brandRegionFactory;

    @Test
    void getAllBrandRegions() throws Exception {
        brandRegionFactory.createDefaults();

        mockMvc.perform(
                get("/v1/pi/dict/brand-regions")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent("dict/response_get_brand_regions.json")));
    }

    @Test
    void getEmptyBrandRegions() throws Exception {
        mockMvc.perform(
                get("/v1/pi/dict/brand-regions")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent("dict/response_get_brand_regions_empty.json")));
    }

    @Test
    void getStaticFilters() throws Exception {
        mockMvc.perform(
                get("/v1/pi/dict/static-filters?filters=orderType,orderPaymentType")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent("dict/response_get_static_filters.json")));
    }

    @Test
    void getUnableToScanReason() throws Exception {
        mockMvc.perform(get(
                "/v1/pi/dict/static-filters?filters=unableToScanUitReason,unableToScanCisReason,unableToScanEanReason"
                )
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent("dict/response_unable_to_scan_reason.json")));
    }

    @Test
    void getAllStaticFilters() throws Exception {
        mockMvc.perform(
                get("/v1/pi/dict/static-filters")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is2xxSuccessful());
    }
}
