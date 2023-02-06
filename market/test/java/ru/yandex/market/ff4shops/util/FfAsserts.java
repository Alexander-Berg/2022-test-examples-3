package ru.yandex.market.ff4shops.util;

import java.util.List;
import java.util.Set;

import org.hamcrest.MatcherAssert;
import org.skyscreamer.jsonassert.comparator.CustomComparator;
import org.springframework.http.ResponseEntity;
import org.xmlunit.diff.NodeMatcher;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author fbokovikov
 */
public class FfAsserts {

    /**
     * Сравнить два XML-документа представленных в виде строк.
     *
     * Оба аргумента разбираются и сравниваются на уровне XML,
     * т. е. без учёта отступов, порядка атрибутов в элементах и т. п.
     *
     * @param expected ожидаемый результат
     * @param actual тестируемый результат
     */
    public static void assertXmlEquals(String expected, String actual) {
        MatcherAssert.assertThat(actual, new XmlEqualsMatcher(expected));
    }

    /**
     * Сравнить два XML-документа представленных в виде строк.
     *
     * Оба аргумента разбираются и сравниваются на уровне XML,
     * т. е. без учёта отступов, порядка атрибутов в элементах и т. п. Порядок элементов задаётся параметром
     * {@code nodeMatcher}.
     *
     * @param expected ожидаемый результат
     * @param actual тестируемый результат
     * @param nodeMatcher правило подбора соответствующего элемента, в случае неопределённого порядка дочерних элементов
     */
    public static void assertXmlEquals(String expected, String actual, NodeMatcher nodeMatcher) {
        MatcherAssert.assertThat(actual, new XmlEqualsMatcher(expected, nodeMatcher));
    }

    /**
     * Сравнить два XML-документа представленных в виде строк.
     *
     * Оба аргумента разбираются и сравниваются на уровне XML,
     * т. е. без учёта отступов, порядка атрибутов в элементах и т. п.
     *
     * @param expected ожидаемый результат
     * @param actual тестируемый результат
     * @param ignoredAttributes набор атрибутов, которые следует игнорировать при сравнении сущностей
     */
    public static void assertXmlEquals(String expected,
                                       String actual,
                                       Set<String> ignoredAttributes
    ) {
        MatcherAssert.assertThat(actual, new XmlEqualsMatcher(expected, ignoredAttributes));
    }

    /**
     * Сравнить два JSON'а в виде строк без учета отступов
     * @param expected ожидаемый результат
     * @param actual тестируемый результат
     */
    public static void assertJsonEquals(String expected, String actual) {
        MatcherAssert.assertThat(actual, new JsonEqualsMatcher(expected));
    }

    /**
     * Сравнить два JSON'а в виде строк без учета отступов
     * @param expected ожидаемый результат
     * @param actual тестируемый результат
     * @param ignoredAttributes набор игнорируемых атрибутов
     */
    public static void assertJsonEquals(String expected, String actual, Set<String> ignoredAttributes) {
        MatcherAssert.assertThat(actual, new JsonEqualsMatcher(expected, ignoredAttributes));
    }

    /**
     * Сравнить два JSON'а в виде строк без учета отступов
     * @param expected ожидаемый результат
     * @param actual тестируемый результат
     * @param customComparator кастомный компаратор для сравнения JSON'ов
     */
    public static void assertJsonEquals(String expected, String actual, CustomComparator customComparator) {
        MatcherAssert.assertThat(actual, new JsonEqualsMatcher(expected, customComparator));
    }

    public static void assertHeader(ResponseEntity entity, String header, Object value) {
        List<String> values = entity.getHeaders().get(header);
        assertEquals(1, values.size());
        assertEquals(String.valueOf(value), values.get(0));
    }
}
