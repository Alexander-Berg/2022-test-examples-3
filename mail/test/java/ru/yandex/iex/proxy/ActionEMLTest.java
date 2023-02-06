package ru.yandex.iex.proxy;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Ignore;

@Ignore
public class ActionEMLTest extends EMLTestBase {
    public static final String ACTION = "action";

    @Override
    public String factname() {
        return ACTION;
    }

    @Override
    public String configExtra() {
        return "entities.message-type-2 = registration\n"
            + "postprocess.message-type-2 = "
                + " action:http://localhost:" + IexProxyCluster.IPORT
                + "/action\n\n";
    }

    @Override
    public Set<String> checkEntities() {
        return new HashSet<>(Arrays.asList(
            ACTION,
            "_registration",
            "_microhtml",
            "_micro",
            "_unsubscribe"));
    }
}
