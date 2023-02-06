package ru.yandex.market.deliverycalculator.workflow.solomon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import ru.yandex.market.deliverycalculator.workflow.solomon.model.BoilingKey;
import ru.yandex.market.deliverycalculator.workflow.solomon.model.BoilingState;
import ru.yandex.market.deliverycalculator.workflow.solomon.util.SolomonUtils;
import ru.yandex.solomon.sensors.labels.Label;
import ru.yandex.solomon.sensors.labels.Labels;
import ru.yandex.solomon.sensors.registry.SensorId;

/**
 * Утилиты для проверки взаимодействия с {@link BoilingSolomonService}.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
@ParametersAreNonnullByDefault
public final class BoilingSolomonTestUtil {

    public BoilingSolomonTestUtil() {
        throw new UnsupportedOperationException();
    }

    /**
     * Проверить, что в соломон добавились метрики о варке.
     */
    public static void checkStageEvents(final BoilingSolomonService boilingSolomonService,
                                        final BoilingKey... expectedKeys) {
        // Начало варки
        final ArgumentCaptor<BoilingKey> startKeyCaptor = ArgumentCaptor.forClass(BoilingKey.class);
        Mockito.verify(boilingSolomonService, Mockito.times(expectedKeys.length))
                .startStage(startKeyCaptor.capture(), Mockito.anyLong());
        final List<BoilingKey> startKeys = startKeyCaptor.getAllValues();
        MatcherAssert.assertThat(startKeys, Matchers.containsInAnyOrder(expectedKeys));

        // Конец варки
        final ArgumentCaptor<BoilingKey> finishKeyCaptor = ArgumentCaptor.forClass(BoilingKey.class);
        Mockito.verify(boilingSolomonService, Mockito.times(expectedKeys.length))
                .finishStage(finishKeyCaptor.capture(), Mockito.anyLong());
        final List<BoilingKey> finishKeys = finishKeyCaptor.getAllValues();
        MatcherAssert.assertThat(finishKeys, Matchers.containsInAnyOrder(expectedKeys));
    }

    /**
     * Почистить все сенсоры Соломона.
     */
    public static void clearSensors(final BoilingSolomonService boilingSolomonService) {
        final List<SensorId> sensors = new ArrayList<>();
        SolomonUtils.SOLOMON_REGISTRY.supply(123L, new SolomonConsumerAdapter() {

            private final Map<String, String> labels = new HashMap<>();

            @Override
            public void onLabelsBegin(final int countHint) {
                labels.clear();
            }

            @Override
            public void onLabelsEnd() {
                final String sensorName = labels.remove("sensor");
                final SensorId sensorId = new SensorId(sensorName, Labels.of(labels));
                sensors.add(sensorId);
            }

            @Override
            public void onLabel(final Label label) {
                labels.put(label.getKey(), label.getValue());
            }
        });

        sensors.forEach(SolomonUtils.SOLOMON_REGISTRY::removeSensor);
        clearBoilingSensors(boilingSolomonService);
    }

    private static void clearBoilingSensors(final BoilingSolomonService boilingSolomonService) {
        // Перестаем отслеживать
        final Multimap<BoilingKey, Labels> forClean = ArrayListMultimap.create();
        boilingSolomonService.getStates(Collections.emptySet()).stream()
                .map(BoilingState::getBoilingKey)
                .forEach(e -> forClean.put(e, null));
        boilingSolomonService.clearStates(forClean);
        boilingSolomonService.sendStage(() -> {
        });
    }
}
