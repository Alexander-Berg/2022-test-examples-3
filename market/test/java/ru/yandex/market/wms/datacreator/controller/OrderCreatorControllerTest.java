package ru.yandex.market.wms.datacreator.controller;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DatabaseTearDown;
import org.junit.jupiter.api.Test;

import ru.yandex.market.wms.datacreator.config.DataCreatorIntegrationTest;

import static com.github.springtestdbunit.annotation.DatabaseOperation.DELETE_ALL;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

class OrderCreatorControllerTest extends DataCreatorIntegrationTest {

    @Test
    @DatabaseSetup(value = "/controller/order/getSumQtyOpen/before.xml",
            connection = "wmwhse1Connection")
    @DatabaseTearDown(value = "/controller/order/tear-down.xml", connection = "wmwhse1Connection", type = DELETE_ALL)
    void getSumQtyOpenForOrder() throws Exception {
        mockMvc.perform(get("/order/getSumQtyOpen/0005952016"))
                .andExpect(content().string("4"))
                .andReturn();
    }

    @Test
    @DatabaseSetup(value = "/controller/order/getSumQtyOpen/before.xml",
            connection = "wmwhse1Connection")
    @DatabaseTearDown(value = "/controller/order/tear-down.xml", connection = "wmwhse1Connection", type = DELETE_ALL)
    void getSumQtyOpenForOrderWithZeroOpenQty() throws Exception {
        mockMvc.perform(get("/order/getSumQtyOpen/0005952017"))
                .andExpect(content().string("0"))
                .andReturn();
    }

    @Test
    @DatabaseSetup(value = "/controller/order/getSumQtyOpen/before.xml",
            connection = "wmwhse1Connection")
    @DatabaseTearDown(value = "/controller/order/tear-down.xml", connection = "wmwhse1Connection", type = DELETE_ALL)
    void getSumQtyForOrderForNonExistingOrder() throws Exception {
        final String nonExistingOrderKey = "0005952020";
        mockMvc.perform(get("/order/getSumQtyOpen/" + nonExistingOrderKey))
                .andExpect(status().is5xxServerError())
                .andExpect(jsonPath("$['message']")
                        .value("No orders were found for orderKey " + nonExistingOrderKey))
                .andReturn();
    }

    @Test
    @DatabaseSetup(value = "/controller/order/getOrderStatusHistory/before.xml",
            connection = "wmwhse1Connection")
    @DatabaseTearDown(value = "/controller/order/tear-down.xml", connection = "wmwhse1Connection", type = DELETE_ALL)
    void getHistoryOfShippedOrder() throws Exception {
        mockMvc.perform(get("/order/gerOrderStatusHistory/0005952096"))
                .andExpect(content().string(
                        getFileContent("controller/order/getOrderStatusHistory/shipped-order-response.json")
                ))
                .andExpect(status().isOk())
                .andReturn();
    }

    @Test
    @DatabaseSetup(value = "/controller/order/getOrderStatusHistory/before.xml",
            connection = "wmwhse1Connection")
    @DatabaseTearDown(value = "/controller/order/tear-down.xml", connection = "wmwhse1Connection", type = DELETE_ALL)
    void getHistoryOfNewOrder() throws Exception {
        mockMvc.perform(get("/order/gerOrderStatusHistory/0005952097"))
                .andExpect(content().string("[\"UNKNOWN\"]"))
                .andExpect(status().isOk())
                .andReturn();
    }

    @Test
    @DatabaseSetup(value = "/controller/order/getOrderStatusHistory/before.xml",
            connection = "wmwhse1Connection")
    @DatabaseTearDown(value = "/controller/order/tear-down.xml", connection = "wmwhse1Connection", type = DELETE_ALL)
    void getHistoryOfNonExistingOrder() throws Exception {
        mockMvc.perform(get("/order/gerOrderStatusHistory/0005952098"))
                .andExpect(content().string("[]"))
                .andExpect(status().isOk())
                .andReturn();
    }
}
