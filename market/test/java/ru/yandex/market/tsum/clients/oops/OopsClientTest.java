package ru.yandex.market.tsum.clients.oops;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.market.request.netty.HttpClientConfig;
import ru.yandex.market.request.netty.NettyHttpClientContext;

/**
 * @author Tigina Maria <a href="mailto:tiginamaria@yandex-team.ru"></a>
 * @date 04.09.19
 */
public class OopsClientTest {

    private OopsClient client;

    private static final String SAS_HOSTNAME = "sas1-1081.search.yandex.net";
    private static final String VLA_HOSTNAME = "vla1-4712.search.yandex.net";

    @Before
    public void setUp() throws Exception {
        HttpClientConfig config = new HttpClientConfig();
        client = new OopsClient("http://oops.yandex-team.ru", new NettyHttpClientContext(config));
    }

    @Test
    @Ignore(value = "integration test to check api works")
    public void getSasServerInfoTest() {
        client.getDisksInfo(SAS_HOSTNAME);
        client.getCpuInfo(SAS_HOSTNAME);
        client.getVlansInfo(SAS_HOSTNAME);
        client.getDnsInfoMap(SAS_HOSTNAME);
    }

    @Test
    @Ignore(value = "integration test to check api works")
    public void getVlsServerInfoTest() {
        client.getDisksInfo(VLA_HOSTNAME);
        client.getCpuInfo(VLA_HOSTNAME);
        client.getVlansInfo(VLA_HOSTNAME);
        client.getDnsInfoMap(VLA_HOSTNAME);
    }
}
