package ru.yandex.yt.yqltest.impl;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 06.09.2021
 */
public class YqlTestMockDir extends YqlTestMockObject {
    private final Map<String, YqlTestMockTable> tables = new LinkedHashMap<>();

    public Map<String, YqlTestMockTable> getTables() {
        return tables;
    }

}
