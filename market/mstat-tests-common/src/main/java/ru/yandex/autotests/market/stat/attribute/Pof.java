package ru.yandex.autotests.market.stat.attribute;

import org.apache.commons.lang.math.RandomUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by kateleb on 21.05.15.
 */
public class Pof {
    private static List<String> knowPofStates = new ArrayList<>();
    private static List<String> unknowPofStates = new ArrayList<>();
    private static List<String> shortPofStates = new ArrayList<>();
    private static List<String> longPofStates = new ArrayList<>();

    private static List<String> getKnowPofStates() {
        if (knowPofStates.isEmpty()) {
            knowPofStates.add("1000");
            knowPofStates.add("1001");
            knowPofStates.add("3002");

        }
        return knowPofStates;
    }

    private static List<String> getUnknowPofStates() {
        if (unknowPofStates.isEmpty()) {
            //   unknowPofStates.addAll(extractPofFromElliptics(POFS_WITH_UNKNOW_STATE));
            unknowPofStates.add(Values.maxInt());
            unknowPofStates.add("2061932");
        }
        return unknowPofStates;
    }

    public static String getShortRandomKnownAndUnknownPof() {
        if (shortPofStates.isEmpty()) {
            shortPofStates.addAll(getKnowPofStates().stream().filter(it ->
            {
                Integer pof = Integer.valueOf(it);
                return pof < 1500 && pof > 1001;
            }).collect(Collectors.toList()));
            shortPofStates.addAll(getUnknowPofStates().stream().filter(it ->
            {
                Integer pof = Integer.valueOf(it);
                return pof < 1500 && pof > 1001;
            }).collect(Collectors.toList()));
        }
        return shortPofStates.get(RandomUtils.nextInt(shortPofStates.size()));
    }


    public static String getLongRandomKnownAndUnknownPof() {
        if (longPofStates.isEmpty()) {
            longPofStates.addAll(getKnowPofStates().stream().filter(it -> Integer.valueOf(it) >= 1500).collect(Collectors.toList()));
            longPofStates.addAll(getUnknowPofStates().stream().filter(it -> Integer.valueOf(it) >= 1500).collect(Collectors.toList()));
        }
        return longPofStates.get(RandomUtils.nextInt(longPofStates.size()));
    }

    public static String getPof(String pp) {
        if (Integer.valueOf(pp) < 1000) {
            return RandomUtils.nextBoolean() ? pp : getClid();
        }
        return pp;
    }

    public static String getClid() {
        //user came to market from outside: https://wiki.yandex-team.ru/Market/Projects/PartnerProgram/pof
        int startClidPof = 500;
        int i = RandomUtils.nextInt(269);
        return String.valueOf(startClidPof + i);
    }

    public static String getRandomKnownPof() {
        return getKnowPofStates().get(RandomUtils.nextInt(getKnowPofStates().size()));
    }

    public static String getRandomUnknownPof() {
        return getUnknowPofStates().get(RandomUtils.nextInt(getUnknowPofStates().size()));
    }

    public static String getStateForPof(String pof) {
        if (getUnknowPofStates().contains(pof)) {
            return "3";
        } else if (pof.equals("1002")) { //spike
            return "0";
        }
        return "1";
    }
}
