package ru.yandex.market.deliverycalculator.indexer.controller.solomon;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SensorLongValueList {

    @JsonProperty("sensors")
    private List<SensorLongValue> sensors;

    public List<SensorLongValue> getSensors() {
        return sensors;
    }

    public void setSensors(List<SensorLongValue> sensors) {
        this.sensors = sensors;
    }
}
