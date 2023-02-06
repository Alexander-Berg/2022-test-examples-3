package ru.yandex.market.delivery.deliveryintegrationtests.wms.dto;

import java.util.HashMap;
import java.util.Map;

/**
 * Прогресс волны.
 */
public enum WaveProgress {

    CREATED("Создано"),

    RESERVED("Резервирование завершено"),

    STARTED("Запуск задачи завершен");

    private final String state;

    WaveProgress(String state) {
        this.state = state;
    }

    private static final Map<String, WaveProgress> strMap = new HashMap<>();

    static {
        for (WaveProgress s : WaveProgress.values()) {
            strMap.put(s.state, s);
        }
    }

    public static WaveProgress get(String s) {
        if (strMap.containsKey(s))
            return strMap.get(s);
        else {
            String errorString = String.format("No WaveProgress found for input: %s", s);
            throw new IllegalArgumentException(errorString);
        }
    }

    public String getState() {
        return state;
    }
}
