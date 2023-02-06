package ru.yandex.direct.core.entity.metrika.service.validation;

import java.util.List;

import org.junit.Test;

import ru.yandex.direct.core.entity.retargeting.model.MetrikaSegmentPreset;
import ru.yandex.direct.core.entity.retargeting.model.PresetsByCounter;
import ru.yandex.direct.validation.result.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.ids.CollectionDefectIds.Gen.MUST_BE_IN_COLLECTION;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

public class MetrikaSegmentValidationServiceTest {
    private static final int DEFAULT_ID = 1;

    private MetrikaSegmentValidationService metrikaSegmentValidationService = new MetrikaSegmentValidationService();

    @Test
    public void testEmptyInput() {
        var result = metrikaSegmentValidationService.validateSegmentCreationParameters(List.of(), List.of());
        assertThat(result).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void testValidInput() {
        var metrikaSegmentPreset = new MetrikaSegmentPreset()
                .withCounterId(DEFAULT_ID)
                .withPresetId(DEFAULT_ID)
                .withName("Some name")
                .withDomain("ya.ru");
        var presetsByCounter = new PresetsByCounter()
                .withCounterId(DEFAULT_ID)
                .withPresetIds(List.of(DEFAULT_ID));

        var result = metrikaSegmentValidationService.validateSegmentCreationParameters(
                List.of(presetsByCounter), List.of(metrikaSegmentPreset));

        assertThat(result).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void testUnavailableCounterId() {
        var metrikaSegmentPreset = new MetrikaSegmentPreset()
                .withCounterId(DEFAULT_ID)
                .withPresetId(DEFAULT_ID)
                .withName("Some name")
                .withDomain("ya.ru");
        var presetsByCounter = new PresetsByCounter()
                .withCounterId(DEFAULT_ID + 1) // No such counter id in available presets
                .withPresetIds(List.of(DEFAULT_ID));

        var result = metrikaSegmentValidationService.validateSegmentCreationParameters(
                List.of(presetsByCounter), List.of(metrikaSegmentPreset));

        assertThat(result).is(matchedBy(hasDefectDefinitionWith(validationError(counterIdPath(), MUST_BE_IN_COLLECTION))));
    }

    @Test
    public void testUnavailablePresetId() {
        var metrikaSegmentPreset = new MetrikaSegmentPreset()
                .withCounterId(DEFAULT_ID)
                .withPresetId(DEFAULT_ID)
                .withName("Some name")
                .withDomain("ya.ru");
        var presetsByCounter = new PresetsByCounter()
                .withCounterId(DEFAULT_ID)
                .withPresetIds(List.of(DEFAULT_ID + 1)); // No such preset id in available presets

        var result = metrikaSegmentValidationService.validateSegmentCreationParameters(
                List.of(presetsByCounter), List.of(metrikaSegmentPreset));

        assertThat(result).is(matchedBy(hasDefectDefinitionWith(validationError(presetIdPath(), MUST_BE_IN_COLLECTION))));
    }

    private static Path counterIdPath() {
        return path(index(0), field(PresetsByCounter.COUNTER_ID));
    }

    private static Path presetIdPath() {
        return path(index(0), field(PresetsByCounter.PRESET_IDS), index(0));
    }
}
