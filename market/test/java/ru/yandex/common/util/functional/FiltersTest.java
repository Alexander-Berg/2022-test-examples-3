package ru.yandex.common.util.functional;

import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Test;
import ru.yandex.common.util.collections.Cf;
import ru.yandex.common.util.collections.Cu;

import java.util.List;

/**
 * @author btv (btv@yandex-team.ru)
 */
public class FiltersTest extends TestCase {
    private static final Filter<Boolean> alwaysTrue = Filters.alwaysTrue();
    private static final Filter<Boolean> alwaysFalse = Filters.alwaysFalse();

    @Test
    public void testOr() throws Exception {
        Assert.assertTrue(Filters.or(alwaysTrue).fits(true));
        Assert.assertTrue(Filters.or(alwaysTrue, alwaysFalse).fits(true));
        Assert.assertTrue(Filters.or(alwaysFalse, alwaysTrue, alwaysTrue).fits(true));

        Assert.assertFalse(Filters.or(alwaysFalse, alwaysFalse, alwaysFalse).fits(true));
        Assert.assertFalse(Filters.or(alwaysFalse).fits(true));
    }

    @Test
    public void testIterableOr() throws Exception {
        Assert.assertTrue(Filters.or(Cf.list(alwaysTrue)).fits(true));
        Assert.assertTrue(Filters.or(Cf.list(alwaysTrue, alwaysFalse)).fits(true));
        Assert.assertTrue(Filters.or(Cf.list(alwaysFalse, alwaysTrue, alwaysTrue)).fits(true));

        Assert.assertFalse(Filters.or(Cf.list(alwaysFalse, alwaysFalse, alwaysFalse)).fits(true));
    }

    @Test
    public void testAnd() throws Exception {
        Assert.assertFalse(Filters.and(alwaysTrue, alwaysFalse).fits(true));
        Assert.assertFalse(Filters.and(alwaysFalse, alwaysTrue, alwaysTrue).fits(true));
        Assert.assertFalse(Filters.and(alwaysFalse, alwaysFalse, alwaysFalse).fits(true));
        Assert.assertFalse(Filters.and(alwaysFalse).fits(true));

        Assert.assertTrue(Filters.and(alwaysTrue).fits(true));
        Assert.assertTrue(Filters.and(alwaysTrue, alwaysTrue).fits(true));
        Assert.assertTrue(Filters.and(alwaysTrue, alwaysTrue, alwaysTrue, alwaysTrue).fits(true));
    }

    @Test
    public void testIterableAnd() throws Exception {
        Assert.assertFalse(Filters.and(Cf.list(alwaysTrue, alwaysFalse)).fits(true));
        Assert.assertFalse(Filters.and(Cf.list(alwaysFalse, alwaysTrue, alwaysTrue)).fits(true));
        Assert.assertFalse(Filters.and(Cf.list(alwaysFalse, alwaysFalse, alwaysFalse)).fits(true));
        Assert.assertFalse(Filters.and(Cf.list(alwaysFalse)).fits(true));

        Assert.assertTrue(Filters.and(Cf.list(alwaysTrue)).fits(true));
        Assert.assertTrue(Filters.and(Cf.list(alwaysTrue, alwaysTrue)).fits(true));
        Assert.assertTrue(Filters.and(Cf.list(alwaysTrue, alwaysTrue, alwaysTrue, alwaysTrue)).fits(true));
    }

    public void testMemberOf() throws Exception {
        final List<Object> l = Cf.<Object>list(1, 2);
        assertEquals(new Integer(2), Cu.first(Cu.filter(Cf.list(5, 2), Filters.memberOf(l))));
        assertEquals(l, l);
        assertEquals(1, Cf.list(Cu.filter(Cf.list(5, 2), Filters.memberOf(l))).size());
    }
}
