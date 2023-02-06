package ru.yandex.iex.proxy;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ICSEMLTest extends EMLTestBase {
    @Override
    public String factname() {
        return "ics";
    }

    @Override
    public String configExtra() {
        return "entities.message-type-42 = _VOID\n"
                + "postprocess.message-type-42 = "
                + " event-ticket:http://localhost:" + IexProxyCluster.IPORT
                + "/event-ticket\n";
    }

    @Override
    public Set<String> checkEntities() {
        return new HashSet<>(
//                Arrays.asList(new String[]{"event-ticket", "_ics"}));
                Arrays.asList(new String[]{"_ics"}));
    }
}
