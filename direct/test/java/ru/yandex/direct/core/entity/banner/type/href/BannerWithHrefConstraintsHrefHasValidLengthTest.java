package ru.yandex.direct.core.entity.banner.type.href;

import java.util.Collection;

import org.apache.commons.lang3.StringUtils;
import org.junit.runners.Parameterized;

import static java.util.Arrays.asList;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.maxHrefLengthWithoutTemplateMarker;
import static ru.yandex.direct.core.entity.banner.type.href.BannerWithHrefConstraints.hrefHasValidLength;


public class BannerWithHrefConstraintsHrefHasValidLengthTest extends BannerWithHrefConstraintsBaseTest {
    private static final String MAX_LENGTH_HREF_STR = StringUtils.leftPad("http", 1024, "s");

    public BannerWithHrefConstraintsHrefHasValidLengthTest() {
        super(hrefHasValidLength());
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                // positive
                {"короткий href", "http", null},
                {"максимально длинный href", MAX_LENGTH_HREF_STR, null},
                {"максимально длинный href с шаблоном", "#" + MAX_LENGTH_HREF_STR + "#", null},
                {"максимально длинный href с #", "#" + MAX_LENGTH_HREF_STR,
                        maxHrefLengthWithoutTemplateMarker(BannerWithHrefConstants.MAX_LENGTH_HREF)},
                // negative
                {"превышена длина href", MAX_LENGTH_HREF_STR + "d",
                        maxHrefLengthWithoutTemplateMarker(BannerWithHrefConstants.MAX_LENGTH_HREF)},
                {"максимально длинный href с узкими символами", "." + MAX_LENGTH_HREF_STR + ".",
                        maxHrefLengthWithoutTemplateMarker(BannerWithHrefConstants.MAX_LENGTH_HREF)},
        });
    }

}
