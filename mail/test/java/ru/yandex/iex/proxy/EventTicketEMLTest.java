package ru.yandex.iex.proxy;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class EventTicketEMLTest extends EMLTestBase {
    public static final String ETICKET = "event-ticket";
    public static final String ETICKET_POSTPROCESS =
        " event-ticket:http://localhost:" + IexProxyCluster.IPORT
        + "/event-ticket\n";

    @Override
    public String factname() {
        return ETICKET;
    }

    @Override
    public String configExtra() {
        return "entities.message-type-27 = micro, microhtml\n"
            + "entities.message-type-48 = movie, timepad\n"
            + "entities.message-type-5-48 = pkpass, micro, movie\n"
            + "postprocess.message-type-5-48 = " + ETICKET_POSTPROCESS
            + "postprocess.message-type-42 = " + ETICKET_POSTPROCESS;
    }

    @Override
    public Set<String> checkEntities() {
        return new HashSet<>(
                Arrays.asList(new String[]{ETICKET}));
    }
}
