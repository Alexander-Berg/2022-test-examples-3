package ru.yandex.market.deepdive.domain.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.deepdive.domain.order.Order;
import ru.yandex.market.deepdive.domain.order.OrderRepository;
import ru.yandex.market.deepdive.domain.order.OrderService;
import ru.yandex.market.deepdive.domain.order.status.PaymentType;
import ru.yandex.market.deepdive.domain.order.status.Status;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@WebMvcTest(controllers = {OrderController.class, OrderService.class})
@AutoConfigureMockMvc(addFilters = false)
public class OrderControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderRepository repository;

    private static final long PICKUP_POINT_ID = 498L;

    @Before
    public void before() {
        var repositoryOutput = new PageImpl<>(List.of(
                Order.builder()
                        .id(1L).pvzMarketId(101L).pickupPointId(PICKUP_POINT_ID).
                        status(Status.CREATED)
                        .deliveryDate(LocalDate.of(2022, 1, 1))
                        .paymentType(PaymentType.CARD).totalPrice(BigDecimal.valueOf(10L))
                        .build(),
                Order.builder()
                        .id(2L).pvzMarketId(201L).pickupPointId(PICKUP_POINT_ID)
                        .status(Status.CANCELLED)
                        .deliveryDate(LocalDate.of(2022, 2, 2))
                        .paymentType(PaymentType.PREPAID).totalPrice(BigDecimal.valueOf(103.2))
                        .build()
        ), PageRequest.of(0, 20), 2);

        Mockito.when(repository.findAllByPickupPointIdAndStatusInAndPaymentTypeIn(any(),
                any(), any(), any())).thenReturn(repositoryOutput);

    }

    @Test
    public void getAllOrdersTest() throws Exception {
        String fullJsonOutput = "{\"content\":[{\"id\":1,\"pickupPointId\":101,\"status\":\"CREATED\"," +
                "\"deliveryDate\":\"2022-01-01\",\"paymentType\":\"CARD\",\"totalPrice\":10},{\"id\":2," +
                "\"pickupPointId\":201,\"status\":\"CANCELLED\",\"deliveryDate\":\"2022-02-02\"," +
                "\"paymentType\":\"PREPAID\",\"totalPrice\":103.2}],\"pageable\":{\"sort\":{\"sorted\":false," +
                "\"unsorted\":true,\"empty\":true},\"pageNumber\":0,\"pageSize\":20,\"offset\":0,\"paged\":true," +
                "\"unpaged\":false},\"totalPages\":1,\"totalElements\":2,\"last\":true,\"numberOfElements\":2," +
                "\"sort\":{\"sorted\":false,\"unsorted\":true,\"empty\":true},\"first\":true,\"number\":0," +
                "\"size\":20,\"empty\":false}";

        mockMvc.perform(get("/api/orders/" + PICKUP_POINT_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content()
                        .json(fullJsonOutput));
    }

    @Test
    public void badRequestTest1() throws Exception {
        String invalidStatusParam = "BROKEN";
        mockMvc.perform(get("/api/orders/" + PICKUP_POINT_ID).param("statuses", invalidStatusParam))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void badRequestTest2() throws Exception {
        String invalidPaymentParam = "BROKEN";
        mockMvc.perform(get("/api/orders/" + PICKUP_POINT_ID).param("resultPaymentType", invalidPaymentParam))
                .andExpect(status().isBadRequest());
    }


}
