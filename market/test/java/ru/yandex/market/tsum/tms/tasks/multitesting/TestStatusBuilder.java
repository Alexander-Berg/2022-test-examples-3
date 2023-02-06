package ru.yandex.market.tsum.tms.tasks.multitesting;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

/**
 * @author Alexander Kedrik <a href="mailto:alkedr@yandex-team.ru"></a>
 * @date 12.01.2018
 */
public class TestStatusBuilder {
    private String key;

    public static TestStatusBuilder aStatus() {
        return new TestStatusBuilder();
    }

    public TestStatusBuilder key(String key) {
        this.key = key;
        return this;
    }

    public Map<String, String> buildMap() {
        return ImmutableMap.of("key", key);
    }
}
