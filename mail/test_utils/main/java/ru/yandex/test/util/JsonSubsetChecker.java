package ru.yandex.test.util;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

// Like JsonChecker, but in maps checks only keys, that presents in `expected'
public class JsonSubsetChecker extends JsonChecker {
    public JsonSubsetChecker(final String expected) {
        super(expected);
    }

    public JsonSubsetChecker(
        final String expected,
        final double precision,
        final double epsilon)
    {
        super(expected, precision, epsilon);
    }

    public JsonSubsetChecker(final Object expected) {
        super(expected);
    }

    public JsonSubsetChecker(
        final Object expected,
        final double precision,
        final double epsilon)
    {
        super(expected, precision, epsilon);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void filter(
        final Object expected,
        final Object actual)
    {
        if (expected instanceof Map && actual instanceof Map) {
            Map<String, ?> expectedMap = (Map<String, ?>) expected;
            Map<String, ?> actualMap = (Map<String, ?>) actual;
            Iterator<String> iter = actualMap.keySet().iterator();
            while (iter.hasNext()) {
                Object key = iter.next();
                if (expectedMap.containsKey(key)) {
                    filter(expectedMap.get(key), actualMap.get(key));
                } else {
                    iter.remove();
                }
            }
            for (Map.Entry<String, ?> entry: expectedMap.entrySet()) {
                if (entry.getValue() == null) {
                    actualMap.putIfAbsent(entry.getKey(), null);
                }
            }
        } else if (expected instanceof List && actual instanceof List) {
            List<?> expectedList = (List<?>) expected;
            List<?> actualList = (List<?>) actual;
            int size = Math.min(expectedList.size(), actualList.size());
            for (int i = 0; i < size; ++i) {
                filter(expectedList.get(i), actualList.get(i));
            }
        }
    }
}

