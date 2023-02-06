package ru.yandex.autotests.market.stat.attribute.ip;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang.math.RandomUtils;

import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by entarrion on 09.09.15.
 */
public class IP {
    private static final String PREFIX_IP6_TO_IP4 = "::ffff:";
    private static final Random RND = new Random();
    private static final IPv4Utils IP4 = new IPv4Utils(RND);
    private static final IPv6Utils IP6 = new IPv6Utils(RND);
    private static final ImmutableList<String> blackIps = ImmutableList.of(
            "77.75.152.0", "89.108.64.0", "81.13.55.32", "62.213.72.0", "217.23.141.0", "217.23.147.0", "217.23.152.0",
            "213.234.193.0", "62.205.161.0", "195.14.58.0", "77.221.128.0", "194.67.45.0", "83.222.0.0", "217.16.30.0",
            "87.242.72.0", "217.65.8.0", "89.188.97.0", "89.253.192.0", "195.214.192.0", "212.42.64.8",
            "62.118.251.0", "217.112.36.0", "217.112.42.0", "195.2.82.0", "217.16.16.0", "217.16.17.0",
            "217.16.18.0", "217.16.20.0", "217.16.21.0", "217.16.22.0", "217.16.26.0", "217.16.27.0",
            "217.16.28.0");

    public static String generateYandexIPv4() {
        return IP4.ntoa(IP4.generateRandomIpAsNumberFromRange(choiceSegmentForIP4(IPSubnets.YANDEX_IP4_RANGES)));
    }

    public static String generateValidNoYandexIPv4() {
        return IP4.ntoa(IP4.generateRandomIpAsNumberFromRange(choiceSegmentForIP4(IPSubnets.VALID_NO_YANDEX_IP4_RANGES)));
    }

    public static String generateValidIPv4() {
        return IP4.ntoa(IP4.generateRandomIpAsNumberFromRange(choiceSegmentForIP4(IPSubnets.VALID_IP4_RANGES)));
    }

    public static Set<String> generateIpFromDifferentSubnets(int count) {
        Map<String, String> result = new HashMap();
        int lenght = 24;
        while (result.size() < count) {
            Long ip = IP.atonIPv4(IP.generateValidNoYandexIPv4());
            int shift = 32 - lenght;
            result.put(IP.getIp4().ntoa((ip >> shift) << shift) + "/" + String.valueOf(lenght), IP.getIp4().ntoa(ip));
        }
        return new HashSet(result.values());
    }

    public static String generateValidIPv4AsIPv6() {
        return getIPv6FromIPv4(generateValidIPv4());
    }

    public static String generateValidIPv6() {
        return IP6.ntoa(IP6.generateRandomIpAsNumberFromRange(IPSubnets.ONLY_IP6_RANGE));
    }

    public static Set<String> generateRandomSetUniqueIp4FromRange(final String cidr, final Integer count) {
        return IP4.generateRandomSetUniqueIpFromRange(cidr, count).
                stream().map(IP4::ntoa).collect(Collectors.toSet());
    }

    public static Set<String> generateRandomSetUniqueIp6FromRange(final String cidr, final Integer count) {
        return IP6.generateRandomSetUniqueIpFromRange(cidr, count).
                stream().map(IP6::ntoa).collect(Collectors.toSet());
    }

    /**
     * ########################
     */
    public static String generateYandexIPv4ForGeoId() {
        return generateYandexIPv4();
    }

    public static String generateNoYandexIPv4ForGeoId() {
        return generateValidNoYandexIPv4();
    }

    //====================================================================================================
    //                              Различные преобразованиями ip адресов.
    //====================================================================================================

    public static String getIPv4(String ipv4) {
        try {
            return IP4.ntoa(Long.parseLong(ipv4));
        } catch (NumberFormatException e) {
            if (IP4.isIPLiteralAddress(ipv4)) {
                return ipv4;
            } else if (IP6.isIPLiteralAddress(ipv4)) {
                return getIPv4FromIPv6(ipv4);
            }
        }
        throw new IllegalArgumentException("Not valid IPv4: " + ipv4);
    }

    public static String getIPv6FromIPv4(String ipv4) {
        if (IP4.isIPLiteralAddress(ipv4)) {
            return PREFIX_IP6_TO_IP4 + ipv4.trim();
        }
        throw new IllegalArgumentException("Not valid IPv4: " + ipv4);
    }

    public static String getIPv4FromIPv6(String ipv6) {
        if (isIPv4LiteralAddressAsIPv6(ipv6)) {
            String result = ipv6.replaceAll(PREFIX_IP6_TO_IP4, "").trim();
            if (IP4.isIPLiteralAddress(result)) {
                return result;
            }
        }
        throw new IllegalArgumentException("Not valid IPv6 for convert to IPv4: " + ipv6);
    }

    public static boolean isIPv4LiteralAddressAsIPv6(String ip) {
        return IP6.isIPLiteralAddress(ip) && ip.startsWith(PREFIX_IP6_TO_IP4);
    }

    public static Long atonIPv4(String ipv4) {
        return IP4.aton(ipv4);
    }

    public static List<String> extractIPv4Addresses(String ips) {
        return extractIPv4Addresses(ips, ",");
    }

    public static List<String> extractIPv6Addresses(String ips) {
        return extractIPv6Addresses(ips, ",");
    }

    public static List<String> extractIPv4Addresses(String ips, String separator) {
        return Arrays.stream(ips.split(separator)).filter(ip -> IP4.isIPLiteralAddress(ip) || isIPv4LiteralAddressAsIPv6(ip)).collect(Collectors.toList());
    }

    public static List<String> extractIPv6Addresses(String ips, String separator) {
        return Arrays.stream(ips.split(separator)).filter(ip -> IP6.isIPLiteralAddress(ip) && !isIPv4LiteralAddressAsIPv6(ip)).collect(Collectors.toList());
    }

    //====================================================================================================
    //                              Проверки.
    //====================================================================================================
    public static boolean isIP(String ip) {
        try {
            Long number = Long.parseLong(ip);
            return number == (0xffffffffl & number);
        } catch (NumberFormatException e) {
            return isIPv4LiteralAddress(ip) || isIPv6LiteralAddress(ip);
        }
    }

    public static boolean isIPv4(String ipv4) {
        return isIP(ipv4) ? IP4.isIPLiteralAddress(ipv4) || isIPv4LiteralAddressAsIPv6(ipv4) : false;
    }

    public static boolean isYandexIp(String ipv4) {
        return IP4.isFromRanges(IP4.aton(getIPv4(ipv4)), IPSubnets.YANDEX_IP4_RANGES);
    }

    public static boolean isIPv4FromValidSubnets(String ipv4) {
        return !IP4.isFromRanges(IP4.aton(getIPv4(ipv4)), IPSubnets.NO_VALID_IP4_RANGES);
    }

    public static boolean isIPv4LiteralAddress(String ip) {
        return IP4.isIPLiteralAddress(ip);
    }

    public static boolean isIPv6LiteralAddress(String ip) {
        return IP6.isIPLiteralAddress(ip);
    }

    //====================================================================================================
    //                              Разное.
    //====================================================================================================

    public static IPv4Utils getIp4() {
        return IP4;
    }

    public static IPv6Utils getIp6() {
        return IP6;
    }

    public static <T> T choice(Collection<T> seq) {
        return choice((T[]) seq.toArray());
    }

    public static <T> T choice(T... seq) {
        return seq[RND.nextInt(seq.length)];
    }

    /**
     * Выбор сегмента в зависимости от его веса (разброса ip адресов) для равномерной генерации ip
     */
    public static Segment<Long> choiceSegmentForIP4(Segment<Long>... segments)
    {
        BigInteger max = BigInteger.ZERO;
        for(Segment<Long> segment : segments)
        {
            max = max.add(new BigInteger(segment.getEndPoint().toString())).add(BigInteger.ONE)
                    .subtract(new BigInteger(segment.getStartPoint().toString()));
        }
        BigInteger rand;
        do {
            rand = new BigInteger(max.bitLength(), RND);

        } while (rand.compareTo(max) > 0);
        BigInteger accumulator = BigInteger.ZERO;
        for(Segment<Long> segment : segments)
        {
            accumulator = accumulator.add(new BigInteger(segment.getEndPoint().toString())).add(BigInteger.ONE)
                    .subtract(new BigInteger(segment.getStartPoint().toString()));
             if(accumulator.compareTo(rand) >=0) {
                 return segment;
             }
        }
        // До этого места мы просто можем дойти, но если каким-то чудом это произошло - то Exception
        throw new IllegalStateException("Wrong logic");
    }

    public static String generateIpFromBlackSubnet() {
        int i = RandomUtils.nextInt(blackIps.size());
        return blackIps.get(i);
    }
}
