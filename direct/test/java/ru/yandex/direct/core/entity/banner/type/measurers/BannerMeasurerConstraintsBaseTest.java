package ru.yandex.direct.core.entity.banner.type.measurers;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.banner.model.BannerMeasurer;
import ru.yandex.direct.core.entity.banner.model.BannerMeasurerSystem;
import ru.yandex.direct.validation.builder.Constraint;
import ru.yandex.direct.validation.result.Defect;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@RunWith(Parameterized.class)
public abstract class BannerMeasurerConstraintsBaseTest {
    private Constraint<BannerMeasurer, Defect> constraint;

    @Parameterized.Parameter
    public String name;

    @Parameterized.Parameter(1)
    public BannerMeasurer arg;

    @Parameterized.Parameter(2)
    public Defect expectedDefect;

    protected BannerMeasurerConstraintsBaseTest(Constraint<BannerMeasurer, Defect> constraint) {
        this.constraint = constraint;
    }

    @Test
    public void testParametrized() {
        assertThat(constraint.apply(arg), is(expectedDefect));
    }

    protected static BannerMeasurer measurer(BannerMeasurerSystem system, String params) {
        return new BannerMeasurer()
                    .withBannerMeasurerSystem(system)
                    .withParams(params);
    }
}
