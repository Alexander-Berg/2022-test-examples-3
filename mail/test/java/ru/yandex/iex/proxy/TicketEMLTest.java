package ru.yandex.iex.proxy;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

public class TicketEMLTest extends EMLTestBase {
    @Override
    public String factname() {
        return "ticket";
    }

    //disable temporary
    //to enable - implement RASP service
    @Test
    @Override
    public void testEMLs() {
    }

    @Override
    public String configExtra() {
        return "entities.message-type-5-16 = ticket, micro, microhtml\n"
                + "entities.message-type-5-19 = ticket, micro, microhtml\n"
                + "postprocess.message-type-5-16 = "
                + " ticket:http://localhost:" + IexProxyCluster.IPORT
                + "/ticket\n"
                + "postprocess.message-type-5-19 = "
                + "  ticket:http://localhost:" + IexProxyCluster.IPORT
                + "/ticket\n\n";
    }

    @Override
    public Set<String> checkEntities() {
        return new HashSet<>(
//                Arrays.asList(new String[]{"event-ticket", "_ics"}));
                Arrays.asList(new String[]{"_ticket"}));
    }
}
