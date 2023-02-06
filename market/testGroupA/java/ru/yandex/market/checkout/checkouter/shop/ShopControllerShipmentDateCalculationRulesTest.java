package ru.yandex.market.checkout.checkouter.shop;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import ru.yandex.common.util.IOUtils;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.util.balance.ShopSettingsHelper;
import ru.yandex.market.checkout.util.serialization.TestSerializationService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ShopControllerShipmentDateCalculationRulesTest extends AbstractWebTestBase {

    @Autowired
    private TestSerializationService testSerializationService;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc.perform(put("/shops/{shopId}", 1)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(testSerializationService.serializeCheckouterObject(ShopSettingsHelper.getDefaultMeta())));
    }

    @Test
    void testInsert_successful() throws Exception {
        createShipmentDateCalculationRules();
    }

    @Test
    void testInsert_shopNotFound() throws Exception {
        mockMvc.perform(post("/shops/{shopId}/shipment/date-calculation-rule", 2)
                .content(readResourceFile("json/shipmentDateCalcRules.get.shop1.json"))
                .contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetForShop_successful() throws Exception {
        createShipmentDateCalculationRules();

        MvcResult result = mockMvc.perform(get("/shops/{shopId}/shipment/date-calculation-rule", 1L)
                .contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk())
                .andReturn();

        JSONAssert.assertEquals(
                readResourceFile("json/shipmentDateCalcRules.get.shop1.json"),
                result.getResponse().getContentAsString(),
                JSONCompareMode.NON_EXTENSIBLE
        );
    }

    @Test
    void testGetForShop_ruleNotFound() throws Exception {
        createShipmentDateCalculationRules();

        mockMvc.perform(get("/shops/{shopId}/shipment/date-calculation-rule", 2L)
                .contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isNotFound());
    }

    @Test
    void testDeleteForShop_success() throws Exception {
        createShipmentDateCalculationRules();
        mockMvc.perform(delete("/shops/{shopId}/shipment/date-calculation-rule", 1L)
                .contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk());

        mockMvc.perform(get("/shops/{shopId}/shipment/date-calculation-rule", 1L)
                .contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isNotFound());
    }

    @Test
    void testDeleteForShop_ruleNotFound() throws Exception {
        mockMvc.perform(delete("/shops/{shopId}/shipment/date-calculation-rule", 1L)
                .contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk());
    }

    private void createShipmentDateCalculationRules() throws Exception {
        mockMvc.perform(post("/shops/{shopId}/shipment/date-calculation-rule", 1)
                .content(readResourceFile("json/shipmentDateCalcRules.get.shop1.json"))
                .contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk());
    }

    private String readResourceFile(String filePath) {
        try {
            return IOUtils.readInputStream(getClass().getResourceAsStream(filePath));
        } catch (IOException e) {
            throw new RuntimeException("Reading resource " + filePath + "failed");
        }
    }


}
