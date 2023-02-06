package ru.yandex.direct.currency;

import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;

import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.i18n.I18NBundle;
import ru.yandex.direct.i18n.Translator;

@RunWith(Parameterized.class)
public class CurrencyTranslationHolderSymbolTest {

    private static final Translator TRANSLATOR = I18NBundle.makeStubTranslatorFactory().getTranslator(Locale.ENGLISH);

    @Parameterized.Parameter
    public CurrencyCode currencyCode;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<CurrencyCode> parameters() {
        return Arrays.asList(CurrencyCode.values());
    }

    @Test
    //проверяем, что для всех символов валют найдется перевод
    public void translate_everyCurrencyCodeSymbolIsTranslated() {
        var symbol = CurrencyTranslationHolder.ofCurrency(currencyCode).symbol();

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(symbol)
                    .as("Символ")
                    .isNotNull();
            soft.assertThat(symbol.translate(TRANSLATOR))
                    .as("Перевод символа")
                    .isNotNull()
                    .isNotBlank();
        });
    }
}
