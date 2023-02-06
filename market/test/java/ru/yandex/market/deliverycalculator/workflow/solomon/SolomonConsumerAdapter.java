package ru.yandex.market.deliverycalculator.workflow.solomon;

import ru.yandex.solomon.sensors.SensorKind;
import ru.yandex.solomon.sensors.SensorsConsumer;
import ru.yandex.solomon.sensors.histogram.HistogramSnapshot;
import ru.yandex.solomon.sensors.labels.Label;

/**
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
public class SolomonConsumerAdapter implements SensorsConsumer {

    @Override
    public void onLabel(final String key, final String value) {

    }

    @Override
    public void onStreamBegin(final int countHint) {

    }

    @Override
    public void onStreamEnd() {

    }

    @Override
    public void onCommonTime(final long tsMillis) {

    }

    @Override
    public void onSensorBegin(final SensorKind kind) {

    }

    @Override
    public void onSensorEnd() {

    }

    @Override
    public void onLabelsBegin(final int countHint) {

    }

    @Override
    public void onLabelsEnd() {

    }

    @Override
    public void onLabel(final Label label) {

    }

    @Override
    public void onDouble(final long tsMillis, final double value) {

    }

    @Override
    public void onLong(final long tsMillis, final long value) {

    }

    @Override
    public void onHistogram(final long tsMillis, final HistogramSnapshot snapshot) {

    }
}
