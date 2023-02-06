package ru.yandex.market.mstat.planner.util;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.mstat.planner.util.idm.IdmUtils;


public class IdmUtilsTest {

    @Test
    public void testRoleGeneration() {
        Assert.assertEquals("qwer/wedqw", IdmUtils.toRoleString("qwer", "wedqw"));
        Assert.assertEquals("", IdmUtils.toRoleString());
        Assert.assertEquals("qwer/wedqw/132", IdmUtils.toRoleString("qwer", "wedqw", "132"));
    }
}
