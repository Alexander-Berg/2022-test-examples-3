package ru.yandex.market.wms.timetracker.specification;

import java.util.List;
import java.util.Set;

import cz.jirutka.rsql.parser.RSQLParser;
import cz.jirutka.rsql.parser.ast.Node;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.wms.timetracker.specification.rsql.Filter;
import ru.yandex.market.wms.timetracker.specification.rsql.PredicateBuilder;
import ru.yandex.market.wms.timetracker.specification.rsql.RecursivePredicateBuilder;
import ru.yandex.market.wms.timetracker.specification.rsql.SearchOperators;
import ru.yandex.market.wms.timetracker.specification.rsql.Specification;

class PredicateBuilderTest {

    private final Specification<Object> specification = new Specification<>() {

        private static final Set<String> AVAILABLE_FIELDS = Set.of("userName");

        @Override
        public boolean isAvailableField(String field) {
            return AVAILABLE_FIELDS.contains(field);
        }

        @Override
        public boolean needConvertValue(String field) {
            return false;
        }

        @Override
        public String getSelector(String field) {
            return field;
        }

        @Override
        public String getTableName(String selector) {
            return "test";
        }

        @Override
        public List<String> convertValue(String selector, String operatorExpression, List<String> value) {
            return value;
        }
    };

    @Test
    void toPredicate() {

        Node node = new RSQLParser(SearchOperators.OPERATORS).parse("userName=in=(john,jack)");

        PredicateBuilder predicateBuilder = new RecursivePredicateBuilder();

        final String result = predicateBuilder.toPredicate(Filter.of(node, specification));

        final String expected = "(1=1\n AND test.userName IN ('john','jack'))";

        Assertions.assertEquals(expected, result);
    }

    @Test
    void toPredicateWhenOr() {

        Node node = new RSQLParser(SearchOperators.OPERATORS).parse("(userName==jack),(userName==john)");

        PredicateBuilder predicateBuilder = new RecursivePredicateBuilder();

        final String result = predicateBuilder.toPredicate(Filter.of(node, specification));

        final String expected = "(1=1\n AND test.userName = ('jack')) OR (1=1\n AND test.userName = ('john'))";

        Assertions.assertEquals(expected, result);

    }
}
