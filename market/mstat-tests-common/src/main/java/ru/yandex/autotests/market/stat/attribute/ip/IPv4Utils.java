package ru.yandex.autotests.market.stat.attribute.ip;


import com.google.common.primitives.Longs;
import org.apache.commons.net.util.SubnetUtils;
import sun.net.util.IPAddressUtil;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by entarrion on 30.09.15.
 */
public class IPv4Utils implements IPUtils<Long> {
    public static final Long MIN_IP4 = 0l;
    public static final Long MAX_IP4 = 0xffffffffl;
    public static final Segment<Long> ALL_IP4_RANGE = new Segment<>(MIN_IP4, MAX_IP4);
    private static final String NOT_VALID_IP4_AS_NUMBER = "Not valid IPv4 as number: ";
    private Random rnd;

    public IPv4Utils(Random rnd) {
        this.rnd = rnd;
    }

    public IPv4Utils() {
        this(new Random());
    }

    @Override
    public String ntoa(Long number) {
        if (number != (MAX_IP4 & number)) {
            throw new IllegalArgumentException(NOT_VALID_IP4_AS_NUMBER + number);
        }
        try {
            return InetAddress.getByAddress(Arrays.copyOfRange(Longs.toByteArray(number), 4, 8)).getHostAddress();
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException(NOT_VALID_IP4_AS_NUMBER + number);
        }
    }

    @Override
    public String ntoa(String number) {
        try {
            return ntoa(Long.parseLong(number));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(NOT_VALID_IP4_AS_NUMBER + number);
        }
    }

    @Override
    public Long aton(String ip) {
        byte[] bytes = IPAddressUtil.textToNumericFormatV4(ip);
        if (bytes != null) {
            byte[] array = new byte[8];
            System.arraycopy(bytes, 0, array, 4, bytes.length);
            return Longs.fromByteArray(array);
        }
        throw new IllegalArgumentException("Not valid IPv4: " + ip);
    }

    @Override
    public boolean isFromRanges(Long ip, Segment<Long>[] rangesIP) {
        for (Segment<Long> range : rangesIP) {
            if (range.isContainsPoint(ip)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isIPLiteralAddress(String ip) {
        return ip != null && IPAddressUtil.isIPv4LiteralAddress(ip);
    }

    @Override
    public Segment<Long> extractIPRange(String cidr) {
        SubnetUtils.SubnetInfo info = new SubnetUtils(cidr).getInfo();
        return new Segment<>(aton(info.getNetworkAddress()), aton(info.getBroadcastAddress()));
    }

    @Override
    public Long generateRandomIpAsNumberFromRange(Long lowIp, Long highIp) {
        if (lowIp > highIp) {
            throw new IllegalArgumentException("High ip must be more than low ip. [low ip:" + String.valueOf(lowIp)
                    + "; high ip:" + String.valueOf(highIp) + "]");
        }
        return (lowIp + (rnd.nextLong() & MAX_IP4) % (highIp + 1 - lowIp)) & MAX_IP4;
    }

    @Override
    public Set<Long> generateRandomSetUniqueIpFromRange(String cidr, Integer count) {
        return generateRandomSetUniqueIpFromRange(extractIPRange(cidr), count);
    }

    @Override
    public Set<Long> generateRandomSetUniqueIpFromRange(Segment<Long> segment, Integer count) {
        return generateRandomSetUniqueIpFromRange(segment.getStartPoint(), segment.getEndPoint(), count);
    }

    @Override
    public Set<Long> generateRandomSetUniqueIpFromRange(Long lowIp, Long highIp, Integer count) {
        if (highIp - lowIp < count) {
            throw new IllegalArgumentException("IP address range does not have a specified number of unique addresses." +
                    "[lowIP:" + lowIp.toString() + "; highIP:" + highIp.toString() + "; count:" + count.toString() + "]");
        }
        Set<Long> ips = new HashSet<>();
        for (int i = 0; i < count; i++) {
            ips.add(generateRandomIpAsNumberFromRange(lowIp, highIp));
        }
        Long index = lowIp;
        while (ips.size() < count) {
            ips.add(index);
            index++;
        }
        return ips;
    }

    public List<Segment<Long>> invertIPRange(List<Segment<Long>> ranges) {
        return ALL_IP4_RANGE.separation(ranges).stream().
                filter(segment -> segment.getEndPoint() - segment.getStartPoint() > 2).
                map(it -> {
                    Long start = it.getStartPoint().equals(MIN_IP4) ? MIN_IP4 : it.getStartPoint() + 1;
                    Long end = it.getEndPoint().equals(MAX_IP4) ? MAX_IP4 : it.getEndPoint() - 1;
                    return new Segment<Long>(start, end);
                }).collect(Collectors.toList());
    }
}
