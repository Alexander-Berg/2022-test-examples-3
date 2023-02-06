package ru.yandex.market.wms.api.controller.monitoring;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;

import ru.yandex.market.wms.common.spring.IntegrationTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class OrdersWithoutShipmentDateTimeMonitoringControllerTest extends IntegrationTest {
    @Test
    public void testNoOrders() throws Exception {
        mockMvc.perform(get("/monitoring/ordersWithoutShipmentDateTime"))
                .andExpect(status().isOk())
                .andExpect(content().string("0;OK"));
    }

    @Test
    @DatabaseSetup(value = "/monitoring/orders-without-shipmentdatetime/order-with-shipmentdatetime.xml",
            connection = "wmwhseConnection")
    public void testOrderWithShipmentDateTime() throws Exception {
        mockMvc.perform(get("/monitoring/ordersWithoutShipmentDateTime"))
                .andExpect(status().isOk())
                .andExpect(content().string("0;OK"));
    }

    @Test
    @DatabaseSetup(value = "/monitoring/orders-without-shipmentdatetime/order-without-shipmentdatetime.xml",
            connection = "wmwhseConnection")
    public void testOrderWithoutShipmentDateTime() throws Exception {
        mockMvc.perform(get("/monitoring/ordersWithoutShipmentDateTime"))
                .andExpect(status().isOk())
                .andExpect(content().string("2;Number of orders without SHIPMENTDATETIME: 1"));
    }

    @Test
    @DatabaseSetup(value = "/monitoring/orders-without-shipmentdatetime/multiple-orders-without-shipmentdatetime.xml",
            connection = "wmwhseConnection")
    public void testMultipleOrdersWithoutShipmentDateTime() throws Exception {
        mockMvc.perform(get("/monitoring/ordersWithoutShipmentDateTime"))
                .andExpect(status().isOk())
                .andExpect(content().string("2;Number of orders without SHIPMENTDATETIME: 3"));
    }

    @Test
    @DatabaseSetup(
            value = "/monitoring/orders-without-shipmentdatetime/order-without-shipmentdatetime-other-status.xml",
            connection = "wmwhseConnection")
    public void testOrderWithoutShipmentDateTimeAndOtherStatus() throws Exception {
        mockMvc.perform(get("/monitoring/ordersWithoutShipmentDateTime"))
                .andExpect(status().isOk())
                .andExpect(content().string("0;OK"));
    }

    @Test
    @DatabaseSetup(value = "/monitoring/orders-without-shipmentdatetime/order-without-shipmentdatetime-other-type.xml",
            connection = "wmwhseConnection")
    public void testOrderWithoutShipmentDateTimeOtherType() throws Exception {
        mockMvc.perform(get("/monitoring/ordersWithoutShipmentDateTime"))
                .andExpect(status().isOk())
                .andExpect(content().string("0;OK"));
    }
}
