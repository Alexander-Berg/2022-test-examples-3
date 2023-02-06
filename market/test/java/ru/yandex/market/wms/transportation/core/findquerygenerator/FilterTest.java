package ru.yandex.market.wms.transportation.core.findquerygenerator;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.wms.transportation.core.domain.TransportOrderStatus;
import ru.yandex.market.wms.transportation.core.domain.TransportOrderType;

import static ru.yandex.market.wms.shared.libs.querygenerator.rsql.RsqlOperator.EQUALS;
import static ru.yandex.market.wms.shared.libs.querygenerator.rsql.RsqlOperator.NOT_EQUALS;
import static ru.yandex.market.wms.transportation.core.findquerygenerator.LogicalOperator.AND;
import static ru.yandex.market.wms.transportation.core.findquerygenerator.LogicalOperator.OR;

public class FilterTest {

    @Test
    public void test1() {
        Expression expr1 = Expression.of(Term.of(ApiField.DESTINATIONCELLKEY, EQUALS, "LOC01"));
        Expression expr2 = Expression.of(Term.of(ApiField.STATUS, EQUALS, TransportOrderStatus.NEW.name()))
                .add(OR, Term.of(ApiField.STATUS, EQUALS, TransportOrderStatus.ASSIGNED.name()))
                .add(OR, Term.of(ApiField.STATUS, EQUALS, TransportOrderStatus.IN_PROGRESS.name()));
        Filter filter = Filter.of(Expression.of(expr1).add(AND, expr2));

        Assertions.assertTrue(filter.getFilter().isPresent());
        Assertions.assertEquals("(toLoc==LOC01);(status==NEW,status==ASSIGNED,status==IN_PROGRESS)",
                filter.getFilter().get());
    }

    @Test
    public void test2() {
        Expression expr1 = Expression.of(Term.of(ApiField.DESTINATIONCELLKEY, EQUALS, "LOC01"));
        Expression expr2 = Expression.of(Term.of(ApiField.STATUS, EQUALS, TransportOrderStatus.NEW.name()))
                .add(OR, Term.of(ApiField.STATUS, EQUALS, TransportOrderStatus.ASSIGNED.name()))
                .add(AND, Term.of(ApiField.STATUS, EQUALS, TransportOrderStatus.IN_PROGRESS.name()));

        Filter filter = Filter.of(Expression.of(expr1).add(AND, expr2));

        Assertions.assertTrue(filter.getFilter().isPresent());
        Assertions.assertEquals("(toLoc==LOC01);(status==NEW,status==ASSIGNED;status==IN_PROGRESS)",
                filter.getFilter().get());
    }

    @Test
    public void test3() {
        Expression expr1 = Expression.of(Term.of(ApiField.TYPE, NOT_EQUALS, TransportOrderType.AUTOMATIC.name()))
                .add(OR, Term.of(ApiField.STATUS, NOT_EQUALS, TransportOrderStatus.ASSIGNED.name()))
                .add(OR, Term.of(ApiField.STATUS, EQUALS, TransportOrderStatus.IN_PROGRESS.name()));

        Expression expr2 = Expression.of(Term.of(ApiField.UNITKEY, EQUALS, "TM1"))
                .add(OR, Term.of(ApiField.UNITKEY, EQUALS, "TM2"));

        Filter filter = Filter.of(Expression.of(expr1).add(AND, expr2));

        Assertions.assertTrue(filter.getFilter().isPresent());
        Assertions.assertEquals("(type!=AUTOMATIC,status!=ASSIGNED,status==IN_PROGRESS);(unitKey==TM1,unitKey==TM2)",
                filter.getFilter().get());
    }

    @Test
    public void test4() {
        Filter filter = Filter.emptyFilter();
        Assertions.assertFalse(filter.getFilter().isPresent());
    }
}
