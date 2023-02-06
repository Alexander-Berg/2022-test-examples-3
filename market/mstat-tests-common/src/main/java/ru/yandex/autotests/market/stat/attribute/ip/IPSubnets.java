package ru.yandex.autotests.market.stat.attribute.ip;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.net.util.SubnetUtils;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

/**
 * Created by entarrion on 14.09.15.
 * Не применять форматирование к данному классу!
 */
class IPSubnets {
    public static final Segment<BigInteger> ONLY_IP6_RANGE = new Segment<>(new BigInteger(String.valueOf(IPv4Utils.MAX_IP4 + 1l)), IPv6Utils.MAX_IP6);
    public static final Segment<Long>[] YANDEX_IP4_RANGES;

    static {
        String[][] yandexIPs = new String[][]{
                {"213.180.192.0", "213.180.192.255"}, {"95.108.128.0", "95.108.255.255"},
                {"213.180.193.0", "213.180.193.127"}, {"213.180.193.128", "213.180.193.152"},
                {"213.180.194.0", "213.180.194.127"}, {"213.180.194.128", "213.180.194.255"},
                {"213.180.195.0", "213.180.195.127"}, {"213.180.195.128", "213.180.195.255"},
                {"213.180.196.0", "213.180.196.255"}, {"213.180.197.0", "213.180.198.255"},
                {"213.180.199.0", "213.180.199.255"}, {"213.180.200.0", "213.180.200.255"},
                {"213.180.201.0", "213.180.201.255"}, {"213.180.219.0", "213.180.219.127"},
                {"213.180.214.1", "213.180.214.63"}, {"213.180.218.0", "213.180.218.214"},
                {"213.180.218.215", "213.180.218.255"}, {"213.180.215.0", "213.180.215.255"},
                {"213.180.193.154", "213.180.193.255"}, {"213.180.193.153", "213.180.193.153"},
                {"213.180.202.1", "213.180.213.255"}, {"213.180.214.64", "213.180.214.255"},
                {"213.180.216.1", "213.180.217.255"}, {"213.180.218.125", "213.180.218.125"},
                {"213.180.218.214", "213.180.218.214"}, {"213.180.219.128", "213.180.219.255"},
                {"213.180.220.1", "213.180.223.255"}, {"87.250.224.0", "87.250.255.255"},
                {"213.180.218.126", "213.180.218.213"}, {"213.180.218.88", "213.180.218.124"},
                {"77.88.0.0", "77.88.63.255"}, {"93.158.128.0", "93.158.191.255"}};
        // 84.201.164.158
        Segment<Long>[] segments = new Segment[yandexIPs.length];
        for (int i = 0; i < segments.length; i++) {
            segments[i] = new Segment<>(IP.atonIPv4(yandexIPs[i][0]), IP.atonIPv4(yandexIPs[i][1]));
        }
        List<Segment<Long>> result = Segment.unionAllIntersectedSegment(segments);
        YANDEX_IP4_RANGES = result.toArray(new Segment[result.size()]);
    }

    public static final Segment<Long>[] NO_VALID_IP4_RANGES;

    static {
        //Addresses starting with 240 or a higher number have not been allocated and should not be used
        //Addresses starting with a number between 224 and 239 are used for IP multicast
        List<SubnetUtils.SubnetInfo> subnets = Arrays.asList(
                new SubnetUtils("0.0.0.0/8").getInfo(),           // "This" Network
                new SubnetUtils("10.0.0.0/8").getInfo(),          // Private-Use Networks
                new SubnetUtils("100.64.0.0/10").getInfo(),
                new SubnetUtils("172.16.0.0/12").getInfo(),
                new SubnetUtils("127.0.0.0/8").getInfo(),         // Loopback
                new SubnetUtils("169.254.0.0/16").getInfo(),      // Link Local
                new SubnetUtils("172.16.0.0/12").getInfo(),       // Private-Use Networks
                new SubnetUtils("192.168.0.0/16").getInfo(),
                new SubnetUtils("192.0.0.0/24").getInfo(),        // IETF Protocol Assignments
                new SubnetUtils("192.0.2.0/24").getInfo(),        // TEST-NET-1
                new SubnetUtils("192.88.99.0/24").getInfo(),      // 6to4 Relay Anycast
                new SubnetUtils("192.168.0.0/16").getInfo(),      // Private-Use Networks
                new SubnetUtils("198.18.0.0/15").getInfo(),       // Network Interconnect; Device Benchmark Testing
                new SubnetUtils("198.51.100.0/24").getInfo(),     // TEST-NET-2
                new SubnetUtils("203.0.113.0/24").getInfo(),      // TEST-NET-3
                new SubnetUtils("224.0.0.0/4").getInfo(),         // Multicast
                new SubnetUtils("240.0.0.0/4").getInfo(),         // Reserved for Future Use
                new SubnetUtils("255.255.255.255/32").getInfo()); // Limited Broadcast
        Segment<Long>[] segments = new Segment[subnets.size()];
        for (int i = 0; i < segments.length; i++) {
            segments[i] = new Segment(IP.atonIPv4(subnets.get(i).getNetworkAddress()), IP.atonIPv4(subnets.get(i).getBroadcastAddress()));
        }
        List<Segment<Long>> result = Segment.unionAllIntersectedSegment(segments);
        NO_VALID_IP4_RANGES = result.toArray(new Segment[result.size()]);
    }

    public static final Segment<Long>[] VALID_NO_YANDEX_IP4_RANGES;

    static {
        List<Segment<Long>> result = IP.getIp4().invertIPRange(
                Arrays.asList(ArrayUtils.addAll(NO_VALID_IP4_RANGES, YANDEX_IP4_RANGES)));
        VALID_NO_YANDEX_IP4_RANGES = result.toArray(new Segment[result.size()]);
    }

    public static final Segment<Long>[] VALID_IP4_RANGES;

    static {

        List<Segment<Long>> result = IP.getIp4().invertIPRange(
                Arrays.asList(NO_VALID_IP4_RANGES));
        VALID_IP4_RANGES = result.toArray(new Segment[result.size()]);
    }
}
