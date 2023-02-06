package ru.yandex.market.jmf.logic.def.test;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.transaction.Transactional;

import com.google.common.collect.Lists;
import groovy.lang.MissingMethodException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.entity.HasGid;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.script.ScriptService;

@Transactional
@SpringJUnitConfig(classes = InternalLogicDefaultTestConfiguration.class)
public class DbScriptServiceApiQueryTest {
    @Inject
    private ScriptService scriptService;
    @Inject
    private BcpService bcpService;

    private List<Entity> testObjects;

    @BeforeEach
    public void setUp() {
        testObjects = IntStream.range(0, 20)
                .mapToObj(x -> bcpService.<Entity>create(Fqn.of("dbApiTest"), Map.of(
                        "naturalId", x,
                        "number", x
                )))
                .collect(Collectors.toList());

        bcpService.create(Fqn.of("dbApiTestWithoutNaturalId"), Map.of("number", Randoms.intValue()));
    }

    @Test
    public void testGetByGid() {
        scriptService.execute("assert api.db.of('dbApiTest').get('dbApiTest@1') == null");
    }

    @Test
    public void testGetByNaturalId() {
        scriptService.execute("assert api.db.of('dbApiTest').get('1') != null");
    }

    @Test
    public void testGetByNaturalIdExceptionLess() {
        scriptService.execute("assert api.db.of('dbApiTestWithoutNaturalId').get('123') == null");
    }

    @Test
    public void testListWithoutAnyRestrictions() {
        scriptService.execute("assert api.db.of('dbApiTest').list().gid == expected.gid", Map.of(
                "expected", testObjects
        ));
    }

    @Test
    public void testAnyWithoutFilters() {
        boolean result = scriptService.execute("api.db.of('dbApiTest').any()");
        Assertions.assertTrue(result);
    }

    @Test
    public void testCountWithoutAnyRestrictions() {
        scriptService.execute("assert api.db.of('dbApiTest').count() == expected.size()", Map.of(
                "expected", testObjects
        ));
    }

    @Test
    public void testListOrderedByAttribute() {
        var expectedNaturalId =
                Objects.requireNonNull(testObjects.stream().max(Comparator.comparing(x -> x.getAttribute("naturalId")))
                        .orElse(null));
        scriptService.execute(
                "assert api.db.of('dbApiTest').withOrders(api.db.orders.desc('naturalId')).list()[0].naturalId == " +
                        "expected.naturalId",
                Map.of("expected", expectedNaturalId)
        );
    }

    @Test
    public void testFilteredList() {
        scriptService.execute(
                "assert api.db.of('dbApiTest').withFilters(api.db.filters.lessThan('number', '15')).list()[-1].gid == " +
                        "expected.gid",
                Map.of("expected", testObjects.get(14))
        );
    }

    @Test
    public void testAnyWithFilters() {
        boolean result = scriptService.execute(
                "api.db.of('dbApiTest').withFilters(api.db.filters.lessThan('number', '15')).any()"
        );
        Assertions.assertTrue(result);
    }

    @Test
    public void testNegativeAnyWithFilters() {
        boolean result = scriptService.execute(
                "api.db.of('dbApiTest').withFilters(api.db.filters.greaterThan('number', 20)).any()"
        );
        Assertions.assertFalse(result);
    }

    @Test
    public void testFilteredCount() {
        scriptService.execute("assert api.db.of('dbApiTest')" +
                ".withFilters(api.db.filters.lessThan('number', '15')).count() == 15");
    }

    @Test
    public void testListWithOffsetAndLimit() {
        // Две проверки, потому что offset и limit можно использовать в любом порядке
        // В Groovy взятие свойства на списке приводит к взятию свойства на каждом объекте
        Stream.of("assert api.db.of('dbApiTest').offset(5).limit(5).list().gid == expected.gid",
                "assert api.db.of('dbApiTest').limit(5).offset(5).list().gid == expected.gid")
                .forEach(script -> scriptService.execute(script, Map.of("expected", testObjects.subList(5, 10))));
    }

    @Test
    public void testAnyWithOffsetAndLimit() {
        // Две проверки, потому что offset и limit можно использовать в любом порядке
        // В Groovy взятие свойства на списке приводит к взятию свойства на каждом объекте
        Stream.of("assert api.db.of('dbApiTest').offset(5).limit(5).any()",
                "assert api.db.of('dbApiTest').limit(5).offset(5).any()")
                .forEach(script -> scriptService.execute(script));
    }

    @Test
    public void testFilteredListWithOffsetAndLimit() {
        // Две проверки, потому что offset и limit можно использовать в любом порядке
        // В Groovy взятие свойства на списке приводит к взятию свойства на каждом объекте
        Stream.of("assert api.db.of('dbApiTest').withFilters(api.db.filters.lessThan('number', '15')).offset(11).limit(5)" +
                        ".list().gid == expected.gid",
                "assert api.db.of('dbApiTest').withFilters(api.db.filters.lessThan('number', '15')).limit(5).offset(11)" +
                        ".list().gid == expected.gid")
                .forEach(script -> scriptService.execute(script, Map.of("expected", testObjects.subList(11, 15))));
    }

    @Test
    public void testFilteredListOrderedByAttributeWithOffsetAndLimit() {
        // Две проверки, потому что offset и limit можно использовать в любом порядке
        // В Groovy взятие свойства на списке приводит к взятию свойства на каждом объекте
        Stream.of("assert api.db.of('dbApiTest').withFilters(api.db.filters.lessThan('number', '15')).withOrders(api.db" +
                        ".orders.desc('number')).offset(11).limit(5).list().gid == expected.gid",
                "assert api.db.of('dbApiTest').withFilters(api.db.filters.lessThan('number', '15')).withOrders(api.db" +
                        ".orders.desc('number')).limit(5).offset(11).list().gid == expected.gid")
                .forEach(script -> scriptService.execute(script, Map.of("expected",
                        Lists.reverse(testObjects.subList(0, 4)))));
    }

    @Test
    public void testListOrderedByAttributeWithOffsetAndLimit() {
        // Две проверки, потому что offset и limit можно использовать в любом порядке
        // В Groovy взятие свойства на списке приводит к взятию свойства на каждом объекте
        Stream.of("assert api.db.of('dbApiTest').withOrders(api.db.orders.desc('number')).offset(11).limit(5).list().gid " +
                        "== expected.gid",
                "assert api.db.of('dbApiTest').withOrders(api.db.orders.desc('number')).limit(5).offset(11).list().gid == " +
                        "expected.gid")
                .forEach(script -> scriptService.execute(script, Map.of("expected",
                        Lists.reverse(testObjects.subList(4, 9)))));
    }

    @Test
    public void testHqlQueryList() {
        List<String> result = scriptService.execute(
                "api.db.query('''FROM dbApiTest WHERE number > :number''').number(5 as Long).list().gid"
        );
        Assertions.assertEquals(testObjects.stream().skip(6).map(HasGid::getGid).collect(Collectors.toList()), result);
    }

    @Test
    public void testHqlQueryGet() {
        String result = scriptService.execute(
                "api.db.query('''FROM dbApiTest WHERE number > :number''').number(5 as Long).limit(1).get().gid"
        );
        Assertions.assertEquals(testObjects.get(6).getGid(), result);
    }

    @Test
    public void testHqlQueryAny() {
        Boolean result = scriptService.execute(
                "api.db.query('''FROM dbApiTest WHERE number > :number''').number(5 as Long).any()"
        );
        Assertions.assertTrue(result);
    }

    @Test
    public void testNegativeHqlQueryAny() {
        Boolean result = scriptService.execute(
                "api.db.query('''FROM dbApiTest WHERE number > :number''').number(20 as Long).any()"
        );
        Assertions.assertFalse(result);
    }

    @Test
    public void testCouldNotCountWithOrdering() {
        Assertions.assertThrows(MissingMethodException.class, () ->
                scriptService.execute("api.db.of('dbApiTest').withOrders(api.db.orders.desc('number')).count()"));
    }

    @Test
    public void testCouldNotCountWithOffset() {
        Assertions.assertThrows(MissingMethodException.class, () ->
                scriptService.execute("api.db.of('dbApiTest').offset(5).count()"));
    }

    @Test
    public void testCouldNotCountWithLimit() {
        Assertions.assertThrows(MissingMethodException.class, () ->
                scriptService.execute("api.db.of('dbApiTest').limit(5).count()"));
    }

    @Test
    public void testCouldNotAnyWithOrders() {
        Assertions.assertThrows(MissingMethodException.class, () ->
                scriptService.execute("api.db.of('dbApiTest').withOrders(api.db.orders.asc('attr')).any()"));
    }
}
