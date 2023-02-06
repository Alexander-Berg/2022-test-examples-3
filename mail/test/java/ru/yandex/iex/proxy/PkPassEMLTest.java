package ru.yandex.iex.proxy;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Ignore;

@Ignore
public class PkPassEMLTest extends EMLTestBase {
    @Override
    public String factname() {
        return "pkpass";
    }

    @Override
    public String configExtra() {
        return "entities.message-type-5-48 = pkpass, micro\n"
            + "entities.message-type-5-30 = pkpass, micro\n"
            + "postprocess.message-type-5-48 = "
                + " event-ticket:http://localhost:" + IexProxyCluster.IPORT
                + "/event-ticket\n"
            + "postprocess.message-type-5-30 = "
                + "event-ticket:http://localhost:" + IexProxyCluster.IPORT
                + "/event-ticket\n\n";
    }

    @Override
    public Set<String> checkEntities() {
        return new HashSet<>(
                Arrays.asList(new String[]{"event-ticket", "_pkpass"}));
    }
}
