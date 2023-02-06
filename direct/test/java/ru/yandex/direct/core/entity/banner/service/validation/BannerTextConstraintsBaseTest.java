package ru.yandex.direct.core.entity.banner.service.validation;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.validation.builder.Constraint;
import ru.yandex.direct.validation.result.Defect;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@RunWith(Parameterized.class)
public abstract class BannerTextConstraintsBaseTest {

    private Constraint<String, Defect> constraint;

    @Parameterized.Parameter
    public String name;

    @Parameterized.Parameter(1)
    public String arg;

    @Parameterized.Parameter(2)
    public Defect expectedDefect;

    protected BannerTextConstraintsBaseTest(Constraint<String, Defect> constraint) {
        this.constraint = constraint;
    }

    @Test
    public void testParametrized() {
        assertThat(constraint.apply(arg), is(expectedDefect));
    }
}
