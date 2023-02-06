package ru.yandex.direct.testing.i18n;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.assertj.core.api.SoftAssertions;
import org.junit.Ignore;
import org.junit.Test;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

import ru.yandex.direct.i18n.I18NBundle;
import ru.yandex.direct.i18n.NoTranslationFoundException;
import ru.yandex.direct.i18n.bundle.MessageFormatStub;
import ru.yandex.direct.i18n.bundle.TranslationBundle;
import ru.yandex.direct.i18n.bundle.TranslationStub;
import ru.yandex.direct.i18n.dict.BundleDictionaries;
import ru.yandex.direct.i18n.dict.Dictionary;
import ru.yandex.direct.i18n.dict.DictionaryEntry;
import ru.yandex.direct.i18n.dict.DictionaryLoader;
import ru.yandex.direct.i18n.dict.SingularEntry;
import ru.yandex.direct.utils.io.RuntimeIoException;

/**
 * Для запуска теста в нужном пакете нужно отнаследовать этот класс и определить метод, предоставляющий список
 * параметров (для этого использовать вспомогательный {@code prepareParametersForPackage(String packageName) }
 * Пример использования доопределенного класса с параметрами: {@code ru.yandex.direct.api.v5.ForeignTestSuite}
 * <p>
 * Ограничение: переводы проверяются для {@code SinglularEntry}. Множественная форма {@code PluralEntry} игнорируруется.
 */
@Ignore("Базовый класс для других тестов")
public class I18NBundleTest {
    private static final Logger logger = LoggerFactory.getLogger(I18NBundleTest.class);
    private final String packageName;

    public I18NBundleTest(String packageName) {
        this.packageName = packageName;
    }

    static class Param {
        String bundleName;
        String methodName;
        String dictionaryText;
        String annotationText;

        public Param(String bundleName, String methodName, String dictionaryText, String annotationText) {
            this.bundleName = bundleName;
            this.methodName = methodName;
            this.dictionaryText = dictionaryText;
            this.annotationText = annotationText;
        }
    }

    /**
     * Метод предоставляет коллекцию массивов параметров:
     * <ol>
     * <li>имя бандла</li>
     * <li>имя метода</li>
     * <li>перевод в словаре</li>
     * <li>перевод в аннотации</li>
     * </ol>
     * для последующего сравнения в тестах для заданного пакета.
     */
    private static List<Param> getParamsForPackage(String packageName) {
        Map<String, ? extends Dictionary<?>> ruDictDictionaries = getRuDictionaries();

        Reflections reflections = new Reflections(packageName);
        return reflections.getSubTypesOf(TranslationBundle.class)
                .stream().flatMap(c -> Arrays.stream(c.getDeclaredMethods()))
                .filter(m -> m.isAnnotationPresent(MessageFormatStub.class)
                        || m.isAnnotationPresent(TranslationStub.class))
                .map(
                        m -> {
                            String annotationText;
                            if (m.isAnnotationPresent(MessageFormatStub.class)) {
                                annotationText = m.getAnnotation(MessageFormatStub.class).value();
                            } else {
                                annotationText = m.getAnnotation(TranslationStub.class).value();
                            }

                            String bundleClassName = m.getDeclaringClass().getCanonicalName();
                            String bundleShortName = m.getDeclaringClass().getSimpleName();
                            String methodName = m.getName();

                            Dictionary<?> dictionary = ruDictDictionaries.get(bundleClassName);
                            if (dictionary == null) {
                                // bundle'а нет в словаре. Значит и расхождений нет
                                return null;
                            }
                            DictionaryEntry dictForMethod;
                            try {
                                dictForMethod = dictionary.get(methodName);
                            } catch (NoTranslationFoundException e) {
                                // не нашлось перевода для метода
                                return null;
                            }
                            if (dictForMethod instanceof SingularEntry) {
                                String dictionaryText = ((SingularEntry) dictForMethod).getForm();
                                return new Param(bundleShortName, methodName, dictionaryText, annotationText);
                            }
                            return null;
                        }
                )
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private static Map<String, ? extends Dictionary<?>> getRuDictionaries() {
        try {
            ResourcePatternResolver resourceResolver = new PathMatchingResourcePatternResolver();
            Collection<URL> urls = Arrays.stream(resourceResolver.getResources("classpath*:locale/ru/yandex/**/*.json"))
                    .map(I18NBundleTest::resourceUrl)
                    .collect(Collectors.toList());
            BundleDictionaries<?> dicts = new DictionaryLoader("/locale/").loadFromURLs(urls);
            return dicts.getFor(I18NBundle.RU).getDictionaries();
        } catch (IOException e) {
            throw new RuntimeIoException(e);
        }
    }

    //нужно для читабельности лямбд (кинуть IOException прямо в лямбде нельзя)
    private static URL resourceUrl(Resource rsrc) {
        try {
            return rsrc.getURL();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Test
    public void annotationTextMustBeEqualToDictionary() {
        var softly = new SoftAssertions();
        var params = getParamsForPackage(packageName);
        logger.error("Todo: {} params", params.size());
        for (Param param : params) {
            softly.assertThat(param.annotationText)
                    .describedAs("method %s.%s should have annotation translation equal to dictionary",
                            param.bundleName, param.methodName)
                    .isEqualTo(param.dictionaryText);
        }
        softly.assertAll();
    }
}
