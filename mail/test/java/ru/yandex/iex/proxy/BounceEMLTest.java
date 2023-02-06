package ru.yandex.iex.proxy;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Ignore;

@Ignore
public class BounceEMLTest extends EMLTestBase {
    @Override
    public String factname() {
        return "bounce";
    }

    @Override
    public String configExtra() {
        return "entities.message-type-8 = bounce\n";
                //can't test /bounce now
                //check only "cokedump"
//                + "postprocess.message-type-8 = "
//                + " ticket:http://localhost:" + IexProxyCluster.IPORT
//                + "/bounce\n";
    }

    @Override
    public Set<String> checkEntities() {
        return new HashSet<>(
                Arrays.asList(new String[]{"_bounce"}));
    }
}
