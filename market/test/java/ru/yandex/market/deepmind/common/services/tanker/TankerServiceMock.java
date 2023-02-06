package ru.yandex.market.deepmind.common.services.tanker;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author kravchenko-aa
 * @date 03.03.2020
 */
public class TankerServiceMock implements TankerService {
    private Map<String, Map<String, String>> keySets = new HashMap<>();

    @Override
    public Map<String, String> getKeys(String keySetName) {
        return keySets.getOrDefault(keySetName, Collections.emptyMap());
    }

    public void addKeys(String keySet, Map<String, String> keys) {
        Map<String, String> keyMap = keySets.computeIfAbsent(keySet, k -> new HashMap<>());
        keyMap.putAll(keys);
    }
}
