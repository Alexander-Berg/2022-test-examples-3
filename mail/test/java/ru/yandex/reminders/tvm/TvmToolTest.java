package ru.yandex.reminders.tvm;

import lombok.val;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.passport.tvmauth.TicketStatus;
import ru.yandex.reminders.util.TestUtils;
import static org.assertj.core.api.Assertions.assertThat;

@ContextConfiguration(classes = TvmToolTestConfiguration.class)
public class TvmToolTest extends TestUtils {
    private static final String CALLMEBACK_ALIAS = "callmeback";
    @Autowired
    private TvmClient tvmClient;
    @Autowired
    @Qualifier("dummyClient")
    private TvmClient dummyClient;
    @Autowired
    @Qualifier("callmebackClient")
    private TvmClient callmebackClient;

    @Test
    public void dummyClientShouldGetInvalidDst() {
        val ticketBody = dummyClient.getServiceTicketFor(CALLMEBACK_ALIAS);
        val serviceTicket = tvmClient.checkServiceTicket(ticketBody);
        assertThat(serviceTicket.getStatus()).isEqualTo(TicketStatus.INVALID_DST);
    }

    @Test
    public void interactionOfRemindersAndCallmeback() {
        val reminderTicketBody = tvmClient.getServiceTicketFor(CALLMEBACK_ALIAS);
        val reminderTicket = callmebackClient.checkServiceTicket(reminderTicketBody);
        val callmebackTicketBody = callmebackClient.getServiceTicketFor("reminders");
        val callmebackTicket = tvmClient.checkServiceTicket(callmebackTicketBody);

        assertThat(reminderTicket.getStatus()).isEqualTo(TicketStatus.OK);
        assertThat(callmebackTicket.getStatus()).isEqualTo(TicketStatus.OK);
    }
}
