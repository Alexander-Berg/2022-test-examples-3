package ru.yandex.market.jmf.db.api.test.query;

import java.util.Arrays;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.jmf.entity.query.Filter;
import ru.yandex.market.jmf.entity.query.Filters;

public class FiltersTest {

    @Test
    public void eq__toString() {
        Filter filter = Filters.eq("attr", "value");
        String result = filter.toString();
        Assertions.assertEquals("attr = value", result);
    }

    @Test
    public void in__toString() {
        Filter filter = Filters.in("attr", Arrays.asList("value1", "value2"));
        String result = filter.toString();
        Assertions.assertEquals("attr IN [value1, value2]", result);
    }

    @Test
    public void in_empty__toString() {
        Filter filter = Filters.in("attr", Arrays.asList());
        String result = filter.toString();
        Assertions.assertEquals("attr IS NULL", result);
    }

    @Test
    public void and__toString() {
        Filter filter1 = Filters.eq("attr1", "value1");
        Filter filter2 = Filters.eq("attr2", "value2");
        Filter filter = Filters.and(filter1, filter2);
        String result = filter.toString();
        Assertions.assertEquals("(attr1 = value1 AND attr2 = value2)", result);
    }

    @Test
    public void or__toString() {
        Filter filter1 = Filters.eq("attr1", "value1");
        Filter filter2 = Filters.eq("attr2", "value2");
        Filter filter = Filters.or(filter1, filter2);
        String result = filter.toString();
        Assertions.assertEquals("(attr1 = value1 OR attr2 = value2)", result);
    }

    @Test
    public void ne__toString() {
        Filter filter = Filters.ne("attr", "value");
        String result = filter.toString();
        Assertions.assertEquals("attr != value", result);
    }

    @Test
    public void not__toString() {
        Filter filter1 = Filters.eq("attr1", "value1");
        Filter filter = Filters.not(filter1);
        String result = filter.toString();
        Assertions.assertEquals("NOT (attr1 = value1)", result);
    }

    @Test
    public void gt__toString() {
        Filter filter = Filters.greaterThan("attr", "value");
        String result = filter.toString();
        Assertions.assertEquals("attr > value", result);
    }

    @Test
    public void ge__toString() {
        Filter filter = Filters.greaterThanOrEqual("attr", "value");
        String result = filter.toString();
        Assertions.assertEquals("attr >= value", result);
    }

    @Test
    public void lt__toString() {
        Filter filter = Filters.lessThan("attr", "value");
        String result = filter.toString();
        Assertions.assertEquals("attr < value", result);
    }

    @Test
    public void le__toString() {
        Filter filter = Filters.lessThanOrEqual("attr", "value");
        String result = filter.toString();
        Assertions.assertEquals("attr <= value", result);
    }

    @Test
    public void between__toString() {
        Filter filter = Filters.between("attr", "lValue", "bValue");
        String result = filter.toString();
        Assertions.assertEquals("attr between lValue and bValue", result);
    }


}
