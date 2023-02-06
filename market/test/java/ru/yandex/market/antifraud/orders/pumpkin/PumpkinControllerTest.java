package ru.yandex.market.antifraud.orders.pumpkin;

import java.util.Collections;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.antifraud.orders.entity.OrderVerdict;
import ru.yandex.market.antifraud.orders.pumpkin.config.PumpkinControllerConfiguration;
import ru.yandex.market.antifraud.orders.test.annotations.WebLayerTest;
import ru.yandex.market.antifraud.orders.web.AntifraudJsonUtil;
import ru.yandex.market.antifraud.orders.web.dto.LoyaltyBuyerRestrictionsDto;
import ru.yandex.market.antifraud.orders.web.dto.LoyaltyBuyerRestrictionsRequestDto;
import ru.yandex.market.antifraud.orders.web.dto.LoyaltyVerdictDto;
import ru.yandex.market.antifraud.orders.web.dto.OrderCountRequestDtoV2;
import ru.yandex.market.antifraud.orders.web.dto.OrderCountResponseDtoV2;
import ru.yandex.market.antifraud.orders.web.entity.LoyaltyVerdictType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author: aproskriakov
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebLayerTest(PumpkinControllerConfiguration.class)
public class PumpkinControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testPumpkinDetect() throws Exception {
        mockMvc.perform(
                post("/pumpkin/antifraud/detect")
                    .content("{\"uid\": 123}")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().json(AntifraudJsonUtil.OBJECT_MAPPER.writeValueAsString(OrderVerdict.EMPTY)));
    }

    @Test
    public void testPumpkinLoyaltyDetect() throws Exception {
        LoyaltyVerdictDto res = new LoyaltyVerdictDto(LoyaltyVerdictType.OK, Collections.emptyList(), Collections.emptyList(), null);

        mockMvc.perform(
                post("/pumpkin/antifraud/loyalty/detect")
                    .content("{\"uid\": 123}")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().json(AntifraudJsonUtil.OBJECT_MAPPER.writeValueAsString(res)));
    }

    @Test
    public void testPumpkinLoyaltyRestrictions() throws Exception {
        long uid = 123L;
        LoyaltyBuyerRestrictionsRequestDto req = new LoyaltyBuyerRestrictionsRequestDto(uid, null);
        LoyaltyBuyerRestrictionsDto res = LoyaltyBuyerRestrictionsDto.ok(uid, null);

        mockMvc.perform(
                post("/pumpkin/antifraud/loyalty/restrictions")
                        .content(AntifraudJsonUtil.OBJECT_MAPPER.writeValueAsString(req))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(AntifraudJsonUtil.OBJECT_MAPPER.writeValueAsString(res)));
    }

    @Test
    public void testPumpkinLoyaltyOrdersCountV2() throws Exception {
        long puid = 123L;
        OrderCountRequestDtoV2 req = OrderCountRequestDtoV2.builder()
            .puid(puid)
            .build();
        OrderCountResponseDtoV2 res = OrderCountResponseDtoV2.builder()
            .puid(puid)
            .build();

        mockMvc.perform(asyncDispatch(
                mockMvc.perform(
                        post("/pumpkin/antifraud/loyalty/orders-count/v2")
                            .content(AntifraudJsonUtil.OBJECT_MAPPER.writeValueAsString(req))
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON))
                    .andReturn()))
            .andExpect(status().isOk())
            .andExpect(content().json(AntifraudJsonUtil.OBJECT_MAPPER.writeValueAsString(res)));
    }
}
