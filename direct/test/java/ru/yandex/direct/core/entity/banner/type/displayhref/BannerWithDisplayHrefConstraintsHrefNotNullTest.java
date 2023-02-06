package ru.yandex.direct.core.entity.banner.type.displayhref;

import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.validation.result.Defect;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.invalidDisplayHrefUsage;
import static ru.yandex.direct.core.entity.banner.type.displayhref.BannerWithDisplayHrefConstraints.hrefIsSetIfDisplayHrefNotNull;

@RunWith(Parameterized.class)
public class BannerWithDisplayHrefConstraintsHrefNotNullTest {

    private static final String VALID_HREF = "http://www.ya.ru";

    @Parameterized.Parameter
    public String name;

    @Parameterized.Parameter (1)
    public String href;

    @Parameterized.Parameter(2)
    public String displayHref;

    @Parameterized.Parameter(3)
    public Defect expectedDefect;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {"пустой displayHref с пустым href разрешен", null, null, null},
                {"непустой displayHref c непустым href разрешен", VALID_HREF, "/sell", null},

                {"непустой displayHref c пустым href недопустим", null, "/sell", invalidDisplayHrefUsage()},
        });
    }

    @Test
    public void testParametrized() {
        var constraint = hrefIsSetIfDisplayHrefNotNull(new TextBanner().withHref(href));
        assertThat(constraint.apply(displayHref), is(expectedDefect));
    }

}
