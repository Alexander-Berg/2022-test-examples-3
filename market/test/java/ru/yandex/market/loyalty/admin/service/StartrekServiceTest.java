package ru.yandex.market.loyalty.admin.service;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminMockedDbTest;
import ru.yandex.market.loyalty.test.TestFor;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.loyalty.admin.config.MarketLoyaltyAdminMockConfigurer.StartrekServiceConfiguration.TEST_TICKET_STATUS_KEY;
import static ru.yandex.market.loyalty.admin.config.MarketLoyaltyAdminMockConfigurer.StartrekServiceConfiguration.STATUS_CLIENT;
import static ru.yandex.market.loyalty.admin.config.MarketLoyaltyAdminMockConfigurer.StartrekServiceConfiguration.TEST_TICKET_MOCKED_STATUS;
import static ru.yandex.market.loyalty.admin.config.MarketLoyaltyAdminMockConfigurer.StartrekServiceConfiguration.TEST_TICKET_OK;

@TestFor(StartrekService.class)
public class StartrekServiceTest extends MarketLoyaltyAdminMockedDbTest {
    @Autowired
    private StartrekService startrekService;

    @Test
    public void shouldGetCachedTicketStatusName() {
        Assert.assertEquals(TEST_TICKET_MOCKED_STATUS, startrekService.getTicketStatusByKey("same_status"));
        Assert.assertEquals(TEST_TICKET_MOCKED_STATUS, startrekService.getTicketStatusByKey("same_status"));
        Assert.assertEquals(TEST_TICKET_MOCKED_STATUS, startrekService.getTicketStatusByKey("another_status"));
        verify(STATUS_CLIENT, times(2)).getName();
    }

    @Test
    public void miscTests() {
        Assert.assertTrue(startrekService.ticketExists(TEST_TICKET_OK)); // simple mocked answer
        Assert.assertEquals(TEST_TICKET_STATUS_KEY, startrekService.getTicketStatusKey(TEST_TICKET_OK)); // simple
        // mocked answer
    }

}
