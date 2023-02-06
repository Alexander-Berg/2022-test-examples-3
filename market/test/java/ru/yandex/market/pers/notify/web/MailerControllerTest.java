package ru.yandex.market.pers.notify.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import ru.yandex.market.pers.notify.mock.MockedBlackBoxPassportService;
import ru.yandex.market.pers.notify.model.SubscriptionSettings;
import ru.yandex.market.pers.notify.model.Uid;
import ru.yandex.market.pers.notify.settings.SubscriptionAndIdentityService;
import ru.yandex.market.pers.notify.test.MarketMailerMockedDbTest;

import java.util.Collections;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebAppConfiguration
public class MailerControllerTest extends MarketMailerMockedDbTest {

    private static final long UID = 123L;
    private static final String EMAIL = "somemail@exmaple.com";
    @Autowired
    private WebApplicationContext wac;

    private MockMvc mockMvc;

    @Autowired
    private SubscriptionAndIdentityService subscriptionAndIdentityService;
    @Autowired
    private MockedBlackBoxPassportService blackBoxPassportService;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    public void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
        blackBoxPassportService.doReturn(UID, EMAIL);
    }

    @Test
    public void testMonitorings() throws Exception {
        subscriptionAndIdentityService.setSubscriptionsSettings(new Uid(UID), EMAIL, SubscriptionSettings.fromNotificationTypes(Collections.emptySet()));
        checkMonitoringIsOk("IO_ERROR_CNT");
        checkMonitoringIsOk("NO_NEW_SPAM");
    }

    private void checkMonitoringIsOk(String monitoring) throws Exception {
        mockMvc.perform(get("/stat?monitoring=" + monitoring)).andDo(print())
            .andExpect(status().is2xxSuccessful())
            .andExpect(content().string("0;Ok"));
    }

    private void checkMonitoringIsCrit(String monitoring, String expectedMessage) throws Exception {
        mockMvc.perform(get("/stat?monitoring=" + monitoring)).andDo(print())
            .andExpect(status().is2xxSuccessful())
            .andExpect(content().string("2;" + expectedMessage));
    }
}
