package ru.yandex.market.tms.quartz2.solomon;

import org.mockito.ArgumentMatcher;

import ru.yandex.misc.monica.solomon.sensors.PushSensorsData;
import ru.yandex.misc.monica.solomon.sensors.Sensor;

public class PushSensorsDataMather implements ArgumentMatcher<PushSensorsData> {
    private final PushSensorsData expected;

    PushSensorsDataMather(PushSensorsData expected) {
        this.expected = expected;
    }

    @Override
    public boolean matches(PushSensorsData actual) {
        Sensor expectedSensor = expected.sensors.get(0);
        Sensor actualSensor = actual.sensors.get(0);
        return expected.commonLabels.get("project").equals(actual.commonLabels.get("project"))
                && expected.commonLabels.get("cluster").equals(actual.commonLabels.get("cluster"))
                && expected.commonLabels.get("service").equals(actual.commonLabels.get("service"))
                && expectedSensor.value < actualSensor.value
                && expectedSensor.labels.get("sensor").equals(actualSensor.labels.get("sensor"))
                && expectedSensor.labels.get(SolomonJobRunTimeReporter.JOB_NAME_LABEL_NAME).equals(
                actualSensor.labels.get(SolomonJobRunTimeReporter.JOB_NAME_LABEL_NAME)
        );
    }
}

