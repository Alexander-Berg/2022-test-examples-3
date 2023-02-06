package ru.yandex.direct.core.entity.banner.type.measurers;

import java.util.Collection;

import org.junit.runners.Parameterized;

import static java.util.Arrays.asList;
import static ru.yandex.direct.core.entity.banner.model.BannerMeasurerSystem.ADMETRICA;
import static ru.yandex.direct.core.entity.banner.model.BannerMeasurerSystem.DV;
import static ru.yandex.direct.core.entity.banner.model.BannerMeasurerSystem.MEDIASCOPE;
import static ru.yandex.direct.core.entity.banner.model.BannerMeasurerSystem.NO;
import static ru.yandex.direct.core.entity.banner.model.BannerMeasurerSystem.WEBORAMA;
import static ru.yandex.direct.core.entity.banner.type.measurers.BannerMeasurersConstraints.validBannerMeasurerParams;
import static ru.yandex.direct.validation.defect.CommonDefects.invalidValue;

public class BannerMeasurerConstraintsParamsTest extends BannerMeasurerConstraintsBaseTest {
    private static final String DEFAULT_PARAMS = "{\"campaignId\":4,\"placementId\":4,\"creativeId\":4," +
            "\"criteria\":\"ya\",\"type\":\"banner\"}";

    public BannerMeasurerConstraintsParamsTest() {
        super(validBannerMeasurerParams());
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                // общие позитивные тесты
                {
                        "валидный dv без параметров",
                        measurer(DV, ""),
                        null,
                },
                {
                        "валидный MEDIASCOPE с пустым json",
                        measurer(MEDIASCOPE, "{}"),
                        null,
                },
                // плохие форматы json
                {
                        "кривой формат json 1",
                        measurer(DV, "1"),
                        invalidValue(),
                },
                {
                        "кривой формат json true",
                        measurer(DV, "true"),
                        invalidValue(),
                },
                {
                        "кривой формат json !!",
                        measurer(DV, "{}!!"),
                        invalidValue(),
                },
                {
                        "кривой формат json !",
                        measurer(DV, "!{}"),
                        invalidValue(),
                },
                //по системе измерения
                {
                        "MEDIASCOPE c плохим форматом json",
                        measurer(MEDIASCOPE, "{1}"),
                        invalidValue(),
                },
                {
                        "DV можно любой валидный json",
                        measurer(DV, "{\"x\":[1]}"),
                        null,
                },
                {
                        "MEDIASCOPE валидный json 1",
                        measurer(MEDIASCOPE, "{\"x\":1}"),
                        null,
                },
                {
                        "MEDIASCOPE валидный json blab",
                        measurer(MEDIASCOPE, "{\"blab\":\"labla\"}"),
                        null,
                },
                {
                        "измеритель NO нельзя сохранять",
                        measurer(NO, DEFAULT_PARAMS),
                        invalidValue(),
                },
                {
                        "ADMETRICA валидный",
                        measurer(ADMETRICA, DEFAULT_PARAMS),
                        null,
                },
                {
                        "ADMETRICA campaignId не может быть 0",
                        measurer(ADMETRICA, "{\"campaignId\":0,\"placementId\":4,\"creativeId\":4," +
                                "\"criteria\":\"ya\",\"type\":\"banner\"}"),
                        invalidValue(),
                },
                {
                        "ADMETRICA тип не входит в наш enum",
                        measurer(ADMETRICA, "{\"campaignId\":4,\"placementId\":4,\"creativeId\":4," +
                                "\"criteria\":\"ya\",\"type\":\"newType\"}"),
                        invalidValue(),
                },
                {
                        "ADMETRICA валидный",
                        measurer(ADMETRICA, "{\"campaignId\":4,\"placementId\":4,\"creativeId\":4," +
                                "\"criteria\":\"ya\",\"type\":\"banner\"}"),
                        null,
                },
                {
                        "WEBORAMA валидный",
                        measurer(WEBORAMA, "{\"account\":1,\"tte\":1,\"aap\":1}"),
                        null,
                },
                {
                        "WEBORAMA нет параметра aap, где должен быть creativeId",
                        measurer(WEBORAMA, "{\"account\":1,\"tte\":1,\"ap\":1}"),
                        invalidValue(),
                },
        });
    }
}
