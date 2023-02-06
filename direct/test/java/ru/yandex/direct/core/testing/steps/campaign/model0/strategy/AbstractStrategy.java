package ru.yandex.direct.core.testing.steps.campaign.model0.strategy;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractStrategy {

    private Map<String, Object> unknownFields = new HashMap<>();

    public Map<String, Object> getUnknownFields() {
        return unknownFields;
    }

    public void setUnknownFields(Map<String, Object> unknownFields) {
        this.unknownFields = unknownFields;
    }

    @SuppressWarnings("unchecked")
    public <T extends AbstractStrategy> T withUnknownFields(Map<String, Object> unknownFields) {
        this.unknownFields = unknownFields;
        return (T) this;
    }
}
