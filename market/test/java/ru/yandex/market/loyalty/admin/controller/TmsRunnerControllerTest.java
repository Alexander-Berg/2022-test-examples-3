package ru.yandex.market.loyalty.admin.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.checkout.checkouter.client.CheckouterClient;
import ru.yandex.market.checkout.checkouter.client.OrderFilter;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvents;
import ru.yandex.market.loyalty.admin.config.MarketLoyaltyAdmin;
import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminMockedDbTest;
import ru.yandex.market.loyalty.core.service.ConfigurationService;
import ru.yandex.market.loyalty.test.TestFor;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySetOf;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestFor(TmsRunnerController.class)
public class TmsRunnerControllerTest extends MarketLoyaltyAdminMockedDbTest {
    private static final String CHECKOUT_PROCESSOR_LOCK_NAME = "processCheckouterEventsBuckets0_4";
    private static final TypeReference<List<String>> LIST_OF_STRINGS = new TypeReference<List<String>>() {
    };

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private CheckouterClient checkouterClient;

    @Autowired
    @MarketLoyaltyAdmin
    private ObjectMapper objectMapper;

    @Before
    public void setUp() {
        configurationService.set(ConfigurationService.CHECKOUTER_LOGBROKER_ENABLED, false);
    }

    @Test
    public void testList() throws Exception {
        String response = mockMvc
                .perform(get("/tms/list"))
                .andDo(log())
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Collection<String> list = objectMapper.readValue(response, LIST_OF_STRINGS);
        assertThat(list, hasItem(CHECKOUT_PROCESSOR_LOCK_NAME));
    }

    @Test
    public void executeSchedule() throws Exception {
        when(checkouterClient.orderHistoryEvents().getOrderHistoryEvents(anyLong(), anyInt(),
                anySetOf(HistoryEventType.class), eq(false), anySetOf(Integer.class)))
                .thenReturn(new OrderHistoryEvents(Collections.emptyList()));

        mockMvc
                .perform(get("/tms/run/{schedule}", CHECKOUT_PROCESSOR_LOCK_NAME))
                .andDo(log())
                .andExpect(status().isOk())
                .andReturn();

        verify(checkouterClient.orderHistoryEvents())
                .getOrderHistoryEvents(anyLong(), anyInt(), anySetOf(HistoryEventType.class), eq(false),
                        anySetOf(Integer.class), any(OrderFilter.class));
    }
}
