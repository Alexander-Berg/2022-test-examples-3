package ru.yandex.market.antifraud.orders.web.controller;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.antifraud.orders.entity.ue.OrderUeCalculationRequest;
import ru.yandex.market.antifraud.orders.entity.ue.OrderUeCalculationResult;
import ru.yandex.market.antifraud.orders.entity.ue.UeAddressDto;
import ru.yandex.market.antifraud.orders.entity.ue.UeBuyerDto;
import ru.yandex.market.antifraud.orders.entity.ue.UeDeliveryDto;
import ru.yandex.market.antifraud.orders.entity.ue.UeDeliveryType;
import ru.yandex.market.antifraud.orders.entity.ue.UeOrderDto;
import ru.yandex.market.antifraud.orders.entity.ue.UeParcelDto;
import ru.yandex.market.antifraud.orders.entity.ue.UeParcelItemDto;
import ru.yandex.market.antifraud.orders.service.ue.UnitEconomicService;
import ru.yandex.market.antifraud.orders.test.annotations.WebLayerTest;
import ru.yandex.market.antifraud.orders.web.AntifraudJsonUtil;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author dzvyagin
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebLayerTest(UnitEconomicController.class)
public class UnitEconomicControllerTest {

    @Autowired
    private MockMvc mockMvc;


    @MockBean
    private UnitEconomicService unitEconomicService;

    @Test
    public void getUE() throws Exception {
        when(unitEconomicService.calculateUe(any()))
                .thenReturn(new OrderUeCalculationResult(
                        new BigDecimal("15"),
                        new BigDecimal("15"),
                        new BigDecimal("0.1")
                ));
        UeBuyerDto buyer = UeBuyerDto.builder()
                .uid(1L)
                .uuid("uuid")
                .yandexUid("yandexuid")
                .email("mail@yandex.ru")
                .normalizedPhone("88005553535")
                .build();
        UeDeliveryDto delivery = UeDeliveryDto.builder()
                .buyerAddress(new UeAddressDto("Russia", "123456", "Omsk", 123L))
                .shopAddress(new UeAddressDto("Russia", "234566", "Novosibirsk", 234L))
                .deliveryPrice(BigDecimal.TEN)
                .deliveryServiceId(101L)
                .deliveryServiceName("Boxberry")
                .deliveryType(UeDeliveryType.PICKUP)
                .outletId(1234L)
                .parcels(List.of(UeParcelDto.builder()
                        .id(1L)
                        .depth(100L)
                        .height(100L)
                        .weight(100L)
                        .parcelItems(List.of(
                                UeParcelItemDto.builder()
                                        .msku(12345L)
                                        .count(2)
                                        .depth(90L)
                                        .weight(90L)
                                        .height(90L)
                                        .itemId(123L)
                                        .price(BigDecimal.TEN)
                                        .supplierId(112L)
                                        .width(90L)
                                        .build()
                        ))
                        .build()))
                .build();
        UeOrderDto order = UeOrderDto.builder()
            .id(1L)
            .creationDate(Instant.now())
            .buyer(buyer)
            .delivery(delivery)
            .build();
        OrderUeCalculationRequest request = new OrderUeCalculationRequest(List.of(order), null);
        mockMvc.perform(
                post("/ue/order")
                        .content(AntifraudJsonUtil.toJson(request))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .accept(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"revenue\": 15, \"cost\": 15, \"unitEconomic\": 0.1}"));
    }
}
