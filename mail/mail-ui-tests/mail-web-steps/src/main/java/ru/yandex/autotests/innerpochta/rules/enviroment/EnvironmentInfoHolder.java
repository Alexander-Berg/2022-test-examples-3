package ru.yandex.autotests.innerpochta.rules.enviroment;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author a-zoshchuk
 */
public class EnvironmentInfoHolder {

    private static final EnvironmentInfoHolder INSTANCE = new EnvironmentInfoHolder();

    private Map<String, String> environmentInfo = new ConcurrentHashMap<>();

    public Map<String, String> getEnvironmentInfo() {
        return environmentInfo;
    }

    public static EnvironmentInfoHolder getInstance() {
        return INSTANCE;
    }
}
