package ru.yandex.market.mbi.util;

import java.util.List;
import java.util.Set;

import org.hamcrest.MatcherAssert;
import org.skyscreamer.jsonassert.Customization;
import org.xmlunit.diff.DefaultNodeMatcher;
import org.xmlunit.diff.ElementSelectors;
import org.xmlunit.diff.NodeMatcher;

public final class MbiAsserts {

    public static final NodeMatcher IGNORE_ORDER =
            new DefaultNodeMatcher(ElementSelectors.byNameAndAllAttributes);

    private MbiAsserts() {
        throw new UnsupportedOperationException("Shouldn't be instantiated");
    }

    /**
     * Сравнить два JSON-объекта представленных в виде строк.
     * <p>
     * Оба аргумента разбираются и сравниваются на уровне JSON,
     * т. е. без учёта отсутпов, порядка свойств в объектах и т. п.
     *
     * @param expected ожидаемый результат
     * @param actual   тестируемый результат
     */
    public static void assertJsonEquals(String expected, String actual) {
        MatcherAssert.assertThat(actual, MbiMatchers.jsonEquals(expected));
    }

    /**
     * Сравнить два JSON'а в виде строк без учета отступов
     *
     * @param expected       ожидаемый результат
     * @param actual         тестируемый результат
     * @param customizations кастомизации, например для игнорирования атрибутов
     */
    public static void assertJsonEquals(String expected, String actual, List<Customization> customizations) {
        MatcherAssert.assertThat(actual, MbiMatchers.jsonEquals(expected, customizations));
    }

    /**
     * Сравнить два XML-документа представленных в виде строк.
     * <p>
     * Оба аргумента разбираются и сравниваются на уровне XML,
     * т. е. без учёта отступов, порядка атрибутов в элементах и т. п.
     *
     * @param expected ожидаемый результат
     * @param actual   тестируемый результат
     */
    public static void assertXmlEquals(String expected, String actual) {
        MatcherAssert.assertThat(actual, MbiMatchers.xmlEquals(expected));
    }

    /**
     * Сравнить два XML-документа представленных в виде строк.
     * <p>
     * Оба аргумента разбираются и сравниваются на уровне XML,
     * т. е. без учёта отступов, порядка атрибутов в элементах и т. п. Порядок элементов задаётся параметром
     * {@code nodeMatcher}.
     *
     * @param expected    ожидаемый результат
     * @param actual      тестируемый результат
     * @param nodeMatcher правило подбора соответствующего элемента, в случае неопределённого порядка дочерних элементов
     */
    public static void assertXmlEquals(String expected, String actual, NodeMatcher nodeMatcher) {
        MatcherAssert.assertThat(actual, MbiMatchers.xmlEquals(expected, nodeMatcher));
    }

    /**
     * Сравнить два XML-документа представленных в виде строк.
     * <p>
     * Оба аргумента разбираются и сравниваются на уровне XML,
     * т. е. без учёта отступов, порядка атрибутов в элементах и т. п.
     *
     * @param expected          ожидаемый результат
     * @param actual            тестируемый результат
     * @param ignoredAttributes перечень аттрибутов, которые следует игнорировать при сравнении сущностей
     */
    public static void assertXmlEquals(String expected,
                                       String actual,
                                       Set<String> ignoredAttributes
    ) {
        MatcherAssert.assertThat(actual, MbiMatchers.xmlEquals(expected, ignoredAttributes));
    }

}
