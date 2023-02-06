package ru.yandex.direct.core.entity.banner.type.displayhref;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.validation.builder.Constraint;
import ru.yandex.direct.validation.result.Defect;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@RunWith(Parameterized.class)
public abstract class BannerWithDisplayHrefConstraintsBaseTest {

    private Constraint<String, Defect> constraint;

    @Parameterized.Parameter
    public String name;

    @Parameterized.Parameter(1)
    public String displayHref;

    @Parameterized.Parameter(2)
    public Defect expectedDefect;

    protected BannerWithDisplayHrefConstraintsBaseTest(Constraint<String, Defect> constraint) {
        this.constraint = constraint;
    }

    @Test
    public void testParametrized() {
        assertThat(constraint.apply(displayHref), is(expectedDefect));
    }
}
