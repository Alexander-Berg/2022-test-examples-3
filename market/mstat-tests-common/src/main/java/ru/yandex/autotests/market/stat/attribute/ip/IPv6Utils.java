package ru.yandex.autotests.market.stat.attribute.ip;

import sun.net.util.IPAddressUtil;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by entarrion on 30.09.15.
 */
public class IPv6Utils implements IPUtils<BigInteger> {
    public static final BigInteger MIN_IP6 = BigInteger.ZERO;
    public static final BigInteger MAX_IP6 = BigInteger.ONE.shiftLeft(128).subtract(BigInteger.ONE);
    public static final BigInteger TWO = new BigInteger("2");
    public static final Segment<BigInteger> ALL_IP6_RANGE = new Segment<>(MIN_IP6, MAX_IP6);
    public static final Segment<BigInteger> ONLY_IP6_RANGE = new Segment<>(new BigInteger(String.valueOf(IPv4Utils.MAX_IP4 + 1l)), MAX_IP6);
    private static final String NOT_VALID_IP6_AS_NUMBER = "Not valid IPv6 as number: ";
    private Random rnd;

    public IPv6Utils(Random rnd) {
        this.rnd = rnd;
    }

    public IPv6Utils() {
        this(new Random());
    }

    @Override
    public String ntoa(BigInteger number) {
        if (!number.equals(number.and(MAX_IP6))) {
            throw new IllegalArgumentException(NOT_VALID_IP6_AS_NUMBER + number);
        }
        try {
            byte[] bytes = number.toByteArray();
            bytes = bytes[0] == 0 ? Arrays.copyOfRange(bytes, 1, bytes.length) : bytes;
            byte[] array = new byte[16];
            System.arraycopy(bytes, 0, array, 16 - bytes.length, bytes.length);
            return InetAddress.getByAddress(array).getHostAddress();
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException(NOT_VALID_IP6_AS_NUMBER + number);
        }
    }

    @Override
    public String ntoa(String number) {
        try {
            return ntoa(new BigInteger(number));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(NOT_VALID_IP6_AS_NUMBER + number);
        }
    }

    @Override
    public BigInteger aton(String ip) {
        byte[] bytes = IPAddressUtil.textToNumericFormatV6(ip);
        if (bytes != null) {
            return new BigInteger(bytes).and(MAX_IP6);
        }
        throw new IllegalArgumentException("Not valid IPv6: " + ip);
    }

    @Override
    public boolean isFromRanges(BigInteger ip, Segment<BigInteger>[] rangesIPv6) {
        for (Segment<BigInteger> range : rangesIPv6) {
            if (range.isContainsPoint(ip)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isIPLiteralAddress(String ip) {
        return ip != null && IPAddressUtil.isIPv6LiteralAddress(ip);
    }

    @Override
    public Segment<BigInteger> extractIPRange(String cidr) {
        String[] array = cidr.split("/");
        if (array.length != 2) {
            throw new IllegalArgumentException("Could not parse [" + cidr + "]");
        }
        BigInteger bitMask;
        try {
            bitMask = BigInteger.ONE.shiftLeft(128 - Integer.valueOf(array[1])).subtract(BigInteger.ONE);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Could not parse [" + cidr + "]", e);
        }
        if (bitMask.compareTo(BigInteger.ZERO) < 0 || bitMask.compareTo(MAX_IP6) > 0 || !isIPLiteralAddress(array[0])) {
            throw new IllegalArgumentException("Could not parse [" + cidr + "]");
        }
        BigInteger base = aton(array[0]);
        BigInteger low = MAX_IP6.andNot(bitMask).and(base);
        BigInteger high = bitMask.or(base);
        return new Segment<>(low, high);
    }

    @Override
    public BigInteger generateRandomIpAsNumberFromRange(BigInteger lowIp, BigInteger highIp) {
        if (lowIp.compareTo(highIp) == 1) {
            throw new IllegalArgumentException("High ip must be more than low ip. [low ip:" + String.valueOf(lowIp)
                    + "; high ip:" + String.valueOf(highIp) + "]");
        }
        return lowIp.add(new BigInteger(128, rnd).mod(highIp.add(BigInteger.ONE).subtract(lowIp)));
    }

    @Override
    public Set<BigInteger> generateRandomSetUniqueIpFromRange(BigInteger lowIp, BigInteger highIp, Integer count) {
        if (highIp.subtract(lowIp).compareTo(new BigInteger(count.toString())) < 0) {
            throw new IllegalArgumentException("IP address range does not have a specified number of unique addresses." +
                    "[lowIP:" + lowIp.toString() + "; highIP:" + highIp.toString() + "; count:" + count.toString() + "]");
        }
        Set<BigInteger> ips = new HashSet<>();
        for (int i = 0; i < count; i++) {
            ips.add(generateRandomIpAsNumberFromRange(lowIp, highIp));
        }
        BigInteger index = lowIp;
        while (ips.size() < count) {
            ips.add(index);
            index.add(BigInteger.ONE);
        }
        return ips;
    }

    @Override
    public List<Segment<BigInteger>> invertIPRange(List<Segment<BigInteger>> ranges) {
        return ALL_IP6_RANGE.separation(ranges).stream().
                filter(it -> it.getEndPoint().subtract(it.getStartPoint()).compareTo(TWO) > 0).
                map(it -> {
                    BigInteger start = it.getStartPoint().compareTo(MIN_IP6) == 0 ? MIN_IP6 : it.getStartPoint().add(BigInteger.ONE);
                    BigInteger end = it.getEndPoint().compareTo(MAX_IP6) == 0 ? MAX_IP6 : it.getEndPoint().subtract(BigInteger.ONE);
                    return new Segment<>(start, end);
                }).collect(Collectors.toList());
    }
}
