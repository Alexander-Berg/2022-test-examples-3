package ru.yandex.market.fulfillment.stockstorage;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class GuiControllerTest extends AbstractContextualTest {

    @Test
    @DatabaseSetup("classpath:database/states/gui/sku/setup_database.xml")
    public void getSkuGrid() throws Exception {
        String result = mockMvc.perform(get("/gui/sku"))
                .andExpect(status().isOk())
                .andReturn().getResponse()
                .getContentAsString();
        softly
                .assertThat(result)
                .is(jsonMatchingWithoutOrder(extractFileContent("response/gui/sku/get_sku_grid.json")));
    }

    @Test
    @DatabaseSetup("classpath:database/states/gui/sku/setup_database.xml")
    public void getSkuGridFilteredStrictEqual() throws Exception {
        String result = mockMvc.perform(get("/gui/sku/?sku=sku0"))
                .andExpect(status().isOk())
                .andReturn().getResponse()
                .getContentAsString();
        softly
                .assertThat(result)
                .is(jsonMatchingWithoutOrder(extractFileContent("response/gui/sku/get_sku_grid_filtered.json")));
    }

    @Test
    @DatabaseSetup("classpath:database/states/gui/sku/setup_database.xml")
    public void getSkuGridFilteredWhenNoSuchSku() throws Exception {
        String result = mockMvc.perform(get("/gui/sku/?sku=sku01234"))
                .andExpect(status().isOk())
                .andReturn().getResponse()
                .getContentAsString();
        softly
                .assertThat(result)
                .is(jsonMatchingWithoutOrder(extractFileContent("response/gui/sku/get_sku_grid_without_items.json")));
    }

    @Test
    @DatabaseSetup("classpath:database/states/gui/sku/setup_database.xml")
    public void getSkuGridFilteredBadRequest() throws Exception {
        String result = mockMvc.perform(get("/gui/sku/?vendorId=vendorId0"))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse()
                .getContentAsString();
        softly.assertThat(result).is(jsonMatching(
                "{\"code\":\"BAD_REQUEST\",\"message\":\"Filter params should contain at least sku" +
                        " or vendorId + warehouseId\"}"));
    }

    @Test
    @DatabaseSetup("classpath:database/states/gui/sku/setup_database.xml")
    public void successfulGetSkuDetails() throws Exception {
        String result = mockMvc.perform(get("/gui/sku/10000"))
                .andExpect(status().isOk())
                .andReturn().getResponse()
                .getContentAsString();

        softly
                .assertThat(result)
                .is(jsonMatchingWithoutOrder(extractFileContent(
                        "response/gui/sku/get_sku_details.json")));
    }


    @Test
    @DatabaseSetup("classpath:database/states/gui/sku/setup_database.xml")
    public void successfulGetSkuDetailsWithoutStocks() throws Exception {
        String result = mockMvc.perform(get("/gui/sku/10002"))
                .andExpect(status().isOk())
                .andReturn().getResponse()
                .getContentAsString();

        softly
                .assertThat(result)
                .is(jsonMatchingWithoutOrder(extractFileContent(
                        "response/gui/sku/get_sku_details_without_stocks.json")));
    }

    @Test
    @DatabaseSetup("classpath:database/states/gui/sku/setup_database.xml")
    public void getSkuStockDetails() throws Exception {
        String result = mockMvc.perform(get("/gui/sku/10000/stocks"))
                .andExpect(status().isOk())
                .andReturn().getResponse()
                .getContentAsString();

        softly
                .assertThat(result)
                .is(jsonMatchingWithoutOrder(extractFileContent(
                        "response/gui/sku/get_sku_stocks_details.json")));
    }

    @Test
    @DatabaseSetup("classpath:database/states/gui/sku/setup_database.xml")
    public void getSkuFreezeDetails() throws Exception {
        String result = mockMvc.perform(get("/gui/sku/10000/freezes/FIT"))
                .andExpect(status().isOk())
                .andReturn().getResponse()
                .getContentAsString();

        softly
                .assertThat(result)
                .is(jsonMatchingWithoutOrder(extractFileContent(
                        "response/gui/sku/get_sku_freeze_details.json")));
    }

    @Test
    @DatabaseSetup("classpath:database/states/gui/sku/setup_database.xml")
    public void getSkuFreezeDetailsInUnfreezeQueue() throws Exception {
        String result = mockMvc.perform(get("/gui/sku/10000/freezes/PREORDER"))
                .andExpect(status().isOk())
                .andReturn().getResponse()
                .getContentAsString();

        softly
                .assertThat(result)
                .is(jsonMatchingWithoutOrder(extractFileContent(
                        "response/gui/sku/get_sku_freeze_details_in_unfreeze_queue.json")));
    }


    @Test
    @DatabaseSetup("classpath:database/states/gui/sku/setup_database.xml")
    public void getSkuFreezeDetailsNoSuchFreezes() throws Exception {
        String result = mockMvc.perform(get("/gui/sku/10000/freezes/QUARANTINE"))
                .andExpect(status().isOk())
                .andReturn().getResponse()
                .getContentAsString();

        softly
                .assertThat(result)
                .is(jsonMatchingWithoutOrder(extractFileContent(
                        "response/gui/sku/get_sku_freeze_details_no_freezes.json")));
    }

    @Test
    @DatabaseSetup("classpath:database/states/gui/sku/setup_database.xml")
    public void getSkuFreezeDetailsNoSuchStocks() throws Exception {
        String result = mockMvc.perform(get("/gui/sku/10002/freezes/FIT"))
                .andExpect(status().isOk())
                .andReturn().getResponse()
                .getContentAsString();

        softly
                .assertThat(result)
                .is(jsonMatchingWithoutOrder(extractFileContent(
                        "response/gui/sku/get_sku_freeze_details_without_stocks.json")));
    }

    @Test
    @DatabaseSetup("classpath:database/states/gui/sku/setup_database.xml")
    public void unsuccessfulGetSkuDetails() throws Exception {
        mockMvc.perform(get("/gui/sku/1"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(
                        containsString("Can't find sku with id 1"))
                );
    }

    @Test
    @DatabaseSetup({
            "classpath:database/states/backorder/warehouses.xml",
            "classpath:database/states/warehouse_property/1.xml",
            "classpath:database/states/warehouse_property/2.xml"
    })
    public void getPropertiesGrid() throws Exception {
        String result = mockMvc.perform(get("/gui/warehouse"))
                .andExpect(status().isOk())
                .andReturn().getResponse()
                .getContentAsString();

        softly
                .assertThat(result)
                .is(jsonMatchingWithoutOrder(extractFileContent(
                        "response/gui/backorder/get_properties_grid.json")));
    }

    @Test
    @DatabaseSetup("classpath:database/states/backorder/warehouses.xml")
    public void getProperty() throws Exception {
        String result = mockMvc.perform(get("/gui/warehouse/1"))
                .andExpect(status().isOk())
                .andReturn().getResponse()
                .getContentAsString();

        softly
                .assertThat(result)
                .is(jsonMatchingWithoutOrder(extractFileContent("response/gui/backorder/get_property.json")));
    }

    @Test
    @DatabaseSetup("classpath:database/states/backorder/warehouses.xml")
    @ExpectedDatabase(value = "classpath:database/expected/backorder/warehouses.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT)
    public void setProperty() throws Exception {
        String result = mockMvc.perform(post("/gui/warehouse/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent("requests/gui/backorder/set_property.json")))
                .andExpect(status().isOk())
                .andReturn().getResponse()
                .getContentAsString();

        softly
                .assertThat(result)
                .is(jsonMatchingWithoutOrder(extractFileContent("response/gui/backorder/set_property.json")));
    }

    @Test
    public void getNewProperty() throws Exception {
        String result = mockMvc.perform(get("/gui/warehouse/new"))
                .andExpect(status().isOk())
                .andReturn().getResponse()
                .getContentAsString();

        softly
                .assertThat(result)
                .is(jsonMatchingWithoutOrder(extractFileContent(
                        "response/gui/backorder/new_property.json")));
    }

    @Test
    public void propertyNotFound() throws Exception {
        mockMvc.perform(get("/gui/warehouse/1"))
                .andExpect(status().isBadRequest());
    }
}
