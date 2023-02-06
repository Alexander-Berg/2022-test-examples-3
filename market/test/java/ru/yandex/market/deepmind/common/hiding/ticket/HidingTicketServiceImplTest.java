package ru.yandex.market.deepmind.common.hiding.ticket;

import java.util.List;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.deepmind.common.hiding.configuration.HidingConfiguration;
import ru.yandex.market.tracker.tracker.MockSession;

public class HidingTicketServiceImplTest {

    private HidingTicketServiceImpl hidingTicketService;
    private EnhancedRandom random;

    @Before
    public void setUp() {
        var session = new MockSession();
        var open = session.statuses().add("open");
        var closed = session.statuses().add("closed");
        session.transitions().add(List.of(open), "close", closed);

        random = EnhancedRandomBuilder.aNewEnhancedRandomBuilder().seed(1).build();
        hidingTicketService = new HidingTicketServiceImpl("", "", session);
    }

    @Test
    public void createSimpleTicket() {
        var configuration = random.nextObject(HidingConfiguration.class)
            .setComponent(null)
            .setQueue("QUEUE");

        var createTicketConfiguration = configuration.toCreateTicket("http://url.com", "",
            List.of(), List.of(), List.of(), List.of());

        var ticket = hidingTicketService.createTicket(createTicketConfiguration);
        Assertions.assertThat(ticket).isNotNull();
        Assertions.assertThat(ticket.getKey()).isEqualTo("TESTDEEPMIND-1");
    }
}
