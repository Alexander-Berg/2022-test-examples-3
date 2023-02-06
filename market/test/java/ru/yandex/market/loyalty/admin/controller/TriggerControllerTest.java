package ru.yandex.market.loyalty.admin.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.loyalty.admin.controller.dto.TriggerDto;
import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminMockedDbTest;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import ru.yandex.market.loyalty.core.utils.TriggersFactory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.loyalty.lightweight.ExceptionUtils.makeExceptionsUnchecked;

public class TriggerControllerTest extends MarketLoyaltyAdminMockedDbTest {
    private static final ThreadLocal<SimpleDateFormat> DATE_FORMAT = ThreadLocal.withInitial(
            () -> new SimpleDateFormat("yyyy-MM-dd")
    );

    private static final Date EMISSION_DATE_FROM = makeExceptionsUnchecked(() -> DATE_FORMAT.get().parse("2018-01-01"));

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    PromoManager promoManager;
    @Autowired
    private TriggersFactory triggersFactory;

    @Before
    public void init() {
        Promo promo = promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse()
                .setStartEmissionDate(EMISSION_DATE_FROM)
        );

        triggersFactory.createSubscriptionTrigger(promo);
    }

    private static final TypeReference<List<TriggerDto>> LIST_OF_TRIGGER_DTO = new TypeReference<List<TriggerDto>>() {
    };

    @Test
    public void getAllTriggersTest() throws Exception {
        String jsonResponse = mockMvc
                .perform(get("/api/trigger"))
                .andDo(log())
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        List<TriggerDto> triggers = objectMapper.readValue(jsonResponse, LIST_OF_TRIGGER_DTO);
        assertEquals(1, triggers.size());
    }
}
