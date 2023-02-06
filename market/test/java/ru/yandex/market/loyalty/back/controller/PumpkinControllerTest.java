package ru.yandex.market.loyalty.back.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.loyalty.api.model.perk.PerkStatResponse;
import ru.yandex.market.loyalty.api.model.perk.PerkType;
import ru.yandex.market.loyalty.api.model.web.LoyaltyTag;
import ru.yandex.market.loyalty.api.utils.PumpkinUtils;
import ru.yandex.market.loyalty.back.config.BackTestConfig;
import ru.yandex.market.loyalty.core.config.Blackbox;
import ru.yandex.market.loyalty.core.test.BlackboxUtils;
import ru.yandex.market.loyalty.core.test.LoyaltySpringTestRunner;
import ru.yandex.market.loyalty.test.TestFor;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_UID;

/**
 * Created by aproskriakov on 10/25/21
 */
@TestFor(PumpkinController.class)
@RunWith(LoyaltySpringTestRunner.class)
@WebAppConfiguration
@AutoConfigureMockMvc
@ContextConfiguration(classes = BackTestConfig.class)
public class PumpkinControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    @Blackbox
    private RestTemplate blackboxRestTemplate;
    @Autowired
    protected ObjectMapper objectMapper;

    @Test
    public void testPumpkinHeader() throws Exception {
        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);

        mockMvc.perform(
                get("/pumpkin/perk/status")
                        .queryParam(LoyaltyTag.UID, Long.toString(DEFAULT_UID))
                        .queryParam(LoyaltyTag.REGION_ID, "213"))
                .andExpect(status().isOk())
                .andExpect(header().exists(PumpkinUtils.PUMPKIN_HEADER_NAME));
    }

    @Test
    public void shouldRequestYandexPlusPerkTypeOnly() throws Exception {
        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);

        final PerkStatResponse perkStatResponse = objectMapper.readValue(mockMvc.perform(
                get("/pumpkin/perk/status")
                        .queryParam(LoyaltyTag.UID, Long.toString(DEFAULT_UID))
                        .queryParam(LoyaltyTag.REGION_ID, "213")
                        .queryParam(LoyaltyTag.PERK_TYPE, PerkType.YANDEX_PLUS.getCode())
                        .queryParam(LoyaltyTag.PERK_TYPE, PerkType.YANDEX_EMPLOYEE.getCode())
                        .queryParam(LoyaltyTag.PERK_TYPE, PerkType.YANDEX_EXTRA_CASHBACK.getCode())
                        .queryParam(LoyaltyTag.NO_CACHE, "true"))
                .andExpect(status().isOk())
                .andExpect(header().exists(PumpkinUtils.PUMPKIN_HEADER_NAME))
                .andReturn().getResponse().getContentAsString(), PerkStatResponse.class);

        assertThat(perkStatResponse.getStatuses(), hasSize(1));
        assertThat(perkStatResponse.getStatuses().get(0), allOf(
                hasProperty("type", equalTo(PerkType.YANDEX_PLUS)),
                hasProperty("purchased", equalTo(true))));
    }
}
