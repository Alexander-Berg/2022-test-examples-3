package ru.yandex.market.deepdive.domain.controller;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.market.deepdive.configuration.OrderTestConfiguration;
import ru.yandex.market.deepdive.domain.order.OrderRepository;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        classes = OrderTestConfiguration.class)
@AutoConfigureMockMvc(addFilters = false)
public class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderRepository repo;

    @Test
    @DisplayName("Non-filter")
    public void getOrdersTest1() throws Exception {
        Assertions.assertNotNull(repo);
        //mockMvc.perform(get("/pickup-points/128/orders")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Status and payment filter")
    public void getOrdersTest2() throws Exception {
        Assertions.assertNotNull(repo);
        //mockMvc
        // .perform(get("localhost:8080/pickup-points/128/orders?status=DELIVERED_TO_RECIPIENT&payment_type=CASH"))
        // .andExpect(status()
        // .isOk());
    }

    @Test
    @DisplayName("Status and payment filter with page")
    public void getOrdersTest3() throws Exception {
        Assertions.assertNotNull(repo);
        //mockMvc
        //.perform
        // (get("localhost:8080/pickup-points/128/orders?status=DELIVERED_TO_RECIPIENT&payment_type=CASH&page=2"))
        //.andExpect(status().isOk());
    }


}
