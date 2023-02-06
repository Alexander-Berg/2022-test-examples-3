package ru.yandex.direct.core.entity.banner.service.validation;

import java.util.Collection;

import org.apache.commons.lang3.StringUtils;
import org.junit.runners.Parameterized;

import static java.util.Arrays.asList;
import static ru.yandex.direct.core.entity.banner.service.validation.BannerTextConstraints.hasValidLengthWithoutTemplateMarkerAndNarrowCharacters;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.maxTextLengthWithoutTemplateMarker;

public class BannerTextConstraintsMaxLengthWithoutTemplateAndNarrowCharsTest
        extends BannerTextConstraintsBaseTest {

    private static final int MAX_LENGTH = 35;
    private static final String MAX_LENGTH_STR = StringUtils.leftPad("long title", MAX_LENGTH, "y");
    private static final String MAX_NARROW_CHARACTERS_STR = StringUtils.leftPad(".,!:;\"", 15, ".");

    public BannerTextConstraintsMaxLengthWithoutTemplateAndNarrowCharsTest() {
        super(hasValidLengthWithoutTemplateMarkerAndNarrowCharacters(MAX_LENGTH));
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {
                        "короткий текст",
                        "продать товар",
                        null
                },
                {
                        "макимально длинный текст (только широкие символы)",
                        MAX_LENGTH_STR,
                        null
                },
                {
                        "макимально длинный текст + несколько узких символов",
                        MAX_LENGTH_STR + MAX_NARROW_CHARACTERS_STR,
                        null
                },
                {
                        "превышена длина текста",
                        MAX_LENGTH_STR + "y",
                        maxTextLengthWithoutTemplateMarker(MAX_LENGTH)
                },
                {
                        "превышена длина текста (+ узкий символ)",
                        MAX_LENGTH_STR + "y.",
                        maxTextLengthWithoutTemplateMarker(MAX_LENGTH)
                },
                {
                        "максимально длинный текст с шаблоном",
                        "#" + MAX_LENGTH_STR + "#",
                        null
                },
                {
                        "максимально длинный текст с #",
                        "#" + MAX_LENGTH_STR,
                        maxTextLengthWithoutTemplateMarker(MAX_LENGTH)
                }
        });
    }
}
