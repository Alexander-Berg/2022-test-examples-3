package ru.yandex.market.sc.internal.controller.internal;


import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.sc.core.domain.courier.model.InternalCourierDto;
import ru.yandex.market.sc.core.domain.courier.repository.Courier;
import ru.yandex.market.sc.core.domain.courier.repository.CourierRepository;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.internal.test.ScIntControllerTest;
import ru.yandex.market.tpl.common.util.JacksonUtil;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ScIntControllerTest
public class InternalCourierControllerTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    TestFactory testFactory;
    @Autowired
    CourierRepository courierRepository;

    Courier courier;

    @BeforeEach
    void init() {
        courier = testFactory.courier();
        courierRepository.save(courier);
    }

    @SneakyThrows
    @Test
    void updateCourier() {
        mockMvc.perform(
                MockMvcRequestBuilders.put("/internal/couriers/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JacksonUtil.toString(new InternalCourierDto(
                                courier.getId(), "Федотов Василий", "", "", "", ""
                        ))))
                .andExpect(status().isOk());
    }
}
