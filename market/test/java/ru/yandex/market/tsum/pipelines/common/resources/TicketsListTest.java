package ru.yandex.market.tsum.pipelines.common.resources;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Dmitry Andreev <a href="mailto:AndreevDm@yandex-team.ru"></a>
 * @date 30/11/2017
 */
public class TicketsListTest {
    @Test
    public void getTicketsList() throws Exception {
        TicketsList ticketsList = new TicketsList("MARKETINFRA-42");
        Assert.assertEquals(Collections.singletonList("MARKETINFRA-42"), ticketsList.getTickets());

        TicketsList ticketsList2 = new TicketsList("MARKETINFRA-42", "", "MARKETINFRA-21");
        Assert.assertEquals(Arrays.asList("MARKETINFRA-42", "MARKETINFRA-21"), ticketsList2.getTickets());

        TicketsList ticketsList3 = new TicketsList("MARKETINFRA-42", "MARKETINFRA-21", "   MARKETINFRA-7");
        Assert.assertEquals(Arrays.asList("MARKETINFRA-42", "MARKETINFRA-21", "MARKETINFRA-7"),
            ticketsList3.getTickets());


        TicketsList nullTicketsList = new TicketsList();
        Assert.assertEquals(Collections.emptyList(), nullTicketsList.getTickets());
    }


}
