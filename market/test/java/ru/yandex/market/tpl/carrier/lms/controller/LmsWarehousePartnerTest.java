package ru.yandex.market.tpl.carrier.lms.controller;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehousePartner;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehousePartnerRepository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor(onConstructor_=@Autowired)
public class LmsWarehousePartnerTest extends LmsControllerTest{

    private final OrderWarehousePartnerRepository orderWarehousePartnerRepository;

    @BeforeEach
    void setUp() {
        orderWarehousePartnerRepository.saveAndFlush(
                new OrderWarehousePartner("123", "123")
        );

        orderWarehousePartnerRepository.saveAndFlush(
                new OrderWarehousePartner("234", "234")
        );
    }


    @SneakyThrows
    @Test
    void shouldReturnWarehousePartnerPage() {
        mockMvc.perform(get("/LMS/carrier/warehouse-partners"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").value(Matchers.hasSize(2)));
    }
}
