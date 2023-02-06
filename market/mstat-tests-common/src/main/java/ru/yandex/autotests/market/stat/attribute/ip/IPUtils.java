package ru.yandex.autotests.market.stat.attribute.ip;

import java.util.List;
import java.util.Set;

/**
 * Created by entarrion on 30.09.15.
 */
public interface IPUtils<T extends Comparable<T>> {
    String ntoa(T number);

    String ntoa(String number);

    T aton(String ip);

    Segment<T> extractIPRange(String cidr);

    boolean isFromRanges(T ip, Segment<T>[] rangesIP);

    boolean isIPLiteralAddress(String ip);

    default T generateRandomIpAsNumberFromRange(final Segment<T> segment) {
        return generateRandomIpAsNumberFromRange(segment.getStartPoint(), segment.getEndPoint());
    }

    T generateRandomIpAsNumberFromRange(final T lowIp, final T highIp);

    default Set<T> generateRandomSetUniqueIpFromRange(final String cidr, final Integer count) {
        return generateRandomSetUniqueIpFromRange(extractIPRange(cidr), count);
    }

    default Set<T> generateRandomSetUniqueIpFromRange(final Segment<T> segment, final Integer count) {
        return generateRandomSetUniqueIpFromRange(segment.getStartPoint(), segment.getEndPoint(), count);
    }

    Set<T> generateRandomSetUniqueIpFromRange(final T lowIp, final T highIp, final Integer count);

    List<Segment<T>> invertIPRange(List<Segment<T>> ranges);
}
