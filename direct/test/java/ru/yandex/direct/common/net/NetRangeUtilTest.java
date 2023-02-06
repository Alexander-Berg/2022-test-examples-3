package ru.yandex.direct.common.net;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
public class NetRangeUtilTest {
    private final String inspectedIp;
    private final String inspectedNetworks;
    private final boolean isInNetwork;

    public NetRangeUtilTest(String ip, String networks, boolean isInNetworks) {
        this.inspectedIp = ip;
        this.inspectedNetworks = networks;
        this.isInNetwork = isInNetworks;
    }

    @Parameterized.Parameters(name = "{1}")
    public static Collection<Object[]> generateData() {
        return Arrays.asList(new Object[][]{
                {"192.168.1.2", "192.168.1.1, 192.168.1.2, 192.168.1.3", true},
                {"192.168.1.2", "192.168.1.1-192.168.1.3, ::1", true},
                {"192.168.1.2", "192.168.1.1/24, ::1", true},
                {"192.168.1.2", "", false}
        });
    }

    @Test
    public void isIpInNetworksTest() {
        assertThat(NetRangeUtil.isIpInNetworks(inspectedIp, inspectedNetworks)).isEqualTo(isInNetwork);
    }
}
