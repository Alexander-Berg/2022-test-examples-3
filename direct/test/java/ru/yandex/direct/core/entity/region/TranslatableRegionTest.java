package ru.yandex.direct.core.entity.region;

import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.i18n.Language;

import static java.util.Arrays.asList;
import static junit.framework.TestCase.assertTrue;

@RunWith(Parameterized.class)
public class TranslatableRegionTest {

    @Parameterized.Parameter
    public Language language;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Language> data() {
        return asList(Language.class.getEnumConstants());
    }

    @Test
    public void languageIsSupportedByTranslatedRegion() {
        assertTrue("Language is not supported", TranslatableRegion.LOCALE_NAMES_GETTERS.containsKey(language));
    }
}
