package ru.yandex.direct.i18n;

import java.util.Locale;

import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import ru.yandex.direct.i18n.bundle.CachedMethodInterpreter;

public class BrokenTranslationsTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private Translator translator;
    private BrokenTranslations t;

    public BrokenTranslationsTest() {
        this.t = I18NBundle.implement(BrokenTranslations.class, I18NBundle.makeSafeMethodInterpreter());
        this.translator = I18NBundle.makeStubTranslatorFactory()
                .getTranslator(new Locale.Builder().setLanguageTag("ru-RU").build());
    }

    @Test
    public void testExtraParameters() {
        thrown.expect(I18NException.class);
        thrown.expectMessage(
                "Unexpected parameters in: "
                        + "public abstract ru.yandex.direct.i18n.Translatable "
                        + "ru.yandex.direct.i18n.BrokenTranslations.extraParameter(java.lang.String). "
                        + "TranslationStub doesn't allow any method parameters. "
                        + "If you need parametrized translation, "
                        + "consider using MessageFormatStub or PluralStub."
        );
        t.extraParameter("oops");
    }

    /*
        @Test
        @Ignore("MessageFormat может интерпретировать int как дату")
        public void testWrongFormatModifier() {
            thrown.expect(I18NException.class);
            t.justX(1).translate(translator);
        }

        @Test
        @Ignore("MessageFormat не бросает исключение при отсутствующих аргументах")
        public void testNonexistentArgNumber() {
            thrown.expect(I18NException.class);
            t.missingArg(1).translate(translator);
        }
    */
    @Test
    public void testMissingPluralAnnotation() {
        thrown.expect(I18NException.class);
        thrown.expectMessage(
                "One of integral parameters should be annotated with @Plural: "
                        + "public abstract ru.yandex.direct.i18n.Translatable "
                        + "ru.yandex.direct.i18n.BrokenTranslations.missingPluralAnnotation(int)"

        );
        t.missingPluralAnnotation(1).translate(translator);
    }

    @Test
    public void testBadPluralType() {
        thrown.expect(I18NException.class);
        thrown.expectMessage(
                "@Plural parameter must be of integral type: "
                        + "public abstract ru.yandex.direct.i18n.Translatable "
                        + "ru.yandex.direct.i18n.BrokenTranslations.badPluralType(java.lang.String)"
        );
        t.badPluralType("1").translate(translator);
    }

    @Test
    public void testConflictingPlural() {
        thrown.expect(I18NException.class);
        thrown.expectMessage(
                "Conflicting @Plural handlers in: "
                        + "public abstract ru.yandex.direct.i18n.Translatable "
                        + "ru.yandex.direct.i18n.BrokenTranslations.conflictingPlural(long,long)"
        );
        t.conflictingPlural(1, 2).translate(translator);
    }

    @Test
    public void testUnexpectedParameterAnnotations() {
        thrown.expect(I18NException.class);
        thrown.expectMessage(
                "@Plural without @PluralStub on method:"
                        + " public abstract ru.yandex.direct.i18n.Translatable"
                        + " ru.yandex.direct.i18n.BrokenTranslations.unexpectedParameterAnnotations(long)"
        );
        t.unexpectedParameterAnnotations(1).translate(translator);
    }

    @Test
    public void testMethodWithoutAnnotation() {
        thrown.expect(I18NException.class);
        thrown.expectMessage(
                "Translation method without handled annotations: "
                        + "public abstract ru.yandex.direct.i18n.Translatable "
                        + "ru.yandex.direct.i18n.BrokenTranslations.methodWithoutAnnotation()"
        );
        t.methodWithoutAnnotation();
    }

    @Test
    public void testMethodWithMultipleAnnotation() {
        thrown.expect(I18NException.class);
        thrown.expectMessage(
                "Don't know how to handle multiple handlers for method public abstract "
                        + "ru.yandex.direct.i18n.Translatable "
                        + "ru.yandex.direct.i18n.BrokenTranslations.methodWithMultipleAnnotations():");
        // Не полагаемся на toString() у аннотаций, так как порядок, в котором печатаются атрибуты не гарантируется
        thrown.expectMessage("@ru.yandex.direct.i18n.bundle.TranslationStub");
        thrown.expectMessage("@ru.yandex.direct.i18n.bundle.MessageFormatStub");
        t.methodWithMultipleAnnotations();
    }

    @Test
    public void testAnnotationWithoutHandler() {
        thrown.expect(I18NException.class);
        thrown.expectMessage(
                Matchers.containsString("No handler for annotation: @org.junit.Ignore")
        );
        t.methodWithUnhandledAnnotation();
    }

    @Test
    public void testOverloadedMethods() {
        thrown.expect(I18NException.class);
        thrown.expectMessage(
                "Overloaded methods are not allowed: "
                        + "interface ru.yandex.direct.i18n.BrokenTranslationsOverload: "
                        + "overloadedMethod"
        );
        CachedMethodInterpreter.forBundle(
                BrokenTranslationsOverload.class,
                I18NBundle.makeSafeMethodInterpreter()
        );
    }
}
