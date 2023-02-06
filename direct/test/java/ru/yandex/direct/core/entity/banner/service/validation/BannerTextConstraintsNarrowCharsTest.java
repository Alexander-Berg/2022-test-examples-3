package ru.yandex.direct.core.entity.banner.service.validation;

import java.util.Collection;

import org.apache.commons.lang3.StringUtils;
import org.junit.runners.Parameterized;

import static java.util.Arrays.asList;
import static ru.yandex.direct.core.entity.banner.service.validation.BannerTextConstants.MAX_NUMBER_OF_NARROW_CHARACTERS;
import static ru.yandex.direct.core.entity.banner.service.validation.BannerTextConstraints.numberOfNarrowCharactersIsValid;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.maxNumberOfNarrowCharacters;

public class BannerTextConstraintsNarrowCharsTest extends BannerTextConstraintsBaseTest {

    private static final String MAX_NARROW_CHARACTERS_STR =
            StringUtils.leftPad(".,!:;\"", MAX_NUMBER_OF_NARROW_CHARACTERS, ".");

    public BannerTextConstraintsNarrowCharsTest() {
        super(numberOfNarrowCharactersIsValid());
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {"максимальное количество узких символов", MAX_NARROW_CHARACTERS_STR, null},
                {"максимальное количество узких символов + широкий", MAX_NARROW_CHARACTERS_STR + "a", null},
                {"превышено максимальное количество узких символов", MAX_NARROW_CHARACTERS_STR + ".",
                        maxNumberOfNarrowCharacters(MAX_NUMBER_OF_NARROW_CHARACTERS)},
                {"превышено максимальное количество узких символов (идут не подряд)",
                        MAX_NARROW_CHARACTERS_STR + "acb cba.avb",
                        maxNumberOfNarrowCharacters(MAX_NUMBER_OF_NARROW_CHARACTERS)},
        });
    }
}
