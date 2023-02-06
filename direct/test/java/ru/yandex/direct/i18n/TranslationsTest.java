package ru.yandex.direct.i18n;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Locale;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.direct.i18n.types.FullLocalDate;

public class TranslationsTest {
    private Translator translator;
    private Translations t;

    public TranslationsTest() {
        this.t = I18NBundle.implement(Translations.class);
        this.translator = I18NBundle.makeStubTranslatorFactory()
                .getTranslator(new Locale.Builder().setLanguageTag("ru-RU").build());
    }

    @Test
    public void testSimple() {
        Assert.assertEquals(
                "Кампания не найдена",
                t.campaignNotFound().translate(translator)
        );
        Assert.assertEquals(
                "ООО 'Рога и копыта'",
                t.hornsAndHooves().translate(translator)
        );
    }

    @Test
    public void testTemplate() throws ParseException {
        Assert.assertEquals(
                "Привет, Аркадий",
                t.hello("Аркадий").translate(translator)
        );
        Assert.assertEquals(
                "Первый релиз .NET Core: 27 июня 2016 г.",
                t.dotNetCoreFirstRelease(new SimpleDateFormat("yyyy-MM-dd").parse("2016-06-27"))
                        .translate(translator)
        );
        Assert.assertEquals(
                "Первый релиз .NET Core: 27 июня 2016 г.",
                t.dotNetCoreFirstReleaseLocalDate(new FullLocalDate(LocalDate.of(2016, 6, 27)))
                        .translate(translator)
        );
    }

    @Test
    public void testPlural() {
        Assert.assertEquals(
                "Не прошли модерацию -2 кампании",
                t.campaignModerationFailed(-2L).translate(translator)
        );
        Assert.assertEquals(
                "Не прошли модерацию 123 456 кампаний",
                t.campaignModerationFailed(123456L).translate(translator)
        );
    }
}
