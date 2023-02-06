package ru.yandex.direct.core.entity.banner.converter;

import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.banner.model.Language;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.banner.type.language.BannerLanguageConverter.convertLanguage;

@RunWith(Parameterized.class)
public class BannerLanguageConverterTest {

    @Parameterized.Parameter(0)
    public String language;

    @Parameterized.Parameter(1)
    public Language expected;

    @Parameterized.Parameters(name = "Language: {0}")
    public static Collection<Object[]> data() {
        return asList(
                new Object[]{"Yes", Language.YES},
                new Object[]{"No", Language.NO},
                new Object[]{"ru", Language.RU_},
                new Object[]{"en", Language.EN},
                new Object[]{"uk", Language.UK},
                new Object[]{"de", Language.DE},
                new Object[]{"be", Language.BE},
                new Object[]{"kk", Language.KK},
                new Object[]{"tr", Language.TR},
                new Object[]{"uz", Language.UZ},
                new Object[]{"invalid", Language.UNKNOWN}
        );
    }

    @Test
    public void convertLanguageTest() {
        assertThat(convertLanguage(language), is(expected));
    }
}
