package ru.yandex.market.jmf.logic.def.test;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.transaction.Transactional;

import com.google.common.collect.Streams;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.jmf.entity.query.Filter;
import ru.yandex.market.jmf.script.ScriptService;

import static ru.yandex.market.jmf.entity.query.Filters.and;
import static ru.yandex.market.jmf.entity.query.Filters.between;
import static ru.yandex.market.jmf.entity.query.Filters.classOneOf;
import static ru.yandex.market.jmf.entity.query.Filters.contains;
import static ru.yandex.market.jmf.entity.query.Filters.containsAll;
import static ru.yandex.market.jmf.entity.query.Filters.containsAny;
import static ru.yandex.market.jmf.entity.query.Filters.eq;
import static ru.yandex.market.jmf.entity.query.Filters.eqIgnoreCase;
import static ru.yandex.market.jmf.entity.query.Filters.fullTextMatch;
import static ru.yandex.market.jmf.entity.query.Filters.greaterThan;
import static ru.yandex.market.jmf.entity.query.Filters.greaterThanOrEqual;
import static ru.yandex.market.jmf.entity.query.Filters.in;
import static ru.yandex.market.jmf.entity.query.Filters.lessThan;
import static ru.yandex.market.jmf.entity.query.Filters.lessThanOrEqual;
import static ru.yandex.market.jmf.entity.query.Filters.ne;
import static ru.yandex.market.jmf.entity.query.Filters.not;
import static ru.yandex.market.jmf.entity.query.Filters.or;
import static ru.yandex.market.jmf.entity.query.Filters.startsWith;

@Transactional
@SpringJUnitConfig(classes = InternalLogicDefaultTestConfiguration.class)
public class DbScriptServiceApiFiltersTest {
    @Inject
    private ScriptService scriptService;

    @Test
    public void testEqFilter() {
        Filter expected = eq("test", 123);
        Filter actual = scriptService.execute("api.db.of('dbApiTest').withFilters { eq('test', 123) }.filters[0]");
        Assertions.assertEquals(expected.toString(), actual.toString());
    }

    @Test
    public void testEqFilterCollection() {
        Filter expected = eq("test", List.of(1, 2, 3));
        Filter actual = scriptService.execute("api.db.of('dbApiTest').withFilters { eq('test', [1,2,3]) }.filters[0]");
        Assertions.assertEquals(expected.toString(), actual.toString());
    }

    @Test
    public void testInFilter() {
        Filter expected = in("test", List.of(1, 2, 3));
        Filter actual = scriptService.execute("api.db.of('dbApiTest').withFilters { _in('test', [1,2,3]) }.filters[0]");
        Assertions.assertEquals(expected.toString(), actual.toString());
    }

    @Test
    public void testNeFilter() {
        Filter expected = ne("test", 123);
        Filter actual = scriptService.execute("api.db.of('dbApiTest').withFilters { ne('test', 123) }.filters[0]");
        Assertions.assertEquals(expected.toString(), actual.toString());
    }

    @Test
    public void testNeFilterCollection() {
        Filter expected = ne("test", List.of(1, 2, 3));
        Filter actual = scriptService.execute("api.db.of('dbApiTest').withFilters { ne('test', [1,2,3]) }.filters[0]");
        Assertions.assertEquals(expected.toString(), actual.toString());
    }

    @Test
    public void testContainsAnyFilter() {
        Filter expected = containsAny("test", List.of(1, 2, 3));
        Filter actual = scriptService.execute("api.db.of('dbApiTest').withFilters { containsAny('test', [1,2,3]) }" +
                ".filters[0]");
        Assertions.assertEquals(expected.toString(), actual.toString());
    }

    @Test
    public void testContainsAllFilter() {
        Filter expected = containsAll("test", List.of(1, 2, 3));
        Filter actual = scriptService.execute("api.db.of('dbApiTest').withFilters { containsAll('test', [1,2,3]) }" +
                ".filters[0]");
        Assertions.assertEquals(expected.toString(), actual.toString());
    }

    @Test
    public void testContainsFilter() {
        Filter expected = contains("test", "testValue");
        Filter actual = scriptService.execute("api.db.of('dbApiTest').withFilters { contains('test', 'testValue') }" +
                ".filters[0]");
        Assertions.assertEquals(expected.toString(), actual.toString());
    }

    @Test
    public void testEqIgnoreCaseFilter() {
        Filter expected = eqIgnoreCase("test", "testValue");
        Filter actual = scriptService.execute("api.db.of('dbApiTest').withFilters { eqIgnoreCase('test', 'testValue') }" +
                ".filters[0]");
        Assertions.assertEquals(expected.toString(), actual.toString());
    }

    @Test
    public void testBetweenFilter() {
        Filter expected = between("test", 1, 5);
        Filter actual = scriptService.execute("api.db.of('dbApiTest').withFilters { between('test', 1, 5) }.filters[0]");
        Assertions.assertEquals(expected.toString(), actual.toString());
    }

    @Test
    public void testGreaterThanFilter() {
        Filter expected = greaterThan("test", 5);
        Filter actual = scriptService.execute("api.db.of('dbApiTest').withFilters { greaterThan('test', 5) }.filters[0]");
        Assertions.assertEquals(expected.toString(), actual.toString());
    }

    @Test
    public void testGreaterThanOrEqualFilter() {
        Filter expected = greaterThanOrEqual("test", 5);
        Filter actual = scriptService.execute("api.db.of('dbApiTest').withFilters { greaterThanOrEqual('test', 5) }" +
                ".filters[0]");
        Assertions.assertEquals(expected.toString(), actual.toString());
    }

    @Test
    public void testLessThanFilter() {
        Filter expected = lessThan("test", 5);
        Filter actual = scriptService.execute("api.db.of('dbApiTest').withFilters { lessThan('test', 5) }.filters[0]");
        Assertions.assertEquals(expected.toString(), actual.toString());
    }

    @Test
    public void testLessThanOrEqualFilter() {
        Filter expected = lessThanOrEqual("test", 5);
        Filter actual = scriptService.execute("api.db.of('dbApiTest').withFilters { lessThanOrEqual('test', 5) }" +
                ".filters[0]");
        Assertions.assertEquals(expected.toString(), actual.toString());
    }

    @Test
    public void testFullTextMatchFilter() {
        Filter expected = fullTextMatch("test");
        Filter actual = scriptService.execute("api.db.of('dbApiTest').withFilters { fullTextMatch('test') }.filters[0]");
        Assertions.assertEquals(expected.toString(), actual.toString());
    }

    @Test
    public void testClassOnOfFilter() {
        Filter expected = classOneOf("test", List.of("value1", "value2"));
        Filter actual = scriptService.execute(
                "api.db.of('dbApiTest').withFilters { classOneOf('test', ['value1', api.fqn('value2')]) }" +
                        ".filters[0]");
        Assertions.assertEquals(expected.toString(), actual.toString());
    }

    @Test
    public void testNotFilter() {
        Filter expected = not(eq("test", 123));
        Filter[] filters = scriptService.execute("api.db.of('dbApiTest').withFilters { not(eq('test', 123)) }.filters");
        Assertions.assertEquals(1, filters.length);
        Assertions.assertEquals(expected.toString(), filters[0].toString());
    }

    @Test
    public void testAndFilter() {
        Filter expected = and(eq("test", 123), ne("test2", 42));
        Filter[] filters = scriptService.execute("api.db.of('dbApiTest').withFilters { and { eq('test', 123); ne('test2', " +
                "42) } }.filters");
        Assertions.assertEquals(1, filters.length);
        Assertions.assertEquals(expected.toString(), filters[0].toString());
    }

    @Test
    public void testOrFilter() {
        Filter expected = or(eq("test", 123), ne("test2", 42));
        Filter[] filters = scriptService.execute("api.db.of('dbApiTest').withFilters { or { eq('test', 123); ne('test2', " +
                "42) } }.filters");
        Assertions.assertEquals(1, filters.length);
        Assertions.assertEquals(expected.toString(), filters[0].toString());
    }

    @Test
    public void testComplexFilter() {
        Filter[] expected = new Filter[]{
                or(
                        eq("test", 123),
                        ne("test2", 42)
                ),
                and(
                        not(startsWith("test3", "asd")),
                        not(
                                or(
                                        containsAll("test4", List.of(1, 2, 3, 4)),
                                        not(containsAny("test4", List.of(5, 6, 7)))
                                )
                        )
                ),
                in("test5", List.of(5, 2)),
                greaterThan("test6", 5)
        };
        Filter[] filters = scriptService.execute(String.join("\n",
                "api.db.of('dbApiTest').withFilters {",
                "  or {",
                "    eq('test', 123)",
                "    ne('test2', 42)",
                "  }",
                "  and {",
                "    not(startsWith('test3', 'asd'))",
                "    not(",
                "      or {",
                "        containsAll('test4', [1,2,3,4])",
                "        not(containsAny('test4', [5,6,7]))",
                "      }",
                "    )",
                "  }",
                "  _in('test5', [5,2])",
                "  greaterThan('test6', 5)",
                "}.filters"));
        Assertions.assertEquals(4, filters.length);
        Streams.zip(Arrays.stream(expected), Arrays.stream(filters), Pair::new)
                .forEach(p -> Assertions.assertEquals(p.first.toString(), p.second.toString()));
    }

    @Test
    public void testAttributes_WithFilters() {
        String actual = scriptService.execute("api.db.of('dbApiTest').withFilters { eq('test', 123) }.addAttributes" +
                "('number').attributes[0]");
        Assertions.assertEquals("number", actual);
    }

    @Test
    public void testAttributesDouble_WithFilters() {
        String[] actual = scriptService.execute("api.db.of('dbApiTest').withFilters { eq('test', 123) }.withAttributes" +
                "('number').addAttributes('naturalId').attributes");
        Assertions.assertEquals(2, actual.length);
        Assertions.assertEquals("number", actual[0]);
        Assertions.assertEquals("naturalId", actual[1]);
    }

    @Test
    public void testAttributesReplace_WithFilters() {
        String[] actual = scriptService.execute("api.db.of('dbApiTest').withFilters { eq('test', 123) }.withAttributes" +
                "('number').withAttributes('naturalId').attributes");
        Assertions.assertEquals(1, actual.length);
        Assertions.assertEquals("naturalId", actual[0]);
    }

    @Test
    public void testAttributesTwo_WithFilters() {
        String[] actual = scriptService.execute("api.db.of('dbApiTest').withFilters { eq('test', 123) }.withAttributes" +
                "('number', 'naturalId').attributes");
        Assertions.assertEquals(2, actual.length);
        Assertions.assertEquals("number", actual[0]);
        Assertions.assertEquals("naturalId", actual[1]);
    }
}
