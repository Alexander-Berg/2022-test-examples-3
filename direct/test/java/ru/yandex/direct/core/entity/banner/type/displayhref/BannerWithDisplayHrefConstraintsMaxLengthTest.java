package ru.yandex.direct.core.entity.banner.type.displayhref;

import java.util.Collection;

import org.apache.commons.lang3.StringUtils;
import org.junit.runners.Parameterized;

import static java.util.Arrays.asList;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.maxLengthDisplayHref;
import static ru.yandex.direct.core.entity.banner.type.displayhref.BannerWithDisplayHrefConstraints.displayHrefHasValidLength;

public class BannerWithDisplayHrefConstraintsMaxLengthTest extends BannerWithDisplayHrefConstraintsBaseTest {

    private static final String MAX_LENGTH_DISPLAY_HREF_STR = StringUtils.leftPad("display", 20, "h");

    public BannerWithDisplayHrefConstraintsMaxLengthTest() {
        super(displayHrefHasValidLength());
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {"короткий displayHref", "http", null},
                {"максимально длинный displayHref", MAX_LENGTH_DISPLAY_HREF_STR, null},
                {"максимальная длина displayHref + два # (шаблон)", "#" + MAX_LENGTH_DISPLAY_HREF_STR + "#", null},

                {"превышена длина displayHref", MAX_LENGTH_DISPLAY_HREF_STR + "d",
                        maxLengthDisplayHref(BannerWithDisplayHrefConstraints.MAX_LENGTH_DISPLAY_HREF)},
                {"превышена длина displayHref с одним #", MAX_LENGTH_DISPLAY_HREF_STR + "#",
                        maxLengthDisplayHref(BannerWithDisplayHrefConstraints.MAX_LENGTH_DISPLAY_HREF)},
        });
    }
}
