package ru.yandex.strictweb.scriptjava.test;

import ru.yandex.strictweb.scriptjava.base.ScriptJava;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

public class TestCollections {

    private final String helloWorld = "Hello world!";

    public boolean test() {
        boolean testArrayAndVector = testArrayAndVector();
        UnitTest.println("testArrayAndVector " + testArrayAndVector);

        boolean testStringAndVector = testStringAndVector();
        UnitTest.println("testStringAndVector " + testStringAndVector);

        boolean testSet = testSet();
        UnitTest.println("testSet " + testSet);

        boolean testMap = testMap();
        UnitTest.println("testMap " + testMap);

        return testArrayAndVector && testStringAndVector && testSet && testMap;
    }

    /** @noinspection Java8MapApi*/
    private boolean testMap() {
        Map<String, Integer> map = new TreeMap<>();

        for (int i = 0; i < helloWorld.length(); i++) {
            String k = helloWorld.charAt(i) + "";
            if (map.get(k) != null) {
                map.put(k, map.get(k) + 1);
            } else {
                map.put(k, 1);
            }
        }

        return map.get("l") == 3 && map.get("?") == null;
    }

    private boolean testSet() {
        Set<String> set = new TreeSet<>();
        for (int i = 0; i < helloWorld.length(); i++) {
            set.add(helloWorld.charAt(i) + "");
        }

        boolean result = true;
        for (String s : set) {
            result = result && helloWorld.contains(s);
        }

        for (int i = 0; i < helloWorld.length(); i++) {
            result = result && set.contains(helloWorld.charAt(i) + "");
        }

        return result;
    }

    private boolean testStringAndVector() {
        List<String> list = new Vector<>();

        for (int i = 0; i < helloWorld.length(); i++) {
            list.add(helloWorld.charAt(i) + "");
        }

        String b = "";

        for (String s : list) {
            b += s;
        }

        return ScriptJava.compareStrings(helloWorld, b) == 0;
    }

    private boolean testArrayAndVector() {
        int[] inta = new int[10];
        List<Integer> intv = new Vector<>();

        for (int i = 0; i < 10; i++) {
            inta[i] = i;
            intv.add(i);
        }

        int sum = 0;

        for (int i = 0; i < 10; i++) {
            sum += inta[i] + intv.get(i);
        }

        for (Integer i : intv) {
            sum -= i * 2;
        }

        return intv.size() == 10
                && inta.length == intv.size()
                && sum == 0;
    }
}
