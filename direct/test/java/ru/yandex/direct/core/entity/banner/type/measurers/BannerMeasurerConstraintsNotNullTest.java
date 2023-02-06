package ru.yandex.direct.core.entity.banner.type.measurers;

import java.util.Collection;

import org.junit.runners.Parameterized;

import static java.util.Arrays.asList;
import static ru.yandex.direct.core.entity.banner.model.BannerMeasurerSystem.DV;
import static ru.yandex.direct.core.entity.banner.type.measurers.BannerMeasurersConstraints.bannerMeasurerNotNull;
import static ru.yandex.direct.validation.defect.CommonDefects.notNull;

public class BannerMeasurerConstraintsNotNullTest extends BannerMeasurerConstraintsBaseTest {
    public BannerMeasurerConstraintsNotNullTest() {
        super(bannerMeasurerNotNull());
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                // positive
                {
                        "валидный dv без параметров",
                        measurer(DV, ""),
                        null,
                },
                // negative
                {
                        "система измерения не может быть null",
                        measurer(null, ""),
                        notNull(),
                },
        });
    }
}
