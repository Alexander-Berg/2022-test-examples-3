package ru.yandex.iex.proxy;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Ignore;

@Ignore
public class HotelsEMLTest extends EMLTestBase {
    public static final String HOTELS = "hotels";

    @Override
    public String factname() {
        return HOTELS;
    }

    @Override
    public String configExtra() {
        return "entities.message-type-35 = hotels, micro, microhtml\n"
            + "postprocess.message-type-35 = "
                + " hotels:http://localhost:" + IexProxyCluster.IPORT
                + "/hotels\n\n";
    }

    @Override
    public Set<String> checkEntities() {
        return new HashSet<>(
                Arrays.asList(new String[]{HOTELS, "_hotels"}));
    }
}
