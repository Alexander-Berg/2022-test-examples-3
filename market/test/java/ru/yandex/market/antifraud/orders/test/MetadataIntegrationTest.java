package ru.yandex.market.antifraud.orders.test;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;
import java.util.TimeZone;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.antifraud.orders.entity.AntifraudAction;
import ru.yandex.market.antifraud.orders.entity.AntifraudBlacklistRule;
import ru.yandex.market.antifraud.orders.entity.AntifraudBlacklistRuleType;
import ru.yandex.market.antifraud.orders.service.Utils;
import ru.yandex.market.antifraud.orders.storage.dao.AntifraudDao;
import ru.yandex.market.antifraud.orders.storage.dao.ItemLimitRulesDao;
import ru.yandex.market.antifraud.orders.test.annotations.IntegrationTest;
import ru.yandex.passport.tvmauth.CheckedServiceTicket;
import ru.yandex.passport.tvmauth.TvmClient;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@RunWith(SpringJUnit4ClassRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@IntegrationTest
public class MetadataIntegrationTest {

    private static final String CANCEL_ORDER_ACTION = Utils.getBlacklistAction(AntifraudAction.CANCEL_ORDER);

    @Autowired
    private AntifraudDao antifraudDao;

    @Autowired
    private ItemLimitRulesDao itemLimitRulesDao;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TvmClient iTvmClient;

    @Test
    public void getBlacklistRules() throws Exception {
        SimpleDateFormat ft = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss Z");
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Moscow"));
        antifraudDao.saveBlacklistRule(
                new AntifraudBlacklistRule(AntifraudBlacklistRuleType.UID,
                        "123", CANCEL_ORDER_ACTION, "some_reason_1", ft.parse("30-05-2045 00:00:00 +0300"), -1L));
        antifraudDao.saveBlacklistRule(
                new AntifraudBlacklistRule(AntifraudBlacklistRuleType.PHONE,
                        "+7111111111", CANCEL_ORDER_ACTION, "some_reason_2", ft.parse("30-05-2049 00:00:00 +0300"), 555555555L));

        List<String> expectedStrings = Arrays.asList("{\"type\":\"UID\",\"value\":\"123\",\"action\":\"AntifraudAction_CANCEL_ORDER\"," +
                        "\"reason\":\"some_reason_1\",\"expiryAt\":\"30-05-2045 00:00:00\",\"authorUid\":-1}",
                "{\"type\":\"PHONE\",\"value\":\"+7111111111\",\"action\":\"AntifraudAction_CANCEL_ORDER\",\"reason\":\"some_reason_2\"," +
                        "\"expiryAt\":\"30-05-2049 00:00:00\",\"authorUid\":555555555}");
        when(iTvmClient.checkServiceTicket(anyString())).thenReturn(mock(CheckedServiceTicket.class));
        String response = mockMvc.perform(
                get("/metadata/blacklist")
                        .header("X-Ya-Service-Ticket", "token"))
                .andExpect(status().is2xxSuccessful())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(response, CoreMatchers.containsString(expectedStrings.get(0)));
        assertThat(response, CoreMatchers.containsString(expectedStrings.get(1)));
    }

    @Test
    public void postBlacklistRule() throws Exception {
        String newRule = "{\"type\":\"EMAIL\",\"value\":\"test@test.com\",\"reason\":\"some_reason_3\"," +
                "\"expiryAt\":\"30-05-2050 00:00:00\"}";
        when(iTvmClient.checkServiceTicket(anyString())).thenReturn(mock(CheckedServiceTicket.class));
        mockMvc.perform(post("/metadata/blacklist")
                .content(newRule)
                .header("X-Ya-Service-Ticket", "token")
                .header("Content-Type", "application/json"))
                .andExpect(status().is2xxSuccessful());

        Assert.assertNotNull(antifraudDao.getBlacklistRule(
                AntifraudBlacklistRuleType.EMAIL, "test@test.com", CANCEL_ORDER_ACTION));
    }

    @Test
    public void removeBlacklistRule() throws Exception {
        SimpleDateFormat ft = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss Z");
        antifraudDao.saveBlacklistRule(
                new AntifraudBlacklistRule(AntifraudBlacklistRuleType.EMAIL,
                        "test2@test.com", CANCEL_ORDER_ACTION, "some_reason_1", ft.parse("30-05-2045 00:00:00 +0300"), -1L));
        String ruleToRemove = "{\"type\":\"EMAIL\",\"value\":\"test2@test.com\",\"reason\":\"some_reason_3\"," +
                "\"expiryAt\":\"30-05-2050 00:00:00\"}";
        when(iTvmClient.checkServiceTicket(anyString())).thenReturn(mock(CheckedServiceTicket.class));
        mockMvc.perform(delete("/metadata/blacklist")
                .content(ruleToRemove)
                .header("Content-Type", "application/json")
                .header("X-Ya-Service-Ticket", "token"))
                .andExpect(status().is2xxSuccessful());

        Assert.assertNull(antifraudDao.getBlacklistRule(
                AntifraudBlacklistRuleType.EMAIL, "test2@test.com", CANCEL_ORDER_ACTION));
    }

    @Test
    public void getRoles() throws Exception {
        mockMvc.perform(get("/metadata/roles"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(r -> System.out.println(r.getResponse().getContentAsString()));
    }


    @Test
    public void getUsersByRole() throws Exception {
        mockMvc.perform(get("/metadata/roles/order_guys/users"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(r -> System.out.println(r.getResponse().getContentAsString()));
    }

    @Test
    public void addItemLimitRules() throws Exception {
        mockMvc.perform(post("/metadata/item-limit-rules")
                .contentType(MediaType.APPLICATION_JSON)
                .content("[{\"tag\":\"123\",\"modelId\":12,\"maxAmountPerUser\":35}]"))
                .andExpect(status().is2xxSuccessful());
        assertThat(itemLimitRulesDao.getAllRules(), hasSize(greaterThan(1)));
    }
}
