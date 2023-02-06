package ru.yandex.market.antifraud.filter.fields;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.RandomUtils;

/**
 * Created by kateleb on 25.08.15.
 */
public class CategId {
    public static final ImmutableList<Integer> someValidCategs = ImmutableList.of(10194, 20346, 800, 23447,
            10005, 1585, 18425, 18426, 18429, 18427, 1584, 19, 23671, 19966, 962, 1582,
            1583, 843, 976, 977, 1649, 1361, 1362, 1363, 840, 1711, 19726, 1705, 20568,
            20486, 1706, 965, 10059, 1703, 1704, 1702, 1709, 1707, 20586, 1708, 1710,
            963, 1717, 10060, 1716, 964, 1746, 1712, 22747, 23669, 1381, 1383, 1382,
            1384, 1409, 1387, 1385, 1386, 10196, 10197, 10198, 10199, 970);
    public static ImmutableMap<Integer, Integer> KNOWN_HYPERIDS_FOR_CATEG =
            ImmutableMap.of(91491, 444, 13062140, 26388, 12385944, 25067);
    public static int generate() {
        int i = RandomUtils.nextInt(0, someValidCategs.size());
        return someValidCategs.get(i);
    }
}
