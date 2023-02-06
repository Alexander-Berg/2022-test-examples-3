package ru.yandex.market.oms;

import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;

import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.oms.util.DbTestUtils;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("functionalTest")
@AutoConfigureMockMvc
public class GraphQLTest extends AbstractFunctionalTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private DbTestUtils dbTestUtils;


    private final Long userId = 1L;
    private final Long orderId = 1L;
    private final Long itemId = 1L;
    private final Long deliveryId = 1L;
    private final Long addressId = 1L;

    @BeforeEach
    public void beforeEach() {
        dbTestUtils.insertOrder(orderId, userId, deliveryId, OrderStatus.PROCESSING);
        dbTestUtils.insertOrderItem(orderId, itemId);
        dbTestUtils.insertOrderDelivery(orderId, deliveryId, addressId, new Integer[]{});
        dbTestUtils.insertOrderProperties(orderId);
    }

    @AfterEach
    public void afterEach() {
        dbTestUtils.deleteOrderProperties(orderId);
        dbTestUtils.deleteOrderDelivery(deliveryId, addressId);
        dbTestUtils.deleteAllOrderItems(orderId);
        dbTestUtils.deleteOrder(orderId);
    }

    @Disabled // сломан в CI, контекст не видит src/main/properties.d/*
    @Test
    public void query() throws Exception {
        RequestBuilder request = post("/graphql")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "query", """
                                  query ($orderId: Long!) {
                                      order(id: $orderId) {
                                          status
                                          createdAt
                                          updatedAt
                                          revision
                                          items {
                                              title
                                              shopSku
                                          }
                                          delivery {
                                              type
                                              deliveryPartnerType
                                          }
                                      }
                                  }
                                """,
                        "variables", Map.of(
                                "orderId", orderId
                        )
                )));

        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").doesNotExist())
                .andExpect(jsonPath("$.data.order.status").value("PROCESSING"))
                .andDo(print());
    }

}
