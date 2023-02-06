package ru.yandex.market.mbo.cms.auth;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class TvmTicketManagerTest {

    public static final String DUMMY_TICKET = "dummy";
    private TvmTicketManager tvmTicketManager;

    @Before
    public void init() {
        tvmTicketManager = Mockito.spy(TvmTicketManager.class);
    }

    @Test
    public void testCaching() {
        Mockito.doReturn(DUMMY_TICKET).when(tvmTicketManager).obtainTvmTicket();

        String ticket1 = tvmTicketManager.getTvmTicket();
        String ticket2 = tvmTicketManager.getTvmTicket();

        Mockito.verify(tvmTicketManager, Mockito.times(1)).obtainTvmTicket();
        Assert.assertEquals(DUMMY_TICKET, ticket1);
        Assert.assertEquals(DUMMY_TICKET, ticket2);
    }
}
