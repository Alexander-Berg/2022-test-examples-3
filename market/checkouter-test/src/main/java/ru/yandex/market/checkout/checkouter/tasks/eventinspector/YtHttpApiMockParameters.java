package ru.yandex.market.checkout.checkouter.tasks.eventinspector;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * @author sergeykoles
 * Created on: 14.05.18
 */
public class YtHttpApiMockParameters {
    private Map<String, Supplier<String>> getPathToResponse = new HashMap<>();

    public Map<String, Supplier<String>> getGetPathToResponse() {
        return getPathToResponse;
    }

    public YtHttpApiMockParameters withGetPathToResponse(Map<String, Supplier<String>> getPathToResponse) {
        this.getPathToResponse = getPathToResponse;
        return this;
    }
}
