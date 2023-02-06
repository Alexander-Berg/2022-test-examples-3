package ru.yandex.market.mstat.planner.task;

import org.junit.Test;

import static ru.yandex.market.mstat.planner.util.RestUtil.timestamp;

public class EmployeesLoaderTest {

    @Test
    public void testTimeparse() {
        System.out.println(timestamp("2018-11-23"));
        System.out.println(timestamp("2015-05-25T00:00:00"));
        System.out.println(timestamp("2015-05-25T23:52:54"));
    }

}
