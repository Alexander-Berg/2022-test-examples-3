package ru.yandex.market.deliverycalculator.indexer.controller.solomon;

import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import ru.yandex.solomon.sensors.SensorKind;

/**
 * Класс представляющий сенсор, используется только в тестах
 *
 * @author yakun.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SensorLongValue {

    @JsonProperty("kind")
    private SensorKind kind;

    @JsonProperty("labels")
    private Map<String, String> labels;

    @JsonProperty("value")
    private long value;

    public static SensorLongValue create(SensorKind kind, Map<String, String> labels, long value) {
        SensorLongValue result = new SensorLongValue();

        result.setKind(kind);
        result.setLabels(labels);
        result.setValue(value);

        return result;
    }

    public SensorKind getKind() {
        return kind;
    }

    public void setKind(SensorKind kind) {
        this.kind = kind;
    }

    public Map<String, String> getLabels() {
        return labels;
    }

    public void setLabels(Map<String, String> labels) {
        this.labels = labels;
    }

    public long getValue() {
        return value;
    }

    public void setValue(long value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SensorLongValue that = (SensorLongValue) o;
        return value == that.value &&
                kind == that.kind &&
                Objects.equals(labels, that.labels);
    }

    @Override
    public int hashCode() {
        return Objects.hash(kind, labels, value);
    }
}
