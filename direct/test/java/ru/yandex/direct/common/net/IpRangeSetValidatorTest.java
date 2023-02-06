package ru.yandex.direct.common.net;

import java.util.Arrays;
import java.util.Collection;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
public class IpRangeSetValidatorTest {
    private static final IpRangeSetValidator RANGE_VALIDATOR = new IpRangeSetValidator();
    private static final IpRangeSetValidator ANY_IP_RANGE_VALIDATOR = new IpRangeSetValidator();
    private final String ip;
    private final boolean belongsToRanges;

    public IpRangeSetValidatorTest(String ip, boolean belongsToRanges) {
        this.ip = ip;
        this.belongsToRanges = belongsToRanges;
    }

    @BeforeClass
    public static void fillRangeValidator() {
        RANGE_VALIDATOR.addMask("192.168.50.254/30");
        RANGE_VALIDATOR.addMask("192.168.0.1/24");
        RANGE_VALIDATOR.addMask("127.0.0.1");
        RANGE_VALIDATOR.addMask("5.17.140.1/36");
        RANGE_VALIDATOR.addMask("5.17.140.2/32");
        RANGE_VALIDATOR.addMask("192.168.42.42/24");
        //4 rightmost bytes of "0000:0000:0000:0000:0000:0000:c0a8:2a2a" are equal to bytes of "192.168.42.42"
        //but this ip is not converted into IPv4
        RANGE_VALIDATOR.addMask("0000:0000:0000:0000:0000:0000:c0a8:2a2a/120");
        // Host-based-firewall
        RANGE_VALIDATOR.addMask("697@2a02:6b8:c00::/40");

        ANY_IP_RANGE_VALIDATOR.addMask("::/0");
    }

    @Test
    public void containsTest() {
        assertThat(RANGE_VALIDATOR.contains(ip)).isEqualTo(belongsToRanges);
    }

    @Test
    public void anyIpTest() {
        assertThat(ANY_IP_RANGE_VALIDATOR.contains(ip)).isTrue();
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> generateData() {
        return Arrays.asList(new Object[][]{
                {"192.168.0.254", true},
                {"192.168.1.254", false},
                {"192.168.50.252", true},
                {"192.168.50.249", false},
                {"192.168.42.100", true},
                {"192.168.42.43", true},
                {"5.17.140.0", false},
                {"5.17.140.1", true},
                {"5.17.140.2", true},
                {"5.17.140.3", false},
                {"0000:0000:0000:0000:0000:0000:c0a8:2a64", true},
                {"0000:0000:0000:0000:0000:0000:c0a8:2a2a", true},
                {"ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff", false},
                // Host-based-firewall
                {"2a02:6b8:c00:2588::697:3e4c:ec25", true},
                {"2a02:6b8:c00:2588::698:3e4c:ec25", false},
                {"2a02:6b8:d0:2588::697:3e4c:ec25", false},
                {"2a02:6b8:c00:2588:1:697:3e4c:ec25", false},
        });
    }
}
