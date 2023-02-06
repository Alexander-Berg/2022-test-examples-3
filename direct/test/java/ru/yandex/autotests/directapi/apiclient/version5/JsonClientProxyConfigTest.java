package ru.yandex.autotests.directapi.apiclient.version5;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import ru.yandex.autotests.directapi.model.api5.Action;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(Parameterized.class)
@ParametersAreNonnullByDefault
public class JsonClientProxyConfigTest {

    private static final String defaultEndpoint = "http://localhost:8091/some/service";

    private final JsonClientProxyConfig proxyConfig = new JsonClientProxyConfig();

    @Parameter
    public ServiceNames service;

    @Parameter(1)
    public Action method;

    @Parameter(2)
    public String expected;

    @Parameters
    public static Iterable<Object[]> params() {
        return asList(
                new Object[] {ServiceNames.ADS, Action.ADD, "http://test:888/test/ads"},
                new Object[] {ServiceNames.ADS, Action.UPDATE, "http://test:888/test/ads"},
                new Object[] {ServiceNames.BIDS, Action.GET, "http://test:888/test/bids"},
                new Object[] {ServiceNames.CLIENTS, Action.GET, "http://another:9090/clients"},
                new Object[] {ServiceNames.VCARDS, Action.GET, "http://ooo:22222/eee/vcards"},

                new Object[] {ServiceNames.CLIENTS, Action.ADD, defaultEndpoint},
                new Object[] {ServiceNames.AGENCY_CLIENTS, Action.GET, defaultEndpoint}
        );
    }

    @Test
    public void testProxy() {
        assertThat(proxyConfig.getEndpoint(service, method, defaultEndpoint), equalTo(expected));
    }

}
