package ru.yandex.direct.core.entity.banner.type.displayhref;

import java.util.Collection;

import org.junit.runners.Parameterized;

import static java.util.Arrays.asList;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.duplicateSpecCharsInDisplayHref;
import static ru.yandex.direct.core.entity.banner.type.displayhref.BannerWithDisplayHrefConstraints.displayHrefNotContainsDuplicatedSpecialChars;

public class BannerWithDisplayHrefConstraintsDublicatedSpecialCharsTest extends BannerWithDisplayHrefConstraintsBaseTest {

    public BannerWithDisplayHrefConstraintsDublicatedSpecialCharsTest() {
        super(displayHrefNotContainsDuplicatedSpecialChars());
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {"валидный display href 1 ", "/sell", null},
                {"валидный display href 2", "sell-buy", null},

                {"// в display href недопустим", "//sell", duplicateSpecCharsInDisplayHref()},
                {"-- в display href недопустим", "sell--buy", duplicateSpecCharsInDisplayHref()},
        });
    }
}
