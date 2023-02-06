package ru.yandex.market.tsum.clients.teamcity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author s-ermakov
 */
public class TestOccurrences {
    private int count;
    private final List<Test> testOccurrence = new ArrayList<>();

    public int getCount() {
        return count;
    }

    public List<Test> getTestOccurrence() {
        return Collections.unmodifiableList(testOccurrence);
    }

    public static class Test {
        private String id;
        private String name;

        public String getTestAndBuildId() {
            return id;
        }

        public String getName() {
            return name;
        }
    }
}
