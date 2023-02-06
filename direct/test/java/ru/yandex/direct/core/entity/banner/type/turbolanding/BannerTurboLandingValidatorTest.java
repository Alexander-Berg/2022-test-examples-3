package ru.yandex.direct.core.entity.banner.type.turbolanding;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.turbolanding.model.TurboLanding;
import ru.yandex.direct.dbschema.ppc.enums.TurbolandingsPreset;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.inconsistentStateBannerTypeAndTurbolandingType;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.turboPageNotFound;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.path;

@RunWith(Parameterized.class)
public class BannerTurboLandingValidatorTest {
    @Parameterized.Parameter
    public String testName;

    @Parameterized.Parameter(1)
    public Long turboLandingId;

    @Parameterized.Parameter(2)
    public Collection<TurbolandingsPreset> validPresets;

    @Parameterized.Parameter(3)
    public Map<Long, TurboLanding> existingTurboLandings;

    @Parameterized.Parameter(4)
    public List<Defect<Long>> expectedDefects;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                // кейсы с валидным id
                {
                        "turboLandingId валиден и соответствует валидному пресету",
                        1L,
                        singletonList(TurbolandingsPreset.short_preset),
                        Map.of(1L, new TurboLanding().withPreset(TurbolandingsPreset.short_preset)),
                        null
                },

                // null
                {
                        "turboLandingId == null",
                        null,
                        emptyList(),
                        emptyMap(),
                        null
                },

                // несуществующий турболендинг
                {
                        "turboLandingId указывает на несуществующий объект",
                        1L,
                        singletonList(TurbolandingsPreset.short_preset),
                        emptyMap(),
                        singletonList(turboPageNotFound())
                },
                {
                        "turboLandingId указывает на несуществующий контент, при этом пресеты не определены",
                        1L,
                        emptyList(),
                        emptyMap(),
                        singletonList(turboPageNotFound())
                },

                // контент существует и доступен, но не соответствует пресету
                {
                        "turboLandingId указывает на существующий контент, но он не соответствует пресету",
                        1L,
                        singletonList(TurbolandingsPreset.cpm_geoproduct_preset),
                        Map.of(1L, new TurboLanding().withPreset(TurbolandingsPreset.short_preset)),
                        singletonList(inconsistentStateBannerTypeAndTurbolandingType())
                },
        });
    }

    @Test
    public void testContentPromotionIdValidator() {
        var validator = new BannerTurboLandingValidator(validPresets, existingTurboLandings);
        ValidationResult<Long, Defect> validationResult = validator.apply(turboLandingId);

        if (expectedDefects != null) {
            expectedDefects.forEach(expectedDefect -> {
                assertThat(validationResult, hasDefectDefinitionWith(validationError(path(), expectedDefect)));
            });
            assertThat("validator must add only expected errors",
                    validationResult.flattenErrors(),
                    hasSize(expectedDefects.size()));
        } else {
            assertThat(validationResult, hasNoDefectsDefinitions());
        }
    }
}
