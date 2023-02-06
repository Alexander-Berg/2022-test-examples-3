package ru.yandex.direct.i18n.localization;


import java.util.Locale;
import java.util.function.Function;
import java.util.function.Supplier;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.i18n.Language;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static ru.yandex.direct.i18n.I18NBundle.EN;
import static ru.yandex.direct.i18n.I18NBundle.RU;
import static ru.yandex.direct.i18n.I18NBundle.TR;

public class LocalizationMapperTest {


    private final String fromCopyCreator = "from copy creator";
    private final String fromBuilderSupplier = "from supplier creator";

    private final Function<Source, Result> copyCreator = (src) -> {
        var result = new Result();
        result.setSomeField(new SomeField(fromCopyCreator));
        return result;
    };

    private final Supplier<Result> builderSupplier = () -> {
        var result = new Result();
        result.setSomeField(new SomeField(fromBuilderSupplier));
        return result;
    };



    class Source {
        String helloRu = "Привет!";
        String helloEn = "Hello!";
        String helloUa = "Привiт!";
        String helloTr = "Selam!";

        Long var = 10L;
        SomeField someField = new SomeField("");


        SomeField getSomeField() {
            return someField;
        }

        Long getVar() {
            return var;
        }

        String getHelloRu() {
            return helloRu;
        }

        String getHelloEn() {
            return helloEn;
        }

        String getHelloUa() {
            return helloUa;
        }

        String getHelloTr() {
            return helloTr;
        }
    }

    class Result {
        String hello;
        Long num;

        SomeField someField;

        void setHello(String hello) {
            this.hello = hello;
        }

        String getHello() {
            return hello;
        }

        void setNum(Long num) {
            this.num = num;
        }

        void setSomeField(SomeField someField) {
            this.someField = someField;
        }

    }


    class SomeField {
        String someInfo;

        SomeField(String s) {
            someInfo = s;
        }
    }


    private Source sourceStub;

    @Before
    public void init() {
        sourceStub = new Source();
    }

    @Test
    public void translateToRus_Ok() {
        var mapper = getLocalizationMapper();
        checkOk("Ожидался русский перевод", mapper, RU, sourceStub.helloRu);
    }


    @Test
    public void translateNull_GotNull() {
        var mapper = getLocalizationMapper();
        sourceStub.helloRu = null;
        checkOk("Ожидался null", mapper, RU, null);
    }

    @Test
    public void translateFromUnsupported_DefaultLocaleUsage_Ok() {
        // Язык по умолчанию у маппера Language.EN
        var mapper = getLocalizationMapper();

        checkOk("Ожидался английский перевод", mapper, Locale.TRADITIONAL_CHINESE, sourceStub.helloEn);
    }


    /**
     * Проходим по списку TR -> EN -> RU, на котором и останоавливаемся
     */
    @Test
    public void translateFromTurkey_WalkingThroughFallBack_Ok() {
        var mapper = LocalizationMapper.builder()
                .addRuTranslation(Source::getHelloRu)
                .translateTo(Result::setHello)
                .createBy(Result::new)
                .build();

        checkOk("Ожидался русский перевод", mapper, TR, sourceStub.helloRu);
    }

    /**
     * Переводим с английского
     * FallBack: EN -> RU (из I18Bundle::makeTranslatorFactory)
     * Маппер только на украинский и турецкий
     */
    @Test
    public void translateFromEnglish_NoSuitableLanguage_Null() {
        var mapper = LocalizationMapper.builder()
                .addTrTranslation(Source::getHelloTr)
                .addUaTranslation(Source::getHelloUa)
                .translateTo(Result::setHello)
                .createBy(Result::new)
                .build();
        checkOk("Нет подходящего перевода, ожидался null", mapper, Locale.ENGLISH, null);
    }

    @Test
    public void translateFromEnglish_NoSuitableLanguage_UseDefaultValue() {
        var mapper = LocalizationMapper.builder()
                .addTrTranslation(Source::getHelloTr)
                .addUaTranslation(Source::getHelloUa)
                .translateTo(Result::setHello)
                .createBy(Result::new)
                .build();
        String defaultValue = "this is default string";
        String actual = mapper
                .localize(sourceStub, Locale.ENGLISH, defaultValue)
                .getHello();

        assertThat("Ожидалось значение по умолчанию", actual, is(defaultValue));
    }

    @Test
    public void translateFromEnglish_NullValue_NextInTranslationChain() {
        var mapper = getLocalizationMapper();

        sourceStub.helloEn = null;

        checkOk("Ожидался русский перевод", mapper, Locale.ENGLISH, sourceStub.helloRu);
    }

    @Test
    public void copyOtherFieldValue_Ok() {
        var mapper = LocalizationMapper
                .builder()
                .addEnTranslation(Source::getHelloEn)
                .addRuTranslation(Source::getHelloRu)
                .addUaTranslation(Source::getHelloUa)
                .addTrTranslation(Source::getHelloTr)
                .translateTo(Result::setHello)
                .copyBy((src) -> {
                    Result result = new Result();
                    result.setSomeField(src.getSomeField());
                    result.setNum(src.getVar());
                    return result;
                })
                .withDefaultLanguage(Language.RU)
                .build();

        Source src = new Source();
        src.someField.someInfo = "some text in some field";

        var res = mapper.localize(src, EN);
        assertThat("Ожидался английский перевод", res.hello, is(src.helloEn));
        assertEquals("Поле не было скопировано", res.num, src.var);
        assertEquals("Поле не было скопировано", res.someField, src.someField);
    }


    @Test
    public void creatorCall_CopyCreatorSet_Ok() {
        var mapperWithCopyCreator = createMapperWithCopyCreators(copyCreator);

        checkCreatorCall("Ожидался вызов функции копирования", mapperWithCopyCreator, fromCopyCreator);
    }


    @Test
    public void creatorCall_SupplierSet_Ok() {
        var mapperWithSupplier = createMapperWithSupplier(builderSupplier);

        checkCreatorCall("Ожидался вызов функции создания, установленой в builder", mapperWithSupplier,
                fromBuilderSupplier);
    }

    private void checkCreatorCall(String reason,
                                  LocalizationMapper<Source, Result> mapper,
                                  String expected) {
        assertThat(reason, mapper.localize(sourceStub, EN).someField.someInfo, is(expected));
    }

    private LocalizationMapper<Source, Result> getLocalizationMapper() {
        return LocalizationMapper.builder()
                .addEnTranslation(Source::getHelloEn)
                .addTrTranslation(Source::getHelloTr)
                .addUaTranslation(Source::getHelloUa)
                .addRuTranslation(Source::getHelloRu)
                .translateTo(Result::setHello)
                .createBy(Result::new)
                .withDefaultLanguage(Language.EN)
                .build();
    }


    private void checkOk(String reason, LocalizationMapper<Source, Result> mapper, Locale locale, String expected) {
        assertThat(reason, mapper.localize(sourceStub, locale).getHello(), is(expected));
    }


    private LocalizationMapper<Source, Result> createMapperWithCopyCreators(Function<Source, Result> creator) {
        return commonBuilder().copyBy(creator).build();
    }

    private LocalizationMapper<Source, Result> createMapperWithSupplier(Supplier<Result> supplier) {
        return commonBuilder().createBy(supplier).build();
    }

    private LocalizationMapperBuilder.BuilderWithoutCreator<Source, Result> commonBuilder() {
        return LocalizationMapper.builder()
                .addEnTranslation(Source::getHelloEn)
                .addTrTranslation(Source::getHelloTr)
                .addUaTranslation(Source::getHelloUa)
                .addRuTranslation(Source::getHelloRu)
                .translateTo(Result::setHello);
    }
}
