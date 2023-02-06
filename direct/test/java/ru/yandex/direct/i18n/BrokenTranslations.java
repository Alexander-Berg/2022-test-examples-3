package ru.yandex.direct.i18n;

import java.time.MonthDay;

import org.junit.Ignore;

import ru.yandex.direct.i18n.bundle.MessageFormatStub;
import ru.yandex.direct.i18n.bundle.Plural;
import ru.yandex.direct.i18n.bundle.PluralStub;
import ru.yandex.direct.i18n.bundle.TranslationBundle;
import ru.yandex.direct.i18n.bundle.TranslationStub;

interface BrokenTranslations extends TranslationBundle {
    @TranslationStub("ok")
    Translatable ok();

    @TranslationStub("Слишком много фраз")
    Translatable extraParameter(String unexpectedParameter);

    @MessageFormatStub("День ВДВ: {0}")
    Translatable vdv(MonthDay monthDay);

    @MessageFormatStub("{0, date}")
    Translatable justX(int x);

    @MessageFormatStub("{1}")
    Translatable missingArg(int x);

    @PluralStub(one = "1", some = "2", many = "3")
    Translatable missingPluralAnnotation(int numeral);

    @PluralStub(one = "1", some = "2", many = "3")
    Translatable badPluralType(@Plural String numeral);

    @PluralStub(one = "1", some = "2", many = "3")
    Translatable conflictingPlural(@Plural long numeral, @Plural long numeral2);

    @MessageFormatStub("stub")
    Translatable unexpectedParameterAnnotations(@Plural long numeral);

    Translatable methodWithoutAnnotation();

    @TranslationStub("foo")
    @MessageFormatStub("foo")
    Translatable methodWithMultipleAnnotations();

    @Ignore
    Translatable methodWithUnhandledAnnotation();
}
