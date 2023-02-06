package ru.yandex.direct.i18n;

import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;

import org.junit.Test;

import ru.yandex.direct.i18n.bundle.TranslationBundle;
import ru.yandex.direct.i18n.dict.BundleDictionaries;
import ru.yandex.direct.i18n.dict.BundleDictionarySource;
import ru.yandex.direct.i18n.dict.DictionaryEntry;
import ru.yandex.direct.i18n.dict.DictionaryLoader;
import ru.yandex.direct.i18n.dict.DictionaryTranslator;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class ChainingTranslatorTest {
    private final ChainingTranslator enTranslator;
    private final ChainingTranslator ruTranslator;
    private final Translator fallbackTranslator;
    private final BundleOne translations;

    public ChainingTranslatorTest() throws IOException {
        Locale ruLocale = new Locale.Builder().setLanguageTag("ru").build();
        Locale enLocale = new Locale.Builder().setLanguageTag("en").build();

        BundleDictionaries<DictionaryEntry> dictionaries = DictionaryLoader.loadFromSources(Arrays.asList(
                getBundleDictionarySource(BundleOne.class, Language.EN),
                getBundleDictionarySource(BundleOne.class, Language.RU)
        ));

        translations = I18NBundle.implement(BundleOne.class);
        fallbackTranslator = I18NBundle.makeStubTranslatorFactory().getTranslator(ruLocale);
        ruTranslator = new ChainingTranslator(ruLocale,
                new DictionaryTranslator<>(dictionaries.getFor(ruLocale)),
                new DictionaryTranslator<>(dictionaries.getFor(enLocale)),
                fallbackTranslator
        );
        enTranslator = new ChainingTranslator(enLocale,
                new DictionaryTranslator<>(dictionaries.getFor(enLocale)),
                fallbackTranslator
        );
    }

    public static BundleDictionarySource getBundleDictionarySource(
            Class<? extends TranslationBundle> bundle, Language language) throws IOException {
        return new BundleDictionarySource(
                bundle.getCanonicalName(),
                language,
                ChainingTranslatorTest.class.getResource(
                        bundle.getSimpleName() + DictionaryLoader.BUNDLE_TOKENS_SEPARATOR + language.getLangString() + ".json"
                )
        );
    }

    @Test
    public void fallbackToLocaleTest() throws Exception {
        Translatable translatable = translations.campaignNotFound();
        assertThat(translatable.translate(ruTranslator), is(translatable.translate(enTranslator)));
    }

    @Test
    public void nativeTranslationTest() throws Exception {
        Translatable translatable = translations.campaignModerationFailed(1L);
        Translator nativeTranslator = enTranslator.getTranslationChain().get(0);
        assertThat(translatable.translate(enTranslator), is(translatable.translate(nativeTranslator)));
    }

    @Test
    public void fallbackToDefaultTranslatorTest() throws Exception {
        Translatable translatable = translations.payInEUR();
        assertThat(translatable.translate(ruTranslator), is(translatable.translate(fallbackTranslator)));
    }
}
