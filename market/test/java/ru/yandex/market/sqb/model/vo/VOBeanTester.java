package ru.yandex.market.sqb.model.vo;

import java.util.stream.Stream;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.meanbean.test.BeanTester;

/**
 * Unit-тесты для POJO классов VO.
 *
 * @author Vladislav Bauer
 */
class VOBeanTester {

    @ParameterizedTest
    @MethodSource("data")
    void testPOJOContract(final Class<?> beanClass) {
        final BeanTester beanTester = new BeanTester();
        beanTester.testBean(beanClass);
    }

    @ParameterizedTest
    @MethodSource("data")
    void testEqualsAndHashCodeContract(final Class<?> beanClass) {
        EqualsVerifier.forClass(beanClass)
                .suppress(
                        Warning.STRICT_INHERITANCE,
                        Warning.NONFINAL_FIELDS,
                        Warning.ALL_FIELDS_SHOULD_BE_USED
                )
                .verify();
    }


    private static Stream<Arguments> data() {
        return Stream.of(
                Arguments.of(AliasVO.class),
                Arguments.of(ArgumentVO.class),
                Arguments.of(ParameterVO.class),
                Arguments.of(QueryVO.class),
                Arguments.of(TemplateVO.class),
                Arguments.of(IncludeVO.class)
        );
    }

}
