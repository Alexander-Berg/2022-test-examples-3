package ru.yandex.market.antifraud.orders.web.controller;

import java.text.SimpleDateFormat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.antifraud.orders.entity.AntifraudAction;
import ru.yandex.market.antifraud.orders.entity.AntifraudBlacklistRule;
import ru.yandex.market.antifraud.orders.entity.AntifraudBlacklistRuleType;
import ru.yandex.market.antifraud.orders.service.BlacklistService;
import ru.yandex.market.antifraud.orders.service.Utils;
import ru.yandex.market.antifraud.orders.service.notification.NotificationService;
import ru.yandex.market.antifraud.orders.test.annotations.WebLayerTest;
import ru.yandex.market.antifraud.orders.web.dto.loyalty.BonusState;
import ru.yandex.market.antifraud.orders.web.entity.LoyaltyVerdictType;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@WebLayerTest(RestrictionController.class)
public class RestrictionControllerTest {

    private static final String CANCEL_ORDER_ACTION = Utils.getBlacklistAction(AntifraudAction.CANCEL_ORDER);
    private static final String LOYALTY_BLACKLIST_ACTION = Utils.getBlacklistAction(LoyaltyVerdictType.BLACKLIST);
    private static final String BONUS_DISABLED_ACTION = Utils.getBlacklistAction(BonusState.DISABLED);

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BlacklistService blacklistService;
    @MockBean
    private NotificationService notificationService;

    @Test
    public void postBlacklistRule() throws Exception {
        SimpleDateFormat ft = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss Z");
        String newRule =
                "{\"type\":\"EMAIL\",\"value\":\"test@test.com\",\"reason\":\"some_reason_3\"," +
                        "\"expiryAt\":\"30-05-2050 00:00:00\"}";
        String response =
                "{\"type\":\"EMAIL\",\"value\":\"test@test.com\",\"reason\":\"some_reason_3\"," +
                        "\"expiryAt\":\"30-05-2050 00:00:00\"}";
        AntifraudBlacklistRule rule = new AntifraudBlacklistRule(
                AntifraudBlacklistRuleType.EMAIL,
                "test@test.com",
                CANCEL_ORDER_ACTION,
                "some_reason_3",
                ft.parse("30-05-2050 00:00:00 +0300"),
                555555555L
        );
        when(blacklistService.saveBlacklistRule(any())).thenReturn(rule);
        mockMvc.perform(
                        post("/restriction/blacklist")
                                .content(newRule)
                                .header("Content-Type", "application/json"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(response));
    }

    @Test
    public void postLoyaltyBlacklistRule() throws Exception {
        SimpleDateFormat ft = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss Z");
        String newRule =
                "{\"type\":\"EMAIL\",\"value\":\"test@test.com\",\"reason\":\"some_reason_3\"," +
                        "\"expiryAt\":\"30-05-2050 00:00:00\"}";
        String response =
                "{\"type\":\"EMAIL\",\"value\":\"test@test.com\",\"reason\":\"some_reason_3\"," +
                        "\"expiryAt\":\"30-05-2050 00:00:00\"}";
        AntifraudBlacklistRule rule = new AntifraudBlacklistRule(
                AntifraudBlacklistRuleType.EMAIL,
                "test@test.com",
                LOYALTY_BLACKLIST_ACTION,
                "some_reason_3",
                ft.parse("30-05-2050 00:00:00 +0300"),
                555555555L
        );
        when(blacklistService.saveBlacklistRule(any())).thenReturn(rule);
        mockMvc.perform(
                        post("/restriction/loyalty")
                                .content(newRule)
                                .header("Content-Type", "application/json"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(response));
    }

    @Test
    public void postBonusBlacklistRule() throws Exception {
        SimpleDateFormat ft = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss Z");
        String newRule =
                "{\"type\":\"EMAIL\",\"value\":\"test@test.com\",\"reason\":\"some_reason_3\"," +
                        "\"expiryAt\":\"30-05-2050 00:00:00\"}";
        String response =
                "{\"type\":\"EMAIL\",\"value\":\"test@test.com\",\"reason\":\"some_reason_3\"," +
                        "\"expiryAt\":\"30-05-2050 00:00:00\"}";
        AntifraudBlacklistRule rule = new AntifraudBlacklistRule(
                AntifraudBlacklistRuleType.EMAIL,
                "test@test.com",
                BONUS_DISABLED_ACTION,
                "some_reason_3",
                ft.parse("30-05-2050 00:00:00 +0300"),
                555555555L
        );
        when(blacklistService.saveBlacklistRule(any())).thenReturn(rule);
        mockMvc.perform(
                        post("/restriction/bonus")
                                .content(newRule)
                                .header("Content-Type", "application/json"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(response));
    }

    @Test
    public void removeBlacklistRule() throws Exception {
        SimpleDateFormat ft = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss Z");
        String newRule =
                "{\"type\":\"EMAIL\",\"value\":\"test@test.com\",\"reason\":\"some_reason_3\"," +
                        "\"expiryAt\":\"30-05-2050 00:00:00\"}";
        String response =
                "{\"type\":\"EMAIL\",\"value\":\"test@test.com\",\"reason\":\"some_reason_3\"," +
                        "\"expiryAt\":\"30-05-2050 00:00:00\"}";
        AntifraudBlacklistRule rule = new AntifraudBlacklistRule(
                AntifraudBlacklistRuleType.EMAIL,
                "test@test.com",
                CANCEL_ORDER_ACTION,
                "some_reason_3",
                ft.parse("30-05-2050 00:00:00 +0300"),
                555555555L
        );
        when(blacklistService.removeBlacklistRule(any())).thenReturn(rule);
        mockMvc.perform(
                        delete("/restriction/blacklist")
                                .content(newRule)
                                .header("Content-Type", "application/json"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(response));
    }

    @Test
    public void removeLoyaltyBlacklistRule() throws Exception {
        SimpleDateFormat ft = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss Z");
        String newRule =
                "{\"type\":\"EMAIL\",\"value\":\"test@test.com\",\"reason\":\"some_reason_3\"," +
                        "\"expiryAt\":\"30-05-2050 00:00:00\"}";
        String response =
                "{\"type\":\"EMAIL\",\"value\":\"test@test.com\",\"reason\":\"some_reason_3\"," +
                        "\"expiryAt\":\"30-05-2050 00:00:00\"}";
        AntifraudBlacklistRule rule = new AntifraudBlacklistRule(
                AntifraudBlacklistRuleType.EMAIL,
                "test@test.com",
                LOYALTY_BLACKLIST_ACTION,
                "some_reason_3",
                ft.parse("30-05-2050 00:00:00 +0300"),
                555555555L
        );
        when(blacklistService.removeBlacklistRule(any())).thenReturn(rule);
        mockMvc.perform(
                        delete("/restriction/loyalty")
                                .content(newRule)
                                .header("Content-Type", "application/json"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(response));
    }

    @Test
    public void removeBonusBlacklistRule() throws Exception {
        SimpleDateFormat ft = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss Z");
        String newRule =
                "{\"type\":\"EMAIL\",\"value\":\"test@test.com\",\"reason\":\"some_reason_3\"," +
                        "\"expiryAt\":\"30-05-2050 00:00:00\"}";
        String response =
                "{\"type\":\"EMAIL\",\"value\":\"test@test.com\",\"reason\":\"some_reason_3\"," +
                        "\"expiryAt\":\"30-05-2050 00:00:00\"}";
        AntifraudBlacklistRule rule = new AntifraudBlacklistRule(
                AntifraudBlacklistRuleType.EMAIL,
                "test@test.com",
                BONUS_DISABLED_ACTION,
                "some_reason_3",
                ft.parse("30-05-2050 00:00:00 +0300"),
                555555555L
        );
        when(blacklistService.removeBlacklistRule(any())).thenReturn(rule);
        mockMvc.perform(
                        delete("/restriction/bonus")
                                .content(newRule)
                                .header("Content-Type", "application/json"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(response));
    }

}
