package ru.yandex.market.tsum.core.clients.nanny;

import com.google.protobuf.Descriptors;
import nanny.tickets.Tickets;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.tsum.clients.nanny.NannyTicketApiClient;
import ru.yandex.market.tsum.core.TsumDebugRuntimeConfig;

import java.util.Map;

/**
 * @author Nikolay Firov <a href="mailto:firov@yandex-team.ru"></a>
 * @date 31.01.18
 */
@Ignore
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TsumDebugRuntimeConfig.class})
public class NannyTicketApiClientIntegrationTest {
    @Value("${tsum.nanny.api-url}")
    private String nannyApiUrl;

    @Value("${tsum.nanny.oauth-token}")
    private String nannyOAuthToken;

    @Test
    public void updatesStatus() {
        NannyTicketApiClient client = new NannyTicketApiClient(nannyApiUrl, nannyOAuthToken);

        String id = "MARKET-3598816";

        Tickets.Ticket ticket = client.getTicket(id).getValue();
        Tickets.TicketStatus status = ticket.getStatus().toBuilder()
            .setStatus(Tickets.TicketStatus.Status.IN_QUEUE)
            .build();

        client.updateTicket(id, status, "UPDATED BY TSUM NannyTicketApiClientIntegrationTest");

        ticket = client.getTicket(id).getValue();
        Map<Descriptors.FieldDescriptor, Object> asd = ticket.getAllFields();
        Assert.assertEquals(ticket.getStatus().getStatus(), Tickets.TicketStatus.Status.IN_QUEUE);

        status = ticket.getStatus().toBuilder()
            .setStatus(Tickets.TicketStatus.Status.COMMITTED)
            .build();

        client.updateTicket(id, status, "UPDATED BY TSUM NannyTicketApiClientIntegrationTest");

        ticket = client.getTicket(id).getValue();
        Assert.assertEquals(ticket.getStatus().getStatus(), Tickets.TicketStatus.Status.COMMITTED);
    }

    @Test
    public void commitsTicket() {
        NannyTicketApiClient client = new NannyTicketApiClient(nannyApiUrl, nannyOAuthToken);

        String id = "MARKET-3598816";

        client.commitTicket(id, Tickets.SchedulingSettings.Priority.NORMAL);

        Tickets.Ticket ticket = client.getTicket(id).getValue();

        Assert.assertEquals(ticket.getStatus().getStatus(), Tickets.TicketStatus.Status.COMMITTED);
    }
}
