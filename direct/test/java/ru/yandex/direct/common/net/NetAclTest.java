package ru.yandex.direct.common.net;

import java.util.Arrays;
import java.util.Collection;

import com.google.common.net.InetAddresses;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.liveresource.LiveResourceEvent;
import ru.yandex.direct.liveresource.LiveResourceFactory;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
public class NetAclTest {
    public static final String NETWORKS_CONFIG_URL =
            "classpath:///ru/yandex/direct/common/net/network-config.common.unittest.json";
    private static final NetAcl NET_ACL = new NetAcl("[]");
    private final String ip;

    public NetAclTest(String ip) {
        this.ip = ip;
    }

    @Parameterized.Parameters
    public static Collection<Object> generateData() {
        return Arrays.asList(new Object[]{
                "::1",
                "::12",
                "127.0.0.1",
                //Strange IPv4 localhost
                "127.126.125.124"
        });
    }

    @BeforeClass
    public static void fillNetAcl() {
        NET_ACL.update(new LiveResourceEvent(LiveResourceFactory.get(NETWORKS_CONFIG_URL).getContent()));
    }

    @Test
    public void creationTest() throws Exception {
        assertThat(NET_ACL.isInternalIp(InetAddresses.forString(ip))).isTrue();
    }
}
