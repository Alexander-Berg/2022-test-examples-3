package ru.yandex.iex.proxy;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Ignore;

@Ignore
public class EshopEMLTest extends EMLTestBase {
    public static final String ESHOP = "eshop";

    @Override
    public String factname() {
        return ESHOP;
    }

    @Override
    public String configExtra() {
        return "entities.message-type-6-23 = eshop, eshop_regexp, eshop_xpath\n"
            + "entities.message-type-27 = micro, microhtml\n"
            + "postprocess.message-type-6-23 = "
                + " eshop:http://localhost:" + IexProxyCluster.IPORT
                + "/eshop\n\n";
    }

    @Override
    public Set<String> checkEntities() {
        return new HashSet<>(Arrays.asList(ESHOP, "_eshop", "eshop_xpath"));
    }
}
