package ru.yandex.market.mbo.gwt.client.utils;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author commince
 * @date 07.08.2018
 */
public class ListManipulationUtilsTest {

    public static final long E1 = 1L;
    public static final long E2 = 2L;
    public static final long E3 = 3L;
    public static final long E4 = 4L;

    @Test
    public void testToStart() {
        List<Long> list = new ArrayList<>(Arrays.asList(E1, E2, E3, E4));
        ListManipulationUtils.moveToStart(list, E2);
        Assert.assertEquals(Arrays.asList(E2, E1, E3, E4), list);
    }

    @Test
    public void testToEnd() {
        List<Long> list = new ArrayList<>(Arrays.asList(E1, E2, E3, E4));
        ListManipulationUtils.moveToEnd(list, E2);
        Assert.assertEquals(Arrays.asList(E1, E3, E4, E2), list);
    }

    @Test
    public void testForward() {
        List<Long> list = new ArrayList<>(Arrays.asList(E1, E2, E3, E4));
        ListManipulationUtils.moveForward(list, E1);
        Assert.assertEquals(Arrays.asList(E2, E1, E3, E4), list);
    }

    @Test
    public void testBack() {
        List<Long> list = new ArrayList<>(Arrays.asList(E1, E2, E3, E4));
        ListManipulationUtils.moveBack(list, E2);
        Assert.assertEquals(Arrays.asList(E2, E1, E3, E4), list);
    }

}
