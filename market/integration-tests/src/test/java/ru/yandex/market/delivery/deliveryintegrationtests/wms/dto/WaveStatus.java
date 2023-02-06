package ru.yandex.market.delivery.deliveryintegrationtests.wms.dto;

import java.util.HashMap;
import java.util.Map;

/**
 * Статус волны.
 */
public enum WaveStatus {

    NOT_STARTED(0, "Не запущено"),

    RESERVED(3, "Зарезервировано"),

    STARTED(5, "Запущено в диспетчер задач"),

    FINISHED(9, "Завершено");

    private final int id;
    private final String state;

    WaveStatus(int id, String state) {
        this.id = id;
        this.state = state;
    }

    private static final Map<String, WaveStatus> strMap = new HashMap<>();
    private static final Map<Integer, WaveStatus> intMap = new HashMap<>();

    static {
        for (WaveStatus s : WaveStatus.values()) {
            strMap.put(s.state, s);
            intMap.put(s.id, s);
        }
    }

    public static WaveStatus get(int i) {
        if (intMap.containsKey(i))
            return intMap.get(i);
        else {
            String errorString = String.format("No WaveStatus found for input: %s", i);
            throw new IllegalArgumentException(errorString);
        }
    }

    public static WaveStatus get(String s) {
        if (strMap.containsKey(s))
            return strMap.get(s);
        else {
            String errorString = String.format("No WaveStatus found for input: %s", s);
            throw new IllegalArgumentException(errorString);
        }
    }

    public String getState() {
        return state;
    }

    public int getId() {
        return id;
    }
}
