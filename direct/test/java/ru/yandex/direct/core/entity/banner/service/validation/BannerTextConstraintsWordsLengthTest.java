package ru.yandex.direct.core.entity.banner.service.validation;

import java.util.Collection;

import org.apache.commons.lang3.StringUtils;
import org.junit.runners.Parameterized;

import static java.util.Arrays.asList;
import static ru.yandex.direct.core.entity.banner.service.validation.BannerTextConstraints.wordsHaveValidLength;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.maxLengthWordTemplateMarker;
import static ru.yandex.direct.core.entity.banner.type.title.BannerConstantsService.MAX_LENGTH_TITLE_WORD;

public class BannerTextConstraintsWordsLengthTest extends BannerTextConstraintsBaseTest {

    private static final String MAX_LENGTH_TITLE_WORD_STR = StringUtils.leftPad("word", 22, "d");
    private static final String TITLE_WORD_WITH_NON_BREAKING_SPACE = "из муллитокремнеземистого";
    private static final String TOO_LONG_TITLE_WORD_WITH_NON_BREAKING_SPACE = "из муллитокремнеземистогоX";

    public BannerTextConstraintsWordsLengthTest() {
        super(wordsHaveValidLength(MAX_LENGTH_TITLE_WORD));
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{

                {"короткое слово", "я", null},
                {"несколько коротких слов", "несколько коротких слов 123", null},
                {"несколько коротких слов и символы #", "несколько#коротких#слов 123", null},
                {"несколько коротких слов, разделенных дефисами", "несколько-коротких-слов 123", null},
                {"максимально длинное слово", MAX_LENGTH_TITLE_WORD_STR, null},
                {"максимально длинное слово в сочетании слов с неразрывным пробелом",
                        TITLE_WORD_WITH_NON_BREAKING_SPACE, null},
                {"максимально длинное слово с #", "#" + MAX_LENGTH_TITLE_WORD_STR + "#", null},
                {"превышена длина слова в сочетании слов с неразрывным пробелом",
                        TOO_LONG_TITLE_WORD_WITH_NON_BREAKING_SPACE,
                        maxLengthWordTemplateMarker(MAX_LENGTH_TITLE_WORD)},
                {"превышена длина слова",
                        MAX_LENGTH_TITLE_WORD_STR + "d",
                        maxLengthWordTemplateMarker(MAX_LENGTH_TITLE_WORD)},
                {"превышена длина слова (слово содержит узкий символ)",
                        MAX_LENGTH_TITLE_WORD_STR + ".",
                        maxLengthWordTemplateMarker(MAX_LENGTH_TITLE_WORD)}
        });
    }
}
